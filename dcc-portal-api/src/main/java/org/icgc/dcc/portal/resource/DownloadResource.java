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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.net.HttpHeaders.ACCEPT_RANGES;
import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.google.common.net.HttpHeaders.CONTENT_LENGTH;
import static com.google.common.net.HttpHeaders.CONTENT_RANGE;
import static com.google.common.net.HttpHeaders.RANGE;
import static com.sun.jersey.core.header.ContentDisposition.type;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.ok;
import static org.icgc.dcc.downloader.core.DataType.CNSM;
import static org.icgc.dcc.downloader.core.DataType.DONOR;
import static org.icgc.dcc.downloader.core.DataType.DONOR_EXPOSURE;
import static org.icgc.dcc.downloader.core.DataType.DONOR_FAMILY;
import static org.icgc.dcc.downloader.core.DataType.DONOR_THERAPY;
import static org.icgc.dcc.downloader.core.DataType.EXP_ARRAY;
import static org.icgc.dcc.downloader.core.DataType.EXP_SEQ;
import static org.icgc.dcc.downloader.core.DataType.JCN;
import static org.icgc.dcc.downloader.core.DataType.METH_ARRAY;
import static org.icgc.dcc.downloader.core.DataType.METH_SEQ;
import static org.icgc.dcc.downloader.core.DataType.MIRNA_SEQ;
import static org.icgc.dcc.downloader.core.DataType.PEXP;
import static org.icgc.dcc.downloader.core.DataType.SAMPLE;
import static org.icgc.dcc.downloader.core.DataType.SGV_CONTROLLED;
import static org.icgc.dcc.downloader.core.DataType.SPECIMEN;
import static org.icgc.dcc.downloader.core.DataType.SSM_CONTROLLED;
import static org.icgc.dcc.downloader.core.DataType.SSM_OPEN;
import static org.icgc.dcc.downloader.core.DataType.STSM;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;

import javax.annotation.Nullable;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import lombok.Cleanup;
import lombok.Data;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.icgc.dcc.downloader.client.DownloaderClient;
import org.icgc.dcc.downloader.client.ExportedDataFileSystem;
import org.icgc.dcc.downloader.client.ExportedDataFileSystem.AccessPermission;
import org.icgc.dcc.downloader.core.ArchiveJobManager.JobProgress;
import org.icgc.dcc.downloader.core.ArchiveJobManager.JobStatus;
import org.icgc.dcc.downloader.core.DataType;
import org.icgc.dcc.downloader.core.FileInfo;
import org.icgc.dcc.downloader.core.SelectionEntry;
import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.IdsParam;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.User;
import org.icgc.dcc.portal.service.BadRequestException;
import org.icgc.dcc.portal.service.DonorService;
import org.icgc.dcc.portal.service.ForbiddenAccessException;
import org.icgc.dcc.portal.service.NotFoundException;
import org.icgc.dcc.portal.service.ServiceUnavailableException;
import org.icgc.dcc.portal.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.inject.Stage;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.dropwizard.auth.Auth;
import com.yammer.metrics.annotation.Timed;

@Component
@Slf4j
@Api(value = "/v1/download", description = "Resources relating to archive downloading")
@Path("/v1/download")
@Consumes(APPLICATION_JSON)
public class DownloadResource {

  /**
   * Constants.
   */
  private static final String IS_CONTROLLED = "isControlled";
  private static final String APPLICATION_GZIP = "application/x-gzip";
  private static final String APPLICATION_TAR = "application/x-tar";

  // Additional states for UI
  private static final String FOUND_STATUS = "FOUND";
  private static final String NOT_FOUND_STATUS = "NOT_FOUND";
  private static final String EXPIRED_STATUS = "EXPIRED";

  private static final String INDIVIDUAL_TYPE_ARCHIVE_EXTENSION = ".tsv.gz";
  private static final String FULL_ARCHIVE_EXTENSION = ".tar";

  /**
   * Dependencies.
   */
  private final DonorService donorService;
  private final DownloaderClient downloader;
  private final ExportedDataFileSystem fs;

  /**
   * Configuration.
   */
  private final Stage env;

