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
package org.icgc.dcc.portal.server.model;

import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getLong;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getString;

import java.util.Map;

import lombok.Value;
import lombok.val;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Family")
public class Family {

  @ApiModelProperty(value = "Donor has relative with cancer history", required = true)
  String donorHasRelativeWithCancerHistory;

  @ApiModelProperty(value = "Relationship type", required = true)
  String relationshipType;

  @ApiModelProperty(value = "Relationship type other", required = true)
  String relationshipTypeOther;

  @ApiModelProperty(value = "Relationship sex", required = true)
  String relationshipSex;

  @ApiModelProperty(value = "Relationship age", required = true)
  Long relationshipAge;

  @ApiModelProperty(value = "Relationship disease ICD10", required = true)
  String relationshipDiseaseICD10;

  @ApiModelProperty(value = "Relationship disease", required = true)
  String relationshipDisease;

  @JsonCreator
  public Family(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(EntityType.FAMILY);

    donorHasRelativeWithCancerHistory = getString(fieldMap.get(fields.get("donorHasRelativeWithCancerHistory")));
    relationshipType = getString(fieldMap.get(fields.get("relationshipType")));
    relationshipTypeOther = getString(fieldMap.get(fields.get("relationshipTypeOther")));
    relationshipSex = getString(fieldMap.get(fields.get("relationshipSex")));
    relationshipAge = getLong(fieldMap.get(fields.get("relationshipAge")));
    relationshipDiseaseICD10 = getString(fieldMap.get(fields.get("relationshipDiseaseICD10")));
    relationshipDisease = getString(fieldMap.get(fields.get("relationshipDisease")));
  }

}
