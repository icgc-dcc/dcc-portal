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
package org.icgc.dcc.portal.resource.core;

import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.Response.ok;
import static org.icgc.dcc.common.core.json.JsonNodeBuilders.array;
import static org.icgc.dcc.common.core.json.JsonNodeBuilders.object;
import static org.icgc.dcc.portal.resource.Resources.API_FILE_IDS_PARAM;
import static org.icgc.dcc.portal.resource.Resources.API_FILE_IDS_VALUE;
import static org.icgc.dcc.portal.resource.Resources.API_FILE_REPOS_PARAM;
import static org.icgc.dcc.portal.resource.Resources.API_FILE_REPOS_VALUE;
import static org.icgc.dcc.portal.resource.Resources.API_FILTER_PARAM;
import static org.icgc.dcc.portal.resource.Resources.API_FILTER_VALUE;
import static org.icgc.dcc.portal.util.MediaTypes.GZIP;
import static org.icgc.dcc.portal.util.MediaTypes.TEXT_TSV;

import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.resource.Resource;
import org.icgc.dcc.portal.service.ManifestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.core.header.ContentDisposition;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.metrics.annotation.Timed;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Path("/v1/manifests")
@Api(value = "/manifests", description = "Resources relating to manifests")
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class ManifestResource extends Resource {

  /**
   * Dependencies.
   */
  @NonNull
  private final ManifestService manifestService;

  @POST
  @Produces(GZIP)
  @Consumes(APPLICATION_FORM_URLENCODED)
  @Timed
  @ApiOperation(value = "Generate a tar.gz archive containing the manifests of selected repository files.")
  public Response generateManifestArchiveByFileIds(
      @ApiParam(value = API_FILE_IDS_VALUE) @FormParam(API_FILE_IDS_PARAM) List<String> fileIds,
      @ApiParam(value = API_FILE_REPOS_VALUE) @FormParam(API_FILE_REPOS_PARAM) String repoList) {
    log.info("fileIds: '{}', repoList: '{}'", fileIds, repoList);
    checkRequest(null == fileIds,
        "Field '%s' is required", API_FILE_IDS_PARAM);
    checkRequest(fileIds.isEmpty(),
        "Field '%s' must contain a list of repository file IDs", API_FILE_IDS_PARAM);

    val filtersParam = filtersParam(object("file", object("id", object("is", array().with(fileIds)))));
    return generateManifestArchiveByFilters(filtersParam, repoList);
  }

  @GET
  @Produces(GZIP)
  @Timed
  @ApiOperation(value = "Generate a tar.gz archive containing the manifests of matching repository files.")
  public Response generateManifestArchiveByFilters(
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FILE_REPOS_VALUE) @QueryParam(API_FILE_REPOS_PARAM) @DefaultValue("") String repoList) {
    log.info("filtersParam: '{}', repoList: '{}'", filtersParam, repoList);
    val timestamp = new Date();

    StreamingOutput entity = output -> manifestService.generateManifestArchive(
        output, timestamp, query(filtersParam), COMMA_SPLITTER.splitToList(repoList));

    return ok(entity)
        .header(CONTENT_DISPOSITION, attachmentContent(manifestArchiveFileName(timestamp), timestamp))
        .build();
  }

  @GET
  @Path("/{setId}")
  @Produces(TEXT_TSV)
  @ApiOperation(value = "Generates an ICGC Storage manifest from a given set id")
  public Response generateManifestFile(@ApiParam(value = "Set Id", required = true) @PathParam("setId") String setId) {
    log.info("setId: '{}'", setId);
    val timestamp = new Date();

    StreamingOutput entity = output -> manifestService.generateManifestFile(
        output, timestamp, setId);

    return ok(entity)
        .header(CONTENT_DISPOSITION, attachmentContent(manifestFileName(setId, timestamp), timestamp))
        .build();
  }

  private static String manifestFileName(String repoCode, Date timestamp) {
    return "manifest." + repoCode + "." + timestamp.getTime() + ".txt";
  }

  private static String manifestArchiveFileName(Date timestamp) {
    return "manifest." + timestamp.getTime() + ".tar.gz";
  }

  private static ContentDisposition attachmentContent(String fileName, Date timestamp) {
    return ContentDisposition.type("attachment")
        .fileName(fileName)
        .creationDate(timestamp)
        .modificationDate(timestamp)
        .readDate(timestamp)
        .build();
  }

}