  private final static ImmutableMap<DataType, List<DataType>> DataTypeGroupMap = ImmutableMap
      .<DataType, List<DataType>> builder()
      .put(SSM_OPEN, ImmutableList.of(SSM_OPEN))
      .put(SSM_CONTROLLED, ImmutableList.of(SSM_CONTROLLED))
      .put(DONOR, ImmutableList.of(DONOR, DONOR_EXPOSURE, DONOR_FAMILY, DONOR_THERAPY, SPECIMEN, SAMPLE))
      .put(CNSM, ImmutableList.of(CNSM))
      .put(JCN, ImmutableList.of(JCN))
      .put(METH_ARRAY, ImmutableList.of(METH_ARRAY))
      .put(METH_SEQ, ImmutableList.of(METH_SEQ))
      .put(MIRNA_SEQ, ImmutableList.of(MIRNA_SEQ))
      .put(STSM, ImmutableList.of(STSM))
      .put(PEXP, ImmutableList.of(PEXP))
      .put(SGV_CONTROLLED, ImmutableList.of(SGV_CONTROLLED))
      .put(EXP_ARRAY, ImmutableList.of(EXP_ARRAY))
      .put(EXP_SEQ, ImmutableList.of(EXP_SEQ))
      .build();

  private final static class AccessControl {

    private final static Map<String, DataType> PublicAccessibleMap = ImmutableMap.<String, DataType> builder()
        .put(SSM_OPEN.name, SSM_OPEN)
        .put(DONOR.name, DONOR)
        .put(CNSM.name, CNSM)
        .put(JCN.name, JCN)
        .put(METH_SEQ.name, METH_SEQ)
        .put(METH_ARRAY.name, METH_ARRAY)
        .put(MIRNA_SEQ.name, MIRNA_SEQ)
        .put(STSM.name, STSM)
        .put(PEXP.name, PEXP)
        .put(EXP_ARRAY.name, EXP_ARRAY)
        .put(EXP_SEQ.name, EXP_SEQ)
        .build();

    private final static Set<DataType> PublicAccessibleDataTypes = ImmutableSet.copyOf(PublicAccessibleMap.values());

    private final static Map<String, DataType> PrivateAccessibleMap = ImmutableMap
        .<String, DataType> builder()
        .put(SSM_CONTROLLED.name, SSM_CONTROLLED)
        .put(DONOR.name, DONOR)
        .put(CNSM.name, CNSM)
        .put(JCN.name, JCN)
        .put(METH_SEQ.name, METH_SEQ)
        .put(METH_ARRAY.name, METH_ARRAY)
        .put(MIRNA_SEQ.name, MIRNA_SEQ)
        .put(STSM.name, STSM)
        .put(PEXP.name, PEXP)
        .put(EXP_ARRAY.name, EXP_ARRAY)
        .put(EXP_SEQ.name, EXP_SEQ)
        .put(SGV_CONTROLLED.name, SGV_CONTROLLED)
        .build();

    private final static Set<DataType> PrivateAccessibleDataTypes = ImmutableSet
        .copyOf(PrivateAccessibleMap.values());

    private final static Set<DataType> FullAccessibleDataTypes = ImmutableSet.<DataType> builder()
        .addAll(PrivateAccessibleDataTypes)
        .addAll(PublicAccessibleDataTypes)
        .build();
  }

  @ApiOperation("Get download service availability")
  @Path("/status")
  @Produces(APPLICATION_JSON)
  @GET
  @Timed
  public ServiceStatus getServiceStatus() {
    return new ServiceStatus(downloader.isServiceAvailable() && !downloader.isOverCapacity());
  }

  @Autowired
  public DownloadResource(DonorService donorService, DownloaderClient downloader, ExportedDataFileSystem fs, Stage env) {
    this.donorService = donorService;
    this.downloader = downloader;
    this.fs = fs;
    this.env = env;
    log.debug("Download Resource in {} mode", env);
  }

