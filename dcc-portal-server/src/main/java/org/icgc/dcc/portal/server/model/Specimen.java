/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.server.model;

import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Value;
import lombok.val;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Specimen")
public class Specimen {

  @ApiModelProperty(value = "Specimen ID", required = true)
  String id;
  @ApiModelProperty(value = "Submitted Specimen ID", required = true)
  String submittedId;
  @ApiModelProperty(value = "Available", required = true)
  Boolean available;
  @ApiModelProperty(value = "Digital Image Of StainedSection", required = true)
  String digitalImageOfStainedSection;
  @ApiModelProperty(value = "dbXref", required = true)
  String dbXref;
  @ApiModelProperty(value = "Bio Bank", required = true)
  String biobank;
  @ApiModelProperty(value = "Bio Bank ID", required = true)
  String biobankId;
  @ApiModelProperty(value = "Treatment Type", required = true)
  String treatmentType;
  @ApiModelProperty(value = "Treatment Type Other", required = true)
  String treatmentTypeOther;
  @ApiModelProperty(value = "Processing", required = true)
  String processing;
  @ApiModelProperty(value = "Processing Other", required = true)
  String processingOther;
  @ApiModelProperty(value = "Storage", required = true)
  String storage;
  @ApiModelProperty(value = "Type", required = true)
  String type;
  @ApiModelProperty(value = "Type Other", required = true)
  String typeOther;
  @ApiModelProperty(value = "URI", required = true)
  String uri;
  @ApiModelProperty(value = "Specimen Interval (days)", required = true)
  Integer interval;
  @ApiModelProperty(value = "Tumour Confirmed", required = true)
  Boolean tumourConfirmed;
  @ApiModelProperty(value = "Tumour Grade", required = true)
  String tumourGrade;
  @ApiModelProperty(value = "Tumour Grade Supplemental", required = true)
  String tumourGradeSupplemental;
  @ApiModelProperty(value = "Tumour Histological Type", required = true)
  String tumourHistologicalType;
  @ApiModelProperty(value = "Tumour Stage", required = true)
  String tumourStage;
  @ApiModelProperty(value = "Tumour Stage Supplemental", required = true)
  String tumourStageSupplemental;
  @ApiModelProperty(value = "Tumour Stage System", required = true)
  String tumourStageSystem;

  @ApiModelProperty(value = "Percent cellularity", required = true)
  Integer percentCellularity;

  @ApiModelProperty(value = "Level of cellularity", required = true)
  String levelOfCellularity;

  @ApiModelProperty(value = "Samples", required = true)
  List<Sample> samples;

  @SuppressWarnings("unchecked")
  public Specimen(Map<String, Object> fieldMap) {

    // TODO: See if we can remove the dependency on the old IndexModel's FIELDS_MAPPING.
    val fields = FIELDS_MAPPING.get(EntityType.SPECIMEN);
    id = (String) fieldMap.get(fields.get("id"));
    submittedId = (String) fieldMap.get(fields.get("submittedId"));
    available = getTruthy((String) fieldMap.get(fields.get("available")));
    digitalImageOfStainedSection = (String) fieldMap.get(fields.get("digitalImageOfStainedSection"));
    dbXref = (String) fieldMap.get(fields.get("dbXref"));
    biobank = (String) fieldMap.get(fields.get("biobank"));
    biobankId = (String) fieldMap.get(fields.get("biobankId"));
    treatmentType = (String) fieldMap.get(fields.get("treatmentType"));
    treatmentTypeOther = (String) fieldMap.get(fields.get("treatmentTypeOther"));
    processing = (String) fieldMap.get(fields.get("processing"));
    processingOther = (String) fieldMap.get(fields.get("processingOther"));
    storage = (String) fieldMap.get(fields.get("storage"));
    type = (String) fieldMap.get(fields.get("type"));
    typeOther = (String) fieldMap.get(fields.get("typeOther"));
    uri = (String) fieldMap.get(fields.get("uri"));
    interval = (Integer) fieldMap.get(fields.get("interval"));
    tumourConfirmed = getTruthy((String) fieldMap.get(fields.get("tumourConfirmed")));
    tumourGrade = (String) fieldMap.get(fields.get("tumourGrade"));
    tumourGradeSupplemental = (String) fieldMap.get(fields.get("tumourGradeSupplemental"));
    tumourHistologicalType = (String) fieldMap.get(fields.get("tumourHistologicalType"));
    tumourStage = (String) fieldMap.get(fields.get("tumourStage"));
    tumourStageSupplemental = (String) fieldMap.get(fields.get("tumourStageSupplemental"));
    tumourStageSystem = (String) fieldMap.get(fields.get("tumourStageSystem"));
    percentCellularity = (Integer) fieldMap.get(fields.get("percentCellularity"));
    levelOfCellularity = (String) fieldMap.get(fields.get("levelOfCellularity"));

    samples = buildSamples((List<Object>) fieldMap.get("sample"));
  }

  private Boolean getTruthy(String field) {
    return field != null && field.equals("yes");
  }

  @SuppressWarnings("unchecked")
  private List<Sample> buildSamples(List<Object> field) {
    if (field == null) return null;
    val lst = Lists.<Sample> newArrayList();
    for (Object item : field) {
      lst.add(new Sample((Map<String, Object>) item));
    }
    return lst;
  }
}
