/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Data
@NoArgsConstructor
public class BeaconQuery {

  @JsonProperty
  @ApiModelProperty(value = "Allele String: [ACTG]+, D or I (Wildcards not yet supported)", required = true)
  String allele;
  @JsonProperty
  @ApiModelProperty(value = "Chromosome ID: 1-22, X, Y, MT", required = true)
  String chromosome;
  @JsonProperty
  @ApiModelProperty(value = "Position 1-based", required = true)
  int position;
  @JsonProperty
  @ApiModelProperty(value = "Reference ID: GRCh\\d+", required = true)
  String reference;
  @JsonProperty
  @ApiModelProperty(value = "The dataset to query against")
  String dataset;

  @JsonCreator
  public BeaconQuery(
      @JsonProperty("allele") String allele,
      @JsonProperty("chromosome") String chromosome,
      @JsonProperty("position") int position,
      @JsonProperty("reference") String reference,
      @JsonProperty("dataset_id") String dataset) {
    this.allele = allele;
    this.position = position;
    this.chromosome = chromosome;
    this.reference = reference;
    this.dataset = dataset;
  }

}
