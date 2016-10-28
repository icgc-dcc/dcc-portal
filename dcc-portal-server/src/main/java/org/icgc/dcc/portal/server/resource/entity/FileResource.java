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

import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.sun.jersey.core.header.ContentDisposition.type;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.ok;
import static org.icgc.dcc.portal.server.resource.Resources.API_FIELD_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_FIELD_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_FILTER_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_FILTER_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_FROM_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_FROM_VALUE;
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
import static org.icgc.dcc.portal.server.util.MediaTypes.TEXT_TSV;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

import org.icgc.dcc.portal.server.model.File;
import org.icgc.dcc.portal.server.model.Files;
import org.icgc.dcc.portal.server.model.UniqueSummaryQuery;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.model.param.IntParam;
import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.resource.core.ManifestResource;
import org.icgc.dcc.portal.server.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.yammer.metrics.annotation.Timed;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Api(value = "/repository", description = "Resources relating to external files")
@Path("/v1/repository/files")
@Produces(APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class FileResource extends Resource {

  /**
   * Constants.
   */
  private static final String TYPE_ATTACHMENT = "attachment";

  /**
   * Dependencies.
   */
  @NonNull
  private final FileService fileService;

  @Path("/manifest/{manifestId}")
  @GET
  public Response legacyManifest(
      @ApiParam(value = "Manifest ID", required = true) @PathParam("manifestId") String manifestId) {
    // For backwards compatibility for use with ICGC Storage Client
    val uri = UriBuilder.fromResource(ManifestResource.class).path(manifestId).build();
    return Response.seeOther(uri).build();
  }

  @Path("/{fileId}")
  @GET
  @Timed
  @ApiOperation(value = "Find by fileId", response = File.class)
  public File find(
      @ApiParam(value = "File Id", required = true) @PathParam("fileId") String id) {
    return fileService.findOne(id);
  }

  @GET
  @Timed
  @ApiOperation(value = "Returns a list of Files", response = File.class)
  public Files findAll(
      @ApiParam(value = API_FIELD_VALUE, allowMultiple = true) @QueryParam(API_FIELD_PARAM) List<String> fields,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_INCLUDE_VALUE, allowMultiple = true) @QueryParam(API_INCLUDE_PARAM) List<String> include,
      @ApiParam(value = API_FROM_VALUE) @QueryParam(API_FROM_PARAM) @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = API_SIZE_VALUE, allowableValues = API_SIZE_ALLOW) @QueryParam(API_SIZE_PARAM) @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = API_SORT_VALUE) @QueryParam(API_SORT_FIELD) @DefaultValue("id") String sort,
      @ApiParam(value = API_ORDER_VALUE, allowableValues = API_ORDER_ALLOW) @QueryParam(API_ORDER_PARAM) @DefaultValue(DEFAULT_ORDER) String order) {

    val filters = filtersParam.get();
    log.debug("Received filters: '{}'", filters);

    return fileService.findAll(query().fields(fields).filters(filters)
        .from(from.get())
        .size(size.get())
        .sort(sort)
        .order(order)
        .includes(include)
        .build());
  }

  @GET
  @Timed
  @Path("/donors/count")
  @ApiOperation(value = "Counts the number of donors matching the filter.", response = Long.class)
  public Long getUniqueDonorCount(
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    return fileService.getDonorCount(query(filtersParam));
  }

  @GET
  @Timed
  @Path("/summary")
  public Map<String, Long> getSummary(
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    return fileService.getSummary(query().filters(filtersParam.get()).build());
  }

  @GET
  @Timed
  @Path("/export")
  @Produces(TEXT_TSV)
  @ApiOperation(value = "Exports repository file listings to a TSV file.", response = File.class)
  public Response getExport(
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {

    final StreamingOutput outputGenerator =
        outputStream -> fileService.exportFiles(outputStream, query(filtersParam));

    // Make this similar to client-side export naming format
    val fileName = String.format("repository_%s.tsv", (new SimpleDateFormat("yyyy_MM_dd").format(new Date())));

    return ok(outputGenerator).header(CONTENT_DISPOSITION,
        type(TYPE_ATTACHMENT).fileName(fileName).creationDate(new Date()).build()).build();
  }

  @GET
  @Path("/export/{setId}")
  @Produces(TEXT_TSV)
  public Response getExport(@ApiParam(value = "Set Id", required = true) @PathParam("setId") String setId) {

    final StreamingOutput outputGenerator =
        outputStream -> fileService.exportFiles(outputStream, setId);

    val fileName = String.format("repository_%s.tsv", (new SimpleDateFormat("yyyy_MM_dd").format(new Date())));

    return ok(outputGenerator).header(CONTENT_DISPOSITION,
        type(TYPE_ATTACHMENT).fileName(fileName).creationDate(new Date()).build()).build();
  }

  @GET
  @Path("/pcawg/stats")
  @Timed
  @ApiOperation(value = "Get pancancer repositories statistics")
  public Map<String, Map<String, Map<String, Object>>> getPancancerStats() {
    return fileService.getStudyStats("PCAWG");
  }

  @GET
  @Path("/study/stats/{study}")
  @Timed
  @ApiOperation(value = "Get pancancer repositories statistics")
  public Map<String, Map<String, Map<String, Object>>> getStudyStats(
      @NonNull @ApiParam(value = "Study Name") @PathParam("study") String study) {
    return fileService.getStudyStats(study);
  }

  @GET
  @Path("/repo/stats/{repoCode}")
  @Timed
  @ApiOperation(value = "Get pancancer repositories statistics")
  public Map<String, Map<String, Map<String, Object>>> getRepoStats(
      @NonNull @ApiParam(value = "Repository Code") @PathParam("repoCode") String repoCode) {
    return fileService.getRepoStats(repoCode);
  }

  @GET
  @Path("/metadata")
  @Timed
  public Map<String, String> getIndexMetaData() {
    return fileService.getIndexMetadata();
  }

  @POST
  @Path("/summary/manifest")
  @Timed
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public Map<String, Map<String, Long>> getManifestSummary(UniqueSummaryQuery summary) {
    checkRequest(summary == null, "Request body cannot be null or empty.");
    checkRequest(summary.getQuery() == null, "Query field in request body cannot be null or empty.");
    return fileService.getUniqueFileAggregations(summary);
  }

}