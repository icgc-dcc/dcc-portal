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

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.emptyList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.portal.server.resource.Resources.API_FIELD_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_FIELD_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_FILTER_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_FILTER_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_FROM_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_FROM_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_GENE_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_GENE_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_INCLUDE_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_INCLUDE_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_ORDER_ALLOW;
import static org.icgc.dcc.portal.server.resource.Resources.API_ORDER_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_ORDER_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_SIZE_ALLOW;
import static org.icgc.dcc.portal.server.resource.Resources.API_SIZE_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_SIZE_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_SORT_FIELD;
import static org.icgc.dcc.portal.server.resource.Resources.API_SORT_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.DONOR;
import static org.icgc.dcc.portal.server.resource.Resources.FOR_THE;
import static org.icgc.dcc.portal.server.resource.Resources.GENE;
import static org.icgc.dcc.portal.server.resource.Resources.MULTIPLE_IDS;
import static org.icgc.dcc.portal.server.resource.Resources.RETURNS_LIST;
import static org.icgc.dcc.portal.server.resource.Resources.S;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.icgc.dcc.portal.server.model.Drug;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.model.param.IdsParam;
import org.icgc.dcc.portal.server.model.param.IntParam;
import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.service.DrugService;
import org.icgc.dcc.portal.server.service.MutationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.yammer.metrics.annotation.Timed;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * End-points of drug index
 */
@Component
@Slf4j
@Path("/v1/drugs")
@Produces(APPLICATION_JSON)
@Api(value = "/drugs", description = "Resources relating to drugs")
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class DrugResource extends Resource {

  /**
   * Constants.
   */
  private static final String GENE_FILTER_TEMPLATE = "{gene:{id:{is:['%s']}}}";

  /**
   * Dependencies.
   */
  private final DrugService drugService;
  private final MutationService mutationService;

  @Path("/{drugId}")
  @GET
  @Timed
  @ApiOperation(value = "Find a drug by ID", response = Drug.class)
  public Drug findOneDrug(
      @ApiParam(value = "Drug ID", required = true) @PathParam("drugId") String drugId) {
    return drugService.findOne(drugId);
  }

  @Path("/{drugId}/genes/mutations/counts")
  @GET
  @Timed
  @ApiOperation(value = "Find most frequently mutated genes targeted by a drug", response = List.class)
  public List<Map<String, Object>> topMutatedGenes(
      @ApiParam(value = "Drug ID", required = true) @PathParam("drugId") String drugId,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order) {
    val drug = drugService.findOne(drugId);
    val geneIds = drug.getGenes().stream()
        .filter(gene -> !isNullOrEmpty(gene.getEnsemblGeneId()))
        .map(gene -> gene.getEnsemblGeneId())
        .distinct()
        .collect(toImmutableList());

    val queryMap = new LinkedHashMap<String, Query>();
    for (val gene : geneIds) {
      val mergedFilter = mergeFilters(filtersParam.get(), GENE_FILTER_TEMPLATE, gene);
      val query =
          query(emptyList(), emptyList(), mergedFilter, new IntParam(DEFAULT_FROM), size, "", order);
      queryMap.put(gene, query);
    }

    return geneIds.isEmpty() ? emptyList() : mutationService.counts(geneIds, queryMap, size.get(),
        order.equals("desc"));
  }

  @Path("/genes/{" + API_GENE_PARAM + "}")
  @GET
  @Timed
  @ApiOperation(value = RETURNS_LIST + "compound" + S + FOR_THE + GENE + S, response = List.class)
  public List<Drug> findDrugsByGeneIds(
      @ApiParam(value = API_GENE_VALUE + MULTIPLE_IDS, required = true) @PathParam(API_GENE_PARAM) IdsParam geneIds,
      @ApiParam(value = API_SIZE_VALUE, required = false) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size) {
    return drugService.findDrugsByGeneIds(geneIds.get(), size.get());
  }

  @GET
  @Timed
  @ApiOperation(value = "Find drugs via query filters", response = List.class)
  public List<Drug> findDrugs(
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue("") String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order) {
    val filters = filtersParam.get();

    log.debug(FIND_ALL_TEMPLATE, new Object[] { size, DONOR, from, sort, order, filters });

    val query = query(fields, include, filters, from, size, sort, order);

    return drugService.findAll(query);
  }

}
