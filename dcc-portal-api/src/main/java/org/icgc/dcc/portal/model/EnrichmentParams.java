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

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.experimental.Accessors;

import org.icgc.dcc.portal.util.FloatRange;

/**
 * Represents the user defined parameterization of an enrichment analysis.
 */
@Data
@Accessors(chain = true)
public class EnrichmentParams {

  /**
   * Limits.
   */
  public static final int MAX_INPUT_GENES = 10000;
  public static final int MAX_OUTPUT_GENE_SETS = 100;
  public static final float MIN_FDR = 0.005f;
  public static final float MAX_FDR = 0.5f;

  /**
   * Defaults.
   */
  public static final float DEFAULT_FDR = 0.05f;

  /**
   * UI: "# Input genes"
   */
  @Max(MAX_INPUT_GENES)
  private int maxGeneCount;

  /**
   * UI: "Top {{ analysis.params.maxGeneSetCount }} gene sets [...]"
   */
  @Max(MAX_OUTPUT_GENE_SETS)
  private int maxGeneSetCount;

  /**
   * UI: "Top {{ analysis.params.maxGeneSetCount }} gene sets [...]"
   */
  @FloatRange(min = MIN_FDR, max = MAX_FDR)
  private float fdr = DEFAULT_FDR;

  /**
   * Background
   */
  @NotNull
  private Universe universe;

}