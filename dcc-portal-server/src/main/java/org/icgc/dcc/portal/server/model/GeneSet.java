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
import static org.icgc.dcc.portal.server.repository.GeneSetRepository.SOURCE_FIELDS;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getBoolean;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getLong;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getString;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Value;
import lombok.val;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "GeneSet")
public class GeneSet {

  // Common fields
  @ApiModelProperty(value = "ID", required = true)
  String id;

  @ApiModelProperty(value = "Name", required = true)
  String name;

  @ApiModelProperty(value = "Source", required = true)
  String source;

  @ApiModelProperty(value = "Type", required = true)
  String type;

  @ApiModelProperty(value = "Description", required = true)
  String description;

  @ApiModelProperty(value = "Count of genes affected")
  Long geneCount;

  @ApiModelProperty(value = "Has Reactome Diagram")
  String diagrammed;

  // Reactome pathway
  List<List<Map<String, String>>> hierarchy;

  // Gene Ontology
  String ontology;
  List<String> altIds;
  List<String> synonyms;
  List<Map<String, String>> inferredTree;

  @SuppressWarnings("unchecked")
  @JsonCreator
  public GeneSet(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(EntityType.GENE_SET);
    id = getString(fieldMap.get(fields.get("id")));
    name = getString(fieldMap.get(fields.get("name")));
    source = getString(fieldMap.get(fields.get("source")));
    type = getString(fieldMap.get(fields.get("type")));
    description = getString(fieldMap.get(fields.get("description")));
    geneCount = getLong(fieldMap.get(fields.get("geneCount")));

    // Reactome pathway specific fields
    hierarchy = buildPathwayHierarchy((List<List<Map<String, Object>>>) fieldMap.get(SOURCE_FIELDS.get("hierarchy")));
    diagrammed = String.valueOf(getBoolean(fieldMap.get(fields.get("diagrammed")), false));

    // Gene ontology specific fields
    ontology = getString(fieldMap.get(fields.get("ontology")));
    altIds = (List<String>) fieldMap.get(SOURCE_FIELDS.get("altIds"));
    synonyms = (List<String>) fieldMap.get(SOURCE_FIELDS.get("synonyms"));
    inferredTree = buildInferredTree((List<Map<String, Object>>) fieldMap.get(SOURCE_FIELDS.get("inferredTree")));

  }

  private List<List<Map<String, String>>> buildPathwayHierarchy(List<List<Map<String, Object>>> field) {
    val result = Lists.<List<Map<String, String>>> newArrayList();

    if (field == null) {
      return result;
    }

    for (val parentPath : field) {
      val path = Lists.<Map<String, String>> newArrayList();
      for (val pathwayNode : parentPath) {
        val node = Maps.<String, String> newHashMap();
        for (val entry : pathwayNode.entrySet()) {
          node.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        path.add(node);
      }

      result.add(path);
    }
    return result;
  }

  private List<Map<String, String>> buildInferredTree(List<Map<String, Object>> field) {
    val result = Lists.<Map<String, String>> newArrayList();

    if (field == null) {
      return result;
    }

    for (val obj : field) {
      val treeNode = Maps.<String, String> newHashMap();
      treeNode.put("id", (String) obj.get("id"));
      treeNode.put("name", (String) obj.get("name"));
      treeNode.put("relation", (String) obj.get("relation"));
      treeNode.put("level", String.valueOf(obj.get("level")));
      result.add(treeNode);
    }
    return result;
  }

}
