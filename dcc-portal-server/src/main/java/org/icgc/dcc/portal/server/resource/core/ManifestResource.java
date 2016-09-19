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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.sun.jersey.multipart.Boundary.BOUNDARY_PARAMETER;
import static com.sun.jersey.multipart.MultiPartMediaTypes.MULTIPART_MIXED;
import static com.sun.jersey.multipart.MultiPartMediaTypes.MULTIPART_MIXED_TYPE;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static org.icgc.dcc.portal.server.manifest.model.ManifestFormat.FILES;
import static org.icgc.dcc.portal.server.manifest.model.ManifestFormat.JSON;
import static org.icgc.dcc.portal.server.manifest.model.ManifestFormat.TARBALL;
import static org.icgc.dcc.portal.server.util.MediaTypes.GZIP_TYPE;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.icgc.dcc.portal.server.manifest.ManifestContext;
import org.icgc.dcc.portal.server.manifest.ManifestService;
import org.icgc.dcc.portal.server.manifest.model.Manifest;
import org.icgc.dcc.portal.server.manifest.model.ManifestField;
import org.icgc.dcc.portal.server.manifest.model.ManifestFormat;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.model.param.ListParam;
import org.icgc.dcc.portal.server.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.core.header.ContentDisposition;
import com.yammer.metrics.annotation.Timed;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Resource responsible for serving and storing representations of native download manifests.
 */
@Slf4j
@Component
@Path("/v1/manifests")
@Api(tags = "manifests")
@SwaggerDefinition(tags = @Tag(name = "manifests", description = "Resources about manifests"))
public class ManifestResource extends Resource {

  /**
   * Constants.
   */
  private static final String MANIFEST_ID_PARAM = "manifestId";
  private static final String MANIFEST_ID_DESC =
      "The manifest ID of a previously created manifest. Format is a UUID";

  private static final String REPOS_PARAM = "repos";
  private static final String REPOS_DESC =
      "The prioritized list of repo codes to use. When the `unique` parameter is set to true, this field is required and the order of the repos will determine the source of uniqueness of a file if it exists in more than one repo. Zero or more repo codes separated by a comma.";

  private static final String FILTERS_PARAM = "filters";
  private static final String FILTERS_DEFAULT = "{}";
  private static final String FILTERS_EXAMPLE = "{file:{id:{is:[\"FI1234\"]}}}";
  private static final String FILTERS_DESC =
      "The file selection filter to use as input to the manifest. Can be any JQL expression supported by the `/repository/files` endpoint. e.g. `"
          + FILTERS_EXAMPLE + "`";

  private static final String UNIQUE_PARAM = "unique";
  private static final String UNIQUE_DEFAULT = "false";
  private static final String UNIQUE_DESC =
      "Only return unique files by file id (e.g. FI1234)? Useful when file copies exist in more than one repo";

  private static final String FORMAT_PARAM = "format";
  private static final String FORMAT_DEFAULT = "json";
  private static final String FORMAT_VALUES = "tarball,files,json";
  private static final String FORMAT_DESC =
      "The output format of the manifest. One of `tarball`, `files`, or `json`. `tarball` will return an archive of all native manifests spanned by the file set defined in the manifest. `files` will return a series of files whose content type is subject to the `multipart` parameter. `json` will return a JSON representation of the manifest subject to the `fields` parameter";

  private static final String FIELDS_PARAM = "fields";
  private static final String FIELDS_DEFAULT = "id";
  private static final String FIELDS_VALUES = "id,size,md5sum,repoFileId,content";
  private static final String FIELDS_DESC =
      "What additional fields to include when using format `json`. Zero or more of `id`, `size`, `md5sum`, `repoFileId`, or `content` separated by a comma. All fields accept `content` are properties of files. `content` represents the a base64 encoded native manifest";

  private static final String MULTIPART_PARAM = "multipart";
  private static final String MULTIPART_DEFAULT = "false";
  private static final String MULTIPART_DESC =
      "Use multipart output when a format of `files` is specified  Useful when a manifest covers more than one repo and a requesting agent can accept multipart responses. Content type is "
          + MULTIPART_MIXED;

  /**
   * Dependencies.
   */
  @Autowired
  private ManifestService manifestService;

  /**
   * State.
   */
  @Context
  private HttpServletResponse response;

