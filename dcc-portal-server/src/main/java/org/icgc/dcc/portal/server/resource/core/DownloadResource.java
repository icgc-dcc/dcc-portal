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
package org.icgc.dcc.portal.server.resource.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.net.HttpHeaders.RANGE;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.download.core.request.Redirects.getDynamicFileRedirect;
import static org.icgc.dcc.download.core.request.Redirects.getStaticFileRedirect;
import static org.icgc.dcc.portal.server.download.DownloadResources.createGetDataSizeResponse;
import static org.icgc.dcc.portal.server.download.DownloadResources.getAllowedDataTypeSizes;
import static org.icgc.dcc.portal.server.util.JsonUtils.parseDownloadDataTypeNames;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.icgc.dcc.common.core.model.DownloadDataType;
import org.icgc.dcc.download.client.DownloadClient;
import org.icgc.dcc.download.core.jwt.JwtService;
import org.icgc.dcc.download.core.model.JobUiInfo;
import org.icgc.dcc.download.core.response.JobResponse;
import org.icgc.dcc.portal.server.config.ServerProperties.DownloadProperties;
import org.icgc.dcc.portal.server.download.DownloadDataTypes;
import org.icgc.dcc.portal.server.download.DownloadResources;
import org.icgc.dcc.portal.server.download.JobInfo;
import org.icgc.dcc.portal.server.download.ServiceStatus;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.User;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.model.param.SetParam;
import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.security.jersey.Auth;
import org.icgc.dcc.portal.server.service.DonorService;
import org.icgc.dcc.portal.server.service.ForbiddenAccessException;
import org.icgc.dcc.portal.server.service.NotFoundException;
import org.icgc.dcc.portal.server.service.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import com.yammer.metrics.annotation.Timed;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Api(value = "/download", description = "Resources relating to archive downloading")
@Path("/v1/download")
@Consumes(APPLICATION_JSON)
public class DownloadResource extends Resource {

  public static final String ANONYMOUS_USER = "anon";

  /**
   * Dependencies.
   */
  private final DonorService donorService;
  private final DownloadClient downloadClient;

  // Download server URL used for the internal communication. E.g. list static files.
  private final String serverUrl;

  // Used for creation of redirect requests
  private final String publicServerUrl;
  private final JwtService tokenService;

  @Autowired
  public DownloadResource(DonorService donorService, DownloadClient downloadClient, DownloadProperties properties,
      JwtService tokenService) {
    this.donorService = donorService;
    this.downloadClient = downloadClient;
    this.serverUrl = properties.getServerUrl();
    this.tokenService = tokenService;
    checkArgument(!isNullOrEmpty(serverUrl), "The download server URL is undefined.");

    val publicServerUrl = properties.getPublicServerUrl();
    this.publicServerUrl = isNullOrEmpty(publicServerUrl) ? this.serverUrl : publicServerUrl;
  }

  @GET
  @Timed
  @Produces(APPLICATION_JSON)
  @Path("/status")
  @ApiOperation("Get download service availability")
  public ServiceStatus getServiceStatus() {
    return new ServiceStatus(downloadClient.isServiceAvailable());
  }

  @GET
  @Timed
  @Path("/submit")
  @Produces(APPLICATION_JSON)
  @ApiOperation("Submit job to request archive generation")
  public JobInfo submitJob(
      @Auth(required = false) User user,
      @ApiParam(value = "Filter the search results", required = false) @QueryParam("filters") @DefaultValue("{}") FiltersParam filters,
      @ApiParam(value = "Archive param") @QueryParam("info") @DefaultValue("") String info,
      @ApiParam(value = "UI representation of the filter string", required = false, access = "internal") @QueryParam("uiQueryStr") @DefaultValue("{}") String uiQueryStr) {
    ensureServiceRunning();
    val donorIds = resolveDonorIds(filters);

    val uiInfo = JobUiInfo.builder()
        .filter(filters.toString())
        .uiQueryStr(uiQueryStr)
        .controlled(isAuthorized(user))
        .user(getUserId(user))
        .build();

    String downloadId = null;
    try {
      downloadId = downloadClient.submitJob(
          donorIds,
          resolveDownloadDataTypes(user, info),
          uiInfo);
    } catch (Exception e) {
      log.error("Job submission failed.", e);
    }
    checkRequest(downloadId == null, "Failed to submit download.");

    return new JobInfo(downloadId);
  }
  @GET
  @Timed
  @Path("/submitPQL")
  @Produces(APPLICATION_JSON)
  @ApiOperation("Submit job to request archive generation")
  public JobInfo submitPQLJob(
    @Auth(required = false) User user,
    @ApiParam(value = "PQL query", required = false) @QueryParam("pql") @DefaultValue("{}") String pqlString,
    @ApiParam(value = "Archive param") @QueryParam("info") @DefaultValue("") String info) {
    ensureServiceRunning();
    val donorIds = resolveDonorIds(pqlString);

     val uiInfo = JobUiInfo.builder()
       .controlled(isAuthorized(user))
       .user(getUserId(user))
       .build();
    String downloadId = null;

    try {
      downloadId = downloadClient.submitJob(
        donorIds,
        resolveDownloadDataTypes(user, info),
        uiInfo);
    } catch (Exception e) {
      log.error("Job submission failed.", e);
    }
    checkRequest(downloadId == null, "Failed to submit download.");

    return new JobInfo(downloadId);
  }


