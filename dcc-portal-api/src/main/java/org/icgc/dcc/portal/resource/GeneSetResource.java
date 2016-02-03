/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FILTER_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_FILTER_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_GENE_SET_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_GENE_SET_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_FILTERS;
import static org.icgc.dcc.portal.resource.ResourceUtils.TOTAL;
import static org.icgc.dcc.portal.resource.ResourceUtils.generateQueries;
import static org.icgc.dcc.portal.resource.ResourceUtils.mergeFilters;
import static org.icgc.dcc.portal.resource.ResourceUtils.query;

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

import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.GeneSet;
import org.icgc.dcc.portal.model.IdsParam;
import org.icgc.dcc.portal.service.GeneService;
import org.icgc.dcc.portal.service.GeneSetService;
import org.icgc.dcc.portal.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import com.yammer.metrics.annotation.Timed;

@Component
@Slf4j
@Path("/v1/genesets")
@Produces(APPLICATION_JSON)
@Api(value = "/genesets", description = "Resources relating to gene sets")
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class GeneSetResource {

  private final GeneSetService geneSetService;
  private final GeneService geneService;

  @Path("/{" + API_GENE_SET_PARAM + "}/genes/counts")
  @GET
  @Timed
  @ApiOperation(value = "Find number of genes associated with each gene set")
  public Map<String, Long> countGenes(
      @ApiParam(value = API_GENE_SET_VALUE, required = true) @PathParam(API_GENE_SET_PARAM) IdsParam geneSetIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam
      ) {

    ObjectNode filters = filtersParam.get();
    val geneSetIdFilter = "{gene:{geneSetId:{is:['%s']}}}";

    List<String> geneSets = geneSetIds.get();

    val queries = generateQueries(filters, geneSetIdFilter, geneSets);
    val counts = geneService.counts(queries);

    filters = mergeFilters(filters, geneSetIdFilter, JsonUtils.join(geneSets));
    long uniqueCount = geneService.count(query().filters(filters).build());

    counts.put(TOTAL, uniqueCount);

    return counts;
  }

  @Path("/{Id}")
  @GET
  @Timed
  @ApiOperation(value = "Find a gene set by id", notes = "If a gene set does not exist with the specified id an error will be returned", response = GeneSet.class)
  @ApiResponses(value = { @ApiResponse(code = NOT_FOUND_404, message = "GeneSet not found") })
  public GeneSet find(
      @ApiParam(value = "GeneSet ID", required = true) @PathParam("Id") String id,
      @ApiParam(value = "Select fields returned", allowMultiple = true) @QueryParam("field") List<String> fields
      ) {
    log.info("Request for gene set'{}'", id);

    GeneSet geneSet = geneSetService.findOne(id, fields);

    log.info("Returning '{}'", geneSet);

    return geneSet;
  }
}
