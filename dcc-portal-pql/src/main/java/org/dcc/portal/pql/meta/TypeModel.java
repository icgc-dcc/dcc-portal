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
package org.dcc.portal.pql.meta;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.dcc.portal.pql.meta.field.FieldModel.FIELD_SEPARATOR;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.exception.SemanticException;
import org.dcc.portal.pql.meta.field.FieldModel;
import org.dcc.portal.pql.meta.visitor.CreateAliasVisitor;
import org.dcc.portal.pql.meta.visitor.CreateFullyQualifiedNameVisitor;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

@Slf4j
public abstract class TypeModel {

  /**
   * Following public constants used to resolve the special cases in the API.
   */
  public static final String GENE_GO_TERM = "gene.GoTerm";
  public static final String MOLECULAR_FUNCTION = "go_term.molecular_function";
  public static final String BIOLOGICAL_PROCESS = "go_term.biological_process";
  public static final String CELLULAR_COMPONENT = "go_term.cellular_component";

  public static final String HAS_PATHWAY = "hasPathway";
  public static final String HAS_GO_TERM = "hasGoTerm";
  public static final String HAS_CURATED_SET = "hasCuratedSet";
  public static final String HAS_COMPOUND = "hasCompound";

  public static final String GENE_PATHWAY_ID = "gene.pathwayId";
  public static final String GENE_SET_ID = "gene.geneSetId";
  public static final String GENE_GO_TERM_ID = "gene.goTermId";
  public static final String GENE_CURATED_SET_ID = "gene.curatedSetId";
  public static final String GENE_DRUG_ID = "gene.compoundId";

  public static final String GENE_LOCATION = "gene.location";
  public static final String MUTATION_LOCATION = "mutation.location";

  public static final String SCORE = "_score";

  public static final String LOOKUP_PATH = "lookup.path";
  public static final String LOOKUP_INDEX = "lookup.index";
  public static final String LOOKUP_TYPE = "lookup.type";

  public static final String DONOR_LOOKUP = "donor-ids";
  public static final String GENE_LOOKUP = "gene-ids";
  public static final String MUTATION_LOOKUP = "mutation-ids";
  public static final String FILE_LOOKUP = "file-ids";

  /**
   * Contains fields that help to resolve 'the special cases in the API'. This fields should not be split when alias to
   * fully qualified field is resolved.
   */
  public static final List<String> SPECIAL_CASES_FIELDS = ImmutableList.of(
      GENE_GO_TERM,
      GENE_PATHWAY_ID,
      GENE_SET_ID,
      GENE_GO_TERM_ID,
      GENE_DRUG_ID,
      HAS_PATHWAY,
      HAS_GO_TERM,
      HAS_CURATED_SET,
      HAS_COMPOUND,
      GENE_CURATED_SET_ID,
      GENE_LOCATION,
      MUTATION_LOCATION);

  protected final Map<String, FieldModel> fieldsByFullPath;
  protected final Map<String, String> fieldsByAlias;
  protected final Map<String, String> fieldsByInternalAlias;
  protected final List<String> allowedFields;
  protected final List<String> aliases;

  /**
   * Represents fields added as includes to the Query.<br>
   * <br>
   * <b>NB:</b> The list contains fully qualified names, not aliases. Because after the AST is built by
   * PqlParseTreeVisitor includes are resolved to the real fields
   */
  protected final List<String> includeFields;

  private final Map<String, String> hasFieldsMapping = ImmutableMap.of(
      HAS_PATHWAY, GENE_PATHWAY_ID,
      HAS_CURATED_SET, GENE_CURATED_SET_ID,
      HAS_GO_TERM, GENE_GO_TERM_ID,
      HAS_COMPOUND, GENE_DRUG_ID);

  private static final Splitter FIELD_SEPARATOR_SPLITTER = Splitter.on(FIELD_SEPARATOR);

