/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.server.resource.entity;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.icgc.dcc.portal.server.resource.Resources.*;
import static org.icgc.dcc.portal.server.resource.Resources.API_QUERY_PARAM;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.icgc.dcc.portal.server.model.Donors;
import org.icgc.dcc.portal.server.model.Gene;
import org.icgc.dcc.portal.server.model.Genes;
import org.icgc.dcc.portal.server.model.Mutations;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.model.param.IdsParam;
import org.icgc.dcc.portal.server.model.param.IntParam;
import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.service.DonorService;
import org.icgc.dcc.portal.server.service.GeneService;
import org.icgc.dcc.portal.server.service.MutationService;
import org.icgc.dcc.portal.server.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.yammer.metrics.annotation.Timed;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Path("/v1/genes")
@Produces(APPLICATION_JSON)
@Api(value = "/genes", description = "Resources relating to " + GENE)
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class GeneResource extends Resource {

  private static final String GENE_FILTER_TEMPLATE = "{gene:{id:{is:['%s']}}}";
  private static final String MUTATION_GENE_FILTER_TEMPLATE = "{mutation:{id:{is:['%s']}},gene:{id:{is:['%s']}}}";
  private static final String DONOR_GENE_FILTER_TEMPLATE = "{donor:{id:{is:['%s']}},gene:{id:{is:['%s']}}}";
  private static final String PROJECT_GENE_FILTER_TEMPLATE = "{donor:{projectId:{is:['%s']}},gene:{id:{is:['%s']}}}";

  private final GeneService geneService;
  private final DonorService donorService;
  private final MutationService mutationService;

  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + GENE + S, response = Genes.class)
  public Genes findAll(
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_GENE_MUTATION_SORT) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order,
      @ApiParam(value = API_FACETS_ONLY_DESCRIPTION) @QueryParam(API_FACETS_ONLY_PARAM) @DefaultValue("false") boolean facetsOnly) {
    val filters = filtersParam.get();

    log.debug(FIND_ALL_TEMPLATE, new Object[] { size, GENE, from, sort, order, filters });

    val query = query(fields, include, filters, from, size, sort, order);

    return geneService.findAllCentric(query, facetsOnly);
  }

  @GET
  @Timed
  @Path("/pql")
  @ApiOperation(value = RETURNS_LIST + GENE + S, response = Genes.class)
  public Genes findPQL(
      @ApiParam(value = API_QUERY_VALUE) @QueryParam(API_QUERY_PARAM) @DefaultValue(DEFAULT_QUERY) String pql
  ) {

    log.debug(PQL_TEMPLATE,  pql);

    return geneService.findAllCentric(pql, Collections.emptyList());
  }

  @Path("/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + GENE + S)
  public Long count(
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filters) {
    log.info(COUNT_TEMPLATE, GENE, filters.get());

    return geneService.count(query().filters(filters.get()).build());
  }

  @Path("/{" + API_GENE_PARAM + "}")
  @GET
  @Timed
  @ApiOperation(value = FIND_BY_ID, notes = FIND_BY_ID_ERROR, response = Gene.class)
  @ApiResponses(value = { @ApiResponse(code = 404, message = GENE + NOT_FOUND) })
  public Gene find(
      @ApiParam(value = API_GENE_VALUE, required = true) @PathParam(API_GENE_PARAM) String geneId,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include) {
    log.info(FIND_ONE_TEMPLATE, geneId);

    return geneService.findOne(geneId, query().fields(fields).includes(include).build());
  }

  @Path("/{" + API_GENE_PARAM + "}/donors")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + DONOR + S + FOR_THE + GENE + S, response = Donors.class)
  public Donors findDonors(
      @ApiParam(value = API_GENE_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_GENE_PARAM) IdsParam geneIds,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_DONOR_SORT) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order) {
    ObjectNode filters = filtersParam.get();

    log.debug(NESTED_FIND_TEMPLATE, DONOR, geneIds.get());

    filters = mergeFilters(filters, GENE_FILTER_TEMPLATE, JsonUtils.join(geneIds.get()));

    return donorService.findAllCentric(query().filters(filters).fields(fields).includes(include)
        .from(from.get()).size(size.get()).sort(sort).order(order).build());
  }

  @Path("/{" + API_GENE_PARAM + "}/donors/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + FOR_THE + GENE)
  public Long countDonors(
      @ApiParam(value = API_GENE_VALUE, required = true) @PathParam(API_GENE_PARAM) String geneId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_COUNT_TEMPLATE, DONOR, geneId);
    filters = mergeFilters(filters, GENE_FILTER_TEMPLATE, geneId);
    return donorService.count(query().filters(filters).build());
  }

  @Path("/{" + API_GENE_PARAM + "}/donors/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + FOR_THE + GENE + S)
  public Map<String, Long> countDonors(
      @ApiParam(value = API_GENE_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_GENE_PARAM) IdsParam geneIds,
      @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> genes = geneIds.get();

    log.info(NESTED_COUNT_TEMPLATE, DONOR, genes);

    val queries = queries(filters, GENE_FILTER_TEMPLATE, genes);
    val counts = donorService.counts(queries);

    // Get total Donor count using all Genes
    filters = mergeFilters(filters, GENE_FILTER_TEMPLATE, JsonUtils.join(genes));
    long uniqueCount = donorService.count(query().filters(filters).build());
    counts.put(TOTAL, uniqueCount);

    return counts;
  }

  @Path("/{" + API_GENE_PARAM + "}/donors/{" + API_DONOR_PARAM + "}/mutations/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + FOR_THE + GENE + AFFECTED_BY_THE + DONOR)
  public Long countDonorMutations(
      @ApiParam(value = API_GENE_VALUE, required = true) @PathParam(API_GENE_PARAM) String geneId,
      @ApiParam(value = API_DONOR_VALUE, required = true) @PathParam(API_DONOR_PARAM) String donorId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { MUTATION, geneId, donorId });
    filters = mergeFilters(filters, DONOR_GENE_FILTER_TEMPLATE, donorId, geneId);
    return mutationService.count(query().filters(filters).build());
  }

  @Path("/{" + API_GENE_PARAM + "}/donors/{" + API_DONOR_PARAM + "}/mutations/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + GROUPED_BY + DONOR + S + GROUPED_BY + GENE + S)
  public Map<String, LinkedHashMap<String, Long>> countsDonorMutations(
      @ApiParam(value = API_GENE_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_GENE_PARAM) IdsParam geneIds,
      @ApiParam(value = API_DONOR_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_DONOR_PARAM) IdsParam donorIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> genes = geneIds.get();
    List<String> donors = donorIds.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { MUTATION, genes, donors });

    val queries = queries(filters, DONOR_GENE_FILTER_TEMPLATE, donors, genes);
    val counts = mutationService.nestedCounts(queries);

    // Get total Mutation count for each Gene using all Donors
    for (String geneId : genes) {
      filters = mergeFilters(filters, DONOR_GENE_FILTER_TEMPLATE, JsonUtils.join(donors), geneId);
      long uniqueCount = mutationService.count(query().filters(filters).build());
      counts.get(geneId).put(TOTAL, uniqueCount);
    }

    return counts;
  }

  @Path("/{" + API_GENE_PARAM + "}/mutations")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + MUTATION + S + FOR_THE + GENE + S, response = Mutations.class)
  public Mutations findMutations(
      @ApiParam(value = API_GENE_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_GENE_PARAM) IdsParam geneIds,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_GENE_MUTATION_SORT) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_FIND_TEMPLATE, MUTATION, geneIds.get());

    filters = mergeFilters(filters, GENE_FILTER_TEMPLATE, JsonUtils.join(geneIds.get()));

    return mutationService.findAllCentric(query().filters(filters).fields(fields).includes(include)
        .from(from.get()).size(size.get()).sort(sort).order(order).build());
  }

  @Path("/{" + API_GENE_PARAM + "}/mutations/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + FOR_THE + GENE)
  public Long countMutations(
      @ApiParam(value = API_GENE_VALUE, required = true) @PathParam(API_GENE_PARAM) String geneId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_COUNT_TEMPLATE, MUTATION, geneId);

    filters = mergeFilters(filters, GENE_FILTER_TEMPLATE, geneId);

    return mutationService.count(query().filters(filters).build());
  }

  @Path("/{" + API_GENE_PARAM + "}/mutations/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + FOR_THE + GENE + S)
  public Map<String, Long> countMutations(
      @ApiParam(value = API_GENE_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_GENE_PARAM) IdsParam geneIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> genes = geneIds.get();

    log.info(NESTED_COUNT_TEMPLATE, MUTATION, genes);

    val queries = queries(filters, GENE_FILTER_TEMPLATE, genes);
    val counts = mutationService.counts(queries);

    // Get total Mutation count using all Genes
    filters = mergeFilters(filters, GENE_FILTER_TEMPLATE, JsonUtils.join(genes));
    long uniqueCount = mutationService.count(query().filters(filters).build());

    counts.put(TOTAL, uniqueCount);

    return counts;
  }

  @Path("/{" + API_GENE_PARAM + "}/mutations/{" + API_MUTATION_PARAM + "}/donors/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + FOR_THE + GENE + AFFECTED_BY_THE + MUTATION)
  public Long countMutationDonors(
      @ApiParam(value = API_GENE_VALUE, required = true) @PathParam(API_GENE_PARAM) String geneId,
      @ApiParam(value = API_MUTATION_VALUE, required = true) @PathParam(API_MUTATION_PARAM) String mutationId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { DONOR, geneId, mutationId });
    filters = mergeFilters(filters, MUTATION_GENE_FILTER_TEMPLATE, mutationId, geneId);
    return geneService.count(query().filters(filters).build());
  }

  @Path("/{" + API_GENE_PARAM + "}/mutations/{" + API_MUTATION_PARAM + "}/donors/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + GROUPED_BY + MUTATION + S + GROUPED_BY + GENE + S)
  public LinkedHashMap<String, LinkedHashMap<String, Long>> countMutationDonors(
      @ApiParam(value = API_GENE_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_GENE_PARAM) IdsParam geneIds,
      @ApiParam(value = API_MUTATION_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> genes = geneIds.get();
    List<String> mutations = mutationIds.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { DONOR, genes, mutations });

    val queries = queries(filters, MUTATION_GENE_FILTER_TEMPLATE, mutations, genes);
    val counts = donorService.nestedCounts(queries);

    // Get total Donor count for each Gene using all Mutations
    for (String geneId : genes) {
      filters = mergeFilters(filters, MUTATION_GENE_FILTER_TEMPLATE, JsonUtils.join(mutations), geneId);
      long uniqueCount = donorService.count(query().filters(filters).build());
      counts.get(geneId).put(TOTAL, uniqueCount);
    }

    return counts;
  }

  @Path("/{" + API_GENE_PARAM + "}/projects/{" + API_PROJECT_PARAM + "}/mutations/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + FOR_THE + GENE + FOR_THE + PROJECT)
  public Long countProjectMutations(
      @ApiParam(value = API_GENE_VALUE, required = true) @PathParam(API_GENE_PARAM) String geneId,
      @ApiParam(value = API_PROJECT_VALUE, required = true) @PathParam(API_PROJECT_PARAM) String projectId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { MUTATION, geneId, projectId });
    filters = mergeFilters(filters, PROJECT_GENE_FILTER_TEMPLATE, geneId, projectId);
    return mutationService.count(query().filters(filters).build());
  }

  @Path("/{" + API_GENE_PARAM + "}/projects/{" + API_PROJECT_PARAM + "}/mutations/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S + GROUPED_BY + PROJECT + S + GROUPED_BY + GENE + S)
  public Map<String, LinkedHashMap<String, Long>> countsProjectMutations(
      @ApiParam(value = API_GENE_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_GENE_PARAM) IdsParam geneIds,
      @ApiParam(value = API_PROJECT_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> genes = geneIds.get();
    List<String> projects = projectIds.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { MUTATION, genes, projects });
    val queries = queries(filters, PROJECT_GENE_FILTER_TEMPLATE, projects, genes);
    val counts = mutationService.nestedCounts(queries);

    // Get total Mutation count for each Gene using all Projects
    for (String geneId : genes) {
      filters = mergeFilters(filters, PROJECT_GENE_FILTER_TEMPLATE, JsonUtils.join(projects), geneId);
      long uniqueCount = mutationService.count(query().filters(filters).build());
      counts.get(geneId).put(TOTAL, uniqueCount);
    }

    return counts;
  }

  @Path("/{" + API_GENE_PARAM + "}/projects/{" + API_PROJECT_PARAM + "}/donors/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + FOR_THE + GENE + FOR_THE + PROJECT)
  public Long countProjectDonors(
      @ApiParam(value = API_GENE_VALUE, required = true) @PathParam(API_GENE_PARAM) String geneId,
      @ApiParam(value = API_PROJECT_VALUE, required = true) @PathParam(API_PROJECT_PARAM) String projectId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { DONOR, geneId, projectId });
    filters = mergeFilters(filters, PROJECT_GENE_FILTER_TEMPLATE, geneId, projectId);
    return donorService.count(query().filters(filters).build());
  }

  @Path("/{" + API_GENE_PARAM + "}/projects/{" + API_PROJECT_PARAM + "}/donors/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + GROUPED_BY + PROJECT + S + GROUPED_BY + GENE + S)
  public LinkedHashMap<String, LinkedHashMap<String, Long>> countsProjectDonors(
      @ApiParam(value = API_GENE_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_GENE_PARAM) IdsParam geneIds,
      @ApiParam(value = API_PROJECT_VALUE
          + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> genes = geneIds.get();
    List<String> projects = projectIds.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { DONOR, genes, projects });
    val queries = queries(filters, PROJECT_GENE_FILTER_TEMPLATE, projects, genes);
    val counts = donorService.nestedCounts(queries);

    // Get total Donor count for each Gene using all Projects
    for (String geneId : genes) {
      filters = mergeFilters(filters, PROJECT_GENE_FILTER_TEMPLATE, JsonUtils.join(projects), geneId);
      long uniqueCount = donorService.count(query().filters(filters).build());
      counts.get(geneId).put(TOTAL, uniqueCount);
    }

    return counts;
  }

  @Path("/{" + API_GENE_PARAM + "}/affected-transcripts")
  @GET
  public Map<String, List<String>> getAffectedTranscripts(
      @ApiParam(value = API_GENE_VALUE, required = true) @PathParam(API_GENE_PARAM) String geneId) {
    return ImmutableMap.<String, List<String>> of(geneId, geneService.getAffectedTranscripts(geneId));
  }

}
