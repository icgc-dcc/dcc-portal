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
package org.icgc.dcc.portal.model;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@ApiModel(value = "OncogridAnalysis")
public class OncogridAnalysis implements Identifiable<UUID> {

  @NonNull
  private final UUID id;
  @NonNull
  private final UUID geneSet;
  @NonNull
  private final UUID donorSet;
  @NonNull
  private final String geneSetName;
  @NonNull
  private final String donorSetName;

  private final long geneCount;
  private final long donorCount;

  private int version = 1;
  private State state = State.FINISHED;
  private final long timestamp = new Date().getTime();

  @JsonCreator
  public OncogridAnalysis(
      @JsonProperty("id") final UUID id,
      @JsonProperty("geneSet") final UUID geneSet,
      @JsonProperty("geneCount") final long geneCount,
      @JsonProperty("geneSetName") final String geneSetName,
      @JsonProperty("donorSet") final UUID donorSet,
      @JsonProperty("donorCount") final long donorCount,
      @JsonProperty("donorSetName") final String donorSetName) {
    this.id = id;
    this.geneSet = geneSet;
    this.geneCount = geneCount;
    this.geneSetName = geneSetName;
    this.donorSet = donorSet;
    this.donorCount = donorCount;
    this.donorSetName = donorSetName;
  }

  @RequiredArgsConstructor
  @Getter
  public enum State {

    FINISHED("finished"), ERROR("error");

    @NonNull
    private final String name;

  }

}
