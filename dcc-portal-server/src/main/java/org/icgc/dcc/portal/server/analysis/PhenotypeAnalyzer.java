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
package org.icgc.dcc.portal.server.analysis;

import static com.google.common.base.Preconditions.checkState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import static java.lang.String.format;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import org.icgc.dcc.portal.server.model.EntitySetTermFacet;
import org.icgc.dcc.portal.server.model.PhenotypeResult;
import org.icgc.dcc.portal.server.repository.DonorRepository;
import static org.icgc.dcc.portal.server.repository.DonorRepository.FACETS_FOR_PHENOTYPE;
import org.icgc.dcc.portal.server.repository.EntitySetRepository;
import org.icgc.dcc.portal.server.service.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public final class PhenotypeAnalyzer {

  @NonNull
  private final DonorRepository donorRepository;
  @NonNull
  private final EntitySetRepository entitySetRepository;


  public List<PhenotypeResult> getPhenotypeAnalysisResult(@NonNull final Collection<UUID> entitySetIds) {
    // Here we eliminate duplicates and impose ordering (needed for reading the response items).
    val setIds = ImmutableSet.copyOf(entitySetIds).asList();

    val multiResponse = donorRepository.calculatePhenotypeStats(setIds);
    val responseItems = multiResponse.getResponses();
    val responseItemCount = responseItems.length;
    checkState(responseItemCount == setIds.size(),
            "The number of queries does not match the number of responses in a multi-search.");

    val facetKeyValuePairs = FACETS_FOR_PHENOTYPE.entrySet();

    // Building a map with facet name as the key and a list builder as the value.
    val results = facetKeyValuePairs.stream().collect(
            Collectors.toMap(entry -> entry.getKey(), notUsed -> new ImmutableList.Builder<EntitySetTermFacet>()));

    // Here we enumerate the response collection by entity-set UUIDs (the same grouping as in the multi-search).
    for (int i = 0; i < responseItemCount; i++) {
      val aggregations = responseItems[i].getResponse().getAggregations();

      if (null == aggregations) continue;

      val aggsMap = aggregations.asMap();
      val entitySetId = setIds.get(i);

      val entitySet = entitySetRepository.find(entitySetId);
      if (entitySet == null) {
        throw new NotFoundException(entitySetId.toString(), "donor set");
      }

      val entitySetCount = entitySet.getCount();

      // We go through the main Results map for each facet and build the inner list by populating it with instances of
      // EntitySetTermFacet.
      for (val facetKv : facetKeyValuePairs) {
        val facetName = facetKv.getKey();
        Aggregation aggs = aggsMap.get(facetName);

        Long total = 0L;
        Long missing = 0L;
        // We want to include the number of missing donor documents in our final missing count
        if (aggs instanceof Terms) {
          val termsAggs = (Terms) aggs;

          total = termsAggs.getBuckets()
                  .stream()
                  .mapToLong(Terms.Bucket::getDocCount)
                  .sum();

          missing = entitySetCount - total;
        }

        results.get(facetName).add(
                donorRepository.buildEntitySetTermAggs(
                        entitySetId, (Terms) aggs, aggsMap, total, missing, facetKv.getValue()));
      }
    }

    val finalResult = results.entrySet().stream()
            .map(entry ->
                    new PhenotypeResult(entry.getKey(), entry.getValue().build(),
                            computeStats(entry.getKey(), entry.getValue().build())))
            .collect(toImmutableList());

    return finalResult;
  }


  public static double computeStats(@NonNull String name, @NonNull List<EntitySetTermFacet> result) {
    log.info("Starting Phenotype Stats Computations");

    if (name.equals("ageAtDiagnosisGroup")) {
      // no-op for now.
    } else if (name.equals("gender") || name.equals("vitalStatus")) {
      return chiSquaredPValue(result);
    } else {
      throw new UnsupportedOperationException(format("No operation available for group: %s", name));
    }
    return 0.0;
  }

  /**
   * Private helper responsible for computing P-Value
   *
   * @param data Phenotype result for the current group
   */
  private static double chiSquaredPValue(@NonNull List<EntitySetTermFacet> data) {
    long[][] dataTable = new long[2][data.size()];

    // Transform the results into a table where the values match up in correct columns
    for (int i = 0; i < data.size(); i++) {
      val terms = data.get(i).getTerms();
      for (val term : terms) {
        val cohort = term.getTerm().equals("female") || term.getTerm().equals("alive") ? 0 : 1;
        dataTable[cohort][i] = term.getCount();
      }
    }

    val test = new ChiSquareTest();
    val pValue = test.chiSquareTest(dataTable);
    log.debug("P-Value result: {}", pValue);
    return pValue;
  }

}
