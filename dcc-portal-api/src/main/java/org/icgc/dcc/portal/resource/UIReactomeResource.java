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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.primitives.Doubles.tryParse;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static org.icgc.dcc.portal.resource.ResourceUtils.checkRequest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.icgc.dcc.portal.model.DiagramProtein;
import org.icgc.dcc.portal.model.FieldsParam;
import org.icgc.dcc.portal.service.DiagramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wordnik.swagger.annotations.ApiParam;

import lombok.RequiredArgsConstructor;

@Component
@Path("/v1/ui/reactome")
@Produces(APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class UIReactomeResource {

  /**
   * Constants.
   */
  private static final String REACTOME_PREFIX = "R-HSA-";
  private static final String REACTOME_PREFIX_OLD = "REACT_";

  /**
   * Dependencies.
   */
  private final DiagramService diagramService;

  @Path("/protein-map")
  @GET
  public Map<String, DiagramProtein> getReactomeProteinMap(
      @ApiParam(value = "A pathway reactome id", required = true) @QueryParam("pathwayId") String pathwayId,
      @ApiParam(value = "The functional impact filter", required = true) @QueryParam("impactFilter") FieldsParam impactFilter) {

    checkRequest(isInvalidPathwayId(pathwayId), "Pathway id '%s' is empty or not valid", pathwayId);

    return diagramService.mapProteinIds(pathwayId, impactFilter.get());
  }

  @Path("/pathway-diagram")
  @GET
  @Produces(APPLICATION_XML)
  public Response getReactomePathwayDiagram(
      @ApiParam(value = "A pathway reactome id", required = true) @QueryParam("pathwayId") String pathwayId) {

    checkRequest(isInvalidPathwayId(pathwayId), "Pathway id '%s' is empty or not valid", pathwayId);

    return Response.ok(diagramService.getPathwayDiagramString(pathwayId), APPLICATION_XML).build();
  }

  @Path("/pathway-sub-diagram")
  @GET
  public List<String> getShownPathwaySection(
      @ApiParam(value = "A non-diagrammed pathway reactome id", required = true) @QueryParam("pathwayId") String pathwayId) {

    checkRequest(isInvalidPathwayId(pathwayId), "Pathway id '%s' is empty or not valid", pathwayId);

    return diagramService.getShownPathwaySection(pathwayId);
  }

  private static Boolean isInvalidPathwayId(String id) {
    if (isNullOrEmpty(id)) return true;
    if (!(id.startsWith(REACTOME_PREFIX_OLD) || id.startsWith(REACTOME_PREFIX))) return true;

    return tryParse(id.substring(6)) == null;
  }

}
