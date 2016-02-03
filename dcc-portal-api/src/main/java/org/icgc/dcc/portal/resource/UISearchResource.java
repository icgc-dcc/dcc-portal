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

import static com.google.common.base.Charsets.UTF_8;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_GENE_PARAM;
import static org.icgc.dcc.portal.resource.ResourceUtils.API_GENE_VALUE;
import static org.icgc.dcc.portal.resource.ResourceUtils.DEFAULT_ORDER;
import static org.icgc.dcc.portal.resource.ResourceUtils.MULTIPLE_IDS;
import static org.icgc.dcc.portal.util.JsonUtils.merge;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.model.FiltersParam;
import org.icgc.dcc.portal.model.IdsParam;
import org.icgc.dcc.portal.model.Mutations;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.TermFacet;
import org.icgc.dcc.portal.service.DonorService;
import org.icgc.dcc.portal.service.GeneService;
import org.icgc.dcc.portal.service.MutationService;
import org.icgc.dcc.portal.service.OccurrenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.dropwizard.jersey.params.IntParam;

@Slf4j
@Component
@Path("/v1/ui/search")
@Produces(APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UISearchResource {

  /**
   * Constants.
   */
  private static final String DEFAULT_FILTERS = "{}";

  /**
   * Dependencies.
   */
  private final DonorService donorService;
  private final OccurrenceService occurrenceService;
  private final MutationService mutationService;
  private final GeneService geneService;

  /**
   * Resource - Search.
   */
  @Path("/donor-mutations")
  @GET
  public Mutations getDonorMutations(
      @ApiParam(value = "Filter the search results") @QueryParam("filters") @DefaultValue(DEFAULT_FILTERS) FiltersParam filters,
      @ApiParam(value = "The donor to search for ") @QueryParam("donorId") String donorId,
      @ApiParam(value = "From") @QueryParam("from") @DefaultValue("1") IntParam from,
      @ApiParam(value = "Size") @QueryParam("size") @DefaultValue("10") IntParam size) {

    val query =
        Query.builder().filters(filters.get()).sort("_score").from(from.get()).size(size.get()).order(DEFAULT_ORDER)
            .includes(ImmutableList.of("consequences")).build();

    return mutationService.findMutationsByDonor(query, donorId);
  }

  /**
   * This is used to fetch project-donorCount breakdown for a list of genes. It builds the data for gene chart on the
   * projects page.
   * 
   * gene1: [ proj1: K11, proj2: K12 ... projN:K1N ] ... geneM: [ proj1: KM1, proj2: KM2 ... projN:KMN ]
   * 
   * FIXME: Checkout elasticsearch aggregation framework when we have it to see if it can alleviate the amount of
   * requests, which is based on # of genes passed in.
   */
  @Path("/gene-project-donor-counts/{geneIds}")
  @GET
  public Map<String, TermFacet> countProjectDonor(
      @ApiParam(value = "Gene ID. Multiple IDs can be entered as ENSG00000155657,ENSG00000141510", required = true) @PathParam("geneIds") IdsParam geneIds,
      @ApiParam(value = "Filter the search results") @QueryParam("filters") @DefaultValue(DEFAULT_FILTERS) FiltersParam filters) {
    val geneFilterJson = "{gene:{id:{is:[\"%s\"]}}}";
    val userFilter = filters.get();

    val genes = geneIds.get();
    val queries = genes.stream().map(gene -> {
      final FiltersParam geneFilter = new FiltersParam(format(geneFilterJson, gene));
      final ObjectNode filterNode = merge(userFilter, geneFilter.get());

      return Query.builder().filters(filterNode)
          .build();
    }).collect(toImmutableList());

    return donorService.projectDonorCount(genes, queries);
  }

  @Path("/projects/donor-mutation-counts")
  @GET
  public Map<String, Map<String, Integer>> getProjectDonorMutation() {
    return occurrenceService.getProjectMutationDistribution();
  }

  @Path("/file")
  @Consumes(MULTIPART_FORM_DATA)
  @POST
  public Map<String, String> uploadIds(
      @FormDataParam("filepath") InputStream inputStream,
      @FormDataParam("filepath") FormDataContentDisposition fileDetail) throws Exception {
    log.info("Input stream {}", inputStream);
    log.info("Content disposition {}", fileDetail);
    val content = CharStreams.toString(new InputStreamReader(inputStream, UTF_8));

    return ImmutableMap.of("data", content);
  }

  @Path("/gene-symbols/{" + API_GENE_PARAM + "}")
  @GET
  public Map<String, String> ensemblIdGeneSymbolMappings(
      @ApiParam(value = API_GENE_VALUE + MULTIPLE_IDS + " (e.g. ENSG00000155657,ENSG00000141510).", required = true) @PathParam(API_GENE_PARAM) IdsParam geneIds
      ) {
    return geneService.getEnsemblIdGeneSymbolMap(geneIds.get());
  }

}