  public TypeModel(@NonNull List<? extends FieldModel> fields, @NonNull Map<String, String> internalAliases,
      @NonNull List<String> allowedAliases, @NonNull List<String> includeFields) {
    fieldsByFullPath = initFieldsByFullPath(fields);
    log.debug("FieldsByFullPath Map: {}", fieldsByFullPath);

    fieldsByAlias = initFieldsByAlias(fields);
    log.debug("FieldsByAlias Map: {}", fieldsByAlias);

    this.fieldsByInternalAlias = defineInternalAliases(internalAliases);
    this.allowedFields = defineAllowedFields(allowedAliases);
    this.includeFields = includeFields;
    this.aliases = allowedAliases;
  }

  /**
   * @return {@link Type} of this type model.
   */
  public abstract Type getType();

  /**
   * @return fields which are objects and must be retrieved from the {@code _source}. They are located in the includes
   * of the {@code Query} object in the portal-api.
   */
  public final List<String> getIncludeFields() {
    return includeFields;
  }

  /**
   * Returns a list of available facets.
   */
  public abstract List<String> getFacets();

  /**
   * Returns a list of available fields for select(*)
   * @return fully-qualified fields
   */
  public final List<String> getFields() {
    return allowedFields;
  }

  /**
   * @return field aliases
   */
  public final List<String> getAliases() {
    return aliases;
  }

  /**
   * Returns a prefix of the TypeModel. E.g. 'donor.id' has prefix 'donor'.
   */
  public abstract String prefix();

