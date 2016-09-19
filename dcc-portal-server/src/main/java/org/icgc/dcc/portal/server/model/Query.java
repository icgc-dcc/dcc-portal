/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.
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

import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.icgc.dcc.portal.server.util.JsonUtils;
import org.icgc.dcc.portal.server.util.ObjectNodeDeserializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class Query {

  /**
   * Id is always returned, so this is a good way to emulate no fields semantics and not break anything
   */
  public static final List<String> NO_FIELDS = ImmutableList.of("_id");

  public static final List<String> ID_FIELD = ImmutableList.of("id");

  private static final List<String> INCLUDE_FACETS = ImmutableList.of("facets");

  private Integer defaultLimit;

  @JsonDeserialize(using = ObjectNodeDeserializer.class)
  @ApiModelProperty(value = "The filters to apply to the search")
  ObjectNode filters;

  List<String> fields;
  List<String> includes;
  int from;
  int size;
  Integer limit;
  String sort;
  String order;
  String score;
  String query;

  public ObjectNode getFilters() {
    return hasFilters() ? filters.deepCopy() : JsonUtils.MAPPER.createObjectNode();
  }

  public boolean hasFilters() {
    return filters != null && filters.size() > 0;
  }

  public boolean hasFields() {
    return fields != null && !fields.isEmpty();
  }

  public boolean hasInclude(String include) {
    return includes != null && includes.contains(include);
  }

  public int getDefaultLimit() {
    return defaultLimit == null ? 100 : defaultLimit;
  }

  public int getFrom() {
    // Save as 0-base index where 0 and 1 are 0
    return from < 2 ? 0 : from - 1;
  }

  public int getSize() {
    if (limit != null) {
      return size > limit ? limit : size;
    } else {
      return size > getDefaultLimit() ? getDefaultLimit() : size;
    }
  }

  public SortOrder getOrder() {
    if (order == null) return null;
    return SortOrder.valueOf(order.toUpperCase());
  }

  public static List<String> idField() {
    return ID_FIELD;
  }

  public static List<String> includeFacets() {
    return INCLUDE_FACETS;
  }

}