  @ApiOperation("Submit job to request archive generation")
  @Path("/submit")
  @Produces(APPLICATION_JSON)
  @GET
  @Timed
  public JobInfo submitJob(
      @Auth(required = false) User user,

      @ApiParam(value = "Filter the search results", required = false) @QueryParam("filters") @DefaultValue("{}") FiltersParam filters,

      @ApiParam(value = "Archive param") @QueryParam("info") @DefaultValue("") String info,

      @ApiParam(value = "user email address", required = false, access = "internal") @QueryParam("email") @DefaultValue("") String email,

      @ApiParam(value = "download url", required = false, access = "internal") @QueryParam("downloadUrl") @DefaultValue("") String downloadUrl,

      @ApiParam(value = "UI representation of the filter string", required = false, access = "internal") @QueryParam("uiQueryStr") @DefaultValue("{}") String uiQueryStr

      ) {

    boolean isLogin = isLogin(user);

    // dynamic download
    if (!downloader.isServiceAvailable() || downloader.isOverCapacity()) throw new ServiceUnavailableException(
        "Downloader is disabled");
    Set<String> donorIds = donorService.findIds(Query.builder().filters(filters.get()).build());

    if (donorIds.isEmpty()) {
      log.error("No donor ids found for filter: {}", filters);
      throw new NotFoundException("no donor found", "download");
    }

    log.info("Number of donors to be retrieved: {}", donorIds.size());
    List<SelectionEntry<String, String>> typeInfo = JsonUtils.parseListEntry(info);

    final Map<String, DataType> allowedDataTypeMap =
        isLogin ? AccessControl.PrivateAccessibleMap : AccessControl.PublicAccessibleMap;

    ImmutableMap.Builder<String, String> jobInfoBuilder = ImmutableMap.builder();
    jobInfoBuilder.put("filter", filters.toString());
    jobInfoBuilder.put("uiQueryStr", uiQueryStr);
    jobInfoBuilder.put("startTime", String.valueOf(System.currentTimeMillis()));
    jobInfoBuilder.put("hasEmail", String.valueOf(!email.equals("")));
    jobInfoBuilder.put(IS_CONTROLLED, String.valueOf(isLogin));

    try {
      List<SelectionEntry<DataType, String>> filterTypeInfo = newArrayList();
      for (SelectionEntry<String, String> selection : typeInfo) {
        DataType dataType = allowedDataTypeMap.get(selection.getKey().toLowerCase());
        if (dataType == null) throw new NotFoundException(selection.getKey(), "download");

        Map<DataType, Future<Long>> result = downloader.getSizes(donorIds);

        for (DataType type : DataTypeGroupMap.get(dataType)) {
          if (result.get(type).get() > 0) {
            filterTypeInfo.add(new SelectionEntry<DataType, String>(type, selection
                .getValue()));
          }
        }
      }

      if (!EmailValidator.getInstance().isValid(email)) {
        return new JobInfo(downloader.submitJob(donorIds,
            filterTypeInfo, jobInfoBuilder.build(), downloadUrl));
      } else {
        return new JobInfo(downloader.submitJob(donorIds,
            filterTypeInfo, jobInfoBuilder.build(), email, downloadUrl));
      }
    } catch (Exception e) {
      log.error("Job submission failed.", e);
      throw new NotFoundException("Sorry, job submission failed.", "download");
    }
  }

  @ApiOperation("Cancel a download job associated with the supplied 'download id'")
  @Path("/{downloadId}/cancel")
  @Produces(APPLICATION_JSON)
  @GET
  @Timed
  public Map<String, Object> cancelJob(
      @Auth(required = false) User user,

      @ApiParam(value = "download id") @PathParam("downloadId") String downloadId) throws BadRequestException {
    try {
      Map<String, Map<String, String>> jobInfoMap = downloader.getJobInfo(ImmutableSet.of(downloadId));
      boolean isControlled = containsControlledData(jobInfoMap);
      if (isPermissionDenied(user, isControlled)) {
        throw new ForbiddenAccessException("Unauthorized access", "download");
      } else {
        return standardizeStatus(downloadId, downloader.cancelJob(downloadId));
      }
    } catch (IOException e) {
      log.error("fail to cancel the job with id: {}", downloadId, e);
      throw new BadRequestException("fail to cancel the job with id: " + downloadId);
    }
  }

