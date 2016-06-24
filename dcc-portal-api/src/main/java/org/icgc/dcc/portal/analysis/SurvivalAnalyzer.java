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

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.portal.model.SurvivalAnalysis;

import lombok.val;

public class SurvivalAnalyzer {

  private final static String DONOR_FILTER = "{'donor':{'id':{'is':['ES:%s']}}}";

  public static SurvivalAnalysis analyze(SurvivalAnalysis analysis) {
    val setIds = analysis.getEntitySetIds();

    return analysis;
  }

  static List<Interval> compute(int[] time, boolean[] censured) {

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

      int t = time[i];

      // If we have moved past the current interval compute the cumulative survival and adjust the # at risk
      // for the start of the next interval.
      if (t > currentInterval.getEnd()) {
        atRisk -= currentInterval.getNumberCensured();
        float survivors = atRisk - currentInterval.getNumberDied();
        float tmp = survivors / atRisk;
        cumulativeSurvival *= tmp;

        // Skip to the next interval
        atRisk -= currentInterval.getNumberDied();
        while (intervalIter.hasNext() && t > currentInterval.getEnd()) {
          currentInterval = intervalIter.next();
          currentInterval.setCumulativeSurvival(cumulativeSurvival);
        }
      }

      if (censured[i]) {
        currentInterval.addCensure(time[i]);

      } else {
        currentInterval.incDied();
      }
    }
    currentInterval.setCumulativeSurvival(cumulativeSurvival);

    return intervals;

  }

  public static class Interval {

    private int start;
    private int end;
    private int numberDied;
    private List<Integer> censored = new ArrayList<Integer>();
    private float cumulativeSurvival;

    public Interval(int start, int end) {
      this.setStart(start);
      this.setEnd(end);
    }

    void incDied() {
      numberDied++;
    }

    void addCensure(int time) {
      censored.add(time);
    }

    public int getStart() {
      return start;
    }

    public void setStart(int start) {
      this.start = start;
    }

    public int getEnd() {
      return end;
    }

    public void setEnd(int end) {
      this.end = end;
    }

    public int getNumberDied() {
      return numberDied;
    }

    public List<Integer> getCensored() {
      return censored;
    }

    public float getCumulativeSurvival() {
      return cumulativeSurvival;
    }

    public void setCumulativeSurvival(float cumulativeSurvival) {
      this.cumulativeSurvival = cumulativeSurvival;
    }

    public int getNumberCensured() {
      return censored.size();
    }
  }

}
