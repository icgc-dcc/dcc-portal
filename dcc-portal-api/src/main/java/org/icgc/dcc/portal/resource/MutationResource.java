package org.icgc.dcc.portal.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.icgc.dcc.portal.resource.ResourceUtils.AFFECTED_BY_THE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FACETS_ONLY_DESCRIPTION;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FACETS_ONLY_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FIELD_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FIELD_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FILTER_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FILTER_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FROM_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FROM_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_GENE_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_GENE_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_INCLUDE_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_INCLUDE_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_MUTATION_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_MUTATION_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ORDER_ALLOW;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ORDER_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ORDER_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_PROJECT_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_PROJECT_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SIZE_ALLOW;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SIZE_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SIZE_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SORT_FIELD;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SORT_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.COUNT_TEMPLATE;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_DONOR_SORT;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_FILTERS;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_FROM;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_GENE_MUTATION_SORT;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_ORDER;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_SIZE;
import static org.icgc.dcc.portal.resource.ResourceUtils.DONOR;
import static org.icgc.dcc.portal.resource.ResourceUtils.FIND_ALL_TEMPLATE;
import static org.icgc.dcc.portal.resource.ResourceUtils.FIND_BY_ID;
import static org.icgc.dcc.portal.resource.ResourceUtils.FIND_BY_ID_ERROR;
import static org.icgc.dcc.portal.resource.ResourceUtils.FIND_ONE_TEMPLATE;
import static org.icgc.dcc.portal.resource.ResourceUtils.FOR_THE;
import static org.icgc.dcc.portal.resource.ResourceUtils.GENE;
import static org.icgc.dcc.portal.resource.ResourceUtils.GROUPED_BY;
import static org.icgc.dcc.portal.resource.ResourceUtils.MULTIPLE_IDS;
import static org.icgc.dcc.portal.resource.ResourceUtils.MUTATION;
import static org.icgc.dcc.portal.resource.ResourceUtils.NESTED_COUNT_TEMPLATE;
import static org.icgc.dcc.portal.resource.ResourceUtils.NESTED_FIND_TEMPLATE;
import static org.icgc.dcc.portal.resource.ResourceUtils.NESTED_NESTED_COUNT_TEMPLATE;
import static org.icgc.dcc.portal.resource.ResourceUtils.NOT_FOUND;
import static org.icgc.dcc.portal.resource.ResourceUtils.PROJECT;
import static org.icgc.dcc.portal.resource.ResourceUtils.RETURNS_COUNT;
import static org.icgc.dcc.portal.resource.ResourceUtils.RETURNS_LIST;
import static org.icgc.dcc.portal.resource.ResourceUtils.S;
import static org.icgc.dcc.portal.resource.ResourceUtils.TOTAL;
import static org.icgc.dcc.portal.resource.ResourceUtils.generateQueries;
import static org.icgc.dcc.portal.resource.ResourceUtils.mergeFilters;
import static org.icgc.dcc.portal.resource.ResourceUtils.query;
import static org.icgc.dcc.portal.resource.ResourceUtils.regularFindAllJqlQuery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.Donors;
import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.Genes;
import org.icgc.dcc.portal.model.IdsParam;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.Mutation;
import org.icgc.dcc.portal.model.Mutations;
import org.icgc.dcc.portal.service.DonorService;
import org.icgc.dcc.portal.service.GeneService;
import org.icgc.dcc.portal.service.MutationService;
import org.icgc.dcc.portal.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import com.yammer.dropwizard.jersey.params.IntParam;
import com.yammer.metrics.annotation.Timed;

