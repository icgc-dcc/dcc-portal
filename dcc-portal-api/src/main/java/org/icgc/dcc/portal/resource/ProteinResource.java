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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import lombok.RequiredArgsConstructor;

import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.IdsParam;
import org.icgc.dcc.portal.model.Mutations;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.service.MutationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.metrics.annotation.Timed;

@Component
@Path("/v1/protein")
@Produces(APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class ProteinResource {

  private final MutationService mutationService;

  @Path("/{transcriptId}")
  @GET
  @Timed
  @ApiOperation(value = "Returns a list of mutations affected by the transcript(s)", response = Mutations.class)
  public Mutations findMutations(
      @ApiParam(value = "Transcript ID. Multiple IDs can be entered as TRXXX,TRYYY", required = true) @PathParam("transcriptId") IdsParam transcriptId
      ) {
    FiltersParam filters =
        new FiltersParam(String.format("{mutation:{transcriptId:{is:[\"%s\"]}}}",
            Joiner.on("\",\"").join(transcriptId.get())));
    return mutationService.protein(Query.builder().filters(filters.get()).sort("affectedDonorCountTotal").order("desc")
        .build());
  }
}
