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
package org.icgc.dcc.portal.server.resource.set;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.portal.server.resource.Resources.API_FILTER_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_FILTER_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.API_GENE_SET_PARAM;
import static org.icgc.dcc.portal.server.resource.Resources.API_GENE_SET_VALUE;
import static org.icgc.dcc.portal.server.resource.Resources.TOTAL;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.icgc.dcc.portal.server.model.Gene;
import org.icgc.dcc.portal.server.model.GeneSet;
import org.icgc.dcc.portal.server.model.UploadedGeneSet;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.model.param.IdsParam;
import org.icgc.dcc.portal.server.repository.GeneRepository;
import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.service.GeneService;
import org.icgc.dcc.portal.server.service.GeneSetService;
import org.icgc.dcc.portal.server.service.UserGeneSetService;
import org.icgc.dcc.portal.server.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Path("/v1/genesets")
@Produces(APPLICATION_JSON)
@Api(value = "/genesets", description = "Resources relating to gene sets")
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class GeneSetResource extends Resource {

  /**
   * Constants.
   */
  private static final Pattern GENE_DELIMITERS = Pattern.compile("[, \t\r\n]"); // Spaces, tabs, commas, or new lines
  private static final Splitter GENE_ID_SPLITTER = Splitter.on(GENE_DELIMITERS).omitEmptyStrings();
  private static final int MAX_GENE_LIST_SIZE = 1000;

  /**
   * Dependencies.
   */
  @NonNull
  private final UserGeneSetService userGeneSetService;
  @NonNull
  private final GeneSetService geneSetService;
  @NonNull
  private final GeneService geneService;

  @Path("/{id}")
  @GET
  @ApiOperation(value = "Find a gene set by id", notes = "If a gene set does not exist with the specified id an error will be returned", response = GeneSet.class)
  @ApiResponses(value = { @ApiResponse(code = 404, message = "GeneSet not found") })
  public GeneSet get(
      @ApiParam(value = "GeneSet ID", required = true) @PathParam("id") String id,
      @ApiParam(value = "Select fields returned", allowMultiple = true) @QueryParam("field") List<String> fields) {
    log.info("Request for gene set'{}'", id);
    val geneSet = geneSetService.findOne(id, fields);
    log.debug("Returning '{}'", geneSet);

    return geneSet;
  }

  @POST
  @Consumes(APPLICATION_FORM_URLENCODED)
  @Produces(APPLICATION_JSON)
  @ApiOperation(value = "Validate and/or save a gene set")
  public UploadedGeneSet process(
      @ApiParam(value = "The IDs to be saved as a Gene Set") @FormParam("geneIds") String geneIds,
      @ApiParam(value = "Validation") @QueryParam("validationOnly") @DefaultValue("false") boolean validationOnly) {
    val ids = parseIds(geneIds);
    val result = createUploadedGeneSet(ids);

    if (validationOnly) {
      return result;
    }

    val validIds = resolveValidIds(result);

    // Sanity check, we require at least one valid id in order to store
    val invalid = validIds.isEmpty();
    if (invalid) {
      result.getWarnings().add("Request contains no valid gene Ids");

      return result;
    }

    val geneSetId = userGeneSetService.save(validIds);
    result.setGeneListId(geneSetId.toString());

    return result;
  }

  @Path("/{" + API_GENE_SET_PARAM + "}/genes/counts")
  @GET
  @ApiOperation(value = "Find number of genes associated with each gene set")
  public Map<String, Long> countGenes(
      @ApiParam(value = API_GENE_SET_VALUE, required = true) @PathParam(API_GENE_SET_PARAM) IdsParam geneSetIds,
      @ApiParam(value = API_FILTER_VALUE) @QueryParam(API_FILTER_PARAM) @DefaultValue(DEFAULT_FILTERS) FiltersParam filtersParam) {
    val filters = filtersParam.get();
    val geneSetIdFilter = "{gene:{geneSetId:{is:['%s']}}}";
    val geneSets = geneSetIds.get();

    val mergedFilters = mergeFilters(filters, geneSetIdFilter, JsonUtils.join(geneSets));
    val uniqueCount = geneService.count(query().filters(mergedFilters).build());

    val queries = queries(filters, geneSetIdFilter, geneSets);
    val counts = geneService.counts(queries);
    counts.put(TOTAL, uniqueCount);

    return counts;
  }

  private UploadedGeneSet createUploadedGeneSet(List<String> ids) {
    val geneSet = new UploadedGeneSet();
    if (isLimitExceeded(ids)) {
      log.warn("Exceeds maximum size {}", MAX_GENE_LIST_SIZE);
      geneSet.getWarnings().add(
          String.format("Input data exceeds maximum threshold of %s gene identifiers.", MAX_GENE_LIST_SIZE));
      return geneSet;
    }

    val normalizeIds = normalizeIds(ids);
    val validResults = geneService.validateIdentifiers(normalizeIds);
    log.debug("Search results {}", validResults);

    // All matched identifiers
    val matchedIds = resolveMatchedIds(geneSet, validResults);

    // Construct valid and invalid gene matches
    for (val id : ids) {
      val invalid = !matchedIds.contains(id.toLowerCase());
      if (invalid) {
        geneSet.getInvalidGenes().add(id);
      }
    }

    return geneSet;
  }

  private static boolean isLimitExceeded(List<String> ids) {
    return ids.size() > MAX_GENE_LIST_SIZE;
  }

  private static Set<String> resolveMatchedIds(UploadedGeneSet geneSet,
      Map<String, Multimap<String, Gene>> validResults) {
    val matchedIds = Sets.<String> newHashSet();
    for (val searchField : GeneRepository.GENE_ID_SEARCH_FIELDS.values()) {
      val validIds = validResults.get(searchField);
      if (validIds.isEmpty()) continue;

      for (val matchedId : validIds.keySet()) {
        // Case doesn't matter
        matchedIds.add(matchedId.toLowerCase());
      }

      geneSet.getValidGenes().put(searchField, validIds);
    }

    return matchedIds;
  }

  private static List<String> parseIds(String data) {
    return ImmutableList.<String> copyOf(GENE_ID_SPLITTER.split(data));
  }

  private static List<String> normalizeIds(Collection<String> ids) {
    return ids.stream().map(String::toLowerCase).collect(toImmutableList());
  }

  private static Set<String> resolveValidIds(UploadedGeneSet result) {
    // Extract the set of unique Ensembl ids for storage
    val validIds = Sets.<String> newHashSet();
    val validGenes = result.getValidGenes();
    val searchFields = GeneRepository.GENE_ID_SEARCH_FIELDS.values();

    for (val searchField : searchFields) {
      val invalid = !validGenes.containsKey(searchField);
      if (invalid) continue;

      val genes = validGenes.get(searchField).values();
      for (val gene : genes) {
        validIds.add(gene.getId());
      }
    }

    return validIds;
  }

}
