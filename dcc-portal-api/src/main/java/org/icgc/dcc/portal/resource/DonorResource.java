package org.icgc.dcc.portal.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.icgc.dcc.portal.resource.ResourceUtils.AFFECTED_BY_THE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_DONOR_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_DONOR_VALUE;
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

import org.icgc.dcc.portal.model.Donor;
import org.icgc.dcc.portal.model.Donors;
import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.Genes;
import org.icgc.dcc.portal.model.IdsParam;
import org.icgc.dcc.portal.model.IndexModel;
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
@Path("/v1/donors")
@Produces(APPLICATION_JSON)
@Api(value = "/donors", description = "Resources relating to " + DONOR)
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class DonorResource {

  private static final String DONOR_FILTER_TEMPLATE = "{donor:{id:{is:['%s']}}}";
  private static final String MUTATION_DONOR_FILTER_TEMPLATE = "{mutation:{id:{is:['%s']}},donor:{id:{is:['%s']}}}";
  private static final String GENE_DONOR_FILTER_TEMPLATE = "{gene:{id:{is:['%s']}},donor:{id:{is:['%s']}}}";

  private final DonorService donorService;
  private final GeneService geneService;
  private final MutationService mutationService;

  // When the query is keyed by donor id, it makes little sense to use entity set.
  private void removeDonorEntitySet(ObjectNode filters) {
    if (filters.path("donor").path(IndexModel.API_ENTITY_LIST_ID_FIELD_NAME).isMissingNode() == false) {
      ((ObjectNode) filters.get("donor")).remove(IndexModel.API_ENTITY_LIST_ID_FIELD_NAME);
    }
  }

  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + DONOR + S, response = Donors.class)
  public Donors findAll(
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_DONOR_SORT) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order,
      @ApiParam(value = API_FACETS_ONLY_DESCRIPTION) @QueryParam(API_FACETS_ONLY_PARAM) @DefaultValue("false") boolean facetsOnly
      ) {
    val filters = filtersParam.get();

    log.info(FIND_ALL_TEMPLATE, new Object[] { size, DONOR, from, sort, order, filters });

    val query = regularFindAllJqlQuery(fields, include, filters, from, size, sort, order);

    return donorService.findAllCentric(query, facetsOnly);
  }

  @Path("/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S)
  public Long count(
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(COUNT_TEMPLATE, DONOR, filters);

    return donorService.count(query().filters(filters).build());
  }

  @Path("/{" + API_DONOR_PARAM + "}")
  @GET
  @Timed
  @ApiOperation(value = FIND_BY_ID, notes = FIND_BY_ID_ERROR, response = Donor.class)
  @ApiResponses(value = { @ApiResponse(code = NOT_FOUND_404, message = DONOR + NOT_FOUND) })
  public Donor find(
      @ApiParam(value = API_DONOR_VALUE, required = true) @PathParam(API_DONOR_PARAM) String donorId,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include) {
    log.info(FIND_ONE_TEMPLATE, donorId);

    return donorService.findOne(donorId, query().fields(fields).includes(include).build());
  }

  @Path("/{" + API_DONOR_PARAM + "}/genes")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + GENE + S + FOR_THE + DONOR + S, response = Genes.class)
  public Genes findGenes(
      @ApiParam(value = API_DONOR_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_DONOR_PARAM) IdsParam donorIds,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_GENE_MUTATION_SORT) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order) {
    ObjectNode filters = filtersParam.get();
    List<String> donors = donorIds.get();

    removeDonorEntitySet(filters);

    log.info(NESTED_FIND_TEMPLATE, GENE, donors);

    filters = mergeFilters(filters, DONOR_FILTER_TEMPLATE, JsonUtils.join(donors));

    return geneService.findAllCentric(query().filters(filters).fields(fields).includes(include)
        .from(from.get()).size(size.get()).sort(sort).order(order).build());
  }

  @Path("/{" + API_DONOR_PARAM + "}/genes/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + GENE + S + FOR_THE + DONOR)
  public Long countGenes(
      @ApiParam(value = API_DONOR_VALUE, required = true) @PathParam(API_DONOR_PARAM) String donorId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    removeDonorEntitySet(filters);

    log.info(NESTED_COUNT_TEMPLATE, GENE, donorId);

    filters = mergeFilters(filters, DONOR_FILTER_TEMPLATE, donorId);

    return geneService.count(query().filters(filters).build());
  }

  @Path("/{" + API_DONOR_PARAM + "}/genes/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + GENE + S + FOR_THE + DONOR + S)
  public Map<String, Long> countsGenes(
      @ApiParam(value = API_DONOR_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_DONOR_PARAM) IdsParam donorIds,
      @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> donors = donorIds.get();

    removeDonorEntitySet(filters);

    log.info(NESTED_COUNT_TEMPLATE, GENE, donors);

    val queries = generateQueries(filters, DONOR_FILTER_TEMPLATE, donors);
    val counts = geneService.counts(queries);

    // Get total Gene count using all Donors
    filters = mergeFilters(filters, DONOR_FILTER_TEMPLATE, JsonUtils.join(donors));
    long uniqueCount = geneService.count(query().filters(filters).build());

    counts.put(TOTAL, uniqueCount);

    return counts;
  }

  @Path("/{" + API_DONOR_PARAM + "}/genes/{" + API_GENE_PARAM + "}/mutations/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + FOR_THE + DONOR + AFFECTED_BY_THE + GENE)
  public Long countGeneMutations(
      @ApiParam(value = API_DONOR_VALUE, required = true) @PathParam(API_DONOR_PARAM) String donorId,
      @ApiParam(value = API_GENE_VALUE, required = true) @PathParam(API_GENE_PARAM) String geneId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    removeDonorEntitySet(filters);

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { MUTATION, donorId, geneId });

    filters = mergeFilters(filters, GENE_DONOR_FILTER_TEMPLATE, donorId, geneId);

    return mutationService.count(query().filters(filters).build());
  }

  @Path("/{" + API_DONOR_PARAM + "}/genes/{" + API_GENE_PARAM + "}/mutations/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + GROUPED_BY + GENE + S + GROUPED_BY + DONOR + S)
  public Map<String, LinkedHashMap<String, Long>> countsGeneMutations(
      @ApiParam(value = API_DONOR_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_DONOR_PARAM) IdsParam donorIds,
      @ApiParam(value = API_GENE_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_GENE_PARAM) IdsParam geneIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> donors = donorIds.get();
    List<String> genes = geneIds.get();

    removeDonorEntitySet(filters);

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { MUTATION, donors, genes });

    val queries = generateQueries(filters, GENE_DONOR_FILTER_TEMPLATE, genes, donors);
    val counts = mutationService.nestedCounts(queries);

    // Get total Mutation count for each Donor using all Genes
    for (String donorId : donors) {
      filters = mergeFilters(filters, GENE_DONOR_FILTER_TEMPLATE, JsonUtils.join(genes), donorId);
      long uniqueCount = mutationService.count(query().filters(filters).build());
      counts.get(donorId).put(TOTAL, uniqueCount);
    }

    return counts;
  }

  @Path("/{" + API_DONOR_PARAM + "}/mutations")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + MUTATION + S + FOR_THE + DONOR + S, response = Mutations.class)
  public Mutations findMutations(
      @ApiParam(value = API_DONOR_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_DONOR_PARAM) IdsParam donorIds,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_GENE_MUTATION_SORT) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order) {
    ObjectNode filters = filtersParam.get();
    List<String> donors = donorIds.get();

    removeDonorEntitySet(filters);

    log.info(NESTED_FIND_TEMPLATE, MUTATION, donors);

    filters = mergeFilters(filters, DONOR_FILTER_TEMPLATE, JsonUtils.join(donors));

    return mutationService.findAllCentric(query().filters(filters).fields(fields).includes(include)
        .from(from.get()).size(size.get()).sort(sort).order(order).build());
  }

  @Path("/{" + API_DONOR_PARAM + "}/mutations/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + FOR_THE + DONOR)
  public Long countMutations(
      @ApiParam(value = API_DONOR_VALUE, required = true) @PathParam(API_DONOR_PARAM) String donorId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    removeDonorEntitySet(filters);

    log.info(NESTED_COUNT_TEMPLATE, MUTATION, donorId);

    filters = mergeFilters(filters, DONOR_FILTER_TEMPLATE, donorId);

    return mutationService.count(query().filters(filters).build());
  }

  @Path("/{" + API_DONOR_PARAM + "}/mutations/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + FOR_THE + DONOR + S)
  public Map<String, Long> countsMutations(
      @ApiParam(value = API_DONOR_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_DONOR_PARAM) IdsParam donorIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> donors = donorIds.get();

    removeDonorEntitySet(filters);

    log.info(NESTED_COUNT_TEMPLATE, MUTATION, donors);

    val queries = generateQueries(filters, DONOR_FILTER_TEMPLATE, donors);
    val counts = mutationService.counts(queries);

    // Get total Mutation count using all Donors
    filters = mergeFilters(filters, DONOR_FILTER_TEMPLATE, JsonUtils.join(donors));
    long uniqueCount = mutationService.count(query().filters(filters).build());

    counts.put(TOTAL, uniqueCount);

    return counts;
  }

  @Path("/{" + API_DONOR_PARAM + "}/mutations/{" + API_MUTATION_PARAM + "}/genes/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + GENE + S + FOR_THE + DONOR + AFFECTED_BY_THE + MUTATION)
  public Long countMutationGenes(
      @ApiParam(value = API_DONOR_VALUE, required = true) @PathParam(API_DONOR_PARAM) String donorId,
      @ApiParam(value = API_MUTATION_VALUE, required = true) @PathParam(API_MUTATION_PARAM) String mutationId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    removeDonorEntitySet(filters);

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { GENE, donorId, mutationId });

    filters = mergeFilters(filters, MUTATION_DONOR_FILTER_TEMPLATE, mutationId, donorId);

    return donorService.count(query().filters(filters).build());
  }

  @Path("/{" + API_DONOR_PARAM + "}/mutations/{" + API_MUTATION_PARAM + "}/genes/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + GENE + S + GROUPED_BY + MUTATION + S + GROUPED_BY + DONOR + S)
  public LinkedHashMap<String, LinkedHashMap<String, Long>> countMutationGenes(
      @ApiParam(value = API_DONOR_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_DONOR_PARAM) IdsParam donorIds,
      @ApiParam(value = API_MUTATION_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    List<String> donors = donorIds.get();
    List<String> mutations = mutationIds.get();
    ObjectNode filters = filtersParam.get();

    removeDonorEntitySet(filters);

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { GENE, donors, mutations });

    val queries = generateQueries(filters, MUTATION_DONOR_FILTER_TEMPLATE, mutations, donors);
    val counts = geneService.nestedCounts(queries);

    // Get total Gene count for each Donor using all Mutations
    for (String donorId : donors) {
      filters = mergeFilters(filters, MUTATION_DONOR_FILTER_TEMPLATE, JsonUtils.join(mutations), donorId);
      long uniqueCount = geneService.count(query().filters(filters).build());
      counts.get(donorId).put(TOTAL, uniqueCount);
    }

    return counts;
  }

}