  @GET
  @Timed
  @ApiOperation(value = "Generate manifest(s) on the fly as specified by the supplied parameters.", notes = "Does not store the result")
  public void generateManifest(
      // Input
      @ApiParam(value = REPOS_DESC, allowMultiple = true) @QueryParam(REPOS_PARAM) @DefaultValue("") ListParam repoParam,
      @ApiParam(value = FILTERS_DESC) @QueryParam(FILTERS_PARAM) @DefaultValue(FILTERS_DEFAULT) FiltersParam filtersParam,
      @ApiParam(value = UNIQUE_DESC) @QueryParam(UNIQUE_PARAM) @DefaultValue(UNIQUE_DEFAULT) boolean unique,

      // Output
      @ApiParam(value = FORMAT_DESC, allowableValues = FORMAT_VALUES) @QueryParam(FORMAT_PARAM) @DefaultValue(FORMAT_DEFAULT) String format,
      @ApiParam(value = FIELDS_DESC, allowableValues = FIELDS_VALUES, allowMultiple = true) @QueryParam(FIELDS_PARAM) @DefaultValue(FIELDS_DEFAULT) ListParam fieldsParam,
      @ApiParam(value = MULTIPART_DESC) @QueryParam(MULTIPART_PARAM) @DefaultValue(MULTIPART_DEFAULT) boolean multipart) {
    val manifest = createManifest(repoParam, filtersParam, unique, format, fieldsParam, multipart);

    log.debug("Rendering manifest: {}", manifest);
    renderManifest(manifest);
  }

  @POST
  @Consumes(APPLICATION_FORM_URLENCODED)
  @Produces(TEXT_PLAIN)
  @ApiOperation(value = "Saves a manifest definition returning its ID", notes = "Note that properties of the manifest can be overriden when retrieved. All but the `filter` are considered stored \"defaults\" for when the manifest is accessed")
  public String saveManifest(
      // Input
      @ApiParam(value = REPOS_DESC, allowMultiple = true) @FormParam(REPOS_PARAM) @DefaultValue("") ListParam repoParam,
      @ApiParam(value = FILTERS_DESC) @FormParam(FILTERS_PARAM) @DefaultValue(FILTERS_DEFAULT) FiltersParam filtersParam,
      @ApiParam(value = UNIQUE_DESC) @FormParam(UNIQUE_PARAM) @DefaultValue(UNIQUE_DEFAULT) boolean unique,

      // Output
      @ApiParam(value = FORMAT_DESC, allowableValues = FORMAT_VALUES) @FormParam(FORMAT_PARAM) @DefaultValue(FORMAT_DEFAULT) String format,
      @ApiParam(value = FIELDS_DESC, allowableValues = FIELDS_VALUES, allowMultiple = true) @FormParam(FIELDS_PARAM) @DefaultValue(FIELDS_DEFAULT) ListParam fieldsParam,
      @ApiParam(value = MULTIPART_DESC) @FormParam(MULTIPART_PARAM) @DefaultValue(MULTIPART_DEFAULT) boolean multipart) {
    val manifest = createManifest(repoParam, filtersParam, unique, format, fieldsParam, multipart);

    log.debug("Saving manifest: {}", manifest);
    manifestService.saveManifest(manifest);
    log.debug("Saved manifest: {}", manifest.getId());

    return manifest.getId().toString();
  }

  @GET
  @Path("/{" + MANIFEST_ID_PARAM + "}")
  @ApiOperation(value = "Retrieves a manifest by its ID, overriding output specification as indicated", notes = "Everything but the `filter` may be overriden")
  public void getManifest(
      // Input
      @ApiParam(value = MANIFEST_ID_DESC, required = true) @PathParam(MANIFEST_ID_PARAM) UUID manifestId,

      // Input - Overrides
      @ApiParam(value = REPOS_DESC, allowMultiple = true) @QueryParam(REPOS_PARAM) @DefaultValue("") ListParam repoParam,
      @ApiParam(value = UNIQUE_DESC) @QueryParam(UNIQUE_PARAM) @DefaultValue("") boolean unique,

      // Output - Overrides
      @ApiParam(value = FORMAT_DESC, allowableValues = FORMAT_VALUES) @QueryParam(FORMAT_PARAM) @DefaultValue("") String format,
      @ApiParam(value = FIELDS_DESC, allowableValues = FIELDS_VALUES, allowMultiple = true) @QueryParam(FIELDS_PARAM) @DefaultValue("") ListParam fieldsParam,
      @ApiParam(value = MULTIPART_DESC) @QueryParam(MULTIPART_PARAM) @DefaultValue("") boolean multipart) {

    // Get "defaults"
    log.debug("Getting manifest: {}", manifestId);
    val manifest = manifestService.getManifest(manifestId);

    // Apply input overrides if present
    if (hasParam(REPOS_PARAM)) {
      manifest.setRepos(repoParam.get());
    }
    if (hasParam(UNIQUE_PARAM)) {
      manifest.setUnique(unique);
    }

    // Apply output overrides if present
    if (hasParam(FORMAT_PARAM)) {
      checkFormat(format);
      manifest.setFormat(ManifestFormat.get(format));

      if (manifest.getFormat() != ManifestFormat.JSON) {
        // Needed to pass validation
        manifest.setFields(Collections.emptyList());
      }
    }
    if (hasParam(FIELDS_PARAM)) {
      manifest.setFields(parseFields(fieldsParam));
    }
    if (hasParam(MULTIPART_PARAM)) {
      manifest.setMultipart(multipart);
    }

    validateManifest(manifest);

    log.debug("Rendering manifest: {}", manifest);
    renderManifest(manifest);
  }

