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

import static javax.ws.rs.core.MediaType.TEXT_XML;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

import java.io.IOException;
import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.icgc.dcc.portal.server.repository.RepositoryRepository;
import org.icgc.dcc.portal.server.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.swagger.annotations.Api;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Api(hidden = true)
@Path("/v1/ui/collaboratory")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UICollaboratoryResource extends Resource {

  /**
   * Constants.
   */
  private static final String COLLAB_META_URL = "https://object.cancercollaboratory.org:9080/oicr.icgc.meta/metadata/";

  /**
   * Dependencies.
   */
  @NonNull
  private final RepositoryRepository repositories;

  @Path("/metadata/{objectId}")
  @GET
  @SneakyThrows
  public Response getMeta(@PathParam("objectId") String objectId) {
    val repo = repositories.findOne("collaboratory");
    val metaUrl = new URL(COLLAB_META_URL + objectId);
    try {
      val input = metaUrl.openStream();

      return Response.ok(input)
          .type(TEXT_XML)
          .build();
    } catch (IOException e) {
      val status = SERVICE_UNAVAILABLE;
      val message = "Error accessing " + repo.getName() + " metadata url " + metaUrl;
      log.error(message, e);
      return error(status, message + ". " + e.getMessage());
    }
  }

}
