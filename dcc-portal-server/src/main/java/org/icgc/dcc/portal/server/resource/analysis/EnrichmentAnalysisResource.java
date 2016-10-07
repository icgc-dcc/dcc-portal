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
package org.icgc.dcc.portal.server.resource.analysis;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.sun.jersey.core.header.ContentDisposition.type;
import static java.lang.Integer.parseInt;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.icgc.dcc.portal.server.resource.Resources.API_ANALYSIS_ID_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_ANALYSIS_ID_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_FILTER_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_FILTER_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_ORDER_ALLOW;
import static org.icgc.dcc.portal.server.resource.Resources.API_ORDER_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_ORDER_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_PARAMS_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_PARAMS_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_SORT_FIELD;
import static org.icgc.dcc.portal.server.resource.Resources.API_SORT_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.ORDER_VALUES;
import static org.icgc.dcc.portal.server.util.MediaTypes.TEXT_TSV;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.icgc.dcc.portal.server.model.EnrichmentAnalysis;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.param.EnrichmentParamsParam;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.service.EnrichmentAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Api("/analysis")
@Path("/v1/analysis/enrichment")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EnrichmentAnalysisResource extends Resource {

  /**
   * Constants
   */
  private static final Set<String> GENES_SORT_FIELD_NAMES = ImmutableSet.of("symbol", "name", "start", "type",
      "affectedDonorCountFiltered");

  /**
   * Dependencies
   */
  @NonNull
  private final EnrichmentAnalysisService service;

  @GET
  @Path("/{" + API_ANALYSIS_ID_PARAM + "}")
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieves an enrichment analysis by id", response = EnrichmentAnalysis.class)
  public EnrichmentAnalysis getAnalysis(
      @ApiParam(value = API_ANALYSIS_ID_VALUE, required = true) @PathParam(API_ANALYSIS_ID_PARAM) UUID analysisId) {
    // Validate
    validateAnalysisId(analysisId);

    log.info("Getting analysis with id '{}'...", analysisId);
    return service.getAnalysis(analysisId);
  }

  @POST
  @Consumes(APPLICATION_FORM_URLENCODED)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Submits an asynchronous enrichment analysis request. Users must poll the status using the GET resource", response = EnrichmentAnalysis.class)
  public Response submitAnalysis(
      @ApiParam(value = API_PARAMS_VALUE) @FormParam(API_PARAMS_PARAM) EnrichmentParamsParam paramsParam,
      @ApiParam(value = API_FILTER_VALUE) @FormParam(API_FILTER_PARAM) FiltersParam filtersParam,
      @ApiParam(value = API_SORT_VALUE) @FormParam(API_SORT_FIELD) String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @FormParam(API_ORDER_PARAM) String order) {

    // Validate
    checkRequest(paramsParam == null, "'params' empty");
    checkRequest(filtersParam == null, "'filters' empty");
    checkRequest(isNullOrEmpty(sort), "'sort' is missing or empty");
    checkRequest(!GENES_SORT_FIELD_NAMES.contains(sort), "'sort' must be one of " + GENES_SORT_FIELD_NAMES);
    checkRequest(isNullOrEmpty(order), "'order' is missing or empty");
    checkRequest(!ORDER_VALUES.contains(order.toLowerCase()), "'order' must be one of " + ORDER_VALUES);

    // JSR 303 resource parameters are not supported in DW 1
    validate(paramsParam.get());

    // Construct
    val analysis = new EnrichmentAnalysis()
        .setQuery(Query.builder()
            .filters(filtersParam.get())
            .sort(sort)
            .order(order)
            .size(parseInt(DEFAULT_SIZE))
            .build())
        .setParams(paramsParam.get());

    // Submit
    log.info("Submitting analysis '{}'...", analysis);
    service.submitAnalysis(analysis);

    // In the RFC sense for asynchronous tasks
    return Response.status(ACCEPTED).entity(analysis).build();
  }

  @GET
  @Path("/{" + API_ANALYSIS_ID_PARAM + "}")
  @Produces(TEXT_TSV)
  @ApiOperation(value = "Retrieves an enrichment analysis by id", response = EnrichmentAnalysis.class)
  public Response reportAnalysis(
      @ApiParam(value = API_ANALYSIS_ID_VALUE, required = true) @PathParam(API_ANALYSIS_ID_PARAM) final UUID analysisId) {
    val analysis = getAnalysis(analysisId);

    log.info("Generating analysis report with id '{}'...", analysisId);
    return Response
        .ok(new StreamingOutput() {

          @Override
          public void write(OutputStream outputStream) throws IOException, WebApplicationException {
            service.reportAnalysis(analysis, outputStream);
          }

        })
        .header(CONTENT_DISPOSITION, type("attachment")
            .fileName("analysis-" + analysisId + ".tsv")
            .creationDate(new Date())
            .build())
        .build();
  }

  private static void validateAnalysisId(UUID analysisId) {
    checkRequest(analysisId == null, "'analysisId' is empty");
  }

}
