/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ANALYSIS_ID_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ANALYSIS_ID_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.checkRequest;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.PhenotypeAnalysis;
import org.icgc.dcc.portal.service.PhenotypeAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * REST API end-points that provide various functionalities for phenotype analysis.
 */
@Slf4j
@Component
@Path("/v1/analysis/phenotype")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PhenotypeAnalysisResource {

  @NonNull
  private final PhenotypeAnalysisService service;

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Creates a new Phenotype analysis by providing IDs of Donor entity sets.", response = PhenotypeAnalysis.class)
  public PhenotypeAnalysis createAnalysis(final List<UUID> entitySetIds) {
    checkRequest(isEmpty(entitySetIds), "The POST payload is null or empty. It should be a list of UUIDs.");

    return service.createAnalysis(entitySetIds);
  }

  @GET
  @Path("/{" + API_ANALYSIS_ID_PARAM + "}")
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieves the result of a phenotype analysis by its ID.", response = PhenotypeAnalysis.class)
  public PhenotypeAnalysis getAnalysis(
      @ApiParam(value = API_ANALYSIS_ID_VALUE, required = true) @PathParam(API_ANALYSIS_ID_PARAM) final UUID analysisId
      ) {
    checkRequest(analysisId == null, API_ANALYSIS_ID_PARAM + " is null.");

    log.info("Received request with {} of '{}'", API_ANALYSIS_ID_PARAM, analysisId);

    return service.getAnalysisResult(analysisId);
  }

}