  private Manifest createManifest(
      // Input
      ListParam repoParam,
      FiltersParam filtersParam,
      boolean unique,

      // Output
      String format,
      ListParam fieldsParam,
      boolean multipart) {
    checkFormat(format);

    val manifest = new Manifest()
        .setFormat(ManifestFormat.get(format))
        .setRepos(repoParam.get())
        .setFilters(filtersParam.get())
        .setFields(parseFields(fieldsParam))
        .setUnique(unique)
        .setMultipart(multipart);

    validateManifest(manifest);

    return manifest;
  }

  private void validateManifest(Manifest manifest) {
    checkRequest(manifest.isUnique() && manifest.getRepos().isEmpty(),
        "If `unique` is specified then `repos` must be non-empty");
    checkRequest(manifest.getFormat() != ManifestFormat.FILES && manifest.isMultipart(),
        "If `multipart` is specified then format must be `files`");
  }

  private void renderManifest(Manifest manifest) {
    val format = manifest.getFormat();
    if (format == TARBALL) {
      renderTarball(manifest);
    } else if (format == FILES) {
      renderFiles(manifest);
    } else if (format == JSON) {
      renderJson(manifest);
    } else {
      checkFormat(format.getKey());
    }
  }

  @SneakyThrows
  private void renderTarball(Manifest manifest) {
    render(manifest, GZIP_TYPE, true);
  }

  private void renderFiles(Manifest manifest) {
    if (manifest.isMultipart()) {
      render(manifest, multipartMixed(manifest.getTimestamp()), false);
    } else {
      render(manifest, TEXT_PLAIN_TYPE, true);
    }
  }

  private void renderJson(Manifest manifest) {
    render(manifest, APPLICATION_JSON_TYPE, false);
  }

  @SneakyThrows
  private void render(Manifest manifest, MediaType mediaType, boolean attachment) {
    // Using {@link HttpServletResponse} is required to commit the response code and headers eagerly. Typically one
    // would use {@link StreamingOutput} but this commits headers on the first write. Since it takes a long time to
    // issue the first write for a manifest containing a large number of files, this causes considerable delays for a
    // user agent to display the download dialog.
    response.setStatus(OK.getStatusCode());
    response.setContentType(mediaType.toString());
    response.addHeader(CACHE_CONTROL, noCache().toString());
    if (attachment) response.addHeader(CONTENT_DISPOSITION, attachmentContent(manifest).toString());

    // Calling this method automatically commits the response, meaning the status code and headers will be written.
    response.flushBuffer();

    try {
      // Write the manifests
      val context = new ManifestContext(manifest, response.getOutputStream());
      manifestService.generateManifests(context);
    } catch (IOException e) {
      // This can happen when a user aborts the connection while streaming
      log.warn("Error generating manifests: {}", e.getMessage());
    }
  }

  private ContentDisposition attachmentContent(Manifest manifest) {
    val date = new Date(manifest.getTimestamp());
    return ContentDisposition.type("attachment")
        .fileName(manifestService.getFileName(manifest))
        .creationDate(date)
        .modificationDate(date)
        .readDate(date)
        .build();
  }

  private static MediaType multipartMixed(long timestamp) {
    return new MediaType(
        MULTIPART_MIXED_TYPE.getType(), MULTIPART_MIXED_TYPE.getSubtype(),
        singletonMap(BOUNDARY_PARAMETER, "boundary_" + timestamp));
  }

  private static List<ManifestField> parseFields(ListParam fieldsParam) {
    return fieldsParam.get().stream().map(ManifestField::fromString).collect(toList());
  }

  private static void checkFormat(String format) {
    checkRequest(isNullOrEmpty(format),
        "`format` is required");
    checkRequest(!isValidEnum(ManifestFormat.class, format.toUpperCase()),
        "`format` is not a valid value in: %s", Arrays.toString(ManifestFormat.values()).toLowerCase());
  }

}
