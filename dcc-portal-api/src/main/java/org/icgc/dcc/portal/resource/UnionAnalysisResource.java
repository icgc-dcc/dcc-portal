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
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ANALYSIS_ID_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ANALYSIS_ID_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_ENTITY_LIST_DEFINITION_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_SET_ANALYSIS_DEFINITION_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.checkRequest;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.DerivedEntitySetDefinition;
import org.icgc.dcc.portal.model.UnionAnalysisRequest;
import org.icgc.dcc.portal.model.UnionAnalysisResult;
import org.icgc.dcc.portal.service.UnionAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * DropWizard end-points that provide set operation analysis for bench list.
 */

@Slf4j
@Component
@Path("/v1/analysis/union")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UnionAnalysisResource {

  @NonNull
  private final UnionAnalysisService service;

  @GET
  @Path("/{" + API_ANALYSIS_ID_PARAM + "}")
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieves a set analysis by its ID.", response = UnionAnalysisResult.class)
  public UnionAnalysisResult getAnalysis(
      @ApiParam(value = API_ANALYSIS_ID_VALUE, required = true) @PathParam(API_ANALYSIS_ID_PARAM) final UUID analysisId
      ) {

    checkRequest(analysisId == null, API_ANALYSIS_ID_PARAM + " is null.");
    log.info("Received request with {} of '{}'", API_ANALYSIS_ID_PARAM, analysisId);

    return service.getAnalysis(analysisId);
  }

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Creates a set analysis asynchronously. Status can be retrieved by polling the /{id} GET endpoint.",
      response = UnionAnalysisResult.class)
  public Response sumbitAnalysis(@ApiParam(value = API_SET_ANALYSIS_DEFINITION_VALUE) final UnionAnalysisRequest request) {

    checkRequest(request == null, "The payload of /analysis/union is null.");
    log.info("Received union analysis request: '{}'", request);

    val newAnalysis = service.submitAnalysis(request);

    return Response.status(ACCEPTED)
        .entity(newAnalysis)
        .build();
  }

  @POST
  @Path("/preview")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Retrieves a sample data of a set analysis as preview.", response = String.class, responseContainer = "List")
  public List<String> previewSetUnion(
      @ApiParam(value = API_ENTITY_LIST_DEFINITION_VALUE) final DerivedEntitySetDefinition listDefinition
      ) {

    checkRequest(listDefinition == null, "The payload of /analysis/union/preview is null.");

    return service.previewSetUnion(listDefinition);
  }
}