  @ApiOperation("Get download service availability")
  @Path("/{downloadIds}/status")
  @Produces(APPLICATION_JSON)
  @GET
  @Timed
  public List<Map<String, Object>> getJobStatus(

      @Auth(required = false) User user,

      @ApiParam(value = "download id") @PathParam("downloadIds") String downloadIds) {
    // TODO: Use dropwizard's built-in params instead of parsing it ourselves
    // TODO: Probably want to batch this instead of using iteration
    try {
      ImmutableList.Builder<Map<String, Object>> statusList = ImmutableList.builder();
      Set<String> ids = ImmutableSet.copyOf(downloadIds.split(",", -1));
      boolean isControlled = containsControlledData(downloader.getJobInfo(ids));
      if (isPermissionDenied(user, isControlled)) {
        throw new ForbiddenAccessException("Unauthorized access", "download");
      } else {
        Map<String, JobStatus> statusMap = downloader.getStatus(ids);
        for (Entry<String, JobStatus> jobInfo : statusMap.entrySet()) {
          JobStatus jobStatus = jobInfo.getValue();
          String id = jobInfo.getKey();
          statusList.add(standardizeStatus(id, jobStatus));
        }
        Set<String> notFoundIds = Sets.difference(ids, statusMap.keySet());
        for (String downloadId : notFoundIds) {
          statusList.add(ImmutableMap.<String, Object> of("downloadId", downloadId, "status", NOT_FOUND_STATUS));
        }
        return statusList.build();
      }
    } catch (IOException e) {
      log.error("status retrieval error: ", e);
      throw new NotFoundException(downloadIds, "download");
    }
  }

  @ApiOperation("Get download size by type subject to the supplied filter condition(s)")
  @Path("/size")
  @Produces(APPLICATION_JSON)
  @GET
  @Timed
  public Map<Object, Object> getDataTypeSizePerFileType(

      @Auth(required = false) User user,

      @ApiParam(value = "Filter the search donors") @QueryParam("filters") @DefaultValue("{}") FiltersParam filters

      ) {
    // Work out the query for that returns only donor ids that matches the filter conditions
    Set<String> donorIds = donorService.findIds(Query.builder().filters(filters.get()).build());

    Map<DataType, Future<Long>> result = downloader.getSizes(donorIds);

    Builder<String, Long> filterSizesBuilder = ImmutableMap.builder();

    for (DataType dataType : isLogin(user) ? AccessControl.PrivateAccessibleDataTypes : AccessControl.PublicAccessibleDataTypes) {
      try {
        long total = 0;
        for (DataType type : DataTypeGroupMap.get(dataType)) {
          if (result.containsKey(type)) {
            total = total + result.get(type).get();
          }
        }
        filterSizesBuilder.put(dataType.name, total);
      } catch (Exception e) {
        log.error("Fail to retrieve size information.", e);
        throw new BadRequestException("fail to retireve size information");
      }
    }

    Map<Object, Object> returnMap = newHashMap();
    List<Object> typeSizeList = newArrayList();
    for (Entry<String, Long> entry : filterSizesBuilder.build().entrySet()) {
      Map<String, Object> item = newHashMap();
      item.put("label", entry.getKey());
      item.put("sizes", entry.getValue());

      typeSizeList.add(item);
    }

    returnMap.put("fileSize", typeSizeList);

    return returnMap;
  }

