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

import org.icgc.dcc.portal.server.model.Donor;
import org.icgc.dcc.portal.server.model.Donors;
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
@Path("/v1/donors")
@Produces(APPLICATION_JSON)
@Api(value = "/donors", description = "Resources relating to " + DONOR)
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class DonorResource extends Resource {

  private static final String DONOR_FILTER_TEMPLATE = "{donor:{id:{is:['%s']}}}";
  private static final String MUTATION_DONOR_FILTER_TEMPLATE = "{mutation:{id:{is:['%s']}},donor:{id:{is:['%s']}}}";
  private static final String GENE_DONOR_FILTER_TEMPLATE = "{gene:{id:{is:['%s']}},donor:{id:{is:['%s']}}}";

  private final DonorService donorService;
  private final GeneService geneService;
  private final MutationService mutationService;

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
      @ApiParam(value = API_FACETS_ONLY_DESCRIPTION) @QueryParam(API_FACETS_ONLY_PARAM) @DefaultValue("false") boolean facetsOnly) {
    val filters = filtersParam.get();

    log.debug(FIND_ALL_TEMPLATE, new Object[] { size, DONOR, from, sort, order, filters });

    val query = query(fields, include, filters, from, size, sort, order);

    return donorService.findAllCentric(query, facetsOnly);
  }

  @GET
  @Timed
  @Path("/pql")
  @ApiOperation(value = RETURNS_LIST + DONOR + S, response = Donors.class)
  public Donors findPQL(
      @ApiParam(value = API_QUERY_VALUE) @QueryParam(API_QUERY_PARAM) @DefaultValue(DEFAULT_QUERY) String pql) {

    log.debug(PQL_TEMPLATE,  pql);

    return donorService.findAllCentric(pql, Collections.emptyList());
  }

  @Path("/count")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_COUNT + DONOR + S)
  public Long count(
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    ObjectNode filters = filtersParam.get();

    log.debug(COUNT_TEMPLATE, DONOR, filters);

    return donorService.count(query().filters(filters).build());
  }

  @Path("/{" + API_DONOR_PARAM + "}")
  @GET
  @Timed
  @ApiOperation(value = FIND_BY_ID, notes = FIND_BY_ID_ERROR, response = Donor.class)
  @ApiResponses(value = { @ApiResponse(code = 404, message = DONOR + NOT_FOUND) })
  public Donor find(
      @ApiParam(value = API_DONOR_VALUE, required = true) @PathParam(API_DONOR_PARAM) String donorId,
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include) {
    log.debug(FIND_ONE_TEMPLATE, donorId);

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

    log.debug(NESTED_FIND_TEMPLATE, GENE, donors);
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

    log.debug(NESTED_COUNT_TEMPLATE, GENE, donorId);
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

    log.debug(NESTED_COUNT_TEMPLATE, GENE, donors);
    val queries = queries(filters, DONOR_FILTER_TEMPLATE, donors);
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

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { MUTATION, donors, genes });
    val queries = queries(filters, GENE_DONOR_FILTER_TEMPLATE, genes, donors);
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

    log.debug(NESTED_COUNT_TEMPLATE, MUTATION, donors);
    val queries = queries(filters, DONOR_FILTER_TEMPLATE, donors);
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

    log.info(NESTED_NESTED_COUNT_TEMPLATE, new Object[] { GENE, donors, mutations });
    val queries = queries(filters, MUTATION_DONOR_FILTER_TEMPLATE, mutations, donors);
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
