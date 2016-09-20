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

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.icgc.dcc.portal.server.analysis.SurvivalAnalyzer.Interval;
import org.icgc.dcc.portal.server.analysis.SurvivalAnalyzer.DonorValue;

import java.util.List;
import java.util.TreeMap;

import static java.lang.System.arraycopy;
import static java.util.Arrays.stream;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

/**
 * Computes Log-Rank statistics for provided survival curves.
 * References:
 * - http://www.mas.ncl.ac.uk/~njnsm/medfac/docs/surv.pdf
 * - http://www.ncbi.nlm.nih.gov/pmc/articles/PMC3059453/
 */
@Slf4j
public class SurvivalLogRank {

  private int numSets;
  private int[] setTotals;
  private int[] totalObserved;

  private int largestTime;

  private TreeMap<Integer, Sample> sampleGroups;

  public SurvivalLogRank(List<List<Interval>> results) {
    numSets = results.size();
    setTotals = new int[numSets];
    totalObserved = new int[numSets];

    for (int i = 0; i < numSets; i++) {
      setTotals[i] = results.get(i).stream()
              .mapToInt(r -> r.getDonors().size())
              .sum();

      totalObserved[i] = results.get(i).stream()
              .mapToInt(r -> r.getDied())
              .sum();
    }

    log.debug("Totals: {}", setTotals);
    constructSampleGroups(results);
    log.debug("TreeMap Size: {}", sampleGroups.size());
  }

  /**
   * Runs the log rank test and returns an object containing the computed info
   * @return Returns {ChiSquared, Degrees of Freedom, P-Value}
   */
  public SurvivalStats runLogRankTest() {
    int[] alive = new int[numSets];
    arraycopy(setTotals, 0, alive, 0, numSets);
    double[] expectedSums = new double[numSets];

    for (val entry: sampleGroups.entrySet()) {
      if (entry.getKey() > largestTime) break;

      int[] died = entry.getValue().died;
      int[] censored = entry.getValue().censured;
      int totalDied = stream(died).sum();
      int totalAlive = stream(alive).sum();

      for (int i = 0; i < numSets; i++) {
        double expected = totalDied * ((double) alive[i]/ (double) totalAlive);
        expectedSums[i] += expected;
        alive[i] = alive[i] - died[i] - censored[i];
      }
    }

    double chiSquared = 0;
    for (int i = 0; i < numSets; i++) {
      chiSquared += Math.pow(totalObserved[i] - expectedSums[i], 2.0) / expectedSums[i];
    }

    val degreesFreedom = numSets - 1;
    val chiSquaredDistribution = new ChiSquaredDistribution(numSets - 1);
    val pValue = 1 - chiSquaredDistribution.cumulativeProbability(chiSquared);
    return new SurvivalStats(chiSquared, degreesFreedom, pValue);
  }

  /**
   * Constructs a map of time -> ([died columns], [censored columns])
   * @param results intervals of the kaplan meier survival plot.
   */
  private void constructSampleGroups(List<List<Interval>> results) {
    sampleGroups = new TreeMap<Integer, Sample>();

    for (int i = 0; i < numSets; i++) {
      val resultDonors = results.get(i).stream()
              .map(r -> r.getDonors())
              .flatMap(d -> d.stream())
              .collect(toImmutableList());

      for (val donor : resultDonors) {
        val time = donor.getTime();

        Sample sample = sampleGroups.get(time);
        if (sample == null) {
          sample = new Sample(numSets);
        }

        if (isCensured(donor)) {
          sample.censured[i]++;
        } else {
          if (time > largestTime) largestTime = time;
          sample.died[i]++;
        }
        sampleGroups.put(time, sample);
      }
    }

  }

  private boolean isCensured(DonorValue donor) {
    return donor.getStatus().equals("alive") || SurvivalAnalyzer.DISEASE_FREE.contains(donor.getStatus());
  }

  @Value
  public class SurvivalStats {
    public double chiSquared;
    public int degreesFreedom;
    public double pValue;
  }

  private class Sample {

    public Sample(int sets) {
      died = new int[sets];
      censured = new int[sets];
    }

    int[] died;
    int[] censured;

  }

}
