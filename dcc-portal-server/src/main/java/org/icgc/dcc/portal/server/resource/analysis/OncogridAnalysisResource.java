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
package org.icgc.dcc.portal.server.resource.analysis;

import io.swagger.annotations.ApiParam;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.icgc.dcc.portal.server.model.OncogridAnalysis;
import org.icgc.dcc.portal.server.model.OncogridAnalysisRequest;
import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.service.OncogridAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import java.util.UUID;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/analysis/oncogrid")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OncogridAnalysisResource extends Resource {

  @NonNull
  private final OncogridAnalysisService service;

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public OncogridAnalysis createOncogrid(
      @ApiParam(value = "The entity sets") OncogridAnalysisRequest request) {

    return service.createAnalysis(request.getGeneSet(), request.getDonorSet());
  }

  @GET
  @Path("/{oncogrid}")
  @Produces(APPLICATION_JSON)
  public OncogridAnalysis getOncoGrid(
      @ApiParam(value = "OncoGrid ID", required = true) @PathParam("oncogrid") final UUID analysisId) {
    checkRequest(analysisId == null, "");

    return service.getAnalysis(analysisId);
  }

}
