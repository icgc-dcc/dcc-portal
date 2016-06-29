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

package org.icgc.dcc.portal.model;

import static java.util.Collections.emptyList;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getLong;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getString;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.toStringList;

import java.util.List;
import java.util.Map;

import org.icgc.dcc.portal.model.IndexModel.Kind;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Value;
import lombok.val;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Occurrence")
public class Occurrence {

  @ApiModelProperty(value = "Affected Donor ID", required = true)
  String donorId;
  @ApiModelProperty(value = "Mutation ID", required = true)
  String mutationId;
  @ApiModelProperty(value = "Chromosome", required = true)
  String chromosome;
  @ApiModelProperty(value = "Start Position", required = true)
  Long start;
  @ApiModelProperty(value = "End Position", required = true)
  Long end;
  @ApiModelProperty(value = "Affected Project Identifiable", required = true)
  String projectId;
  @ApiModelProperty(value = "Mutation", required = true)
  String mutation;
  @ApiModelProperty(value = "Observation", required = true)
  List<Observation> observations;
  @ApiModelProperty(value = "Gene Id", required = true)
  List<String> geneId;
  @ApiModelProperty(value = "Consequence Type", required = true)
  List<String> consequenceType;
  @ApiModelProperty(value = "Genes", required = true)
  List<OccurrenceGene> genes;

  @JsonCreator
  public Occurrence(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(Kind.OCCURRENCE);
    donorId = getString(fieldMap.get(fields.get("donorId")));
    mutationId = getString(fieldMap.get(fields.get("mutationId")));
    chromosome = getString(fieldMap.get(fields.get("chromosome")));
    start = getLong(fieldMap.get(fields.get("start")));
    end = getLong(fieldMap.get(fields.get("end")));
    projectId = getString(fieldMap.get(fields.get("projectId")));
    mutation = getString(fieldMap.get(fields.get("mutation")));
    observations = buildObservations(getObservations(fieldMap));
    geneId = toStringList(fieldMap.get(fields.get("gene.id")));
    consequenceType = toStringList(fieldMap.get(fields.get("mutation.consequenceType")));
    genes = buildGenes(getGenes(fieldMap));
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> getObservations(Map<String, Object> fieldMap) {
    val observationKey = FIELDS_MAPPING.get(Kind.OCCURRENCE).get("observation");
    if (!fieldMap.containsKey(observationKey)) {
      return emptyList();
    }

    return (List<Map<String, Object>>) fieldMap.get(observationKey);
  }

  private List<Observation> buildObservations(List<Map<String, Object>> list) {
    if (list == null) return null;

    List<Observation> observations = Lists.newArrayList();
    for (Map<String, Object> map : list) {
      observations.add(new Observation(map));
    }
    return observations;
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> getGenes(Map<String, Object> fieldMap) {
    val observationKey = "ssm.gene";
    if (!fieldMap.containsKey(observationKey)) {
      return emptyList();
    }

    return (List<Map<String, Object>>) fieldMap.get(observationKey);
  }

  private List<OccurrenceGene> buildGenes(List<Map<String, Object>> rawGeneList) {
    if (rawGeneList == null) return null;

    return rawGeneList.stream().map(OccurrenceGene::new).collect(toImmutableList());
  }

}
