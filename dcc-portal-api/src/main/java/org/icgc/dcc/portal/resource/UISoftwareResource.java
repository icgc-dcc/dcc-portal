/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.net.URL;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.icgc.dcc.portal.service.StorageClientService;
import org.icgc.dcc.portal.service.StorageClientService.MavenArtifactVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.io.Resources;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@Component
@Path("/v1/ui/software")
@Produces(APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class UISoftwareResource {

  /**
   * Constants.
   */
  private static final String PUBLIC_KEY_FILE_NAME = "icgc-software.pub";
  private static final String PUBLIC_KEY_PATH = "data/" + PUBLIC_KEY_FILE_NAME;

  /**
   * Dependencies.
   */
  private final StorageClientService storageClientService;

  /**
   * Resources - Software.
   */

  @Path("/icgc-storage-client/versions")
  @GET
  @Produces(APPLICATION_JSON)
  public List<MavenArtifactVersion> getArtifacts() {
    val results = storageClientService.getVersions();
    return results;
  }

  @Path("/icgc-storage-client/latest")
  @GET
  @SneakyThrows
  public Response getLatest() {
    URL redirect = new URL(storageClientService.getLatestVersionUrl());
    return Response.seeOther(redirect.toURI()).build();
  }

  @Path("/icgc-storage-client/{version}")
  @GET
  @SneakyThrows
  public Response getVersion(@PathParam("version") String version) {
    URL redirect = new URL(storageClientService.getVersionUrl(version));
    return Response.seeOther(redirect.toURI()).build();
  }

  @Path("/icgc-storage-client/{version}/md5")
  @GET
  @SneakyThrows
  public Response getVersionChecksum(@PathParam("version") String version) {
    val redirect = new URL(storageClientService.getVersionChecksumUrl(version));
    return Response.seeOther(redirect.toURI()).build();
  }

  @Path("/icgc-storage-client/{version}/asc")
  @GET
  @SneakyThrows
  public Response getVersionSignature(@PathParam("version") String version) {
    val redirect = new URL(storageClientService.getVersionSignatureUrl(version));
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
