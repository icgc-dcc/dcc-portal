package org.icgc.dcc.portal.resource;

import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.sun.jersey.core.header.ContentDisposition.type;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.icgc.dcc.portal.resource.ResourceUtils.AFFECTED_BY_THE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_DONOR_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_DONOR_VALUE;
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
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_PROJECT_SORT;
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
import static org.icgc.dcc.portal.util.JsonUtils.MAPPER;
import static org.icgc.dcc.portal.util.MediaTypes.TEXT_TSV;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.icgc.dcc.portal.model.Donors;
import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.Genes;
import org.icgc.dcc.portal.model.IdsParam;
import org.icgc.dcc.portal.model.Mutations;
import org.icgc.dcc.portal.model.Project;
import org.icgc.dcc.portal.model.Projects;
import org.icgc.dcc.portal.service.DonorService;
import org.icgc.dcc.portal.service.GeneService;
import org.icgc.dcc.portal.service.MutationService;
import org.icgc.dcc.portal.service.ProjectService;
import org.icgc.dcc.portal.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Resources;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import com.yammer.dropwizard.jersey.params.IntParam;
import com.yammer.metrics.annotation.Timed;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Path("/v1/projects")
@Produces(APPLICATION_JSON)
@Api(value = "/projects", description = "Resources relating to " + PROJECT)
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }) )
public class ProjectResource {

  private static final String PROJECT_FILTER_TEMPLATE = "{donor:{projectId:{is:['%s']}}}";
  private static final String DONOR_PROJECT_FILTER_TEMPLATE = "{donor:{id:{is:['%s']},projectId:{is:['%s']}}}";
  private static final String MUTATION_PROJECT_FILTER_TEMPLATE =
      "{mutation:{id:{is:['%s']}},donor:{projectId:{is:['%s']}}}";
  private static final String GENE_PROJECT_FILTER_TEMPLATE =
      "{gene:{id:{is:['%s']}},donor:{projectId:{is:['%s']}}}";
  private static final String RELEASE_HISTORY_FILE_PATH = "data/project-history.json";

