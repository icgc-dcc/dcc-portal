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
package org.icgc.dcc.portal.server.util;

import static com.google.common.base.Preconditions.checkState;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.portal.server.model.IndexModel.ALL;
import static org.icgc.dcc.portal.server.model.IndexModel.IS;
import static org.icgc.dcc.portal.server.model.EntityType.GENE;
import static org.icgc.dcc.portal.server.pql.convert.FiltersConverter.ENTITY_SET_PREFIX;
import static org.icgc.dcc.portal.server.util.JsonUtils.MAPPER;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

/**
 * API filter utilities, not to be confused with Elasticsearch filters.
 */
@NoArgsConstructor(access = PRIVATE)
public final class Filters {

  private static final String ID = "id";

  public static ObjectNode emptyFilter() {
    return MAPPER.createObjectNode();
  }

  public static ObjectNode andFilter(@NonNull List<ObjectNode> filters) {
    return andFilter(filters.toArray(new ObjectNode[filters.size()]));
  }

  public static ObjectNode andFilter(@NonNull ObjectNode... filters) {
    JsonNode left = filters[0];
    for (int i = 1; i < filters.length; i++) {
      val right = filters[i];

      left = andFilter(left, right);
    }

    return (ObjectNode) left;
  }

  public static ObjectNode entityFilter(@NonNull String entityName) {
    val entityFilter = emptyFilter();
    entityFilter.with(entityName);

    return entityFilter;
  }

  public static ObjectNode geneFilter() {
    return entityFilter(GENE.getId());
  }

  public static ObjectNode geneSetFilter(@NonNull String geneSetId) {
    val geneFilter = geneFilter();
    geneFilter.with("gene").set("geneSetId", is(geneSetId));

    return geneFilter;
  }

  public static ObjectNode geneTypeFilter(@NonNull String type) {
    val geneFilter = geneFilter();
    geneFilter.with("gene").set("type", is(type));

    return geneFilter;
  }

  public static ObjectNode goTermFilter(@NonNull String goTermId) {
    val geneFilter = geneFilter();
    geneFilter.with(GENE.getId()).set("goTermId", is(goTermId));

    return geneFilter;
  }

  public static ObjectNode pathwayFilter() {
    val geneFilter = geneFilter();
    geneFilter.with("gene").put("hasPathway", true);

    return geneFilter;
  }

  public static ObjectNode inputGeneSetIdFilter(@NonNull String inputGeneSetId) {
    val geneFilter = geneFilter();
    geneFilter.with("gene").set(ID, is(ENTITY_SET_PREFIX + inputGeneSetId));

    return geneFilter;
  }

  public static ObjectNode inputGeneSetFilter(@NonNull UUID inputGeneSetId) {
    // Method appears to be used by multiple contexts, with and without `ES:`
    val setId = inputGeneSetId.toString();
    val id = setId.startsWith(ENTITY_SET_PREFIX) ? setId : ENTITY_SET_PREFIX + setId;

    val inputGeneSetFilter = geneFilter();
    inputGeneSetFilter.with(GENE.getId()).set(ID, is(id));

    return inputGeneSetFilter;
  }

  public static ObjectNode is(@NonNull String value) {
    val is = emptyFilter();
    is.withArray(IS).add(value);

    return is;
  }

  public static ObjectNode mergeAnalysisUniverse(@NonNull ObjectNode current, @NonNull ObjectNode universe) {
    val result = current.deepCopy();
    if (universe.path("gene").path("goTermId").isMissingNode() == false) {
      val universeId = universe.get("gene").get("goTermId").withArray("is").get(0).asText();

      if (result.path("gene").path("goTermId").path("is").isMissingNode() == false) {

        for (val t : result.with("gene").with("goTermId").withArray("is")) {
          result.with("gene").with("goTermId").withArray(ALL).add(t);
        }
        result.with("gene").with("goTermId").withArray(ALL).add(universeId);
        result.with("gene").with("goTermId").remove(IS);

      } else {
        result.with("gene").with("goTermId").withArray(IS).add(universeId);
      }
    } else if (universe.path("gene").path("hasPathway").isMissingNode() == false) {
      result.with("gene").put("hasPathway", true);
    }
    // } else if (universe.path("gene").path(Universe.REACTOME_PATHWAYS.getGeneSetFacetName()).isMissingNode() == false)
    // {
    // result.with("gene").put(Universe.REACTOME_PATHWAYS.getGeneSetFacetName(), true);
    // }
    return result;
  }

  public static ObjectNode mergeAnalysisInputGeneList(@NonNull ObjectNode current, @NonNull ObjectNode geneEntitySet) {
    val result = current.deepCopy();
    if (geneEntitySet.path("gene").path("id").isMissingNode() == false) {
      val entitySetId = geneEntitySet.path("gene").path(ID).withArray(IS).get(0).asText();
      result.with("gene").set("id", is(entitySetId));
    }
    return result;
  }

  public static ObjectNode mergeAnalysisGeneSetFilter(@NonNull ObjectNode current, @NonNull ObjectNode geneSet) {
    val result = current.deepCopy();
    if (geneSet.path("gene").path("geneSetId").isMissingNode() == false) {
      val geneSetId = geneSet.get("gene").get("geneSetId").withArray("is").get(0).asText();

      if (result.path("gene").path("geneSetId").path("is").isMissingNode() == false) {
        for (val t : result.with("gene").with("geneSetId").withArray("is")) {
          result.with("gene").with("geneSetId").withArray(ALL).add(t);
        }
        result.with("gene").with("geneSetId").withArray(ALL).add(geneSetId);
        result.with("gene").with("geneSetId").remove("is");
      } else {
        result.with("gene").with("geneSetId").withArray(IS).add(geneSetId);
      }
    }
    return result;
  }

  private static JsonNode andFilter(JsonNode left, JsonNode right) {
    val and = emptyFilter();

    if (right.getNodeType() == left.getNodeType()) {
      // Same types

      // Arbitrary
      val field = left;

      if (field.isObject()) {
        val fieldNames = ImmutableSet.<String> builder().addAll(left.fieldNames()).addAll(right.fieldNames()).build();
        for (val fieldName : fieldNames) {
          val leftField = left.path(fieldName);
          val rightField = right.path(fieldName);
          val andField = andFilter(leftField, rightField);

          and.set(fieldName, andField);
        }
      } else if (field.isArray()) {
        val values = Sets.<Object> newLinkedHashSet();
        for (val element : Iterables.concat(left, right)) {
          if (element.isNumber()) {
            values.add(element.asInt());
          } else if (element.isBoolean()) {
            values.add(element.asBoolean());
          } else if (element.isTextual()) {
            values.add(element.asText());
          } else if (element.isPojo()) {
            val pojo = (POJONode) element;
            val value = pojo.getPojo();
            values.add(value);
          } else {
            values.add(element);
          }
        }

        val result = MAPPER.createArrayNode();
        for (val value : values) {
          result.addPOJO(value);
        }

        return result;
      } else if (field.isValueNode()) {
        val result = MAPPER.createArrayNode();
        result.add(left);
        result.add(right);

        return result;
      } else if (field.isMissingNode()) {
        // Can't happen - TODO: Explain Why it cannot happen. Also empty branch???
      }
    } else {
      // Different types
      if (left.isMissingNode()) {
        return right;
      } else if (right.isMissingNode()) {
        return left;
      } else {
        checkState(false, "Node type(s) not expected: %s, %s", left, right);
      }
    }

    return and;
  }

}
