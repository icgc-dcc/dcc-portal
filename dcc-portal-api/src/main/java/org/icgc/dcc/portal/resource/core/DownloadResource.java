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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.net.HttpHeaders.RANGE;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.download.core.request.Redirects.getDynamicFileRedirect;
import static org.icgc.dcc.download.core.request.Redirects.getStaticFileRedirect;
import static org.icgc.dcc.portal.download.DownloadResources.createGetDataSizeResponse;
import static org.icgc.dcc.portal.download.DownloadResources.createJobProgressResponse;
import static org.icgc.dcc.portal.download.DownloadResources.createUiJobInfoResponse;
import static org.icgc.dcc.portal.download.DownloadResources.getAllowedDataTypeSizes;
import static org.icgc.dcc.portal.download.DownloadResources.parseDownloadIds;
import static org.icgc.dcc.portal.util.JsonUtils.parseDownloadDataTypeNames;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.model.DownloadDataType;
import org.icgc.dcc.download.client.DownloadClient;
import org.icgc.dcc.download.core.jwt.JwtService;
import org.icgc.dcc.download.core.model.DownloadFile;
import org.icgc.dcc.download.core.model.JobUiInfo;
import org.icgc.dcc.download.core.response.JobResponse;
import org.icgc.dcc.portal.config.PortalProperties.DownloadProperties;
import org.icgc.dcc.portal.download.DownloadDataTypes;
import org.icgc.dcc.portal.download.DownloadResources;
import org.icgc.dcc.portal.download.JobInfo;
import org.icgc.dcc.portal.download.ServiceStatus;
import org.icgc.dcc.portal.model.FileInfo;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.User;
import org.icgc.dcc.portal.model.param.FiltersParam;
import org.icgc.dcc.portal.model.param.IdsParam;
import org.icgc.dcc.portal.resource.Resource;
import org.icgc.dcc.portal.service.DonorService;
import org.icgc.dcc.portal.service.ForbiddenAccessException;
import org.icgc.dcc.portal.service.NotFoundException;
import org.icgc.dcc.portal.service.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.yammer.dropwizard.auth.Auth;
import com.yammer.metrics.annotation.Timed;

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
      @ApiParam(value = "UI representation of the filter string", required = false, access = "internal") @QueryParam("uiQueryStr") @DefaultValue("{}") String uiQueryStr
      ) {
    ensureServiceRunning();
    val donorIds = resolveDonorIds(filters);

    try {
      val uiInfo = JobUiInfo.builder()
          .filter(filters.toString())
          .uiQueryStr(uiQueryStr)
          .controlled(isAuthorized(user))
          .user(getUserId(user))
          .build();

      val downloadId = downloadClient.submitJob(
          donorIds,
          resolveDownloadDataTypes(user, info),
          uiInfo);

      return new JobInfo(downloadId);
    } catch (Exception e) {
      log.error("Job submission failed.", e);
      throw new NotFoundException("Sorry, job submission failed.", "download");
    }
  }

  @GET
  @Timed
  @Produces(APPLICATION_JSON)
  @Path("/{downloadIds}/status")
  @ApiOperation("Get download job status")
  public List<Map<String, Object>> getJobStatus(
      @Auth(required = false) User user,
      @ApiParam(value = "download id") @PathParam("downloadIds") String downloadIds) {
    // TODO: Change to one id only
    val ids = ImmutableSet.copyOf(downloadIds.split(",", -1));
    checkState(ids.size() == 1, "The API doesn't support status for multiple download IDs");

    val jobs = ids.stream()
        .map(id -> downloadClient.getJob(id))
        .collect(toImmutableList());

    ensureAccessPermissions(user, jobs);

    val jobResponses = jobs.stream()
        .map(job -> createJobProgressResponse(job))
        .collect(toList());

    return jobResponses;
  }

  @GET
  @Timed
  @Path("/size")
  @Produces(APPLICATION_JSON)
  @ApiOperation("Get download size by type subject to the supplied filter condition(s)")
  public Map<String, Object> getDataTypeSizePerFileType(
      @Auth(required = false) User user,
      @ApiParam(value = "Filter the search donors") @QueryParam("filters") @DefaultValue("{}") FiltersParam filters
      ) {
    // Work out the query for that returns only donor ids that matches the filter conditions
    val donorIds = donorService.findIds(Query.builder().filters(filters.get()).build());
    val dataTypeSizes = DownloadResources.normalizeSizes(downloadClient.getSizes(donorIds));
    val allowedDataTypes = resolveAllowedDataTypes(user);
    val allowedDataTypeSizes = getAllowedDataTypeSizes(dataTypeSizes, allowedDataTypes);

    return singletonMap("fileSize", createGetDataSizeResponse(allowedDataTypeSizes));
  }

  @GET
  @Timed
  @Produces(APPLICATION_JSON)
  @Path("{downloadIds}/info")
  @ApiOperation("Get the job info based on IDs")
  public Map<String, Map<String, Object>> getDownloadInfo(
      @Auth(required = false) User user,
      @ApiParam(value = "id", required = false) @PathParam("downloadIds") @DefaultValue("") IdsParam downloadIds
      ) throws IOException {
    val ids = parseDownloadIds(downloadIds);
    checkState(ids.size() == 1, "The API doesn't support info for multiple download IDs");
    val id = ids.get(0);
    val job = downloadClient.getJob(id);

    ensureAccessPermissions(user, Collections.singletonList(job));

    return singletonMap(id, createUiJobInfoResponse(job));
  }

  @GET
  @Timed
  @Path("/info{dir:.*}")
  @Produces(APPLICATION_JSON)
  @ApiOperation("Get file info under the specified directory")
  public List<FileInfo> listDirectory(
      @Auth(required = false) User user,
      @ApiParam(value = "listing of the specified directory under the download relative directory", required = false) @PathParam("dir") String dir
      ) throws IOException {
    val files = downloadClient.listFiles(dir);
    val authorized = isAuthorized(user);

    return files.stream()
        .filter(filterFiles(authorized))
        .map(file -> new FileInfo(file.getName(), file.getType().getSymbol(), file.getSize(), file.getDate()))
        .collect(toImmutableList());
  }

  @GET
  @Timed
  @Path("/readme{dir:.*}")
  @Produces(APPLICATION_JSON)
  @ApiOperation("Get readme under the specified directory")
  public String getReadMe(
      @ApiParam(value = "directory that contains the readme", required = false) @PathParam("dir") String dir
      ) throws IOException {
    checkRequest(dir.trim().isEmpty(), "Invalid argument");

    val token = tokenService.createToken(dir);

    return downloadClient.getReadme(token);
  }

  @ApiOperation("Get archive based by type subject to the supplied filter condition(s)")
  @GET
  @Timed
  public Response getStaticArchive(
      @Auth(required = false) User user,
      @ApiParam(value = "filename to download", required = true) @QueryParam("fn") @DefaultValue("") String filePath,
      @HeaderParam(RANGE) String range
      ) throws IOException {
    checkRequest(filePath.trim().isEmpty(), "Invalid argument fn");

    if (!isAuthorized(user) && isControlled(filePath)) {
      throw new ForbiddenAccessException("Unauthorized access", "download");
    }

    ensureServiceRunning();
    if (filePath.contains("README.txt")) {
      val body = getReadMe(filePath);

      return Response.ok(body)
          .type(MediaType.TEXT_PLAIN)
          .build();
    }

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
      @PathParam("downloadId") String downloadId
      ) throws IOException {
    ensureServiceRunning();

    val job = downloadClient.getJob(downloadId);
    if (job == null) {
      throw new NotFoundException("The archive is not available for download anymore.", "download");
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
      @PathParam("dataType") final String dataType
      ) throws IOException {
    ensureServiceRunning();

    val job = downloadClient.getJob(downloadId);
    if (job == null) {
      throw new NotFoundException("The archive is not available for download anymore.", "download");
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

  private void ensureAccessPermissions(User user, List<JobResponse> jobs) {
    val controlled = DownloadResources.hasControlledData(jobs);
    if (isPermissionDenied(user, controlled) || !isUserDownload(user, jobs)) {
      throw new ForbiddenAccessException("Unauthorized access", "download");
    }
  }

  private static boolean isUserDownload(User user, List<JobResponse> jobs) {
    val userId = getUserId(user);

    return jobs.stream()
        .allMatch(job -> job.getJobInfo().getUser().equals(userId));
  }

  private boolean isPermissionDenied(User user, boolean isControlled) {
    if (isControlled && !isAuthorized(user)) {
      return true;
    } else {
      return false;
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

  private static Predicate<? super DownloadFile> filterFiles(final boolean authorized) {
    return file -> {
      String fileName = file.getName();

      return authorized ? !fileName.contains("open") : !fileName.contains("controlled");
    };
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
}