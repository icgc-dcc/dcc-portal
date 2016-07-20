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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;

import javax.annotation.Nonnull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.icgc.dcc.portal.server.model.Repository;
import org.icgc.dcc.portal.server.repository.RepositoryRepository;
import org.icgc.dcc.portal.server.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import lombok.RequiredArgsConstructor;

@Component
@Path("/" + RepositoryResource.VERSION + "/" + RepositoryResource.NAME)
@Api(tags = { RepositoryResource.NAME })
@Produces(APPLICATION_JSON)
@SwaggerDefinition(tags = @Tag(name = RepositoryResource.NAME, description = "Resources relating to external repositories"))
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class RepositoryResource extends Resource {

  /**
   * Constants.
   */
  public static final String NAME = "repositories";
  public static final String VERSION = "v1";

  /**
   * Dependencies.
   */
  @Nonnull
  private final RepositoryRepository repository;

  @GET
  public List<Repository> list() {
    return repository.findAll();
  }

  @GET
  @Path("/{repositoryId}")
  public Repository get(
      @ApiParam(value = "Repository ID", required = true) @PathParam("repositoryId") String repositoryId) {
    return repository.findOne(repositoryId);
  }

}