  private final ProjectService projectService;
  private final GeneService geneService;
  private final DonorService donorService;
  private final MutationService mutationService;

  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + PROJECT + S, response = Projects.class)
  public Projects findAll(
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_PROJECT_SORT) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order) {
    ObjectNode filters = filtersParam.get();

    log.info(FIND_ALL_TEMPLATE, new Object[] { size, PROJECT, from, sort, order, filters });

    return projectService.findAll(query().fields(fields).filters(filters).includes(include)
        .from(from.get()).size(size.get()).sort(sort).order(order).build());

  }

  @Path("/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + PROJECT + S)
  public Long count(
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(COUNT_TEMPLATE, PROJECT, filters);

    return projectService.count(query().filters(filters).build());
  }

  @Path("/{" + API_PROJECT_PARAM + "}")
  @GET
  @Timed
  @ApiOperation(value = FIND_BY_ID, notes = FIND_BY_ID_ERROR, response = Project.class)
  @ApiResponses(value = { @ApiResponse(code = NOT_FOUND_404, message = PROJECT + NOT_FOUND) })
  public Project find(
      @ApiParam(value = API_PROJECT_VALUE, required = true) @PathParam(API_PROJECT_PARAM) String projectId,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include) {
    log.info(FIND_ONE_TEMPLATE, projectId);

    return projectService.findOne(projectId, query().fields(fields).includes(include).build());
  }

  @Path("/{" + API_PROJECT_PARAM + "}/donors")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + DONOR + S + FOR_THE + PROJECT + S, response = Donors.class)
  public Donors findDonors(
      @ApiParam(value = API_PROJECT_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_DONOR_SORT) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order) {
    ObjectNode filters = filtersParam.get();
    List<String> projects = projectIds.get();

    log.info(NESTED_FIND_TEMPLATE, DONOR, projects);

    filters = mergeFilters(filters, PROJECT_FILTER_TEMPLATE, JsonUtils.join(projects));

    return donorService.findAllCentric(query().filters(filters).fields(fields).includes(include)
        .from(from.get()).size(size.get()).sort(sort).order(order).build());
  }

  @Path("/{" + API_PROJECT_PARAM + "}/donors/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + FOR_THE + PROJECT)
  public Long countDonors(
      @ApiParam(value = API_PROJECT_VALUE, required = true) @PathParam(API_PROJECT_PARAM) String projectId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_COUNT_TEMPLATE, DONOR, projectId);

    filters = mergeFilters(filters, PROJECT_FILTER_TEMPLATE, projectId);

    return donorService.count(query().filters(filters).build());
  }

  @Path("/{" + API_PROJECT_PARAM + "}/donors/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + FOR_THE + PROJECT + S)
  public Map<String, Long> countsDonors(
      @ApiParam(value = API_PROJECT_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> projects = projectIds.get();

    log.info(NESTED_COUNT_TEMPLATE, DONOR, projects);

    val queries = generateQueries(filters, PROJECT_FILTER_TEMPLATE, projects);
    val counts = donorService.counts(queries);

    // Get total Donor count using all Projects
    filters = mergeFilters(filters, PROJECT_FILTER_TEMPLATE, JsonUtils.join(projects));
    long uniqueCount = donorService.count(query().filters(filters).build());
    counts.put(TOTAL, uniqueCount);

    return counts;
  }

  @Path("/{" + API_PROJECT_PARAM + "}/donors/{" + API_DONOR_PARAM + "}/genes/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + GENE + S + FOR_THE + PROJECT + AFFECTED_BY_THE + DONOR)
  public Long countDonorGenes(
      @ApiParam(value = API_PROJECT_VALUE, required = true) @PathParam(API_PROJECT_PARAM) String projectId,
      @ApiParam(value = API_DONOR_VALUE, required = true) @PathParam(API_DONOR_PARAM) String donorId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { GENE, projectId, donorId });

    filters = mergeFilters(filters, DONOR_PROJECT_FILTER_TEMPLATE, donorId, projectId);

    return geneService.count(query().filters(filters).build());
  }

  @Path("/{" + API_PROJECT_PARAM + "}/donors/{" + API_DONOR_PARAM + "}/genes/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + GENE + S + GROUPED_BY + DONOR + S + GROUPED_BY + PROJECT + S)
  public LinkedHashMap<String, LinkedHashMap<String, Long>> countsDonorGenes(
      @ApiParam(value = API_PROJECT_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
      @ApiParam(value = API_DONOR_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_DONOR_PARAM) IdsParam donorIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> donors = donorIds.get();
    List<String> projects = projectIds.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { GENE, projects, donors });

    val queries = generateQueries(filters, DONOR_PROJECT_FILTER_TEMPLATE, donors, projects);
    val counts = geneService.nestedCounts(queries);

    // Get total Gene count for each Project using all Donors
    for (String projectId : projects) {
      filters = mergeFilters(filters, DONOR_PROJECT_FILTER_TEMPLATE, JsonUtils.join(donors), projectId);
      long uniqueCount = geneService.count(query().filters(filters).build());
      counts.get(projectId).put(TOTAL, uniqueCount);
    }

    return counts;
  }

  @Path("/{" + API_PROJECT_PARAM + "}/donors/{" + API_DONOR_PARAM + "}/mutations/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + FOR_THE + PROJECT + AFFECTED_BY_THE + DONOR)
  public Long countDonorMutations(
      @ApiParam(value = API_PROJECT_VALUE, required = true) @PathParam(API_PROJECT_PARAM) String projectId,
      @ApiParam(value = API_DONOR_VALUE, required = true) @PathParam(API_DONOR_PARAM) String donorId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { MUTATION, projectId, donorId });

    filters = mergeFilters(filters, DONOR_PROJECT_FILTER_TEMPLATE, donorId, projectId);

    return mutationService.count(query().filters(filters).build());
  }

  @Path("/{" + API_PROJECT_PARAM + "}/donors/{" + API_DONOR_PARAM + "}/mutations/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + GROUPED_BY + DONOR + S + GROUPED_BY + PROJECT + S)
  public Map<String, LinkedHashMap<String, Long>> countsDonorMutations(
      @ApiParam(value = API_PROJECT_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
      @ApiParam(value = API_DONOR_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_DONOR_PARAM) IdsParam donorIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> donors = donorIds.get();
    List<String> projects = projectIds.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { MUTATION, projects, donors });

    val queries = generateQueries(filters, DONOR_PROJECT_FILTER_TEMPLATE, donors, projects);
    val counts = mutationService.nestedCounts(queries);

    // Get total Mutation count for each Project using all Donors
    for (String projectId : projects) {
      filters = mergeFilters(filters, DONOR_PROJECT_FILTER_TEMPLATE, JsonUtils.join(donors), projectId);
      long uniqueCount = mutationService.count(query().filters(filters).build());
      counts.get(projectId).put(TOTAL, uniqueCount);
    }

    return counts;
  }

  @Produces(TEXT_TSV)
  @Path("/{" + API_PROJECT_PARAM + "}/samples")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + "Samples for the Project")
  public Response findSamples(@ApiParam(value = API_PROJECT_VALUE) @PathParam(API_PROJECT_PARAM) String projectId) {
    log.info(NESTED_FIND_TEMPLATE, "Samples", projectId);

    Donors donors = donorService.getDonorAndSampleByProject(projectId);

    List<Map<String, Object>> samples = donorService.getSamples(donors.getHits());

    StreamingOutput sampleStream = donorService.asSampleStream(samples);
    String fileName = donorService.sampleFilename(projectId);

    return Response.ok(sampleStream).header(CONTENT_DISPOSITION,
        type("attachment").fileName(fileName).creationDate(new Date()).build()).build();
  }

  @Path("/{" + API_PROJECT_PARAM + "}/genes")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + GENE + S + FOR_THE + PROJECT + S, response = Genes.class)
  public Genes findGenes(
      @ApiParam(value = API_PROJECT_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_GENE_MUTATION_SORT) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order) {
    ObjectNode filters = filtersParam.get();
    List<String> projects = projectIds.get();

    log.info(NESTED_FIND_TEMPLATE, GENE, projects);

    filters = mergeFilters(filters, PROJECT_FILTER_TEMPLATE, JsonUtils.join(projects));

    return geneService.findAllCentric(query().filters(filters).fields(fields).includes(include)
        .from(from.get())
        .size(size.get()).sort(sort).order(order).build());
  }

  @Path("/{" + API_PROJECT_PARAM + "}/genes/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + GENE + S + FOR_THE + PROJECT)
  public Long countGenes(
      @ApiParam(value = API_PROJECT_VALUE, required = true) @PathParam(API_PROJECT_PARAM) String projectId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_COUNT_TEMPLATE, GENE, projectId);

    filters = mergeFilters(filters, PROJECT_FILTER_TEMPLATE, projectId);

    return geneService.count(query().filters(filters).build());
  }

  @Path("/{" + API_PROJECT_PARAM + "}/genes/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + GENE + S + FOR_THE + PROJECT + S)
  public Map<String, Long> countsGenes(
      @ApiParam(value = API_PROJECT_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
      @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> projects = projectIds.get();

    log.info(NESTED_COUNT_TEMPLATE, GENE, projects);

    val queries = generateQueries(filters, PROJECT_FILTER_TEMPLATE, projects);
    val counts = geneService.counts(queries);

    // Get total Gene count using all Projects
    filters = mergeFilters(filters, PROJECT_FILTER_TEMPLATE, JsonUtils.join(projects));

    Long uniqueCount = geneService.count(query().filters(filters).build());
    counts.put(TOTAL, uniqueCount);

    return counts;
  }

  @Path("/{" + API_PROJECT_PARAM + "}/genes/{" + API_GENE_PARAM + "}/mutations/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + FOR_THE + PROJECT + AFFECTED_BY_THE + GENE)
  public Long countGeneMutations(
      @ApiParam(value = API_PROJECT_VALUE, required = true) @PathParam(API_PROJECT_PARAM) String projectId,
      @ApiParam(value = API_GENE_VALUE, required = true) @PathParam(API_GENE_PARAM) String geneId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { MUTATION, projectId, geneId });

    filters = mergeFilters(filters, GENE_PROJECT_FILTER_TEMPLATE, geneId, projectId);

    return mutationService.count(query().filters(filters).build());
  }

  @Path("/{" + API_PROJECT_PARAM + "}/genes/{" + API_GENE_PARAM + "}/mutations/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + GROUPED_BY + GENE + S + GROUPED_BY + PROJECT + S)
  public Map<String, LinkedHashMap<String, Long>> countsGeneMutations(
      @ApiParam(value = API_PROJECT_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
      @ApiParam(value = API_GENE_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_GENE_PARAM) IdsParam geneIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> projects = projectIds.get();
    List<String> genes = geneIds.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { MUTATION, projects, genes });

    val queries = generateQueries(filters, GENE_PROJECT_FILTER_TEMPLATE, genes, projects);
    val counts = mutationService.nestedCounts(queries);

    // Get total Mutation count for each Project using all Genes
    for (String projectId : projects) {
      filters = mergeFilters(filters, GENE_PROJECT_FILTER_TEMPLATE, JsonUtils.join(genes), projectId);
      long uniqueCount = mutationService.count(query().filters(filters).build());
      counts.get(projectId).put(TOTAL, uniqueCount);
    }

    return counts;
  }

  @Path("/{" + API_PROJECT_PARAM + "}/genes/{" + API_GENE_PARAM + "}/donors/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + FOR_THE + PROJECT + AFFECTED_BY_THE + GENE)
  public Long countGeneDonors(
      @ApiParam(value = API_PROJECT_VALUE, required = true) @PathParam(API_PROJECT_PARAM) String projectId,
      @ApiParam(value = API_GENE_VALUE, required = true) @PathParam(API_GENE_PARAM) String geneId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { DONOR, projectId, geneId });

    filters = mergeFilters(filters, GENE_PROJECT_FILTER_TEMPLATE, geneId, projectId);

    return donorService.count(query().filters(filters).build());
  }

  @Path("/{" + API_PROJECT_PARAM + "}/genes/{" + API_GENE_PARAM + "}/donors/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + GROUPED_BY + GENE + S + GROUPED_BY + PROJECT + S)
  public LinkedHashMap<String, LinkedHashMap<String, Long>> countsGeneDonors(
      @ApiParam(value = API_PROJECT_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
      @ApiParam(value = API_GENE_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_GENE_PARAM) IdsParam geneIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> projects = projectIds.get();
    List<String> genes = geneIds.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { DONOR, projects, genes });

    val queries = generateQueries(filters, GENE_PROJECT_FILTER_TEMPLATE, genes, projects);
    val counts = donorService.nestedCounts(queries);

    // Get total Donor count for each Project using all Genes
    for (String projectId : projects) {
      filters = mergeFilters(filters, GENE_PROJECT_FILTER_TEMPLATE, JsonUtils.join(genes), projectId);
      long uniqueCount = donorService.count(query().filters(filters).build());
      counts.get(projectId).put(TOTAL, uniqueCount);
    }

    return counts;
  }

  @Path("/{" + API_PROJECT_PARAM + "}/mutations")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + MUTATION + S + FOR_THE + PROJECT + S, response = Mutations.class)
  public Mutations findMutations(
      @ApiParam(value = API_PROJECT_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_GENE_MUTATION_SORT) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order) {
    ObjectNode filters = filtersParam.get();
    List<String> projects = projectIds.get();

    log.info(NESTED_FIND_TEMPLATE, MUTATION, projects);

    filters = mergeFilters(filters, PROJECT_FILTER_TEMPLATE, JsonUtils.join(projects));

    return mutationService.findAllCentric(query().filters(filters).fields(fields).includes(include)
        .from(from.get()).size(size.get()).sort(sort).order(order).build());
  }

  @Path("/{" + API_PROJECT_PARAM + "}/mutations/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + FOR_THE + PROJECT)
  public Long countMutations(
      @ApiParam(value = API_PROJECT_VALUE, required = true) @PathParam(API_PROJECT_PARAM) String projectId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_COUNT_TEMPLATE, MUTATION, projectId);

    filters = mergeFilters(filters, PROJECT_FILTER_TEMPLATE, projectId);

    return mutationService.count(query().filters(filters).build());
  }

  @Path("/{" + API_PROJECT_PARAM + "}/mutations/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + FOR_THE + PROJECT + S)
  public Map<String, Long> countsMutations(
      @ApiParam(value = API_PROJECT_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> projects = projectIds.get();

    log.info(NESTED_COUNT_TEMPLATE, MUTATION, projects);

    val queries = generateQueries(filters, PROJECT_FILTER_TEMPLATE, projects);
    val counts = mutationService.counts(queries);

    // Get total Mutation count using all Projects
    filters = mergeFilters(filters, PROJECT_FILTER_TEMPLATE, JsonUtils.join(projects));

    Long uniqueCount = mutationService.count(query().filters(filters).build());
    counts.put(TOTAL, uniqueCount);

    return counts;
  }

  @Path("/{" + API_PROJECT_PARAM + "}/mutations/{" + API_MUTATION_PARAM + "}/donors/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + FOR_THE + PROJECT + AFFECTED_BY_THE + MUTATION)
  public Long countMutationDonors(
      @ApiParam(value = API_PROJECT_VALUE, required = true) @PathParam(API_PROJECT_PARAM) String projectId,
      @ApiParam(value = API_MUTATION_VALUE, required = true) @PathParam(API_MUTATION_PARAM) String mutationId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { DONOR, projectId, mutationId });

    filters = mergeFilters(filters, MUTATION_PROJECT_FILTER_TEMPLATE, mutationId, projectId);

    return donorService.count(query().filters(filters).build());
  }

  @Path("/{" + API_PROJECT_PARAM + "}/mutations/{" + API_MUTATION_PARAM + "}/donors/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + GROUPED_BY + MUTATION + S + GROUPED_BY + PROJECT + S)
  public LinkedHashMap<String, LinkedHashMap<String, Long>> countsMutationDonors(
      @ApiParam(value = API_PROJECT_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
      @ApiParam(value = API_MUTATION_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> mutations = mutationIds.get();
    List<String> projects = projectIds.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { DONOR, projects, mutations });

    val queries = generateQueries(filters, MUTATION_PROJECT_FILTER_TEMPLATE, mutations, projects);
    val counts = donorService.nestedCounts(queries);

    // Get total Donor count for each Project using all Mutations
    for (String projectId : projects) {
      filters = mergeFilters(filters, MUTATION_PROJECT_FILTER_TEMPLATE, JsonUtils.join(mutations), projectId);
      long uniqueCount = donorService.count(query().filters(filters).build());
      counts.get(projectId).put(TOTAL, uniqueCount);
    }

    return counts;
  }

  @Path("/{" + API_PROJECT_PARAM + "}/mutations/{" + API_MUTATION_PARAM + "}/genes/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + GENE + S + FOR_THE + PROJECT + AFFECTED_BY_THE + MUTATION)
  public Long countMutationGenes(
      @ApiParam(value = API_PROJECT_VALUE, required = true) @PathParam(API_PROJECT_PARAM) String projectId,
      @ApiParam(value = API_MUTATION_VALUE, required = true) @PathParam(API_MUTATION_PARAM) String mutationId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { GENE, projectId, mutationId });

    filters = mergeFilters(filters, MUTATION_PROJECT_FILTER_TEMPLATE, mutationId, projectId);

    return geneService.count(query().filters(filters).build());
  }

  @Path("/{" + API_PROJECT_PARAM + "}/mutations/{" + API_MUTATION_PARAM + "}/genes/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + GENE + S + GROUPED_BY + MUTATION + S + GROUPED_BY + PROJECT + S)
  public LinkedHashMap<String, LinkedHashMap<String, Long>> countMutationGenes(
      @ApiParam(value = API_PROJECT_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
      @ApiParam(value = API_MUTATION_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    List<String> projects = projectIds.get();
    List<String> mutations = mutationIds.get();
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { GENE, projects, mutations });

    val queries = generateQueries(filters, MUTATION_PROJECT_FILTER_TEMPLATE, mutations, projects);
    val counts = geneService.nestedCounts(queries);

    // Get total Gene count for each Project using all Mutations
    for (String projectId : projects) {
      filters = mergeFilters(filters, MUTATION_PROJECT_FILTER_TEMPLATE, JsonUtils.join(mutations), projectId);
      long uniqueCount = geneService.count(query().filters(filters).build());
      counts.get(projectId).put(TOTAL, uniqueCount);
    }

    return counts;
  }

  @Path("/history")
  @GET
  @Timed
  @ApiOperation(value = "Returns history of donor count of all projects at every release")
  public JsonNode getHistory() {
    try {
      return MAPPER.readTree(Resources.getResource(RELEASE_HISTORY_FILE_PATH));
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't read or parse release histroy data - file '"
          + RELEASE_HISTORY_FILE_PATH
          + "' might be corrupt or missing.", e);
    }
  }

}