  @GET
  @Timed
  @Path("/size")
  @Produces(APPLICATION_JSON)
  @ApiOperation("Get download size by type subject to the supplied filter condition(s)")
  public Map<String, Object> getDataTypeSizePerFileType(
      @Auth(required = false) User user,
      @ApiParam(value = "Filter the search donors") @QueryParam("filters") @DefaultValue("{}") FiltersParam filters) {
    // Work out the query for that returns only donor ids that matches the filter conditions
    val donorIds = donorService.findIds(Query.builder().filters(filters.get()).build());
    checkRequest(donorIds.isEmpty(), "No donors found for query '%s'", filters.get());

    val sizes = downloadClient.getSizes(donorIds);
    val dataTypeSizes = DownloadResources.normalizeSizes(sizes);
    val allowedDataTypes = resolveAllowedDataTypes(user);
    val allowedDataTypeSizes = getAllowedDataTypeSizes(dataTypeSizes, allowedDataTypes);

    return singletonMap("fileSize", createGetDataSizeResponse(allowedDataTypeSizes));
  }

  @GET
  @Timed
  @Path("/sizePQL")
  @Produces(APPLICATION_JSON)
  @ApiOperation("Get download size by type subject to the supplied filter condition(s)")
  public Map<String, Object> getDataTypeSizePerFileTypeFromPQL(
    @Auth(required = false) User user,
    @ApiParam(value = "Filter the search donors") @QueryParam("pql") @DefaultValue("{}") String pql) {

    val donorIds = donorService.findIds(pql);
    checkRequest(donorIds.isEmpty(), "No donors found for pql '%s'", pql);

    val sizes = downloadClient.getSizes(donorIds);
    val dataTypeSizes = DownloadResources.normalizeSizes(sizes);
    val allowedDataTypes = resolveAllowedDataTypes(user);
    val allowedDataTypeSizes = getAllowedDataTypeSizes(dataTypeSizes, allowedDataTypes);

    return singletonMap("fileSize", createGetDataSizeResponse(allowedDataTypeSizes));
  }

  @GET
  @Timed
  @Path("/info{dir:.*}")
  @Produces(APPLICATION_JSON)
  @ApiOperation("Get file info under the specified directory")
  public ArrayNode listDirectory(
      @Auth(required = false) User user,
      @ApiParam(value = "Listing of the specified directory under the download relative directory", required = false) @PathParam("dir") String dir,
      @ApiParam(value = "Field names to include") @QueryParam("fields") @DefaultValue("") SetParam fields,
      @ApiParam(value = "Perform listing recursively?") @QueryParam("recursive") @DefaultValue("false") boolean recursive,
      @ApiParam(value = "Flatten tree into list?") @QueryParam("flatten") @DefaultValue("false") boolean flatten)
      throws IOException {
    ensureServiceRunning();
    val files = downloadClient.listFiles(dir, recursive);
    if (files == null) {
      throwNotFoundException(dir);
    }
    val authorized = isAuthorized(user);

    val mapper = new DownloadFileListMapper(fields.get(), authorized, flatten);
    return mapper.map(files);
  }

  @ApiOperation("Get archive based by type subject to the supplied filter condition(s)")
  @GET
  @Timed
  public Response getStaticArchive(
      @Auth(required = false) User user,
      @ApiParam(value = "filename to download", required = true) @QueryParam("fn") @DefaultValue("") String filePath,
      @HeaderParam(RANGE) String range) throws IOException {
    checkRequest(filePath.trim().isEmpty(), "Invalid argument fn");

    if (!isAuthorized(user) && isControlled(filePath)) {
      throw new ForbiddenAccessException("Unauthorized access", "download");
    }

    ensureServiceRunning();

    val token = tokenService.createToken(filePath);
    val redirectUri = getStaticFileRedirect(publicServerUrl, token);

    return Response.temporaryRedirect(redirectUri).build();
  }

