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

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.portal.server.model.BaseEntitySet.Type.DONOR;
import static org.icgc.dcc.portal.server.analysis.KaplanMeier.Interval;
import static org.icgc.dcc.portal.server.analysis.KaplanMeier.DonorValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.SearchHit;
import org.icgc.dcc.portal.server.model.SurvivalAnalysis;
import org.icgc.dcc.portal.server.model.SurvivalAnalysis.Result;
import org.icgc.dcc.portal.server.model.UnionUnit;
import org.icgc.dcc.portal.server.repository.EntitySetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SurvivalAnalyzer {

  /**
   * Constants.
   */
  private final static List<String> OVERALL = ImmutableList.of("alive");
  private final static List<String> OVERALL_SORT =
      ImmutableList.of("donor_survival_time", "donor_interval_of_last_followup");
  private final static List<String> OVERALL_FIELDS = ImmutableList.<String> builder()
      .add("donor_vital_status")
      .addAll(OVERALL_SORT)
      .build();
  public final static List<String> DISEASE_FREE = ImmutableList.of(
      "complete remission", "partial remission", "stable", "no evidence of disease");
  private final static List<String> DISEASE_FREE_SORT = ImmutableList.of("donor_interval_of_last_followup");
  private final static List<String> DISEASE_FREE_FIELDS = ImmutableList.<String> builder()
      .add("disease_status_last_followup")
      .addAll(DISEASE_FREE_SORT)
      .build();

  /**
   * Dependencies.
   */
  @NonNull
  private final UnionAnalyzer unionAnalyzer;
  @NonNull
  private final EntitySetRepository entitySetRepository;

  /**
   * Method for computing the overall and disease free
   * @param analysis SurvivalAnalysis object with no results.
   * @return SurvivalAnalysis with populated results.
   */
  public SurvivalAnalysis analyze(SurvivalAnalysis analysis) {
    analysis.setResults(new ArrayList<>());
    val setIds = analysis.getEntitySetIds();
    val entitySetMap = getEntitySetMap(setIds);

    boolean intersection = false;
    for (val setId : setIds) {
      val unionUnit = entitySetMap.get(setId);

      // The original size of the set, so we know if there is an intersection.
      val originalCount = entitySetRepository.find(setId).getCount();

      val overallIntervals = runAnalysis(unionUnit, false, SurvivalAnalyzer::hasData);
      val diseaseFreeIntervals = runAnalysis(unionUnit, true, SurvivalAnalyzer::hasDiseaseStatusData);

      analysis.getResults().add(
          analysis.new Result(setId, overallIntervals.getValue(), diseaseFreeIntervals.getValue()));

      if (overallIntervals.getKey() != originalCount.intValue() && !intersection) {
        intersection = true;
      }
    }

    analysis.setIntersection(intersection);
    val results = analysis.getResults();

    val overall = results.stream().map(Result::getOverall).collect(toImmutableList());
    analysis.setOverallStats(new SurvivalLogRank(overall).runLogRankTest());

    val diseaseFree = results.stream().map(Result::getDiseaseFree).collect(toImmutableList());
    analysis.setDiseaseFreeStats(new SurvivalLogRank(diseaseFree).runLogRankTest());

    return analysis;
  }

  private Entry<Integer, List<Interval>> runAnalysis(UnionUnit unionunit, boolean diseaseFree,
                                                                 Predicate<SearchHit> filter) {
    val sort = diseaseFree ? DISEASE_FREE_SORT : OVERALL_SORT;
    val fields = diseaseFree ? DISEASE_FREE_FIELDS : OVERALL_FIELDS;

    val response = unionAnalyzer.computeExclusion(unionunit, DONOR, fields, sort);
    SearchHit[] hits = response.getHits().getHits();
    val donors = filterDonors(hits, filter);

    // Cannot produce curve with one data point.
    val intervals = donors.length <= 1 ? Collections.<Interval> emptyList() : compute(donors, diseaseFree);
    return Maps.immutableEntry(hits.length, intervals);
  }

  private static List<Interval> compute(SearchHit[] donors, boolean diseaseFree) {
    val censuredTerms = diseaseFree ? DISEASE_FREE : OVERALL;
    val censuredField = diseaseFree ? DISEASE_FREE_FIELDS.get(0) : OVERALL_FIELDS.get(0);
    Arrays.asList(donors).sort(comparing(x -> diseaseFree ? getDiseaseTime(x) : getOverallTime(x)));
    val donorValues = Arrays.stream(donors)
        .map( d ->
            new DonorValue(
              d.getId(),
              d.getSource().get(censuredField).toString(),
              diseaseFree ? getDiseaseTime(d) : getOverallTime(d),
              !censuredTerms.contains(d.sourceAsMap().get(censuredField).toString())
            )
        )
        .collect(toList());

    return KaplanMeier.compute(donorValues);
  }

  private static boolean hasData(SearchHit donor) {
    val statusOptional = Optional.ofNullable(donor.sourceAsMap().get("donor_vital_status"));
    if (!statusOptional.isPresent()) {
      return false;
    }

    val status = statusOptional.get().toString();
    if (!status.equalsIgnoreCase("alive") && !status.equalsIgnoreCase("deceased")) {
      return false;
    }

    val time = getOverallTime(donor);
    return time > 0;
  }

  private static Integer getOverallTime(SearchHit donor) {
    val survivalOptional = Optional.ofNullable((Integer) donor.sourceAsMap().get("donor_survival_time"));
    val intervalOptional = Optional.ofNullable((Integer) donor.sourceAsMap().get("donor_interval_of_last_followup"));

    if (survivalOptional.isPresent()) {
      return survivalOptional.get();
    } else if (intervalOptional.isPresent()) {
      return intervalOptional.get();
    } else {
      return -1;
    }
  }

  private static Integer getDiseaseTime(SearchHit donor) {
    val censuredTime = DISEASE_FREE_SORT.get(0);
    return (Integer) donor.sourceAsMap().get(censuredTime);
  }

  private static boolean hasDiseaseStatusData(SearchHit donor) {
    for (val field : DISEASE_FREE_FIELDS) {
      if (donor.sourceAsMap().get(field) == null) return false;
    }

    return true;
  }

  private static Map<UUID, UnionUnit> getEntitySetMap(List<UUID> sets) {
    val entitySetMapBuilder = new ImmutableMap.Builder<UUID, UnionUnit>();

    for (val id : sets) {
      val exclusions = sets.stream()
          .filter(s -> !s.equals(id))
          .collect(toImmutableSet());

      entitySetMapBuilder.put(id, new UnionUnit(ImmutableSet.of(id), exclusions));
    }

    return entitySetMapBuilder.build();
  }

  private static SearchHit[] filterDonors(SearchHit[] hits, Predicate<SearchHit> predicate) {
    return Arrays.stream(hits)
        .filter(predicate)
        .toArray(SearchHit[]::new);
  }

}