  /**
   * Checks if {@code field} is nested field.
   * 
   * @param field - fully qualified name. Not field alias.
   */
  public final boolean isNested(@NonNull String field) {
    val fullyQualifiedName = getFullName(field);
    val nestedPaths = split(fullyQualifiedName);
    log.debug("Nested Paths: {}", nestedPaths);

    for (val path : nestedPaths) {
      log.debug("Processing path: {}", path);
      val pathByFullPath = fieldsByFullPath.get(path);

      if (pathByFullPath == null) {
        throw new SemanticException(format("Search field %s is not available under %s", path, prefix()));
      }

      if (pathByFullPath.isNested()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks if {@code field} is nested under the {@code path}. The {@code field} may nested couple time. Returns
   * <b>true</b> if one of the parent is nested under the {@code path}.
   */
  public final boolean isNested(@NonNull String field, @NonNull String path) {
    if (!isNested(field)) {
      return false;
    }

    val nestedPath = getNestedPath(field);

    return nestedPath.startsWith(path);
  }

  public final boolean isAliasDefined(String alias) {
    return (null == alias) ? false : fieldsByAlias.containsKey(alias);
  }

  /**
   * Returns fully qualified name of the field that has {@code alias} defined.
   * @throws NoSuchElementException if there is a field with such an alias.
   */
  public final String getField(@NonNull String field) {
    if (hasFieldsMapping.keySet().contains(field)) {
      return hasFieldsMapping.get(field);
    }

    val alias = fieldsByAlias.get(field);
    if (alias == null) {
      throw new SemanticException("Field %s is not defined in the type model", field);
    }

    return alias;
  }

  public final FieldModel getFieldModelByAlias(@NonNull String alias) {
    val result = fieldsByFullPath.get(getField(alias));

    if (null == result) {
      throw new SemanticException("Field %s does not have a matching field model.", alias);
    }

    return result;
  }

  /**
   * Returns fully qualified name by an internal alias.
   * @throws NoSuchElementException if there is a field with such an alias.
   */
  public final String getInternalField(@NonNull String internalAlias) {
    val result = fieldsByInternalAlias.get(internalAlias);
    if (result == null) {
      throw new SemanticException("Field %s is not defined in the type model", internalAlias);
    }

    return result;
  }

  /**
   * Get field alias by {@code field}.
   * @param field is a fully qualified field
   * @return alias of the {@code field}
   * @throws IllegalArgumentException is the alias was not found
   */
  public final Set<String> getAliasByField(@NonNull String field) {
    val aliases = fieldsByAlias.entrySet().stream()
        .filter(entry -> entry.getValue().equals(field))
        .map(entry -> entry.getKey())
        .collect(toImmutableSet());
    checkArgument(aliases.size() > 0, "Failed to resolve alias from field '%s'", field);

    return aliases;
  }

  @Override
  public String toString() {
    val builder = new StringBuilder();
    val newLine = System.getProperty("line.separator");
    for (val entity : fieldsByFullPath.entrySet()) {
      val value = entity.getValue();
      builder.append(format("Path: %s, Type: %s, Nested: %s", entity.getKey(), value.getType(), value.isNested()));
      builder.append(newLine);
    }

    return builder.toString();
  }

  private List<String> split(String fullyQualifiedName) {
    val result = ImmutableList.<String> builder();
    val list = FIELD_SEPARATOR_SPLITTER.splitToList(fullyQualifiedName);
    val prefix = new StringBuilder();

    for (int i = 0; i < list.size(); i++) {
      result.add(prefix.toString() + list.get(i));
      prefix.append(list.get(i) + FIELD_SEPARATOR);
    }

    return result.build().reverse();
  }

  /**
   * @param field - field alias
   * @return path under which the {@code field} is nested
   */
  public final String getNestedPath(@NonNull String field) {
    val fullyQualifiedName = getFullName(field);

    for (val path : split(fullyQualifiedName)) {
      checkState(fieldsByFullPath.containsKey(path),
          "fieldsByFullPath does not contain this key: '%s'.", path);

      val pathByFullPath = fieldsByFullPath.get(path);

      if (pathByFullPath.isNested()) {
        return path;
      }
    }

    throw new IllegalArgumentException("Can't get nested path for a non-nested field");
  }

  /**
   * @return closest parent's path if one exists. Otherwise, returns {@code path}.
   */
  public final String getParentNestedPath(@NonNull String path) {
    checkState(!path.isEmpty(), "Empty nested path %s", path);
    for (val token : split(path)) {
      if (token.equals(path)) {
        continue;
      }

      val tokenByFullPath = fieldsByFullPath.get(token);
      if (tokenByFullPath.isNested()) {
        return token;
      }
    }

    return path;
  }

  /**
   * @param path - nested path which may be nested itself
   * @return all parent nested paths + {@code path}
   */
  public final List<String> getNestedPaths(@NonNull String path) {
    checkState(!path.isEmpty(), "Empty nested path %s", path);
    val result = ImmutableList.<String> builder();
    for (val token : split(path)) {
      val tokenByFullPath = fieldsByFullPath.get(token);
      if (tokenByFullPath.isNested()) {
        result.add(token);
      }
    }

    return result.build().reverse();
  }

  public final boolean isIdentifiable(@NonNull String field) {
    val fieldModel = fieldsByFullPath.get(field);
    checkNotNull(fieldModel, "Failed to resolve field model form '%s'", field);

    return fieldModel.isIdentifiable();
  }

  private String getFullName(String path) {
    val uiAlias = fieldsByAlias.get(path);

    return (uiAlias == null) ? path : uiAlias;
  }

  /**
   * Defines common aliases and adds type specific ones.
   */
  private Map<String, String> defineInternalAliases(Map<String, String> internalAliases) {
    return new ImmutableMap.Builder<String, String>()
        .put(LOOKUP_INDEX, "terms-lookup")
        .put(LOOKUP_PATH, "values")
        .putAll(internalAliases)
        .build();
  }

  private static Map<String, FieldModel> initFieldsByFullPath(List<? extends FieldModel> fields) {
    val result = Maps.<String, FieldModel> newHashMap();
    val visitor = new CreateFullyQualifiedNameVisitor();
    for (val field : fields) {
      result.putAll(field.accept(visitor));
    }

    return result;
  }

  private static Map<String, String> initFieldsByAlias(List<? extends FieldModel> fields) {
    val result = new ImmutableMap.Builder<String, String>();
    val visitor = new CreateAliasVisitor();
    for (val field : fields) {
      result.putAll(field.accept(visitor));
    }

    return result.build();
  }

  private List<String> defineAllowedFields(List<String> allowedAliases) {
    val result = new ImmutableList.Builder<String>();
    for (val alias : allowedAliases) {
      result.add(fieldsByAlias.get(alias));
    }

    return result.build();
  }

}
