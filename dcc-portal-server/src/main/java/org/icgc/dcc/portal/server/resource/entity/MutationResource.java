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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yammer.metrics.annotation.Timed;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.dcc.portal.server.model.Donors;
import org.icgc.dcc.portal.server.model.Genes;
import org.icgc.dcc.portal.server.model.Mutation;
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

import javax.ws.rs.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.icgc.dcc.portal.server.resource.Resources.*;

@Component
@Slf4j
@Path("/v1/mutations")
@Produces(APPLICATION_JSON)
@Api(value = "/mutations", description = "Resources relating to " + MUTATION)
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class MutationResource extends Resource {

  private static final String MUTATION_FILTER_TEMPLATE = "{mutation:{id:{is:['%s']}}}";
  private static final String GENE_MUTATION_FILTER_TEMPLATE = "{gene:{id:{is:['%s']}},mutation:{id:{is:['%s']}}}";
  private static final String PROJECT_MUTATION_FILTER_TEMPLATE =
    "{donor:{projectId:{is:['%s']}},mutation:{id:{is:['%s']}}}";

  private final MutationService mutationService;
  private final GeneService geneService;
  private final DonorService donorService;

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
    @ApiParam(value = API_FACETS_ONLY_DESCRIPTION) @QueryParam(API_FACETS_ONLY_PARAM) @DefaultValue("false") boolean facetsOnly) {
    val filters = filtersParam.get();

    log.debug(FIND_ALL_TEMPLATE, new Object[] { size, MUTATION, from, sort, order, filters });
    val query = query(fields, include, filters, from, size, sort, order);
    return mutationService.findAllCentric(query, facetsOnly);
  }

  @GET
  @Timed
  @Path("/pql")
  @ApiOperation(value = RETURNS_LIST + MUTATION + S, response = Mutation.class)
  public Mutations findPQL(
    @ApiParam(value = API_QUERY_VALUE) @QueryParam(API_QUERY_PARAM) @DefaultValue(DEFAULT_QUERY) String pql
  ) {

    log.debug(PQL_TEMPLATE, pql);

    return mutationService.findAllCentric(pql, Collections.emptyList());
  }

  @Path("/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + MUTATION + S)
  public Long count(
    @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.debug(COUNT_TEMPLATE, GENE, filters);
    return mutationService.count(query().filters(filters).build());
  }

  @Path("/{" + API_MUTATION_PARAM + "}")
  @GET
  @Timed
  @ApiOperation(value = FIND_BY_ID, notes = FIND_BY_ID_ERROR, response = Mutation.class)
  @ApiResponses(value = { @ApiResponse(code = 404, message = MUTATION + NOT_FOUND) })
  public Mutation find(
    @ApiParam(value = API_MUTATION_VALUE, required = true) @PathParam(API_MUTATION_PARAM) String mutationId,
    @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
    @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include) {
    log.info(FIND_ONE_TEMPLATE, mutationId);

    return mutationService.findOne(mutationId, query().fields(fields).includes(include).build());
  }

  @Path("/{" + API_MUTATION_PARAM + "}/donors")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + DONOR + S + FOR_THE + MUTATION + S, response = Donors.class)
  public Donors findDonors(
    @ApiParam(value = API_MUTATION_VALUE
      + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
    @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
    @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
    @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
    @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
    @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
    @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_DONOR_SORT) String sort,
    @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order) {
    ObjectNode filters = filtersParam.get();

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
    @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_COUNT_TEMPLATE, DONOR, mutationId);
    filters = mergeFilters(filters, MUTATION_FILTER_TEMPLATE, mutationId);
    return donorService.count(query().filters(filters).build());
  }

  @Path("/{" + API_MUTATION_PARAM + "}/donors/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + FOR_THE + MUTATION + S)
  public Map<String, Long> countDonors(
    @ApiParam(value = API_MUTATION_VALUE
      + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
    @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> mutations = mutationIds.get();

    log.info(NESTED_COUNT_TEMPLATE, DONOR, mutations);
    val queries = queries(filters, MUTATION_FILTER_TEMPLATE, mutations);
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
    @ApiParam(value = API_MUTATION_VALUE
      + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
    @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
    @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
    @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
    @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
    @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
    @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue(DEFAULT_GENE_MUTATION_SORT) String sort,
    @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order) {
    ObjectNode filters = filtersParam.get();
    List<String> mutations = mutationIds.get();

    log.debug(NESTED_FIND_TEMPLATE, GENE, mutations);
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
    @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.debug(NESTED_COUNT_TEMPLATE, GENE, mutationId);
    filters = mergeFilters(filters, MUTATION_FILTER_TEMPLATE, mutationId);
    return geneService.count(query().filters(filters).build());
  }

  @Path("/{" + API_MUTATION_PARAM + "}/genes/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + GENE + S + FOR_THE + MUTATION + S)
  public Map<String, Long> countGenes(
    @ApiParam(value = API_MUTATION_VALUE
      + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
    @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> mutations = mutationIds.get();

    log.info(NESTED_COUNT_TEMPLATE, GENE, mutations);
    val queries = queries(filters, MUTATION_FILTER_TEMPLATE, mutations);
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
    @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { DONOR, mutationId, geneId });
    filters = mergeFilters(filters, GENE_MUTATION_FILTER_TEMPLATE, geneId, mutationId);
    return donorService.count(query().filters(filters).build());
  }

  @Path("/{" + API_MUTATION_PARAM + "}/genes/{" + API_GENE_PARAM + "}/donors/counts")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S + GROUPED_BY + GENE + S + GROUPED_BY + MUTATION + S)
  public LinkedHashMap<String, LinkedHashMap<String, Long>> countsGeneDonors(
    @ApiParam(value = API_MUTATION_VALUE
      + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
    @ApiParam(value = API_GENE_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_GENE_PARAM) IdsParam geneIds,
    @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> mutations = mutationIds.get();
    List<String> genes = geneIds.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { DONOR, mutations, genes });
    val queries = queries(filters, GENE_MUTATION_FILTER_TEMPLATE, genes, mutations);
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
    @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

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
    @ApiParam(value = API_MUTATION_VALUE
      + MULTIPLE_IDS, required = true) @PathParam(API_MUTATION_PARAM) IdsParam mutationIds,
    @ApiParam(value = API_PROJECT_VALUE
      + MULTIPLE_IDS, required = true) @PathParam(API_PROJECT_PARAM) IdsParam projectIds,
    @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();
    List<String> projects = projectIds.get();
    List<String> mutations = mutationIds.get();

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { DONOR, mutations, projects });
    val queries = queries(filters, PROJECT_MUTATION_FILTER_TEMPLATE, projects, mutations);
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
