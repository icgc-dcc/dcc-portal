package org.icgc.dcc.portal.resource;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.icgc.dcc.common.core.util.Joiners.COMMA;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.validation.Validation;

import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.Query.QueryBuilder;
import org.icgc.dcc.portal.service.BadRequestException;
import org.icgc.dcc.portal.util.JsonUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.yammer.dropwizard.jersey.params.IntParam;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Resources {

  public static final Set<String> ORDER_VALUES = ImmutableSet.of("asc", "desc");

  public static final String DEFAULT_FILTERS = "{}";
  public static final String DEFAULT_SIZE = "10";
  public static final String DEFAULT_FROM = "1";
  public static final String DEFAULT_ORDER = "desc";
  public static final String DEFAULT_PROJECT_SORT = "totalLiveDonorCount";
  public static final String DEFAULT_DONOR_SORT = "ssmAffectedGenes";
  public static final String DEFAULT_GENE_MUTATION_SORT = "affectedDonorCountFiltered";
  public static final String DEFAULT_OCCURRENCE_SORT = "donorId";

  public static final String COUNT_TEMPLATE = "Request for a count of {} with filters '{}'";
  public static final String FIND_ALL_TEMPLATE =
      "Request for '{}' {} from index '{}', sorted by '{}' in '{}' order with filters '{}'";
  public static final String FIND_ONE_TEMPLATE = "Request for '{}'";
  public static final String NESTED_FIND_TEMPLATE = "Request {} for '{}'";
  public static final String NESTED_COUNT_TEMPLATE = "Request {} count for '{}'";
  public static final String NESTED_NESTED_COUNT_TEMPLATE = "Request count of '{}' in '{}' affected by '{}'";

  public static final String RETURNS_LIST = "Returns a list of ";
  public static final String RETURNS_COUNT = "Returns a count of ";
  public static final String GROUPED_BY = " grouped by ";
  public static final String DONOR = "Donor";
  public static final String MUTATION = "Mutation";
  public static final String TOTAL = "Total";
  public static final String GENE = "Gene";
  public static final String PROJECT = "Project";
  public static final String OCCURRENCE = "Occurrence";
  public static final String S = "(s)";
  public static final String AFFECTED_BY_THE = " affected by the ";
  public static final String FOR_THE = " for the ";
  public static final String FIND_BY_ID_ERROR =
      "Returns information of a mutation by ID. If the mutation ID is not found, this returns a 404 error.";
  public static final String FIND_BY_ID = "Find by Identifiable";
  public static final String NOT_FOUND = " not found";
  public static final String MULTIPLE_IDS = ". Multiple IDs can be separated by a comma";

  public static final String API_DONOR_VALUE = "Donor ID";
  public static final String API_DONOR_PARAM = "donorId";
  public static final String API_MUTATION_VALUE = "Mutation ID";
  public static final String API_MUTATION_PARAM = "mutationId";
  public static final String API_GENE_VALUE = "Gene ID";
  public static final String API_GENE_PARAM = "geneId";
  public static final String API_GENE_SET_PARAM = "geneSetId";
  public static final String API_GENE_SET_VALUE = "Gene Set ID";
  public static final String API_PROJECT_VALUE = "Project ID";
  public static final String API_PROJECT_PARAM = "projectId";
  public static final String API_OCCURRENCE_VALUE = "Occurrence ID";
  public static final String API_OCCURRENCE_PARAM = "occurrenceId";
  public static final String API_ORDER_ALLOW = "asc,desc";
  public static final String API_ORDER_PARAM = "order";
  public static final String API_ORDER_VALUE = "Order to sort the column";
  public static final String API_SORT_FIELD = "sort";
  public static final String API_SORT_VALUE = "Column to sort results on";
  public static final String API_SIZE_ALLOW = "range[1,100]";
  public static final String API_SIZE_PARAM = "size";
  public static final String API_SIZE_VALUE = "Number of results returned";
  public static final String API_FROM_PARAM = "from";
  public static final String API_FROM_VALUE = "Start index of results";
  public static final String API_INCLUDE_PARAM = "include";
  public static final String API_INCLUDE_VALUE = "Include addition data in the response";
  public static final String API_FIELD_PARAM = "field";
  public static final String API_FIELD_VALUE = "Select fields returned";
  public static final String API_FILTER_PARAM = "filters";
  public static final String API_FILTER_VALUE = "Filter the search results";
  public static final String API_SCORE_FILTERS_PARAM = "scoreFilters";
  public static final String API_SCORE_FILTER_VALUE = "Used to filter scoring differently from results";
  public static final String API_ANALYSIS_VALUE = "Analysis";
  public static final String API_ANALYSIS_PARAM = "analysis";
  public static final String API_ANALYSIS_ID_VALUE = "Analysis ID";
  public static final String API_ANALYSIS_ID_PARAM = "analysisId";
  public static final String API_PARAMS_VALUE = "EnrichmentParams";
  public static final String API_PARAMS_PARAM = "params";
  public static final String API_FILE_IDS_PARAM = "fileIds";
  public static final String API_FILE_IDS_VALUE = "Limits the file manifest archive to this list of file IDs";
  public static final String API_FILE_REPOS_PARAM = "repositories";
  public static final String API_FILE_REPOS_VALUE =
      "Limits the file manifest archive to this list of file repositories";
  public static final String API_FILE_REPO_CODE_PARAM = "repoCode";
  public static final String API_FILE_REPO_CODE_VALUE = "File Repository Code";
  public static final String API_FACETS_ONLY_PARAM = "facetsOnly";
  public static final String API_FACETS_ONLY_DESCRIPTION = "Retrieves facet results only";

  public static final String API_ENTITY_LIST_ID_VALUE = "Entity Set ID";
  public static final String API_ENTITY_LIST_ID_PARAM = "entitySetId";

  public static final String API_ENTITY_LIST_DEFINITION_VALUE = "Entity Set Definition";
  public static final String API_ENTITY_LIST_DEFINITION_PARAM = "entityListDefinition";
  public static final String API_SET_ANALYSIS_DEFINITION_VALUE = "Set Analysis Definition";

  public static final String API_ASYNC = "Asyncronous API Request";

  private static final Joiner COMMA_JOINER = COMMA.skipNulls();
  private static final List<String> EMPTY_VALUES = newArrayList("", null);

  public static LinkedHashMap<String, Query> generateQueries(ObjectNode filters, String filterTemplate,
      List<String> ids) {
    val queries = Maps.<String, Query> newLinkedHashMap();

    for (String id : ids) {
      val filter = mergeFilters(filters, filterTemplate, id);
      queries.put(id, query().filters(filter).build());
    }
    return queries;
  }

  public static LinkedHashMap<String, Query> generateQueries(ObjectNode filters, String filterTemplate,
      List<String> ids,
      String anchorId) {
    val queries = Maps.<String, Query> newLinkedHashMap();

    for (String id : ids) {
      val filter = mergeFilters(filters, filterTemplate, id, anchorId);
      queries.put(id, query().filters(filter).build());
    }
    return queries;
  }

  public static LinkedHashMap<String, LinkedHashMap<String, Query>> generateQueries(ObjectNode filters,
      String filterTemplate,
      List<String> ids,
      List<String> anchorIds) {
    val queries = Maps.<String, LinkedHashMap<String, Query>> newLinkedHashMap();

    for (String anchorId : anchorIds) {
      queries.put(anchorId, generateQueries(filters, filterTemplate, ids, anchorId));
    }
    return queries;
  }

  public static ObjectNode mergeFilters(ObjectNode filters, String template, Object... objects) {
    return JsonUtils.merge(filters, (new FiltersParam(String.format(template, objects)).get()));
  }

  public static QueryBuilder query() {
    return Query.builder();
  }

  /**
   * @see http://stackoverflow.com/questions/23704616/how-to-validate-a-single-parameter-in-dropwizard
   */
  public static void validate(@NonNull Object object) {
    val errorMessages = new ArrayList<String>();
    val validator = Validation.buildDefaultValidatorFactory().getValidator();

    val violations = validator.validate(object);
    if (!violations.isEmpty()) {
      for (val violation : violations) {
        errorMessages.add("'" + violation.getPropertyPath() + "' " + violation.getMessage());
      }

      throw new BadRequestException(COMMA_JOINER.join(errorMessages));
    }

  }

  public static void checkRequest(boolean errorCondition, String formatTemplate, Object... args) {

    if (errorCondition) {
      // We don't want exception within an exception-handling routine.
      final Supplier<String> errorMessageProvider = () -> {
        try {
          return format(formatTemplate, args);
        } catch (Exception e) {
          final String errorDetails = "message: '" + formatTemplate +
              "', parameters: '" + COMMA_JOINER.join(args) + "'";
          log.error("Error while formatting message - " + errorDetails, e);

          return "Invalid web request - " + errorDetails;
        }
      };

      throw new BadRequestException(errorMessageProvider.get());
    }
  }

  public static List<String> removeNullAndEmptyString(List<String> source) {
    if (isEmpty(source)) {
      return source;
    }

    source.removeAll(EMPTY_VALUES);

    return source;
  }

  public static Query regularFindAllJqlQuery(List<String> fields, List<String> include, ObjectNode filters,
      IntParam from, IntParam size, String sort, String order) {
    val query = query()
        .fields(fields).filters(filters)
        .from(from.get()).size(size.get())
        .sort(sort).order(order);

    removeNullAndEmptyString(include);
    if (!include.isEmpty()) {
      query.includes(include);
    }

    return query.build();
  }

}
