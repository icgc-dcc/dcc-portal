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
package org.icgc.dcc.portal.server.resource.ui;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.net.URL;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.service.SoftwareService;
import org.icgc.dcc.portal.server.service.SoftwareService.ArtifactFolder;
import org.icgc.dcc.portal.server.service.SoftwareService.MavenArtifactVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.io.Resources;

import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@Component
@Api(hidden = true)
@Path("/v1/ui/software")
@Produces(APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UISoftwareResource extends Resource {

  /**
   * Constants.
   */
  private static final String PUBLIC_KEY_FILE_NAME = "icgc-software.pub";
  private static final String PUBLIC_KEY_PATH = "data/" + PUBLIC_KEY_FILE_NAME;

  /**
   * Dependencies.
   */
  private final SoftwareService softwareService;

  /**
   * Resources - Software.
   */

  @Path("/icgc-storage-client/versions")
  @GET
  @Produces(APPLICATION_JSON)
  public List<MavenArtifactVersion> getArtifacts() {
    val results = softwareService.getMavenVersions();
    return results;
  }

  @Path("/icgc-get/versions")
  @GET
  @Produces(APPLICATION_JSON)
  public List<ArtifactFolder> getIcgcGetArtifacts() {
    val results = softwareService.getIcgcGetVersions();
    return results;
  }

  @Path("/icgc-storage-client/latest")
  @GET
  @SneakyThrows
  public Response getLatest() {
    URL redirect = new URL(softwareService.getLatestVersionUrl());
    return Response.seeOther(redirect.toURI()).build();
  }

  @Path("/icgc-get/{os}/latest")
  @GET
  @SneakyThrows
  public Response getICGCGetLatest(@PathParam("os") String os) {
    URL redirect = new URL(softwareService.getLatestIcgcGetVersionUrl(os));
    return Response.seeOther(redirect.toURI()).build();
  }

  @Path("/icgc-storage-client/{version}")
  @GET
  @SneakyThrows
  public Response getVersion(@PathParam("version") String version) {
    URL redirect = new URL(softwareService.getIcgcStorageClientVersionUrl(version));
    return Response.seeOther(redirect.toURI()).build();
  }

  @Path("/icgc-get/{version}/{os}")
  @GET
  @SneakyThrows
  public Response getICGCGetLinuxVersion(@PathParam("version") String version, @PathParam("os") String os) {
    URL redirect = new URL(softwareService.getIcgcGetVersionUrl(version, os));
    return Response.seeOther(redirect.toURI()).build();
  }

  @Path("/icgc-storage-client/{version}/md5")
  @GET
  @SneakyThrows
  public Response getVersionChecksum(@PathParam("version") String version) {
    val redirect = new URL(softwareService.getVersionChecksumUrl(version));
    return Response.seeOther(redirect.toURI()).build();
  }

  @Path("/icgc-storage-client/{version}/asc")
  @GET
  @SneakyThrows
  public Response getVersionSignature(@PathParam("version") String version) {
    val redirect = new URL(softwareService.getVersionSignatureUrl(version));
    return Response.seeOther(redirect.toURI()).build();
  }

  @Path("/key")
  @GET
  @SneakyThrows
  public Response getKey() {
    val url = Resources.getResource(PUBLIC_KEY_PATH);
    return Response.ok(Resources.toByteArray(url))
        .type("text/plain")
        .header("Content-Disposition", "attachment; filename=\"" + PUBLIC_KEY_FILE_NAME + "\"")
        .build();
  }

}
