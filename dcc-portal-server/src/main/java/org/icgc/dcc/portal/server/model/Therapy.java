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
@ApiModel(value = "Therapy")
public class Therapy {

  @ApiModelProperty(value = "First therapy type", required = true)
  String firstTherapyType;

  @ApiModelProperty(value = "First therapy therapeutic intent", required = true)
  String firstTherapyTherapeuticIntent;

  @ApiModelProperty(value = "First therapy start interval", required = true)
  Long firstTherapyStartInterval;

  @ApiModelProperty(value = "First therapy duration", required = true)
  Long firstTherapyDuration;

  @ApiModelProperty(value = "First therapy response", required = true)
  String firstTherapyResponse;

  @ApiModelProperty(value = "Second therapy type", required = true)
  String secondTherapyType;

  @ApiModelProperty(value = "Second therapy therapeutic intent", required = true)
  String secondTherapyTherapeuticIntent;

  @ApiModelProperty(value = "Second therapy start interval", required = true)
  Long secondTherapyStartInterval;

  @ApiModelProperty(value = "Second therapy duration", required = true)
  Long secondTherapyDuration;

  @ApiModelProperty(value = "Second therapy response", required = true)
  String secondTherapyResponse;

  @ApiModelProperty(value = "Other therapy", required = true)
  String otherTherapy;

  @ApiModelProperty(value = "Other therapy response", required = true)
  String otherTherapyResponse;

  @JsonCreator
  public Therapy(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(EntityType.THERAPY);

    firstTherapyType = getString(fieldMap.get(fields.get("firstTherapyType")));
    firstTherapyTherapeuticIntent = getString(fieldMap.get(fields.get("firstTherapyTherapeuticIntent")));
    firstTherapyStartInterval = getLong(fieldMap.get(fields.get("firstTherapyStartInterval")));
    firstTherapyDuration = getLong(fieldMap.get(fields.get("firstTherapyDuration")));
    firstTherapyResponse = getString(fieldMap.get(fields.get("firstTherapyResponse")));

    secondTherapyType = getString(fieldMap.get(fields.get("secondTherapyType")));
    secondTherapyTherapeuticIntent = getString(fieldMap.get(fields.get("secondTherapyTherapeuticIntent")));
    secondTherapyStartInterval = getLong(fieldMap.get(fields.get("secondTherapyStartInterval")));
    secondTherapyDuration = getLong(fieldMap.get(fields.get("secondTherapyDuration")));
    secondTherapyResponse = getString(fieldMap.get(fields.get("secondTherapyResponse")));

    otherTherapy = getString(fieldMap.get(fields.get("otherTherapy")));
    otherTherapyResponse = getString(fieldMap.get(fields.get("otherTherapyResponse")));
  }

}