@Component
@Slf4j
@Path("/v1/mutations")
@Produces(APPLICATION_JSON)
@Api(value = "/mutations", description = "Resources relating to " + MUTATION)
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class MutationResource {

  private static final String MUTATION_FILTER_TEMPLATE = "{mutation:{id:{is:['%s']}}}";
  private static final String GENE_MUTATION_FILTER_TEMPLATE = "{gene:{id:{is:['%s']}},mutation:{id:{is:['%s']}}}";
  private static final String PROJECT_MUTATION_FILTER_TEMPLATE =
      "{donor:{projectId:{is:['%s']}},mutation:{id:{is:['%s']}}}";

  private final MutationService mutationService;
  private final GeneService geneService;
  private final DonorService donorService;

  // When the query is keyed by gene id, it makes little sense to use entity set.
  private void removeMutationEntitySet(ObjectNode filters) {
    if (filters.path("mutation").path(IndexModel.API_ENTITY_LIST_ID_FIELD_NAME).isMissingNode() == false) {
      ((ObjectNode) filters.get("mutation")).remove(IndexModel.API_ENTITY_LIST_ID_FIELD_NAME);
    }
  }

  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + MUTATION + S, response = Mutation.class)
  public Mutations findAll(
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_GENE_MUTATION_SORT) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order,
      @ApiParam(value = API_FACETS_ONLY_DESCRIPTION) @QueryParam(API_FACETS_ONLY_PARAM) @DefaultValue("false") boolean facetsOnly
      ) {
    val filters = filtersParam.get();

    log.info(FIND_ALL_TEMPLATE, new Object[] { size, MUTATION, from, sort, order, filters });

    val query = regularFindAllJqlQuery(fields, include, filters, from, size, sort, order);

    return mutationService.findAllCentric(query, facetsOnly);
  }

  @Path("/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S)
  public Long count(
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam
      ) {
    ObjectNode filters = filtersParam.get();

    log.info(COUNT_TEMPLATE, GENE, filters);

    return mutationService.count(query().filters(filters).build());
  }

  @Path("/{" + API_MUTATION_PARAM + "}")
  @GET
  @Timed
  @ApiOperation(value = FIND_BY_ID, notes = FIND_BY_ID_ERROR, response = Mutation.class)
  @ApiResponses(value = { @ApiResponse(code = NOT_FOUND_404, message = MUTATION + NOT_FOUND) })
  public Mutation find(
      @ApiParam(value = API_MUTATION_VALUE, required = true) @PathParam(API_MUTATION_PARAM) String mutationId,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include
      ) {
    log.info(FIND_ONE_TEMPLATE, mutationId);

    return mutationService.findOne(mutationId, query().fields(fields).includes(include).build());
  }

  @Path("/{" + API_MUTATION_PARAM + "}/donors")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + DONOR + S + FOR_THE + MUTATION + S, response = Donors.class)
  public Donors findDonors(
      @ApiParam(value = API_MUTATION_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_DONOR_SORT) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order
      ) {
    ObjectNode filters = filtersParam.get();

    removeMutationEntitySet(filters);

    log.info(NESTED_FIND_TEMPLATE, DONOR, mutationIds.get());

    filters = mergeFilters(filters, MUTATION_FILTER_TEMPLATE, JsonUtils.join(mutationIds.get()));

    return donorService.findAllCentric(query().filters(filters).fields(fields).includes(include)
        .from(from.get()).size(size.get()).sort(sort).order(order).build());
  }

  @Path("/{" + API_MUTATION_PARAM + "}/donors/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + FOR_THE + MUTATION)
  public Long countDonors(
      @ApiParam(value = API_MUTATION_VALUE, required = true) @PathParam(API_MUTATION_PARAM) String mutationId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam
      ) {
    ObjectNode filters = filtersParam.get();

    removeMutationEntitySet(filters);

    log.info(NESTED_COUNT_TEMPLATE, DONOR, mutationId);

    filters = mergeFilters(filters, MUTATION_FILTER_TEMPLATE, mutationId);

    return donorService.count(query().filters(filters).build());
  }

  @Path("/{" + API_MUTATION_PARAM + "}/donors/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + FOR_THE + MUTATION + S)
  public Map<String, Long> countDonors(
      @ApiParam(value = API_MUTATION_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
      @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam
      ) {
    ObjectNode filters = filtersParam.get();
    List<String> mutations = mutationIds.get();

    removeMutationEntitySet(filters);

    log.info(NESTED_COUNT_TEMPLATE, DONOR, mutations);

    val queries = generateQueries(filters, MUTATION_FILTER_TEMPLATE, mutations);
    val counts = donorService.counts(queries);

    // Get total Donor count using all Mutations
    filters = mergeFilters(filters, MUTATION_FILTER_TEMPLATE, JsonUtils.join(mutations));
    long uniqueCount = donorService.count(query().filters(filters).build());
    counts.put(TOTAL, uniqueCount);

    return counts;
  }

  @Path("/{" + API_MUTATION_PARAM + "}/genes")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + GENE + S + FOR_THE + MUTATION + S, response = Genes.class)
  public Genes findGenes(
      @ApiParam(value = API_MUTATION_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_GENE_MUTATION_SORT) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order
      ) {
    ObjectNode filters = filtersParam.get();
    List<String> mutations = mutationIds.get();

    removeMutationEntitySet(filters);

    log.info(NESTED_FIND_TEMPLATE, GENE, mutations);

    filters = mergeFilters(filters, MUTATION_FILTER_TEMPLATE, JsonUtils.join(mutations));

    return geneService.findAllCentric(query().filters(filters).fields(fields).includes(include)
        .from(from.get()).size(size.get()).sort(sort).order(order).build());
  }

  @Path("/{" + API_MUTATION_PARAM + "}/genes/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + GENE + S + FOR_THE + MUTATION)
  public Long countGenes(
      @ApiParam(value = API_MUTATION_VALUE, required = true) @PathParam(API_MUTATION_PARAM) String mutationId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam
      ) {
    ObjectNode filters = filtersParam.get();

    removeMutationEntitySet(filters);

    log.info(NESTED_COUNT_TEMPLATE, GENE, mutationId);

    filters = mergeFilters(filters, MUTATION_FILTER_TEMPLATE, mutationId);

    return geneService.count(query().filters(filters).build());
  }

  @Path("/{" + API_MUTATION_PARAM + "}/genes/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + GENE + S + FOR_THE + MUTATION + S)
  public Map<String, Long> countGenes(
      @ApiParam(value = API_MUTATION_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
      @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam
      ) {
    ObjectNode filters = filtersParam.get();
    List<String> mutations = mutationIds.get();

    removeMutationEntitySet(filters);

    log.info(NESTED_COUNT_TEMPLATE, GENE, mutations);

    val queries = generateQueries(filters, MUTATION_FILTER_TEMPLATE, mutations);
    val counts = geneService.counts(queries);

    // Get total Gene count using all Mutations
    filters = mergeFilters(filters, MUTATION_FILTER_TEMPLATE, JsonUtils.join(mutations));
    long uniqueCount = geneService.count(query().filters(filters).build());
    counts.put(TOTAL, uniqueCount);

    return counts;
  }

  @Path("/{" + API_MUTATION_PARAM + "}/genes/{" + API_GENE_PARAM + "}/donors/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + FOR_THE + MUTATION + AFFECTED_BY_THE + GENE)
  public Long countGeneDonors(
      @ApiParam(value = API_MUTATION_VALUE, required = true) @PathParam(API_MUTATION_PARAM) String mutationId,
      @ApiParam(value = API_GENE_VALUE, required = true) @PathParam(API_GENE_PARAM) String geneId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam
      ) {
    ObjectNode filters = filtersParam.get();

    removeMutationEntitySet(filters);

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { DONOR, mutationId, geneId });

    filters = mergeFilters(filters, GENE_MUTATION_FILTER_TEMPLATE, geneId, mutationId);

    return donorService.count(query().filters(filters).build());
  }

  @Path("/{" + API_MUTATION_PARAM + "}/genes/{" + API_GENE_PARAM + "}/donors/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + GROUPED_BY + GENE + S + GROUPED_BY + MUTATION + S)
  public LinkedHashMap<String, LinkedHashMap<String, Long>> countsGeneDonors(
      @ApiParam(value = API_MUTATION_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
      @ApiParam(value = API_GENE_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_GENE_PARAM) IdsParam geneIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam
      ) {
    ObjectNode filters = filtersParam.get();
    List<String> mutations = mutationIds.get();
    List<String> genes = geneIds.get();

    removeMutationEntitySet(filters);

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { DONOR, mutations, genes });

    val queries = generateQueries(filters, GENE_MUTATION_FILTER_TEMPLATE, genes, mutations);
    val counts = donorService.nestedCounts(queries);

    // Get total Donor count for each Mutation using all Genes
    for (String mutationId : mutations) {
      filters = mergeFilters(filters, GENE_MUTATION_FILTER_TEMPLATE, JsonUtils.join(genes), mutationId);
      Long uniqueCount = donorService.count(query().filters(filters).build());
      counts.get(mutationId).put(TOTAL, uniqueCount);
    }

    return counts;
  }

  @Path("/{" + API_MUTATION_PARAM + "}/projects/{" + API_PROJECT_PARAM + "}/donors/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + FOR_THE + MUTATION + FOR_THE + PROJECT)
  public Long countProjectDonors(
      @ApiParam(value = API_MUTATION_VALUE, required = true) @PathParam(API_MUTATION_PARAM) String mutationId,
      @ApiParam(value = API_PROJECT_VALUE, required = true) @PathParam(API_PROJECT_PARAM) String projectId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam
      ) {
    ObjectNode filters = filtersParam.get();

    removeMutationEntitySet(filters);

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { DONOR, mutationId, projectId });

    val donors = new FiltersParam(String.format(PROJECT_MUTATION_FILTER_TEMPLATE, projectId, mutationId));
    JsonUtils.merge(filters, donors.get());

    return donorService.count(query().filters(filters).build());
  }

  @Path("/{" + API_MUTATION_PARAM + "}/projects/{" + API_PROJECT_PARAM + "}/donors/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + GROUPED_BY + PROJECT + S + GROUPED_BY + MUTATION + S)
  public LinkedHashMap<String, LinkedHashMap<String, Long>> countsProjectDonors(
      @ApiParam(value = API_MUTATION_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
      @ApiParam(value = API_PROJECT_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam
      ) {
    ObjectNode filters = filtersParam.get();
    List<String> projects = projectIds.get();
    List<String> mutations = mutationIds.get();

    removeMutationEntitySet(filters);

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { DONOR, mutations, projects });

    val queries = generateQueries(filters, PROJECT_MUTATION_FILTER_TEMPLATE, projects, mutations);
    val counts = donorService.nestedCounts(queries);

    // Get total Donor count for each Mutation using all Genes
    for (String mutationId : mutations) {
      filters = mergeFilters(filters, PROJECT_MUTATION_FILTER_TEMPLATE, JsonUtils.join(projects), mutationId);
      long uniqueCount = donorService.count(query().filters(filters).build());
      counts.get(mutationId).put(TOTAL, uniqueCount);
    }

    return counts;
  }
}