  @ApiOperation("Get the job info based on IDs")
  @Path("{downloadIds}/info")
  @Produces(APPLICATION_JSON)
  @GET
  @Timed
  public Map<String, Object> getDownloadInfo(

      @Auth(required = false) User user,

      // TODO: after merge with shane's branch, use pathparam to handle this
      @ApiParam(value = "id", required = false) @PathParam("downloadIds") @DefaultValue("") IdsParam downloadIds

      ) throws IOException {
    try {

      if (downloadIds.get() != null && downloadIds.get().size() != 0) {
        Map<String, Map<String, String>> jobInfoMap = downloader.getJobInfo(ImmutableSet.copyOf(downloadIds.get()));
        ImmutableMap.Builder<String, Object> reportMapBuilder = ImmutableMap.builder();
        boolean isControlled = containsControlledData(jobInfoMap);
        if (isPermissionDenied(user, isControlled)) {
          throw new ForbiddenAccessException("Unauthorized access", "download");
        } else {
          for (Entry<String, Map<String, String>> jobInfo : jobInfoMap.entrySet()) {
            reportMapBuilder.put(jobInfo.getKey(),
                ImmutableMap.<String, String> builder()
                    .putAll(jobInfo.getValue())
                    .put("status", FOUND_STATUS).build());
          }

          Set<String> notFoundIds =
              Sets.difference(ImmutableSet.<String> copyOf(downloadIds.get()), jobInfoMap.keySet());
          for (String downloadId : notFoundIds) {
            reportMapBuilder.put(downloadId, ImmutableMap.of("status", NOT_FOUND_STATUS));
          }
          return reportMapBuilder.build();
        }
      } else {
        throw new NotFoundException("Request denied", "download");
      }
    } finally {
      log.debug("Request job info for : {}", downloadIds.get());
    }
  }

  @ApiOperation("Get file info under the specified directory")
  @Path("/info{dir:.*}")
  @Produces(APPLICATION_JSON)
  @GET
  @Timed
  public List<FileInfo> listDirectory(

      @Auth(required = false) User user,

      // TODO: queryparam like fn
      @ApiParam(value = "listing of the specified directory under the download relative directory", required = false) @PathParam("dir") String dir

      ) throws IOException {
    try {
      if (dir.trim().isEmpty()) dir = "/";
      return listInfo(new File(dir), isLogin(user));
    } catch (FileNotFoundException e) {
      throw new BadRequestException("Directory not found: " + dir);
    } finally {
      log.info("Request dir: {}", dir);
    }
  }

  @ApiOperation("Get readme under the specified directory")
  @Path("/readme{dir:.*}")
  @Produces(APPLICATION_JSON)
  @GET
  @Timed
  public String getReadMe(

      // TODO: queryparam like fn
      @ApiParam(value = "directory that contains the readme", required = false) @PathParam("dir") String dir

      ) throws IOException {
    try {
      if (dir.trim().isEmpty()) dir = "/";
      return "Directory for readme: " + dir + "\n===\n\n" + "PLACEHOLDER FOR README" + "\n\n" + "HEADER 1 \n---\n\n"
          + "some content";
    } finally {
      log.info("Request dir: {}", dir);
    }
  }

  @ApiOperation("Get archive based by type subject to the supplied filter condition(s)")
  @GET
  @Timed
  public Response getStaticArchive(

      @Auth(required = false) User user,

      @ApiParam(value = "filename to download", required = true)//
      @QueryParam("fn")//
      @DefaultValue("") String filePath,

      @HeaderParam(RANGE) String range

      ) throws IOException {

    if (filePath.trim().equals("")) throw new BadRequestException("Missing argument fn");

    boolean isLogin = isLogin(user);
    ResponseBuilder rb = ok();
    StreamingOutput archiveStream = null;
    String filename = null;

    File downloadFile = new File(filePath);
    Predicate<File> predicate =
        (isLogin ? new LoginUserAccessiblePredicate() : new EveryoneAccessiblePredicate());

    boolean hasValidPermission = false;
    try {
      if (fs.isFile(downloadFile) && predicate.apply(downloadFile)) {
        hasValidPermission = true;
      }
    } catch (IOException e) {
      log.error("Permission Denied", e);
    }

    if (!hasValidPermission) {
      throw new NotFoundException(filePath, "download");
    }

    val contentLength = fs.getSize(downloadFile);

    if (range != null) {
      val rangeHeader = parseRange(range, contentLength);
      log.debug("Parsed range header: {}", rangeHeader);

      rb.header(ACCEPT_RANGES, "bytes")
          .header(CONTENT_RANGE, rangeHeader);
    }

    archiveStream = archiveStream(downloadFile, getFromByte(range));
    rb.header(CONTENT_LENGTH, contentLength);
    filename = downloadFile.getName();

    return rb.entity(archiveStream).type(getFileMimeType(filename))
        .header(CONTENT_DISPOSITION,
            type("attachment")
                .fileName(filename)
                .creationDate(new Date())
                .build())
        .build();
  }

