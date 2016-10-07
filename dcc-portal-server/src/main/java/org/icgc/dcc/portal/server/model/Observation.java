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
package org.icgc.dcc.portal.server.model;

import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;

import java.util.Map;

import lombok.Value;
import lombok.val;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Observation")
public class Observation {

  String alignmentAlgorithm;
  String baseCallingAlgorithm;
  String biologicalValidationStatus;
  String experimentalProtocol;
  Integer mutantAlleleReadCount;
  String otherAnalysisAlgorithm;
  String platform;
  Double probability;
  String rawDataAccession;
  String rawDataRepository;
  String sequencingStrategy;
  Integer totalReadCount;
  String variationCallingAlgorithm;
  String verificationStatus;

  String matchedICGCSampleId;
  String icgcSampleId;
  String icgcSpecimenId;
  String submittedSampleId;
  String submittedMatchedSampleId;

  @JsonCreator
  public Observation(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(EntityType.OBSERVATION);

    matchedICGCSampleId = (String) fieldMap.get(fields.get("matchedICGCSampleId"));
    icgcSampleId = (String) fieldMap.get(fields.get("icgcSampleId"));
    icgcSpecimenId = (String) fieldMap.get(fields.get("icgcSpecimenId"));
    submittedSampleId = (String) fieldMap.get(fields.get("submittedSampleId"));
    submittedMatchedSampleId = (String) fieldMap.get(fields.get("submittedMatchedSampleId"));

    alignmentAlgorithm = (String) fieldMap.get(fields.get("alignmentAlgorithm"));
    baseCallingAlgorithm = (String) fieldMap.get(fields.get("baseCallingAlgorithm"));
    biologicalValidationStatus = (String) fieldMap.get(fields.get("biologicalValidationStatus"));
    experimentalProtocol = (String) fieldMap.get(fields.get("experimentalProtocol"));

    mutantAlleleReadCount = (Integer) fieldMap.get(fields.get("mutantAlleleReadCount"));
    otherAnalysisAlgorithm = (String) fieldMap.get(fields.get("otherAnalysisAlgorithm"));
    platform = (String) fieldMap.get(fields.get("platform"));
    probability = (Double) fieldMap.get(fields.get("probability"));
    rawDataAccession = (String) fieldMap.get(fields.get("rawDataAccession"));
    rawDataRepository = (String) fieldMap.get(fields.get("rawDataRepository"));
    sequencingStrategy = (String) fieldMap.get(fields.get("sequencingStrategy"));
    totalReadCount = (Integer) fieldMap.get(fields.get("totalReadCount"));
    variationCallingAlgorithm = (String) fieldMap.get(fields.get("variation_callingAlgorithm"));
    verificationStatus = (String) fieldMap.get(fields.get("verificationStatus"));
  }
}