  @GET
  @Timed
  @Path("/{downloadId}")
  @ApiOperation("Get archive based by type subject to the supplied filter condition(s)")
  public Response getFullArchive(
      @Auth(required = false) User user,
      @PathParam("downloadId") String downloadId) throws IOException {
    ensureServiceRunning();

    val job = downloadClient.getJob(downloadId);
    if (job == null) {
      throwArchiveNotFound();
    }

    ensureUsersDownload(user, job);

    val token = tokenService.createToken(downloadId, getUserId(user));
    val redirectUri = getDynamicFileRedirect(publicServerUrl, token);

    return Response.temporaryRedirect(redirectUri).build();
  }

  @GET
  @Timed
  @Path("/{downloadId}/{dataType}")
  @ApiOperation("Get archive based by type subject to the supplied filter condition(s)")
  public Response getIndividualTypeArchive(
      @Auth(required = false) User user,
      @PathParam("downloadId") String downloadId,
      @PathParam("dataType") final String dataType) throws IOException {
    ensureServiceRunning();

    val job = downloadClient.getJob(downloadId);
    if (job == null) {
      throwArchiveNotFound();
    }

    ensureUsersDownload(user, job);
    val downloadType = getDownloadDataType(dataType, isAuthorized(user));
    ensureTypeAvailability(job.getDataType(), downloadType);

    val token = tokenService.createToken(downloadId, getUserId(user));
    val redirectUri = getDynamicFileRedirect(publicServerUrl, token, downloadType);

    return Response.temporaryRedirect(redirectUri).build();
  }

  private Set<String> resolveDonorIds(FiltersParam filters) {
    val donorIds = donorService.findIds(Query.builder().filters(filters.get()).build());
    if (donorIds.isEmpty()) {
      log.error("No donor ids found for filter: {}", filters);
      throw new NotFoundException("No donor found", "download");
    }
    log.info("Number of donors to be retrieved: {}", donorIds.size());

    return donorIds;
  }

  private Set<String> resolveDonorIds(String pqlString) {
    val donorIds = donorService.findIds(pqlString);
    if (donorIds.isEmpty()) {
      log.error("No donor ids found for PQL: {}", pqlString);
      throw new NotFoundException("No donor found", "download");
    }
    log.info("Number of donors to be retrieved: {}", donorIds.size());

    return donorIds;
  }

  private Set<DownloadDataType> resolveAllowedDataTypes(User user) {
    return isAuthorized(user) ? DownloadDataTypes.CONTROLLED_DATA_TYPES : DownloadDataTypes.PUBLIC_DATA_TYPES;
  }

  private Set<DownloadDataType> resolveDownloadDataTypes(User user, String rawDownloadDataTypes) {
    val dataTypeNames = parseDownloadDataTypeNames(rawDownloadDataTypes);
    val authorized = isAuthorized(user);
    val requestedDataTypes = dataTypeNames.stream()
        .map(name -> DownloadDataType.from(name, authorized))
        .collect(toImmutableSet());

    return Sets.intersection(resolveAllowedDataTypes(user), requestedDataTypes);
  }

  private void ensureServiceRunning() {
    if (!downloadClient.isServiceAvailable()) {
      throw new ServiceUnavailableException("Downloader is disabled");
    }
  }

  private boolean isAuthorized(User user) {
    return user != null;
  }

  private static void ensureUsersDownload(User user, JobResponse job) {
    // Check if the user is trying to download permitted file
    val downloadUser = job.getJobInfo().getUser();
    val userId = getUserId(user);
    if (!userId.equals(downloadUser)) {
      log.warn("User {} was trying to download forbidden archive: {}", userId, job);
      throw new ForbiddenAccessException("Unauthorized access", "download");
    }
  }

  private static String getUserId(User user) {
    val userId = user == null ? ANONYMOUS_USER : user.getEmailAddress();
    checkRequest(userId == null, "Incorrectly authorized user.");

    return userId;
  }

  private static boolean isControlled(String path) {
    return path.contains("controlled");
  }

  private static void ensureTypeAvailability(Set<DownloadDataType> dataType, DownloadDataType downloadType) {
    if (!dataType.contains(downloadType)) {
      throw new NotFoundException(downloadType.getId(), "download");
    }
  }

  private static DownloadDataType getDownloadDataType(String dataType, boolean authenticated) {
    val dataTypeName = dataType.toLowerCase();
    DownloadDataType downloadDataType = null;
    try {
      downloadDataType = DownloadDataType.from(dataTypeName, authenticated);
    } catch (Exception e) {
      checkRequest(downloadDataType == null, "Unknown data type %s", dataType);
    }

    return downloadDataType;
  }

  private static void throwNotFoundException(String path) {
    throw new NotFoundException(path, "Archive");
  }

  private static void throwArchiveNotFound() {
    throw new NotFoundException("The archive is no longer available for download", "download");
  }

}