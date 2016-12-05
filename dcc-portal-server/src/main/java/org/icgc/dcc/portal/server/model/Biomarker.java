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
package org.icgc.dcc.portal.server.model;

import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getString;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Value;
import lombok.val;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Biomarker")
public class Biomarker {

  @ApiModelProperty(value = "Specimen ID", required = true)
  String specimenId;

  @ApiModelProperty(value = "Biomarker Name", required = true)
  String biomarkerName;

  @ApiModelProperty(value = "Biomarker Threshold", required = true)
  String biomarkerThreshold;

  @ApiModelProperty(value = "Biomarker Positive", required = true)
  String biomarkerPositive;

  @JsonCreator
  public Biomarker(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(EntityType.BIOMARKER);
    specimenId = getString(fieldMap.get(fields.get("specimenId")));
    biomarkerName = getString(fieldMap.get(fields.get("biomarkerName")));
    biomarkerThreshold = getString(fieldMap.get(fields.get("biomarkerThreshold")));
    biomarkerPositive = getString(fieldMap.get(fields.get("biomarkerPositive")));
  }

}
