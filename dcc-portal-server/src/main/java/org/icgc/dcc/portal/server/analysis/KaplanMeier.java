/*
 * Copyright (c) 2017 The Ontario Institute for Cancer Research. All rights reserved.
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

import lombok.Data;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class KaplanMeier {

  public static List<Interval> compute(List<DonorValue> donors) {
    val size = donors.size();
    int[] time = new int[donors.size()];
    boolean[] censured = new boolean[donors.size()];

    for (int i = 0; i < size; i++) {
      val donor = donors.get(i);
      time[i] = donor.getTime();
      censured[i] = !donor.isDeceased();
    }

    val intervals = new ArrayList<Interval>();
    int startTime = 0;
    int endTime = 0;

    for (int i = 0; i < time.length; i++) {
      endTime = time[i];
      if (!censured[i] && endTime > startTime) {
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

    // Guard with short return if no intervals in iterator.
    if (!intervalIter.hasNext()) {
      return intervals;
    }

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

      currentInterval.addDonor(donors.get(i));

      if (!censured[i]) {
        currentInterval.incDied();
      }
    }
    currentInterval.setCumulativeSurvival(cumulativeSurvival);

    confidenceIntervals(intervals, size);
    return intervals;
  }

  private static void confidenceIntervals(List<Interval> intervals, int numDonors) {
    for (int i = 0; i< intervals.size(); i++) {
      val interval = intervals.get(i);
      if (interval.getCumulativeSurvival() <= 0f) {
        interval.setUpperConfidence(0f);
        interval.setLowerConfidence(0f);
      } else if (interval.getCumulativeSurvival() >= 1f) {
        interval.setUpperConfidence(1f);
        interval.setLowerConfidence(1f);
      } else {
        double loglog = Math.log( -1.0 * Math.log(interval.cumulativeSurvival));

        int atRisk = numDonors;
        double sigma = 0.0;
        for(int j = 0; j < i; j++) {
          val sigmaInterval = intervals.get(j);
          val died = (double) sigmaInterval.getDied();
          sigma += died / (atRisk * (atRisk - died));
          atRisk -= died;
        }

        double variance = sigma / Math.pow(Math.log(interval.getCumulativeSurvival()), 2);

        val c1 = loglog + (1.96 * Math.sqrt(variance));
        val c2 = loglog - (1.96 * Math.sqrt(variance));

        interval.setUpperConfidence((float) Math.exp( -1.0 * Math.exp(c2)));
        interval.setLowerConfidence((float) Math.exp( -1.0 * Math.exp(c1)));

        log.debug("{} {} {}", interval.getCumulativeSurvival(), interval.getLowerConfidence(), interval.getUpperConfidence());
      }
    }
  }

  @Data
  public static class Interval {

    private final int start;
    private final int end;
    private int died;
    private List<DonorValue> donors = new ArrayList<>();
    private float cumulativeSurvival;
    private float upperConfidence;
    private float lowerConfidence;

    void incDied() {
      died++;
    }

    void addDonor(DonorValue donor) {
      donors.add(donor);
    }

    int getCensured() {
      int sum = 0;
      for (val donor : donors) {
        sum += donor.getStatus().equalsIgnoreCase("alive") ? 1 : 0;
      }
      return sum;
    }

  }

  @Value
  protected static class DonorValue {

    String id;
    String status;
    int time;
    boolean deceased;

  }

}
