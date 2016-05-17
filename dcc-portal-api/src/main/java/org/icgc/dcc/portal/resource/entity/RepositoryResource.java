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
package org.icgc.dcc.portal.resource.entity;

import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.icgc.dcc.portal.model.Repository;
import org.icgc.dcc.portal.resource.Resource;
import org.icgc.dcc.portal.service.NotFoundException;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import lombok.val;

@Component
@Path("/" + RepositoryResource.NAME)
@Api(tags = { RepositoryResource.NAME })
@SwaggerDefinition(tags = @Tag(name = RepositoryResource.NAME, description = "Resources relating to external repositories"))
public class RepositoryResource extends Resource {

  public static final String NAME = "repositories";

  @GET
  public List<Map<String, String>> list() {
    return Stream.of(Repository.values()).map(this::convert).collect(toImmutableList());
  }

  @GET
  @Path("/{repoCode}")
  public Map<String, String> get(
      @ApiParam(value = "Repo Code", required = true) @PathParam("repoCode") String repoCode) {
    try {
      val repo = Repository.get(repoCode);
      if (repo == null) {
        throw new NotFoundException(repoCode, "repository");
      }

      return convert(repo);
    } catch (Exception e) {
      throw new NotFoundException(repoCode, "repository");
    }
  }

  private Map<String, String> convert(Repository value) {
    // TODO: Add "type"
    return ImmutableMap.of("repoCode", value.getRepoCode(), "name", value.getName());
  }

}