/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.server.pql.convert;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.filterKeys;
import static com.google.common.collect.Maps.transformEntries;
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Sets.newTreeSet;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.dcc.portal.pql.es.visitor.special.EntitySetVisitor.IDENTIFIABLE_VALUE_PREFIX;
import static org.dcc.portal.pql.meta.IndexModel.getTypeModel;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.meta.Type.PROJECT;
import static org.dcc.portal.pql.meta.TypeModel.GENE_SET_ID;
import static org.dcc.portal.pql.util.Converters.stringValue;
import static org.icgc.dcc.common.core.util.Separators.EMPTY_STRING;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.portal.server.pql.convert.model.Operation.ALL;
import static org.icgc.dcc.portal.server.pql.convert.model.Operation.HAS;
import static org.icgc.dcc.portal.server.pql.convert.model.Operation.IS;
import static org.icgc.dcc.portal.server.pql.convert.model.Operation.NOT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.util.Pair;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.meta.TypeModel;
import org.icgc.dcc.common.core.util.Joiners;
import org.icgc.dcc.portal.server.pql.convert.model.JqlArrayValue;
import org.icgc.dcc.portal.server.pql.convert.model.JqlField;
import org.icgc.dcc.portal.server.pql.convert.model.JqlFilters;
import org.icgc.dcc.portal.server.pql.convert.model.JqlValue;
import org.icgc.dcc.portal.server.pql.convert.model.Operation;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FiltersConverter {

  private static final String GENE_PATH = "gene";
  private static final String MISSING_VALUE = "_missing";
  private static final String EMPTY_NESTED_PATH = "";
  private static final String PQL_OR_TEMPLATE = "or(%s)";
  private static final String NOT_TEMPLATE = "not(%s)";
  private static final String IN_TEMPLATE = "in(%s,%s)";
  private static final String EQ_TEMPLATE = "eq(%s,%s)";
  private static final String NE_TEMPLATE = "ne(%s,%s)";
  private static final String EXISTS_TEMPLATE = "exists(%s)";
  private static final String MISSING_TEMPLATE = "missing(%s)";
  private static final String NESTED_TEMPLATE = "nested(%s,%s)";
  public static final String ENTITY_SET_PREFIX = IDENTIFIABLE_VALUE_PREFIX;
  public static final String ENTITY_SET_ID = "entitySetId";

  private static final Ordering<String> NATURAL_ORDER = Ordering.<String> natural();
  private static final Joiner COMMA_JOINER = Joiners.COMMA.skipNulls();

  private static final List<String> VALID_PROJECT_FILTERS = ImmutableList.of(
      "id",
      "primarySite",
      "primaryCountries",
      "availableDataTypes",
      "state",
      "tumourType");

  private static final List<String> SPECIAL_FIELDS_NESTING = ImmutableList.of(
      "gene.goTermId",
      "gene.hasPathway",
      "gene.hasCompound",
      GENE_SET_ID,
      ENTITY_SET_ID,
      "mutation.location",
      "gene.location");

  private static final List<String> NESTED_FIELDS = ImmutableList.of(
      "consequenceType",
      "functionalImpact",
      "platform",
      "sequencingStrategy",
      "verificationStatus");

  /**
   * @see GeneFacetFilters
   */
  private static final Collection<String> GENE_FACET_FIELDS = ImmutableList.of(
      "hasPathway",
      "hasCompound",
      "pathwayId",
      "compoundId");

  @NonNull
  public String convertFilters(JqlFilters filters, Type indexType) {
    if (indexType == PROJECT) {
      filters = cleanProjectFilters(filters);
    }

    // These fields are required to create the correct nesting order. E.g. nested(gene, nested(gene.ssm ...))
    val fieldsGroupedByNestedPath = groupFields(filters, indexType, f -> f.getOperation() != NOT);
    val notFieldsGroupedByNestedPath = groupFields(filters, indexType, f -> f.getOperation() == NOT);

    val groupedFilters = getGroupedFilters(fieldsGroupedByNestedPath, indexType, false);
    val notFilters = getGroupedFilters(notFieldsGroupedByNestedPath, indexType, true);

    return Stream.concat(groupedFilters.values().stream(), notFilters.values().stream()).collect(joining(","));
  }

  private ListMultimap<String, JqlField> groupFields(JqlFilters filters, Type indexType,
      Predicate<JqlField> streamFilter) {
    val groupedByNestedPath = ArrayListMultimap.<String, JqlField> create();

    for (val entry : filters.getEntityValues().entrySet()) {
      val values = entry.getValue().stream()
          .filter(streamFilter)
          .map(this::removeNot)
          .collect(toList());
      if (!values.isEmpty()) {
        val fieldsByNestedPath = groupFieldsByNestedPath(entry.getKey(), values, indexType);
        groupedByNestedPath.putAll(fieldsByNestedPath);
      }
    }

    return groupedByNestedPath;
  }

  /**
   * We transform NOT operations to IS operations as we are moving the NOT higher up in the AST.
   * 
   * @param field JqlField
   * @return JqlField where the operation is IS.
   */
  private JqlField removeNot(JqlField field) {
    if (field.getOperation() == NOT) {
      return new JqlField(field.getName(), IS, field.getValue(), field.getPrefix());
    } else {
      return field;
    }
  }

  private Map<String, String> getGroupedFilters(ListMultimap<String, JqlField> fieldsGroupedByNestedPath,
      Type indexType, boolean isNotFilter) {
    if (fieldsGroupedByNestedPath.isEmpty()) {
      return emptyMap();
    }

    log.debug("Fields by nested path: {}", fieldsGroupedByNestedPath);
    val sortedDescPaths = newTreeSet(fieldsGroupedByNestedPath.keySet()).descendingSet();
    log.debug("Sorted: {}", sortedDescPaths);
    val groupedPaths = groupNestedPaths(sortedDescPaths, getTypeModel(indexType));
    log.debug("Groupped paths: {}", groupedPaths);

    val groupedFilters = transformEntries(groupedPaths.asMap(), (key, values) -> {
      final String filter = createFilterByNestedPath(indexType, fieldsGroupedByNestedPath,
          newArrayList(newTreeSet(values).descendingSet()), isNotFilter);

      String retFilter = isEncloseWithCommonParent(values) ? encloseWithCommonParent(key, filter) : filter;
      return isNotFilter ? format(NOT_TEMPLATE, retFilter) : retFilter;
    });

    return groupedFilters;
  }

  private static JqlFilters cleanProjectFilters(JqlFilters filters) {
    val indexType = PROJECT.getId();
    val entityValues = transformValues(filterKeys(filters.getEntityValues(), key -> key.equals(indexType)),
        value -> filterValidProjectFilters(value));

    return new JqlFilters(entityValues);
  }

  private static List<JqlField> filterValidProjectFilters(List<JqlField> source) {
    return source.stream().filter(f -> VALID_PROJECT_FILTERS.contains(f.getName())).collect(toImmutableList());
  }

  private String encloseWithCommonParent(String commonPath, String filter) {
    return format(NESTED_TEMPLATE, commonPath, filter);
  }

  static boolean isEncloseWithCommonParent(Collection<String> nestedPaths) {
    return nestedPaths.size() > 1 && !hasCommonParent(nestedPaths);
  }

  /**
   * Checks if {@code nestedPaths} already contains a nested path which is common for all.
   */
  private static boolean hasCommonParent(Collection<String> nestedPaths) {
    val firstPath = NATURAL_ORDER.min(nestedPaths);
    return nestedPaths.stream()
        .allMatch(path -> path.startsWith(firstPath));
  }

  /**
   * Groups nested paths by most common parent, so they can be nested at the parent's level.
   */
  static ListMultimap<String, String> groupNestedPaths(Collection<String> nestedPaths, TypeModel typeModel) {
    val result = ArrayListMultimap.<String, String> create();
    val sortedPaths = newArrayList(newTreeSet(nestedPaths));
    val size = sortedPaths.size();

    if (size == 1) {
      val path = sortedPaths.get(0);
      result.put(path, path);
      return result;
    }

    String parentPath = EMPTY_NESTED_PATH;
    for (int i = 0; i < size; i++) {
      val currentPath = sortedPaths.get(i);

      if (currentPath.equals(EMPTY_NESTED_PATH)) {
        result.put(parentPath, currentPath);
        continue;
      }

      val resolvedPath = typeModel.getParentNestedPath(currentPath);

      // DonorCentric: [gene.ssm, gene.ssm.observation]
      if (parentPath.equals(EMPTY_NESTED_PATH)
          && doesNextPathStartWith(sortedPaths, currentPath, i, size)) {
        parentPath = currentPath;

        // DonorCentric: [gene.ssm.consequence, gene.ssm.observation]
      } else if (currentPath.startsWith(resolvedPath) && parentPath.equals(EMPTY_NESTED_PATH)
          && doesNextPathStartWith(sortedPaths, resolvedPath, i, size)) {
        parentPath = resolvedPath;

        // Change from one common path to another
      } else if (currentPath.startsWith(resolvedPath) && !currentPath.startsWith(parentPath)) {
        parentPath = currentPath;

        // MutationCentric: [ssm_occurrence, transcript]
      } else if (parentPath.equals(EMPTY_NESTED_PATH)) {
        parentPath = currentPath;
      }

      result.put(parentPath, currentPath);
    }

    return result;
  }

  private static boolean hasNextPath(int i, int size) {
    return i < (size - 1);
  }

  @NonNull
  private static boolean doesNextPathStartWith(List<String> paths, String prefix, int i, int size) {
    val hasNext = hasNextPath(i, size);

    if (hasNext) {
      return paths.get(i + 1).startsWith(prefix);
    }

    return false;
  }

  /**
   * Creates a filter for all fields with are nested under common nested path. E.g. gene - gene.ssm -
   * gene.ssm.observation - gene.ssm.consequence
   *
   * @param sortedDescPaths - descending sorted paths. E.g. gene.ssm - gene
   */
  static String createFilterByNestedPath(Type indexType, ListMultimap<String, JqlField> sortedFields,
      List<String> sortedDescPaths, boolean isNotFilter) {
    val size = sortedDescPaths.size();

    if (size < 1) {
      return "";
    }

    val firstPath = head(sortedDescPaths);
    val firstValue = resolveFirstNestedPath(indexType, firstPath, sortedFields);

    if (size == 1) {
      return firstValue;
    }

    val initialValue = createReduceValuePair(firstValue, firstPath);

    val result = prepareForReduce(tail(sortedDescPaths))
        .reduce(initialValue, (accumulated, value) -> {
          final String nestedPath = unboxReduceValue(value);
          final String newReducedValue =
              resolveRestNestedPath(indexType, accumulated, nestedPath, sortedFields, isNotFilter);

          return createReduceValuePair(newReducedValue, nestedPath);
        });

    return unboxReduceValue(result);
  }

  private static String resolveFirstNestedPath(Type indexType, String nestedPath,
      ListMultimap<String, JqlField> sortedFields) {
    val typeFilter = createTypeFilter(sortedFields.get(nestedPath), indexType);

    return isNestFilter(nestedPath, indexType) ? format(NESTED_TEMPLATE, resolveNestedPath(nestedPath, indexType),
        typeFilter) : typeFilter;
  }

  private static String resolveRestNestedPath(Type indexType, Pair<String, String> reduceValuePair, String nestedPath,
      ListMultimap<String, JqlField> sortedFields, boolean isNotFilter) {
    val filter = createTypeFilter(sortedFields.get(nestedPath), indexType);
    val reducedValue = unboxReduceValue(reduceValuePair);
    val previousSiblingPath = reduceValuePair.getSecond();

    return isNestFilter(nestedPath,
        indexType) ? (isChildNesting(nestedPath, previousSiblingPath) ? format(
            isNotFilter ? "nested(%s,or(%s,%s))" : "nested(%s,and(%s,%s))",
            resolveNestedPath(nestedPath, indexType), reducedValue,
            filter) : format("nested(%s,%s),%s", nestedPath, filter, reducedValue)) : format("%s,%s", filter,
                reducedValue);
  }

  private static <T> T head(@NonNull List<T> list) {
    return list.isEmpty() ? null : list.get(0);
  }

  private static <T> Stream<T> tail(@NonNull List<T> list) {
    val size = list.size();

    return (size < 2) ? Stream.empty() : IntStream.range(1, size).boxed()
        .map(i -> list.get(i));
  }

  /*
   * This prepares for a reduce operation. Due to that our accumulator is a pair of (accumulatedValue,
   * previousSiblingPath), we have to wrap elements in our collection to the same type as the accumulator.
   */
  private static Stream<Pair<String, String>> prepareForReduce(@NonNull Stream<String> stream) {
    return stream.map(value -> createReduceValuePair(value, value));
  }

  private static Pair<String, String> createReduceValuePair(String reducedValue, String previousSiblingPath) {
    return new Pair<String, String>(reducedValue, previousSiblingPath);
  }

  private static String unboxReduceValue(@NonNull Pair<String, String> reduceValuePair) {
    return reduceValuePair.getFirst();
  }

  /**
   * gene, gene.ssm - true<br>
   * transcript, gene - false
   */
  private static boolean isChildNesting(String parentPath, String childPath) {
    return childPath.startsWith(parentPath);
  }

  private static String resolveNestedPath(String nestedPath, Type indexType) {
    if (nestedPath.equals(GENE_PATH)) {
      switch (indexType) {
      case DONOR_CENTRIC:
        return "gene";
      case MUTATION_CENTRIC:
        return "transcript";
      default:
        break;
      }
    }

    return nestedPath;
  }

  /**
   * Defines if filter should be nested because it's filtering on a nested field
   */
  private static boolean isNestFilter(String nestedPath, Type indexType) {
    return !(nestedPath.equals(EMPTY_NESTED_PATH));
  }

  @NonNull
  private static String toPqlFilter(Collection<JqlField> jqlFields, Type indexType) {
    if (jqlFields.isEmpty()) {
      return null;
    }

    return COMMA_JOINER.join(transform(jqlFields,
        field -> createFilter(field, indexType)));
  }

  private static String createTypeFilter(Collection<JqlField> fields, Type indexType) {
    val geneFacetFilters = new GeneFacetFilters();
    val remainingFields = Lists.<JqlField> newArrayList();

    // Separates fields into different categories.
    for (val jqlField : fields) {
      val fieldName = jqlField.getName();
      if (isGeneFacetField(fieldName)) {
        val fieldFamily = getGeneFacetFieldFamily(fieldName);
        val hasField = jqlField.getOperation() == Operation.HAS;
        if (hasField) {
          geneFacetFilters.addHasField(fieldFamily, jqlField);
        } else {
          geneFacetFilters.addIdField(fieldFamily, jqlField);
        }
      } else {
        remainingFields.add(jqlField);
      }
    }

    // Special handling when pathwayId and hasPathway or (compoundId and hasCompound) are both present; if not, process
    // normally
    val typeFilter = new ArrayList<String>();
    val fieldsByFamily = geneFacetFilters.getFieldsByFamily();
    for (val familyFields : fieldsByFamily) {
      if (familyFields.size() > 1) {
        val familyFilter = orFilterHelper(toPqlFilter(familyFields, indexType));
        typeFilter.add(familyFilter);
      } else {
        remainingFields.addAll(familyFields);
      }
    }

    return joinFilters(remainingFields, typeFilter.isEmpty() ? null : COMMA_JOINER.join(typeFilter), indexType);
  }

  private static boolean isGeneFacetField(String fieldName) {
    return GENE_FACET_FIELDS.stream()
        .anyMatch(geneFacetField -> geneFacetField.equals(fieldName));
  }

  private static String getGeneFacetFieldFamily(String fieldName) {
    return fieldName
        .replaceAll("^has", EMPTY_STRING)
        .replaceAll("Id$", EMPTY_STRING)
        .toLowerCase();
  }

  private static String orFilterHelper(String filter) {
    return (null == filter) ? null : format(PQL_OR_TEMPLATE, filter);
  }

  private static String joinFilters(ArrayList<JqlField> remaining, String pathways, Type type) {
    return COMMA_JOINER.join(
        toPqlFilter(remaining, type),
        pathways);
  }

  private static boolean isNestedField(@NonNull JqlField field) {
    return NESTED_FIELDS.contains(field.getName());
  }

  /**
   * Groups fields by the most common nested path.<br>
   * E.g. gene, gene.ssm, gene.ssm.consequence, gene.ssm.observation are in one bucket with key 'gene'
   */
  static ListMultimap<String, JqlField> groupFieldsByNestedPath(String typePrefix,
      List<JqlField> fields, Type indexType) {
    val result = ArrayListMultimap.<String, JqlField> create();

    if (isTypeMatch(typePrefix, indexType)) {
      result.putAll(EMPTY_NESTED_PATH, fields);
      return result;
    }

    // Because of the particularity of MutationCentric type some of its fields are re-mapped to nested fields
    // The fields must be correctly separated before the next steps.
    val isMutation = (indexType == MUTATION_CENTRIC) && typePrefix.equals("mutation");
    if (isMutation) {
      final Predicate<JqlField> nestedPred = (f) -> isNestedField(f);
      final Predicate<JqlField> notNestedPred = (f) -> !isNestedField(f);

      val nonNestedFields = fields.stream().filter(notNestedPred).collect(toList());
      result.putAll(EMPTY_NESTED_PATH, nonNestedFields);

      // Resets the 'fields' variable for further processing (down below).
      fields = fields.stream().filter(nestedPred).collect(toList());
    }

    val typeModel = getTypeModel(indexType);
    for (val field : fields) {
      result.put(resolveNestedPath(field, typeModel), field);
    }

    return result;
  }

  private static String resolveNestedPath(JqlField field, TypeModel indexModel) {
    val indexType = indexModel.getType();
    val fieldName = parseFieldName(field, indexType);

    if (SPECIAL_FIELDS_NESTING.contains(fieldName) || SPECIAL_FIELDS_NESTING.contains(field.getName())) {
      return resolveSpecialCasesNestedPath(fieldName, indexType);
    }

    if (indexModel.isNested(fieldName)) {
      return indexModel.getNestedPath(fieldName);
    }

    return EMPTY_NESTED_PATH;
  }

  private static String resolveSpecialCasesNestedPath(String fieldName, Type indexType) {
    switch (indexType) {
    case DONOR_CENTRIC:
      if (fieldName.startsWith("gene")) {
        return "gene";
      }

      if (fieldName.equals("donor.entitySetId")) {
        return EMPTY_NESTED_PATH;
      }

      return "gene.ssm";

    case GENE_CENTRIC:
      if (fieldName.startsWith("gene")) {
        return EMPTY_NESTED_PATH;
      }

      if (fieldName.equals("donor.entitySetId")) {
        return "donor";
      }

      return "donor.ssm";

    case MUTATION_CENTRIC:
      if (fieldName.startsWith("gene")) {
        return "transcript";
      }

      if (fieldName.equals("donor.entitySetId")) {
        return "ssm_occurrence";
      }

      return EMPTY_NESTED_PATH;

    case OBSERVATION_CENTRIC:
      if (fieldName.startsWith("gene")) {
        return "ssm.gene";
      }

      return "ssm";
    default:
      break;
    }

    throw new IllegalArgumentException(format("Could not resolve nested path for field %s in type %s", fieldName,
        indexType.getId()));
  }

  private static boolean isTypeMatch(String typePrefix, Type indexType) {
    // Does not apply to MUTATION_CENTRIC, because its filters must be separated on nested and non-nested
    // See comment @ line 186 groupFieldsByNestedPath()
    if (indexType == MUTATION_CENTRIC) {
      return false;
    }

    return indexType.getId().startsWith(typePrefix);
  }

  private static String createFilter(JqlField jqlField, Type indexType) {
    val operation = jqlField.getOperation();

    if (operation == HAS) {
      val typeModel = getTypeModel(indexType);
      val fieldName = typeModel.getField(jqlField.getName());

      return resolveMissingFilter(fieldName, jqlField);
    }

    if (operation == ALL) {
      return resolveAllFilter(jqlField, indexType);
    }

    val fieldValue = jqlField.getValue();

    if (fieldValue.contains(MISSING_VALUE)) {
      return createMissingFilter(jqlField, indexType);
    }

    val filterType = createFilterByValueType(jqlField, indexType);

    if (operation == NOT && fieldValue.isArray()) {
      return format(NOT_TEMPLATE, filterType);
    }

    return filterType;
  }

  private static String resolveAllFilter(JqlField jqlField, Type indexType) {
    val values = createAllFilters(jqlField, indexType);

    return format("and(%s)", COMMA_JOINER.join(values));
  }

  private static List<String> createAllFilters(JqlField jqlField, Type indexType) {
    val fieldName = parseFieldName(jqlField, indexType);
    val values = (JqlArrayValue) jqlField.getValue();

    return Lists.transform(values.get(),
        rawValue -> format(IN_TEMPLATE, fieldName, stringValue(rawValue)));
  }

  private static boolean isTrue(JqlValue boolValue) {
    if (null == boolValue) {
      return false;
    }

    return Boolean.TRUE.equals(boolValue.get());
  }

  private static String resolveMissingFilter(String fieldName, JqlField jqlField) {
    val formatTemplate = isTrue(jqlField.getValue()) ? EXISTS_TEMPLATE : MISSING_TEMPLATE;

    return format(formatTemplate, fieldName);
  }

  private static String createMissingFilter(JqlField jqlField, Type indexType) {
    val fieldName = parseFieldName(jqlField, indexType);
    val fieldValue = jqlField.getValue();

    if (fieldValue.isArray()) {
      val arrayFilter = createArrayFilterForMissingField(jqlField, indexType);
      val missingFilter = format(MISSING_TEMPLATE, fieldName);
      val orFilter = arrayFilter.isPresent() ? format("or(%s,%s)", missingFilter, arrayFilter.get()) : missingFilter;

      if (jqlField.getOperation() == NOT) {
        return format(NOT_TEMPLATE, orFilter);
      } else {
        return orFilter;
      }
    }

    val formatTemplate = isTrue(fieldValue) ? MISSING_TEMPLATE : EXISTS_TEMPLATE;
    return format(formatTemplate, fieldName);
  }

  private static Optional<String> createArrayFilterForMissingField(JqlField jqlField, Type indexType) {
    val values = newArrayList(((JqlArrayValue) jqlField.getValue()).get());
    values.remove(MISSING_VALUE);

    if (values.isEmpty()) {
      return Optional.empty();
    }

    val newJqlValue = new JqlArrayValue(values);
    val newJqlField = new JqlField(jqlField.getName(), jqlField.getOperation(), newJqlValue, jqlField.getPrefix());

    return Optional.of(createInFilter(newJqlField, indexType));
  }

  private static String createFilterByValueType(JqlField jqlField, Type indexType) {
    return jqlField.getValue().isArray() ? createInFilter(jqlField, indexType) : createEqFilter(jqlField, indexType);
  }

  private static String createEqFilter(JqlField jqlField, Type indexType) {
    val fieldValue = jqlField.getValue().get();
    val formatTemplate = (IS == jqlField.getOperation()) ? EQ_TEMPLATE : NE_TEMPLATE;

    return format(formatTemplate, parseFieldName(jqlField, indexType), stringValue(fieldValue));
  }

  private static String createInFilter(JqlField jqlField, Type indexType) {
    val arrayField = (JqlArrayValue) jqlField.getValue();

    return format(IN_TEMPLATE, parseFieldName(jqlField, indexType), arrayField.textValue());
  }

  private static String parseFieldName(JqlField jqlField, Type indexType) {
    val fieldName = jqlField.getName();

    if (isNestedField(jqlField) && indexType == MUTATION_CENTRIC) {// && jqlField.getPrefix().equals("mutation")) {
      return fieldName;
    }

    return format("%s.%s", jqlField.getPrefix(), fieldName);
  }

}
