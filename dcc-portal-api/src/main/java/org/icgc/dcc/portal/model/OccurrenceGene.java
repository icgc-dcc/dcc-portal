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

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;

import io.swagger.annotations.ApiModel;
import lombok.Value;
import lombok.val;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Occurrence Gene")
public class OccurrenceGene {

  String biotype;
  String geneId;
  String chromosome;
  List<OccurrenceConsequence> consequence;

  @JsonCreator
  public OccurrenceGene(Map<String, Object> fieldMap) {
    biotype = (String) fieldMap.get("biotype");
    geneId = (String) fieldMap.get("_gene_id");
    chromosome = (String) fieldMap.get("chromosome");
    consequence = buildConsequence(getConsequences(fieldMap));
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> getConsequences(Map<String, Object> fieldMap) {
    val consequenceKey = "consequence";
    if (!fieldMap.containsKey(consequenceKey)) {
      return emptyList();
    }

    return (List<Map<String, Object>>) fieldMap.get(consequenceKey);
  }

  private List<OccurrenceConsequence> buildConsequence(List<Map<String, Object>> list) {
    if (list == null) return null;

    val consequences = Lists.<OccurrenceConsequence> newArrayList();
    for (val map : list) {
      consequences.add(new OccurrenceConsequence(
          (String) map.get("consequence_type"),
          (String) map.get("functional_impact_prediction_summary")));
    }
    return consequences;
  }

  @Value
  class OccurrenceConsequence {

    String consequenceType;
    String functionalImpact;

  }

}
