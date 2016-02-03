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
package org.icgc.dcc.portal.analysis;

import static com.google.common.base.Stopwatch.createStarted;
import static org.icgc.dcc.common.core.util.FormatUtils.formatCount;
import static org.icgc.dcc.portal.analysis.EnrichmentAnalyses.adjustRawGeneSetResults;
import static org.icgc.dcc.portal.analysis.EnrichmentAnalyses.calculateExpectedGeneCount;
import static org.icgc.dcc.portal.analysis.EnrichmentAnalyses.calculateGeneCountPValue;
import static org.icgc.dcc.portal.analysis.EnrichmentQueries.geneSetOverlapQuery;
import static org.icgc.dcc.portal.analysis.EnrichmentQueries.overlapQuery;
import static org.icgc.dcc.portal.analysis.EnrichmentSearchResponses.getUniverseTermsFacet;
import static org.icgc.dcc.portal.model.EnrichmentAnalysis.State.ANALYZING;
import static org.icgc.dcc.portal.model.EnrichmentAnalysis.State.ERROR;
import static org.icgc.dcc.portal.model.EnrichmentAnalysis.State.FINISHED;
import static org.icgc.dcc.portal.model.EnrichmentAnalysis.State.POST_PROCESSING;
import static org.icgc.dcc.portal.model.Query.idField;
import static org.icgc.dcc.portal.repository.TermsLookupRepository.TermLookupType.GENE_IDS;
import static org.icgc.dcc.portal.util.Facets.getFacetCounts;
import static org.icgc.dcc.portal.util.SearchResponses.getHitIds;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.icgc.dcc.portal.model.EnrichmentAnalysis.Overview;
import org.icgc.dcc.portal.model.EnrichmentAnalysis.Result;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.Universe;
import org.icgc.dcc.portal.repository.DonorRepository;
import org.icgc.dcc.portal.repository.EnrichmentAnalysisRepository;
import org.icgc.dcc.portal.repository.GeneRepository;
import org.icgc.dcc.portal.repository.GeneSetRepository;
import org.icgc.dcc.portal.repository.MutationRepository;
import org.icgc.dcc.portal.repository.TermsLookupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Technical specification for this feature may be found here:
 * 
 * https://wiki.oicr.on.ca/display/DCCSOFT/Data+Portal+-+Enrichment+Analysis+-+Technical+Specification
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class EnrichmentAnalyzer {

  /**
   * DCC-2856: Gene set cardinality threshold.
   */
  private static final int GENE_SET_GENE_COUNT_THRESHOLD = 0;

  /**
   * Dependencies.
   */
  @NonNull
  private final TermsLookupRepository termsLookupRepository;
  @NonNull
  private final EnrichmentAnalysisRepository analysisRepository;
  @NonNull
  private final GeneRepository geneRepository;
  @NonNull
  private final GeneSetRepository geneSetRepository;
  @NonNull
  private final DonorRepository donorRepository;
  @NonNull
  private final MutationRepository mutationRepository;

  /**
   * This method runs asynchronously to perform enrichment analysis.
   * 
   * @param analysis the definition
   */
  @Async
  @SneakyThrows
  public void analyze(@NonNull UUID analysisId) {
    val watch = createStarted();

    /**
     * Phase 0: Pending
     */

    log.info("[{}] Getting analysis...", analysisId);
    val analysis = analysisRepository.find(analysisId);
    val dataVersion = analysis.getVersion();

    // Updated state for UI polling
    analysis.setState(ANALYZING);

    log.info("[{}] Updating initial analysis @ {} ...", analysisId, watch);
    analysisRepository.update(analysis, dataVersion);

    try {

      /**
       * Phase 1: Analyzing
       */

      // Shorthands
      val query = analysis.getQuery();
      val params = analysis.getParams();
      val universe = params.getUniverse();
      val inputGeneListId = analysis.getId();
      log.info("Analyzing {}...", analysis);

      // Determine "InputGeneList"
      log.info("[{}] Finding input gene list @ {}...", analysisId, watch);
      val inputGeneList = findInputGeneList(query, params.getMaxGeneCount());

      // Save ids in index for efficient search using "term lookup"
      log.info("[{}] Indexing input gene list ({} genes) @ {}...",
          new Object[] { analysisId, formatCount(inputGeneList.size()), watch });
      indexInputGeneList(inputGeneListId, inputGeneList);

      // Get all gene-set gene counts of the input query
      log.info("[{}] Calculating overlap gene set counts @ {}...", analysisId, watch);
      val overlapGeneSetCounts = findOverlapGeneSetCounts(
          query,
          universe,
          inputGeneListId);

      // Overview section
      log.info("[{}]Calculating overview @ {}...", analysisId, watch);
      val overview = analyzeOverview(query, universe, inputGeneListId);

      log.info("[{}] Finsined gene set gene counts @ {}...", analysisId, watch);
      val geneSetGeneCounts = findGeneSetGeneCounts(overlapGeneSetCounts.keySet());

      // Perform gene-set specific calculations
      log.info("[{}] Calculating raw gene set results @ {}...", analysisId, watch);
      val rawResults = analyzeGeneSetResults(
          query,
          universe,
          inputGeneListId,

          geneSetGeneCounts,
          overlapGeneSetCounts,
          overview.getOverlapGeneCount(),
          overview.getUniverseGeneCount());

      // Unfiltered gene-set count
      overview.setOverlapGeneSetCount(rawResults.size());

      // Statistical adjustment
      log.info("[{}] Adjusting raw gene set results @ {}...", analysisId, watch);
      val adjustedResults = adjustRawGeneSetResults(params.getFdr(), rawResults);

      // Log the number of gene sets that are in overlap and fits the FDR criteria
      overview.setOverlapFdrGeneSetCount(adjustedResults.size());

      // Keep only the number of results that the user requested
      val limitedAdjustedResults = limitGeneSetResults(adjustedResults, params.getMaxGeneSetCount());

      // Add additional descriptive data
      log.info("[{}] Setting gene set names @ {}...", analysisId, watch);
      setGeneSetNames(limitedAdjustedResults);

      // Update state for UI polling
      analysis.setOverview(overview);
      analysis.setResults(limitedAdjustedResults);
      analysis.setState(POST_PROCESSING);

      log.info("[{}] Updating initial analysis @ {} ...", analysisId, watch);
      analysisRepository.update(analysis, dataVersion);

      /**
       * Phase 2: Post-Processing
       */

      log.info("Calculating final gene set results @ {}...", watch);
      postProcessGeneSetResults(query, universe, inputGeneListId, limitedAdjustedResults);

      // Update state for UI polling
      analysis.setState(FINISHED);

      log.info("[{}] Updating final analysis @ {} ...", analysisId, watch);
      analysisRepository.update(analysis, dataVersion);

      log.info("Finished analyzing in {}", watch);
    } catch (Throwable t) {
      // Update state for UI termination
      analysis.setState(ERROR);

      log.error("[{}] Error analyzing @ {}: {}", new Object[] { analysisId, watch, t.getMessage() });
      analysisRepository.update(analysis, dataVersion);

      throw t;
    }
  }

  private Overview analyzeOverview(Query query, Universe universe, UUID inputGeneListId) {
    return new Overview()
        .setOverlapGeneCount(countGenes(overlapQuery(query, universe, inputGeneListId)))
        .setUniverseGeneCount(countUniverseGenes(universe))
        .setUniverseGeneSetCount(countUniverseGeneSets(universe));
  }

  private List<Result> analyzeGeneSetResults(Query query, Universe universe, UUID inputGeneListId,
      Map<String, Integer> geneSetGeneCounts, Map<String, Integer> overlapGeneSetGeneCounts, int overlapGeneCount,
      int universeGeneCount) {
    val results = Lists.<Result> newArrayList();
    int i = 0;
    for (val entry : overlapGeneSetGeneCounts.entrySet()) {
      val geneSetId = entry.getKey();
      val geneSetGeneCount = geneSetGeneCounts.get(geneSetId);
      int geneSetOverlapGeneCount = entry.getValue();

      log.info("[{}/{}] Processing {}", new Object[] { i++, overlapGeneSetGeneCounts.size(), geneSetId });
      if (geneSetId.equals(universe.getGeneSetId())) {
        // T6: Skip universe as this will trivially be most enriched by definition
        log.info("Skipping universe gene set: {}", geneSetId);
        continue;
      }

      if (geneSetGeneCount < GENE_SET_GENE_COUNT_THRESHOLD) {
        // DCC-2856: Any of the candidate gene sets must have c or more total genes. i.e., gene sets with m >= c.
        // Otherwise, the gene set will be simply ignored
        log.info("Skipping gene set due to cardinality threshold: {}", geneSetId);
        continue;
      }

      val result = analyzeGeneSetResult(
          query,
          universe,
          inputGeneListId,
          geneSetId,

          // Formula inputs
          geneSetGeneCount,
          geneSetOverlapGeneCount,
          overlapGeneCount,
          universeGeneCount);

      // Add result for the current gene-set
      results.add(result);
    }

    return results;
  }

  private Result analyzeGeneSetResult(Query query, Universe universe, UUID inputGeneListId, String geneSetId,
      int geneSetGeneCount, int geneSetOverlapGeneCount, int overlapGeneCount, int universeGeneCount) {
    // Statistics
    val expectedGeneCount = calculateExpectedGeneCount(
        overlapGeneCount,
        geneSetGeneCount, universeGeneCount);
    val pValue = calculateGeneCountPValue(
        geneSetOverlapGeneCount, overlapGeneCount, // The "four numbers"
        geneSetGeneCount, universeGeneCount);

    log.debug("q = {}, k = {}, m = {}, n = {}, pValue = {}",
        new Object[] { geneSetOverlapGeneCount, overlapGeneCount, geneSetGeneCount, universeGeneCount, pValue });

    // Assemble
    return new Result()
        .setGeneSetId(geneSetId)

        .setGeneCount(geneSetGeneCount)
        .setOverlapGeneSetGeneCount(geneSetOverlapGeneCount)

        .setExpectedValue(expectedGeneCount)
        .setPValue(pValue);
  }

  private static List<Result> limitGeneSetResults(List<Result> results, int maxGeneSetCount) {
    return results.size() < maxGeneSetCount ? results : results.subList(0, maxGeneSetCount);
  }

  private void setGeneSetNames(List<Result> results) {

    val geneSetNames = findGeneSetNames(Result.getGeneSetIds(results));
    for (val result : results) {
      val geneSetId = result.getGeneSetId();

      // Update
      result.setGeneSetName(geneSetNames.get(geneSetId));
    }
  }

  private void postProcessGeneSetResults(Query query, Universe universe, UUID inputGeneListId, List<Result> results) {
    for (int i = 0; i < results.size(); i++) {
      val geneSetResult = results.get(i);
      val geneSetId = geneSetResult.getGeneSetId();

      log.debug("[{}/{}] Post-processing {}", new Object[] { i + 1, results.size(), geneSetId });
      val geneSetOverlapQuery = geneSetOverlapQuery(query, universe, inputGeneListId, geneSetId);

      // Update
      geneSetResult
          .setOverlapGeneSetDonorCount(countDonors(geneSetOverlapQuery))
          .setOverlapGeneSetMutationCount(countMutations(geneSetOverlapQuery));
    }
  }

  /*
   * Data access methods
   */

  private List<String> findInputGeneList(Query query, int maxGeneCount) {
    val limitedGeneQuery = Query.builder()
        .fields(idField())
        .filters(query.getFilters())
        .sort(query.getSort())
        .order(query.getOrder().toString())

        // This is non standard in terms of size of result set, but its just ids
        .size(maxGeneCount)
        .limit(maxGeneCount)
        .build();

    return getHitIds(geneRepository.findAllCentric(limitedGeneQuery));
  }

  private Map<String, Integer> findGeneSetGeneCounts(Iterable<String> geneSetIds) {
    return geneSetRepository.countGenes(geneSetIds);
  }

  private Map<String, Integer> findOverlapGeneSetCounts(Query query, Universe universe, UUID inputGeneListId) {
    val overlapQuery = overlapQuery(query, universe, inputGeneListId);
    val response = geneRepository.findGeneSetCounts(overlapQuery);
    val geneSetFacet = getUniverseTermsFacet(response, universe);

    return getFacetCounts(geneSetFacet);
  }

  private Map<String, String> findGeneSetNames(Iterable<String> geneSetIds) {
    return geneSetRepository.findName(geneSetIds);
  }

  private int countGenes(Query query) {
    return (int) geneRepository.count(query);
  }

  private int countDonors(Query query) {
    return (int) donorRepository.count(query);
  }

  private int countMutations(Query query) {
    return (int) mutationRepository.count(query);
  }

  private int countGeneSetGenes(String geneSetId) {
    return geneSetRepository.countGenes(geneSetId);
  }

  private int countUniverseGenes(Universe universe) {
    if (universe.isGo()) {
      return countGeneSetGenes(universe.getGeneSetId());
    } else {
      return (int) geneRepository.count(Query.builder().filters(universe.getFilter()).build());
    }
  }

  private int countUniverseGeneSets(Universe universe) {
    return geneSetRepository.countDecendants(universe.getGeneSetType(), Optional.fromNullable(universe.getGeneSetId()));
  }

  @SneakyThrows
  private void indexInputGeneList(UUID inputGeneListId, List<String> inputGeneList) {
    termsLookupRepository.createTermsLookup(GENE_IDS, inputGeneListId, inputGeneList);
  }

}