  @ApiOperation("Get archive based by type subject to the supplied filter condition(s)")
  @GET
  @Timed
  @Path("/{downloadId}")
  public Response getFullArchive(

      @Auth(required = false) User user,

      @PathParam("downloadId") String downloadId

      ) throws IOException {
    boolean isLogin = isLogin(user);
    ResponseBuilder rb = ok();
    StreamingOutput archiveStream = null;
    String filename = null;
    // dynamic download
    if (!downloader.isServiceAvailable() || downloader.isOverCapacity()) throw new ServiceUnavailableException(
        "Downloader is disabled");

    final Set<DataType> allowedDataTypes =
        isLogin ? AccessControl.FullAccessibleDataTypes : AccessControl.PublicAccessibleDataTypes;

    Map<String, JobStatus> jobStatus = downloader.getStatus(ImmutableSet.<String> of(downloadId));
    JobStatus status = jobStatus.get(downloadId);
    if (status == null || status.isExpired()) {

      throw new NotFoundException(downloadId, "download");
    }

    Map<DataType, JobProgress> typeProgressMap = status.getProgressMap();
    for (Entry<DataType, JobProgress> typeStatus : typeProgressMap.entrySet()) {
      if (!typeStatus.getValue().isCompleted()) {
        throw new NotFoundException(downloadId, "download");
      }
    }
    // check if types are allowed for download
    Set<DataType> availableDataTypeGroup = Sets.intersection(typeProgressMap.keySet(), DataTypeGroupMap.keySet());
    if (!allowedDataTypes.containsAll(availableDataTypeGroup)) {
      log.error("permission denied for download types that need access control: " + typeProgressMap.entrySet()
          + ", download id: " + downloadId);
      throw new NotFoundException(downloadId, "download");
    }

    archiveStream = archiveStream(downloadId, ImmutableList.copyOf(typeProgressMap.keySet()));
    filename = fileName(FULL_ARCHIVE_EXTENSION);

    return rb.entity(archiveStream).type(getFileMimeType(filename))
        .header(CONTENT_DISPOSITION,
            type("attachment")
                .fileName(filename)
                .creationDate(new Date())
                .build())
        .build();
  }

  @ApiOperation("Get archive based by type subject to the supplied filter condition(s)")
  @GET
  @Timed
  @Path("/{downloadId}/{dataType}")
  public Response getIndividualTypeArchive(

      @Auth(required = false) User user,

      @PathParam("downloadId") String downloadId,
      @PathParam("dataType") final String dataType

      ) throws IOException {
    boolean isLogin = isLogin(user);
    ResponseBuilder rb = ok();
    StreamingOutput archiveStream = null;
    String filename = null;
    // dynamic download
    if (!downloader.isServiceAvailable() || downloader.isOverCapacity()) throw new ServiceUnavailableException(
        "Downloader is disabled");

    final Set<DataType> allowedDataTypes =
        isLogin ? AccessControl.FullAccessibleDataTypes : AccessControl.PublicAccessibleDataTypes;

    Map<String, JobStatus> jobStatus = downloader.getStatus(ImmutableSet.<String> of(downloadId));
    JobStatus status = jobStatus.get(downloadId);
    if (status == null || status.isExpired()) {

      throw new NotFoundException(downloadId, "download");
    }
    Map<DataType, JobProgress> typeProgressMap = status.getProgressMap();
    Set<DataType> downloadableTypes = Sets.intersection(typeProgressMap.keySet(), allowedDataTypes);

    Set<DataType> selectedType = Sets.filter(downloadableTypes, new Predicate<DataType>() {

      @Override
      public boolean apply(DataType input) {
        return input.name.equals(dataType);
      }

    });
    if (selectedType.isEmpty()) {
      log.error("permission denied for download type that needs access control: " + dataType
          + ", download id: " + downloadId);
      throw new NotFoundException(downloadId, "download");
    }

    List<DataType> downloadTypes = DataTypeGroupMap.get(selectedType.iterator().next());
    ImmutableList.Builder<DataType> actualDownloadTypes = ImmutableList.builder();
    for (DataType type : downloadTypes) {
      // to handle optional sub-type
      if (typeProgressMap.get(type) != null) {
        if (!typeProgressMap.get(type).isCompleted()) {
          log.error("Data type is not ready for download yet. Data Type: " + type + ", Dowload ID: " + downloadId);
          throw new NotFoundException(downloadId, "download");
        } else {
          actualDownloadTypes.add(type);
        }
      }
    }
    String extension = INDIVIDUAL_TYPE_ARCHIVE_EXTENSION;
    downloadTypes = actualDownloadTypes.build();
    if (downloadTypes.size() == 1) {
      archiveStream = archiveStream(downloadId, downloadTypes.get(0));
    } else {
      archiveStream = archiveStream(downloadId, downloadTypes);
      extension = FULL_ARCHIVE_EXTENSION;
    }

    filename = fileName(extension);
    return rb.entity(archiveStream).type(getFileMimeType(filename))
        .header(CONTENT_DISPOSITION,
            type("attachment")
                .fileName(filename)
                .creationDate(new Date())
                .build())
        .build();
  }

