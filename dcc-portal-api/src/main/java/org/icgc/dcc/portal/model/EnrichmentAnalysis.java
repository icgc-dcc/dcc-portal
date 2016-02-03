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
package org.icgc.dcc.portal.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.icgc.dcc.portal.model.EnrichmentAnalysis.State.PENDING;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import lombok.Data;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableList;
import com.wordnik.swagger.annotations.ApiModel;

/**
 * The total state of an enrichment analysis including its inputs, state and outputs.
 */
@Data
@JsonInclude(NON_NULL)
@ApiModel(value = "EnrichmentAnalysis")
@Accessors(chain = true)
public class EnrichmentAnalysis implements Identifiable<UUID> {

  /**
   * Resource identifier
   */
  private UUID id;

  /**
   * Creation timestamp
   */
  private long timestamp = new Date().getTime();

  /*
   * This is the default value and is used for migration when this version field is introduced.
   */
  private int version = 1;

  /**
   * State of the analysis
   */
  private State state = PENDING;

  /**
   * Input from Advanced Search
   */
  private Query query;

  private EnrichmentParams params;

  /**
   * Summary of analysis
   */
  private Overview overview;

  /**
   * Detailed Gene-set results
   */
  private List<Result> results;

  @Data
  @Accessors(chain = true)
  public static class Overview {

    /**
     * UI: "#Gene sets in overlap"
     */
    private int overlapGeneSetCount;

    /**
     * UI: "#Gene sets in Universe"
     */
    private int universeGeneSetCount;

    /**
     * UI: "#Genes in overlap"
     */
    private int overlapGeneCount;

    /**
     * UI: "#Genes in Universe"
     */
    private int universeGeneCount;

    /**
     * UI: #Gene sets in overlap and <= FDR"
     */
    private int overlapFdrGeneSetCount;

  }

  @Data
  @Accessors(chain = true)
  public static class Result {

    /**
     * UI: "ID"
     */
    private String geneSetId;

    /**
     * UI: "Name"
     */
    private String geneSetName;

    /**
     * UI: "#Genes"
     */
    private int geneCount;

    /**
     * UI: "#Genes in overlap"
     */
    private int overlapGeneSetGeneCount;

    /**
     * UI: "#Donors in overlap"
     */
    private int overlapGeneSetDonorCount;

    /**
     * UI: "#Mutations"
     */
    private int overlapGeneSetMutationCount;

    /**
     * UI: "Expected"
     */
    private double expectedValue;

    /**
     * UI: "P-Value"
     */
    private double pValue;

    /**
     * UI: "Adjusted P-Value"
     */
    private double adjustedPValue;

    public static Iterable<String> getGeneSetIds(@NonNull Iterable<Result> results) {
      val geneSetIds = ImmutableList.<String> builder();
      for (val result : results) {
        geneSetIds.add(result.getGeneSetId());
      }

      return geneSetIds.build();
    }

  }

  /**
   * The state of an executing enrichment analysis.
   */
  public enum State {

    PENDING,
    ANALYZING,
    POST_PROCESSING,
    FINISHED,
    ERROR;

  }

}
