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
@ApiModel(value = "Exposure")
public class Exposure {

  @ApiModelProperty(value = "Exposure type", required = true)
  String exposureType;

  @ApiModelProperty(value = "Exposure intensity", required = true)
  Long exposureIntesity;

  @ApiModelProperty(value = "Tabacco smoking history indicator", required = true)
  String tobaccoSmokingHistoryIndicator;

  @ApiModelProperty(value = "Tabacco smoking intensity", required = true)
  Long tobaccoSmokingIntensity;

  @ApiModelProperty(value = "Alcohol history", required = true)
  String alcoholHistory;

  @ApiModelProperty(value = "Alcohol history intensity", required = true)
  String alcoholHistoryIntensity;

  @JsonCreator
  public Exposure(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(EntityType.EXPOSURE);
    exposureType = getString(fieldMap.get(fields.get("exposureType")));
    exposureIntesity = getLong(fieldMap.get(fields.get("exposureIntesity")));
    tobaccoSmokingHistoryIndicator = getString(fieldMap.get(fields.get("tobaccoSmokingHistoryIndicator")));
    tobaccoSmokingIntensity = getLong(fieldMap.get(fields.get("tobaccoSmokingIntensity")));
    alcoholHistory = getString(fieldMap.get(fields.get("alcoholHistory")));
    alcoholHistoryIntensity = getString(fieldMap.get(fields.get("alcoholHistoryIntensity")));
  }

}