  @Data
  public static class ServiceStatus {

    private final boolean serviceStatus;

  }

  @Data
  public static class JobInfo {

    private final String downloadId;
  }

  /*
   * See:
   * https://github.com/aruld/jersey-streaming/blob/master/src/main/java/com/aruld/jersey/streaming/MediaResource.java
   */
  private static String parseRange(String range, long length) {
    val ranges = range.split("=")[1].split("-");
    val from = getFromByte(range);

    long to = length - 1;

    if (ranges.length == 2) {
      to = parseInt(ranges[1]);
    }

    return String.format("bytes %d-%d/%d", from, to, length);
  }

  private static long getFromByte(String range) {
    if (range == null) {
      return 0;
    }

    val ranges = range.split("=")[1].split("-");
    return parseLong(ranges[0]);
  }

  private boolean isPermissionDenied(User user, boolean isControlled) {
    if (isControlled && !isLogin(user)) {
      return true;
    } else {
      return false;
    }
  }

  private Map<String, Object> standardizeStatus(String id, JobStatus jobStatus) {
    if (jobStatus.isNotFound()) {
      return ImmutableMap.<String, Object> of("downloadId", id, "status", NOT_FOUND_STATUS);
    }

    Map<DataType, JobProgress> jobProgressMap = jobStatus.getProgressMap();
    Set<DataType> selectedGroups = Sets.intersection(jobProgressMap.keySet(), DataTypeGroupMap.keySet());
    ImmutableList.Builder<Map<String, String>> groupProgressListBuilder = ImmutableList.builder();

    for (DataType selectedGroup : selectedGroups) {
      ImmutableMap.Builder<String, String> uiBuilder = ImmutableMap.builder();
      JobProgress groupProgress = new JobProgress(0, 0);
      for (DataType dataType : DataTypeGroupMap.get(selectedGroup)) {
        if (jobProgressMap.get(dataType) != null) {
          groupProgress
              .setDenominator(jobProgressMap.get(dataType).getDenominator() + groupProgress.getDenominator());
          groupProgress.setNumerator(jobProgressMap.get(dataType).getNumerator() + groupProgress.getNumerator());
        }
      }
      uiBuilder.put("dataType", selectedGroup.name);
      uiBuilder.put("completed", String.valueOf(groupProgress.isCompleted()));
      uiBuilder.put("numerator", String.valueOf(groupProgress.getNumerator()));
      uiBuilder.put("denominator", String.valueOf(groupProgress.getDenominator()));
      uiBuilder.put("percentage", String.valueOf(groupProgress.getPercentage()));
      groupProgressListBuilder.add(uiBuilder.build());
    }
    String status = getUIStates(jobStatus);
    return ImmutableMap.<String, Object> of("downloadId", id, "status", status,
        "progress",
        groupProgressListBuilder.build());
  }

  private String getUIStates(JobStatus jobStatus) {

    String status = jobStatus.getWorkflowStatus().name();
    if (jobStatus.isNotFound()) {
      status = NOT_FOUND_STATUS;
    } else if (jobStatus.isExpired()) {
      status = EXPIRED_STATUS;
    }
    return status;
  }

