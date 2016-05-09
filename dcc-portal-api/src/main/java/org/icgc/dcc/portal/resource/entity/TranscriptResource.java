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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.jetty.http.HttpStatus.NOT_FOUND_404;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.icgc.dcc.portal.model.Genes;
import org.icgc.dcc.portal.model.Mutations;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.Transcript;
import org.icgc.dcc.portal.model.param.FiltersParam;
import org.icgc.dcc.portal.model.param.IdsParam;
import org.icgc.dcc.portal.resource.Resource;
import org.icgc.dcc.portal.service.GeneService;
import org.icgc.dcc.portal.service.MutationService;
import org.icgc.dcc.portal.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import com.yammer.dropwizard.jersey.params.IntParam;
import com.yammer.metrics.annotation.Timed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Path("/v1/transcripts")
@Produces(APPLICATION_JSON)
@Api(value = "/transcripts", description = "Resources relating to transcripts")
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class TranscriptResource extends Resource {

  private static final String DEFAULT_FILTERS = "{}";
  private static final String DEFAULT_SIZE = "10";
  private static final String DEFAULT_FROM = "1";
  private static final String DEFAULT_ORDER = "desc";
  private static final String DEFAULT_SORT = "affectedDonorCountFiltered";

  private final MutationService mutationService;
  private final GeneService geneService;

  @Path("/{transcriptId}")
  @GET
  @Timed
  @ApiOperation(value = "Find a transcript by id", notes = "If a transcript does not exist with the specified id an error will be returned", response = Transcript.class)
  @ApiResponses(value = { @ApiResponse(code = NOT_FOUND_404, message = "Transcript not found") })
  public Transcript find(
      @ApiParam(value = "Transcript ID", required = true) @PathParam("transcriptId") String transcriptId,
      @ApiParam(value = "Select fields returned", allowMultiple = true) @QueryParam("field") List<String> fields) {
    log.info("Request for Transcript {}", transcriptId);

    FiltersParam filters = new FiltersParam(String.format("{gene:{affectedTranscriptIds:{is:\"%s\"}}}", transcriptId));

    Genes genes =
        geneService.findAllCentric(Query.builder().filters(filters.get()).fields(fields)
            .includes(Lists.newArrayList("transcripts"))
            .from(0).size(1).sort("affectedDonorCountFiltered").order("desc").build());

    if (genes.getHits().isEmpty()) {
      log.info("Transcript {} not found.", transcriptId);
      String msg = String.format("{\"code\": 404, \"message\":\"Transcript %s not found.\"}", transcriptId);
      throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity(msg).build());
    }

    Transcript t = null;
    for (Transcript transcript : genes.getHits().get(0).getTranscripts()) {
      if (transcript.getId().equals(transcriptId)) {
        t = transcript;
        break;
      }
    }

    return t;
  }

  @Path("/{transcriptId}/mutations")
  @GET
  @Timed
  @ApiOperation(value = "Returns a list of mutations affected by the transcript(s)", response = Mutations.class)
  public Mutations findMutations(
      @ApiParam(value = "Transcript ID. Multiple IDs can be entered as TRXXX,TRYYY", required = true) @PathParam("transcriptId") IdsParam transcriptId,
      @ApiParam(value = "Select fields returned", allowMultiple = true) @QueryParam("field") List<String> fields,
      @ApiParam(value = "Include addition data in the response", allowMultiple = true) @QueryParam("include") List<String> include,
      @ApiParam(value = "Filter the search results") @QueryParam("filters") @DefaultValue(DEFAULT_FILTERS) FiltersParam filters,
      @ApiParam(value = "Start index of results") @QueryParam("from") @DefaultValue(DEFAULT_FROM) IntParam from,
      @ApiParam(value = "Number of results returned", allowableValues = "range[1,100]") @QueryParam("size") @DefaultValue(DEFAULT_SIZE) IntParam size,
      @ApiParam(value = "Column to sort results on") @QueryParam("sort") @DefaultValue(DEFAULT_SORT) String sort,
      @ApiParam(value = "Order to sort the column", allowableValues = "asc,desc") @QueryParam("order") @DefaultValue(DEFAULT_ORDER) String order) {
    log.info("Request for Mutations affected by Transcript {}", transcriptId);
    log.info("Link out to Mutations Resource using transcriptId as filter");

    FiltersParam genes =
        new FiltersParam(String.format("{mutation:{transcriptId:{is:[\"%s\"]}}}",
            Joiner.on("\",\"").join(transcriptId.get())));
    JsonUtils.merge(filters.get(), genes.get());

    return mutationService.findAllCentric(Query.builder().filters(filters.get()).fields(fields).includes(include)
        .from(from.get()).size(size.get()).sort(sort).order(order).build());
  }
}
