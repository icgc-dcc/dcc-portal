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

import static java.lang.Math.min;
import static java.util.Collections.sort;
import static lombok.AccessLevel.PRIVATE;

import java.util.Comparator;
import java.util.List;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.icgc.dcc.portal.model.EnrichmentAnalysis.Result;

import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;

@NoArgsConstructor(access = PRIVATE)
public final class EnrichmentAnalyses {

  public static double calculateExpectedGeneCount(int k, int m, int n) {
    return k * ((double) m / n);
  }

  public static double calculateGeneCountPValue(int q, int k, int m, int n) {
    val distribution = new HypergeometricDistribution(n, m, k);
    val pValue = 1.0 - min(1.0, distribution.cumulativeProbability(q - 1)); // min is needed to prevent negatives

    return pValue;
  }

  public static List<Result> adjustRawGeneSetResults(double fdr, @NonNull List<Result> rawResults) {
    // Sort ascending by raw p-value
    val sortedRawResults = sortRawResults(rawResults);

    // Threshold - Find threshold probability, t
    double t = 0.0;
    val m = sortedRawResults.size();

    for (int i = 1; i <= sortedRawResults.size(); i++) {
      val result = sortedRawResults.get(i - 1);

      // P(i) < i·α/N
      if (result.getPValue() < i * fdr / m) {
        // Largest p-value
        t = result.getPValue();
      }
    }

    // Adjust - Normalization
    val adjustedResults = Lists.<Result> newArrayList();
    for (int i = 1; i <= sortedRawResults.size(); i++) {
      val result = sortedRawResults.get(i - 1);

      // Below the significance threshold?
      if (result.getPValue() > t) {
        continue;
      }

      // Initial adjustment per spec
      val adjustedPValue = result.getPValue() * m / i;
      result.setAdjustedPValue(adjustedPValue);

      adjustedResults.add(result);
    }

    // Adjust - Minimization
    for (int i = adjustedResults.size(); i >= 1; i--) {
      val adjustedResult = adjustedResults.get(i - 1);
      val followingResult = i == adjustedResults.size() ? adjustedResult : adjustedResults.get(i);

      // Update the final adjusted p-value
      adjustedResult.setAdjustedPValue(min(adjustedResult.getAdjustedPValue(), followingResult.getAdjustedPValue()));
    }

    // Sort ascending by adjusted p-value
    return sortAdjustedResults(adjustedResults);
  }

  private static List<Result> sortRawResults(@NonNull List<Result> rawResults) {
    sort(rawResults, new Comparator<Result>() {

      @Override
      public int compare(Result a, Result b) {
        return Doubles.compare(a.getPValue(), b.getPValue());
      }

    });

    return rawResults;
  }

  private static List<Result> sortAdjustedResults(@NonNull List<Result> adjustedResults) {
    sort(adjustedResults, new Comparator<Result>() {

      @Override
      public int compare(Result a, Result b) {
        return Doubles.compare(a.getAdjustedPValue(), b.getAdjustedPValue());
      }

    });

    return adjustedResults;
  }

}