  /**
   * @param jobInfoMap
   * @return
   */
  private boolean containsControlledData(Map<String, Map<String, String>> jobInfoMap) {
    for (val entry : jobInfoMap.entrySet()) {
      String isControlled = entry.getValue().get(IS_CONTROLLED);
      if (isControlled != null && Boolean.valueOf(isControlled)) {
        return true;
      }
    }
    return false;
  }

  private StreamingOutput archiveStream(final String downloadId, final List<DataType> selectedDataTypes) {
    return new StreamingOutput() {

      @Override
      public void write(final OutputStream out) throws IOException, WebApplicationException {
        if (!downloader.streamArchiveInGzTar(out, downloadId, selectedDataTypes)) {
          throw new BadRequestException("Data not found for download id: " + downloadId);
        }
      }
    };
  }

  /**
   * @param downloadId
   * @param dataType
   * @return StreamingOutput
   */
  private StreamingOutput archiveStream(final String downloadId, final DataType dataType) {
    return new StreamingOutput() {

      @Override
      public void write(final OutputStream out) throws IOException, WebApplicationException {
        if (!downloader.streamArchiveInGz(out, downloadId, dataType)) {
          throw new BadRequestException("Data not found for download id: " + downloadId);
        }
      }
    };
  }

  private StreamingOutput archiveStream(final File relativePath, long from) {
    return new StreamingOutput() {

      @Override
      public void write(final OutputStream out) throws IOException, WebApplicationException {
        try {
          @Cleanup
          InputStream in = fs.createInputStream(relativePath, from);
          IOUtils.copy(in, out);
        } catch (Exception e) {
          log.warn("Exception thrown from Dynamic Download Resource.", e);
        }
      }
    };
  }

  private static String fileName(String extension) {
    return format("icgc-dataset-%s" + extension, currentTimeMillis());
  }

  private final class EveryoneAccessiblePredicate implements Predicate<File> {

    @Override
    public boolean apply(@Nullable File file) {
      AccessPermission perm;
      try {
        perm = fs.getPermission(file);
        if (perm == AccessPermission.UNCHECKED || perm == AccessPermission.OPEN) return true;
      } catch (IOException e) {
        throw new BadRequestException("File not found: " + file);
      }
      return false;
    }
  }

  private final class LoginUserAccessiblePredicate implements Predicate<File> {

    @Override
    public boolean apply(@Nullable File file) {
      AccessPermission perm;
      try {
        perm = fs.getPermission(file);
        if (perm == AccessPermission.UNCHECKED || perm == AccessPermission.CONTROLLED) return true;
      } catch (IOException e) {
        throw new BadRequestException("File not found: " + file);
      }
      return false;
    }
  }

  private List<FileInfo> listInfo(File dir, boolean isLogin) throws IOException {
    List<FileInfo> info = newArrayList();

    for (File file : Iterables.filter(fs.listFiles(dir),
        isLogin ? new LoginUserAccessiblePredicate() : new EveryoneAccessiblePredicate())) {
      String type = "d";
      long size = 0;
      long date = fs.getModificationTime(file);
      String name = file.getName();
      if (fs.isFile(file)) {
        size = fs.getSize(file);
        type = "f";
      }

      info.add(new FileInfo(FilenameUtils.concat(dir.getPath(), name), type, size, date));
    }

    Collections.sort(info, new Comparator<FileInfo>() {

      @Override
      public int compare(FileInfo thisInfo, FileInfo thatInfo) {
        return thisInfo.getName().compareTo(thatInfo.getName());
      }

    });
    return info;
  }

  private boolean isLogin(User user) {
    return (env == Stage.DEVELOPMENT || user != null);
  }

  private String getFileMimeType(String filename) {

    String ext = FilenameUtils.getExtension(filename);
    String type = TEXT_PLAIN;
    if (ext.equals("gz")) {
      type = APPLICATION_GZIP;
    } else if (ext.equals("tar")) {
      type = APPLICATION_TAR;
    }
    return type;
  }

}