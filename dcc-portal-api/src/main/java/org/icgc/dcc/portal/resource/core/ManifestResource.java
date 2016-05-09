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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.sun.jersey.multipart.Boundary.BOUNDARY_PARAMETER;
import static com.sun.jersey.multipart.MultiPartMediaTypes.MULTIPART_MIXED_TYPE;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.ok;
import static org.icgc.dcc.portal.resource.Resources.API_FILE_REPOS_VALUE;
import static org.icgc.dcc.portal.resource.Resources.API_FILTER_PARAM;
import static org.icgc.dcc.portal.resource.Resources.API_FILTER_VALUE;
import static org.icgc.dcc.portal.util.MediaTypes.GZIP;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.icgc.dcc.portal.manifest.ManifestContext;
import org.icgc.dcc.portal.manifest.ManifestService;
import org.icgc.dcc.portal.manifest.model.Manifest;
import org.icgc.dcc.portal.manifest.model.ManifestField;
import org.icgc.dcc.portal.manifest.model.ManifestFormat;
import org.icgc.dcc.portal.model.param.FiltersParam;
import org.icgc.dcc.portal.model.param.ListParam;
import org.icgc.dcc.portal.resource.Resource;
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
   * Constants.
   */

  // TODO: Improve formal documentation

  private static final String API_MANIFEST_ID_PARAM = "manifestId";
  private static final String API_MANIFEST_ID_VALUE = "The manifest ID";

  private static final String API_FILE_FORMAT_VALUE = "The output format";
  private static final String API_FILE_FORMAT_PARAM = "format";

  private static final String API_FILE_REPOS_PARAM = "repos";

  private static final String API_FILE_FIELDS_VALUE = "What fields to include";
  private static final String API_FILE_FIELDS_PARAM = "fields";

  private static final String API_FILE_UNIQUE_VALUE = "Only return unique files?";
  private static final String API_FILE_UNIQUE_PARAM = "unique";

  private static final String API_FILE_MULTIPART_VALUE = "Multipart output?";
  private static final String API_FILE_MULTIPART_PARAM = "multipart";

  /**
   * Dependencies.
   */
  @NonNull
  private final ManifestService manifestService;

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Saves a manifest returning the posted body with its ID populated", response = Manifest.class)
  public Manifest saveManifest(Manifest manifest) {
    checkRequest(manifest == null,
        "Manifest definition is required");
    checkRequest(manifest.getFormat() == null,
        "manifest 'format' is required");
    validateManifest(manifest);

    manifestService.saveManifest(manifest);

    return manifest;
  }

  @GET
  @Path("/{" + API_MANIFEST_ID_PARAM + "}")
  @ApiOperation(value = "Retrieves a manifest by its ID, overriding output specification as indicated")
  public Response getManifest(
      // Input
      @ApiParam(value = API_MANIFEST_ID_VALUE, required = true) @PathParam(API_MANIFEST_ID_PARAM) UUID manifestId,

      // Output - Overrides
      @ApiParam(value = API_FILE_FORMAT_VALUE) @QueryParam(API_FILE_FORMAT_PARAM) @DefaultValue("") String format,
      @ApiParam(value = API_FILE_FIELDS_VALUE) @QueryParam(API_FILE_FIELDS_PARAM) @DefaultValue("") ListParam fieldsParam,
      @ApiParam(value = API_FILE_MULTIPART_VALUE) @QueryParam(API_FILE_MULTIPART_PARAM) @DefaultValue("") boolean multipart) {

    val manifest = manifestService.getManifest(manifestId);

    // Overrides
    if (hasParam(API_FILE_FORMAT_PARAM)) {
      checkFormat(format);
      manifest.setFormat(ManifestFormat.fromString(format));

      if (manifest.getFormat() != ManifestFormat.JSON) {
        // Needed to pass validation
        manifest.setFields(Collections.emptyList());
      }
    }
    if (hasParam(API_FILE_FIELDS_PARAM)) {
      manifest.setFields(parseFields(fieldsParam));
    }
    if (hasParam(API_FILE_MULTIPART_PARAM)) {
      manifest.setMultipart(multipart);
    }

    validateManifest(manifest);

    return renderManifest(manifest);
  }

  @GET
  @Timed
  @ApiOperation(value = "Generate manifest(s) on the fly as specified by the supplied parameters")
  public Response generateManifest(
      // Input
      @ApiParam(value = API_FILE_REPOS_VALUE) @QueryParam(API_FILE_REPOS_PARAM) @DefaultValue("") ListParam repoParam,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam,
      @ApiParam(value = API_FILE_UNIQUE_VALUE) @QueryParam(API_FILE_UNIQUE_PARAM) @DefaultValue("false") boolean unique,

      // Output
      @ApiParam(value = API_FILE_FORMAT_VALUE) @QueryParam(API_FILE_FORMAT_PARAM) @DefaultValue("json") String format,
      @ApiParam(value = API_FILE_FIELDS_VALUE) @QueryParam(API_FILE_FIELDS_PARAM) @DefaultValue("") ListParam fieldsParam,
      @ApiParam(value = API_FILE_MULTIPART_VALUE) @QueryParam(API_FILE_MULTIPART_PARAM) @DefaultValue("false") boolean multipart) {
    log.debug("filtersParam: '{}', repoParam: '{}'", filtersParam, repoParam);

    checkRequest(isNullOrEmpty(format),
        "'format' is required");
    checkFormat(format);

    val manifest = new Manifest()
        .setFormat(ManifestFormat.fromString(format))
        .setRepos(repoParam.get())
        .setFilters(filtersParam.get())
        .setFields(parseFields(fieldsParam))
        .setUnique(unique)
        .setMultipart(multipart);

    validateManifest(manifest);

    return renderManifest(manifest);
  }

  private void validateManifest(Manifest manifest) {
    checkRequest(manifest.isUnique() && manifest.getRepos().isEmpty(),
        "If 'unique' is specified then 'repos' must be non-empty");
    checkRequest(manifest.getFormat() != ManifestFormat.FILES && manifest.isMultipart(),
        "If 'multipart' is specified then format must be 'files'");
    checkRequest(manifest.getFormat() != ManifestFormat.JSON && !manifest.getFields().isEmpty(),
        "If the 'json' format is not specified then 'fields' must be empty");
  }

  private Response renderManifest(Manifest manifest) {
    switch (manifest.getFormat()) {
    case TARBALL:
      return tarball(manifest);
    case FILES:
      return files(manifest);
    case JSON:
      return json(manifest);
    default:
      throw new IllegalStateException();
    }
  }

  private Response tarball(Manifest manifest) {
    return ok(entityStream(manifest), GZIP)
        .header(CONTENT_DISPOSITION,
            attachmentContent(manifest.getFileName(), manifest.getTimestamp()))
        .build();
  }

  private Response files(Manifest manifest) {
    if (manifest.isMultipart()) {
      val mediaType = multipartMixed(manifest.getTimestamp());
      return ok(entityStream(manifest), mediaType).build();
    } else {
      return ok(entityStream(manifest), TEXT_PLAIN_TYPE)
          .header(CONTENT_DISPOSITION,
              attachmentContent(manifest.getFileName(), manifest.getTimestamp()))
          .build();
    }
  }

  private Response json(Manifest manifest) {
    return ok(entityStream(manifest), APPLICATION_JSON).build();
  }

  private StreamingOutput entityStream(Manifest manifest) {
    // Stream output
    return output -> manifestService.generateManifests(new ManifestContext(manifest, output));
  }

  private static List<ManifestField> parseFields(ListParam fieldsParam) {
    return fieldsParam.get().stream().map(ManifestField::fromString).collect(toList());
  }

  private static void checkFormat(String format) {
    checkRequest(!isValidEnum(ManifestFormat.class, format.toUpperCase()),
        "'format' is not a valid value in: %s", Arrays.toString(ManifestFormat.values()).toLowerCase());
  }

  private static ContentDisposition attachmentContent(String fileName, long timestamp) {
    val date = new Date(timestamp);
    return ContentDisposition.type("attachment")
        .fileName(fileName)
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

}
