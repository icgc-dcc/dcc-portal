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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiModelProperty;
import lombok.Value;
import lombok.val;

import java.util.List;
import java.util.Map;

import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Transcript {

  @ApiModelProperty(value = "Transcript ID", required = true)
  String id;
  @ApiModelProperty(value = "Name", required = true)
  String name;
  @ApiModelProperty(value = "Biotype", required = true)
  String type;
  @ApiModelProperty(value = "Is the Transcript Canonical", required = true)
  Boolean isCanonical;
  @ApiModelProperty(value = "Start Position", required = true)
  Long start;
  @ApiModelProperty(value = "End Position", required = true)
  Long end;
  @ApiModelProperty(value = "CDNA Coding Start Position", required = true)
  Long cdnaCodingStart;
  @ApiModelProperty(value = "CDNA Coding End Position", required = true)
  Long cdnaCodingEnd;
  @ApiModelProperty(value = "Coding Region Start Position", required = true)
  Long codingRegionStart;
  @ApiModelProperty(value = "Coding Region End Position", required = true)
  Long codingRegionEnd;
  @ApiModelProperty(value = "Start Exon", required = true)
  Long startExon;
  @ApiModelProperty(value = "End Exon", required = true)
  Long endExon;
  @ApiModelProperty(value = "Length of Transcript", required = true)
  Long length;
  @ApiModelProperty(value = "Length in Amino Acids", required = true)
  Long lengthAminoAcid;
  @ApiModelProperty(value = "Length of CDS", required = true)
  Long lengthCds;
  @ApiModelProperty(value = "Number of Exons", required = true)
  Long numberOfExons;
  @ApiModelProperty(value = "Sequence Exon Start Position", required = true)
  Long seqExonStart;
  @ApiModelProperty(value = "Sequence Exon End Position", required = true)
  Long seqExonEnd;
  @ApiModelProperty(value = "Translation ID", required = true)
  String translationId;
  @ApiModelProperty(value = "AA Mutation", required = true)
  String aaMutation;
  @ApiModelProperty(value = "Consequence", required = true)
  Consequence consequence;
  @ApiModelProperty(value = "Domains", required = true)
  List<Domain> domains;
  @ApiModelProperty(value = "Exons", required = true)
  List<Exon> exons;
  @ApiModelProperty(value = "Functional Impact Summary", required = true)
  String functionalImpact;

  @SuppressWarnings("unchecked")
  @JsonCreator
  public Transcript(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(EntityType.TRANSCRIPT);
    id = (String) fieldMap.get(fields.get("id"));
    name = (String) fieldMap.get(fields.get("name"));
    type = (String) fieldMap.get(fields.get("type"));
    isCanonical = (Boolean) fieldMap.get(fields.get("isCanonical"));
    start = getLong(fieldMap.get(fields.get("start")));
    end = getLong(fieldMap.get(fields.get("end")));
    cdnaCodingStart = getLong(fieldMap.get(fields.get("cdnaCodingStart")));
    cdnaCodingEnd = getLong(fieldMap.get(fields.get("cdnaCodingEnd")));
    codingRegionStart = getLong(fieldMap.get(fields.get("codingRegionStart")));
    codingRegionEnd = getLong(fieldMap.get(fields.get("codingRegionEnd")));
    startExon = getLong(fieldMap.get(fields.get("startExon")));
    endExon = getLong(fieldMap.get(fields.get("endExon")));
    length = getLong(fieldMap.get(fields.get("length")));
    lengthAminoAcid = getLong(fieldMap.get(fields.get("lengthAminoAcid")));
    lengthCds = getLong(fieldMap.get(fields.get("lengthCds")));
    numberOfExons = getLong(fieldMap.get(fields.get("numberOfExons")));
    seqExonStart = getLong(fieldMap.get(fields.get("seqExonStart")));
    seqExonEnd = getLong(fieldMap.get(fields.get("seqExonEnd")));
    translationId = (String) fieldMap.get(fields.get("translationId"));
    aaMutation = (String) fieldMap.get(fields.get("aaMutation"));
    consequence = buildConsequence((Map<String, Object>) fieldMap.get("consequence"),
            (String) fieldMap.get(fields.get("functionalImpact")));
    domains = buildDomains((List<Map<String, Object>>) fieldMap.get("domains"));
    exons = buildExons((List<Map<String, Object>>) fieldMap.get("exons"));
    functionalImpact = (String) fieldMap.get(fields.get("functionalImpact"));
  }

  private Consequence buildConsequence(Map<String, Object> consequenceMap, String functionalImpact) {
    if (consequenceMap == null) {
      return null;
    }
    val consequence = new Consequence(consequenceMap);
    if (functionalImpact != null) {
      consequence.addFunctionalImpact(functionalImpact);
    }
    return consequence;
  }

  private List<Domain> buildDomains(List<Map<String, Object>> field) {
    if (field == null) return null;
    val lst = Lists.<Domain> newArrayList();
    for (val item : field) {
      lst.add(new Domain(item));
    }
    return lst;
  }

  private List<Exon> buildExons(List<Map<String, Object>> field) {
    if (field == null) return null;
    val lst = Lists.<Exon> newArrayList();
    for (val item : field) {
      lst.add(new Exon(item));
    }
    return lst;
  }

  private Long getLong(Object field) {
    if (field instanceof Long) return (Long) field;
    if (field instanceof Integer) return (long) (Integer) field;
    else
      return null;
  }
}
