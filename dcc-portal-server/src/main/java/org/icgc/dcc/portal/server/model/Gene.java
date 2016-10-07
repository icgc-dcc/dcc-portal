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
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getLong;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getString;

import java.util.List;
import java.util.Map;

import lombok.Value;
import lombok.val;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Gene")
public class Gene {

  @ApiModelProperty(value = "Gene ID", required = true)
  String id;
  @ApiModelProperty(value = "Symbol", required = true)
  String symbol;
  @ApiModelProperty(value = "Name", required = true)
  String name;
  @ApiModelProperty(value = "Biotype", required = true)
  String type;
  @ApiModelProperty(value = "Chromosome", required = true)
  String chromosome;
  @ApiModelProperty(value = "Start Position", required = true)
  Long start;
  @ApiModelProperty(value = "End Position", required = true)
  Long end;
  @ApiModelProperty(value = "Total number of Donors affected by the Gene", required = true)
  Long affectedDonorCountTotal;
  @ApiModelProperty(value = "Filtered number of Donors affected by the Gene", required = true)
  Long affectedDonorCountFiltered;
  @ApiModelProperty(value = "Strand", required = true)
  Long strand;
  @ApiModelProperty(value = "Description", required = true)
  String description;
  @ApiModelProperty(value = "Synonyms", required = true)
  List<String> synonyms;
  @ApiModelProperty(value = "Map of External Database IDs", required = true)
  Map<String, List<String>> externalDbIds;
  @ApiModelProperty(value = "Affected Transcript IDs", required = true)
  List<String> affectedTranscriptIds;

  // @ApiModelProperty(value = "Cancer Gene List", required = true)
  // List<String> list;

  @ApiModelProperty(value = "Transcripts")
  List<Transcript> transcripts;
  @ApiModelProperty(value = "Projects that have a Donor affected by the Gene")
  List<Project> projects;

  // @ApiModelProperty(value = "Pathways associated with this Gene")
  // List<Pathway> pathways;

  @ApiModelProperty(value = "Genesets associated with this Gene")
  List<GeneSetAnnotation> sets;

  @SuppressWarnings("unchecked")
  @JsonCreator
  public Gene(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(EntityType.GENE);
    id = getString(fieldMap.get(fields.get("id")));
    symbol = getString(fieldMap.get(fields.get("symbol")));
    name = getString(fieldMap.get(fields.get("name")));
    type = getString(fieldMap.get(fields.get("type")));
    chromosome = getString(fieldMap.get(fields.get("chromosome")));
    start = getLong(fieldMap.get(fields.get("start")));
    end = getLong(fieldMap.get(fields.get("end")));
    strand = getLong(fieldMap.get(fields.get("strand")));
    description = getString(fieldMap.get(fields.get("description")));
    synonyms = (List<String>) fieldMap.get(fields.get("synonyms"));
    externalDbIds = (Map<String, List<String>>) fieldMap.get(fields.get("externalDbIds"));
    affectedTranscriptIds = (List<String>) fieldMap.get(fields.get("affectedTranscriptIds"));
    affectedDonorCountTotal = getLong(fieldMap.get(fields.get("affectedDonorCountTotal")));
    affectedDonorCountFiltered = getLong(fieldMap.get(fields.get("affectedDonorCountFiltered")));
    // list = (List<String>) fieldMap.get(fields.get("list"));
    transcripts = buildTranscripts((List<Map<String, Object>>) fieldMap.get("transcripts"));
    projects = buildProjects(fieldMap);
    // pathways = buildPathways((List<Map<String, Object>>) fieldMap.get("pathways"));
    sets = buildGeneSets((List<Map<String, Object>>) fieldMap.get("sets"));
  }

  private List<Transcript> buildTranscripts(List<Map<String, Object>> field) {
    if (field == null) return null;
    List<Transcript> lst = Lists.newArrayList();
    for (Map<String, Object> item : field) {
      lst.add(new Transcript(item));
    }
    return lst;
  }

  private List<GeneSetAnnotation> buildGeneSets(List<Map<String, Object>> field) {
    if (field == null) return null;
    val result = Lists.<GeneSetAnnotation> newArrayList();
    for (Map<String, Object> item : field) {
      result.add(new GeneSetAnnotation(item));
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private List<Project> buildProjects(Map<String, Object> fieldMap) {
    val ps = (List<Map<String, Object>>) fieldMap.get("project");
    val filterProjectIds = (List<String>) fieldMap.get("projectIds");
    val projects = Lists.<Project> newArrayList();

    if (ps != null) {
      for (val p : ps) {
        // Only filter if ids provided
        if (filterProjectIds == null || filterProjectIds.isEmpty()
            || filterProjectIds.contains(p.get("_project_id").toString())) {
          p.put("_summary._total_donor_count", ((Map<String, Object>) p.get("_summary")).get("_total_donor_count"));
          p.put("_summary._affected_donor_count",
              ((Map<String, Object>) p.get("_summary")).get("_affected_donor_count"));
          p.put("_summary._available_data_type", ((Map<String, Object>) p.get("_summary")).get("_available_data_type"));
          p.put("_summary._ssm_tested_donor_count",
              ((Map<String, Object>) p.get("_summary")).get("_ssm_tested_donor_count"));
          projects.add(new Project(p));
        }
      }
      return projects;
    } else {
      return null;
    }
  }

}
