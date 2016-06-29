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
package org.icgc.dcc.portal.analysis;

import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.portal.model.BaseEntitySet.Type.DONOR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.search.SearchHit;
import org.icgc.dcc.portal.model.SurvivalAnalysis;
import org.icgc.dcc.portal.model.SurvivalAnalysis.Result;
import org.icgc.dcc.portal.model.UnionUnit;
import org.icgc.dcc.portal.repository.DonorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SurvivalAnalyzer {

  /**
   * Dependencies.
   */
  @NonNull
  private final DonorRepository donorRepository;
  @NonNull
  private final UnionAnalyzer unionAnalyzer;

  public SurvivalAnalysis analyze(SurvivalAnalysis analysis) {
    analysis.setResults(new ArrayList<Result>());
    val sets = analysis.getEntitySetIds();
    val entitySetMap = getEntitySetMap(sets);

    for (val id : sets) {
      val response = unionAnalyzer.computeExclusion(entitySetMap.get(id), DONOR);

      val filteredDonors = Arrays.stream(response.getHits().getHits())
          .filter(SurvivalAnalyzer::hasData)
          .toArray(size -> new SearchHit[size]);

      val intervals = compute(filteredDonors);
      analysis.getResults().add(analysis.new Result(id, intervals));
    }

    return analysis;
  }

  private static List<Interval> compute(SearchHit[] donors) {
    int[] time = new int[donors.length];
    boolean[] censured = new boolean[donors.length];

    for (int i = 0; i < donors.length; i++) {
      val donor = donors[i];
      time[i] = (int) donor.field("donor_survival_time").getValue();
      censured[i] = ((String) donor.field("donor_vital_status").getValue()).equalsIgnoreCase("alive");
    }

    val intervals = new ArrayList<Interval>();
    int startTime = 0;
    int endTime = 0;

    for (int i = 0; i < time.length; i++) {
      endTime = time[i];
      if (censured[i] == false && endTime > startTime) {
        intervals.add(new Interval(startTime, endTime));
        startTime = endTime;
      }
    }
    if (endTime > startTime) {
      intervals.add(new Interval(startTime, endTime));
    }

    // init variables. Initially everyone is at risk, and the cumulative survival is 1
    float atRisk = time.length;
    float cumulativeSurvival = 1;
    val intervalIter = intervals.iterator();

    // This implementation later mutates this reference.
    Interval currentInterval = intervalIter.next();
    currentInterval.setCumulativeSurvival(cumulativeSurvival);

    for (int i = 0; i < time.length; i++) {

      long t = time[i];

      // If we have moved past the current interval compute the cumulative survival and adjust the # at risk
      // for the start of the next interval.
      if (t > currentInterval.getEnd()) {
        atRisk -= currentInterval.getCensured();
        float survivors = atRisk - currentInterval.getDied();
        float tmp = survivors / atRisk;
        cumulativeSurvival *= tmp;

        // Skip to the next interval
        atRisk -= currentInterval.getDied();
        while (intervalIter.hasNext() && t > currentInterval.getEnd()) {
          currentInterval = intervalIter.next();
          currentInterval.setCumulativeSurvival(cumulativeSurvival);
        }
      }

      val donor = new DonorValue(
          donors[i].getId(),
          (String) donors[i].field("donor_vital_status").getValue(),
          time[i]);
      currentInterval.addDonor(donor);

      if (!censured[i]) {
        currentInterval.incDied();
      }
    }
    currentInterval.setCumulativeSurvival(cumulativeSurvival);

    return intervals;
  }

  private static boolean hasData(SearchHit donor) {
    val status = (String) donor.field("donor_vital_status").getValue();
    return status.equalsIgnoreCase("alive") || status.equalsIgnoreCase("deceased");
  }

  private static Map<UUID, UnionUnit> getEntitySetMap(List<UUID> sets) {
    val entitySetMapBuilder = new ImmutableMap.Builder<UUID, UnionUnit>();

    for (val id : sets) {
      val exclusions = sets.stream()
          .filter(s -> !s.equals(id))
          .collect(toImmutableSet());

      entitySetMapBuilder.put(id, new UnionUnit(ImmutableSet.<UUID> of(id), exclusions));
    }

    return entitySetMapBuilder.build();
  }

  @Data
  public static class Interval {

    private final int start;
    private final int end;
    private int died;
    private List<DonorValue> donors = new ArrayList<DonorValue>();
    private float cumulativeSurvival;

    void incDied() {
      died++;
    }

    void addDonor(DonorValue donor) {
      donors.add(donor);
    }

    public int getCensured() {
      int sum = 0;
      for (val donor : donors) {
        sum += donor.getStatus().equalsIgnoreCase("alive") ? 1 : 0;
      }
      return sum;
    }

  }

  @Value
  public static class DonorValue {

    String id;
    String status;
    int survivalTime;

  }

}
