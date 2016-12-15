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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Represents the definition of a set originated from Advanced Search
 */
@Value
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "EntitySetDefinition")
public class EntitySetDefinition extends BaseEntitySetDefinition {

  private final static int MIN_SIZE = 1;
  private final static int MAX_SIZE = Integer.MAX_VALUE;

  @NonNull
  @ApiModelProperty(value = "The filter used for the query; must be URL-encoded.", required = true)
  private final ObjectNode filters;
  @NonNull
  @ApiModelProperty(value = "The field used to sort the query result.", required = true)
  private final String sortBy;
  @NonNull
  @ApiModelProperty(value = "The sort order of query result.", required = true)
  private final SortOrder sortOrder;
  @ApiModelProperty(value = "A user-defined limit for the number of entities to return from query.")
  private final int size;

  private final static class JsonPropertyName {

    final static String FILTERS = "filters";
    final static String SORT_BY = "sortBy";
    final static String SORT_ORDER = "sortOrder";
    final static String NAME = "name";
    final static String DESCRIPTION = "description";
    final static String TYPE = "type";
    final static String SIZE = "size";
    final static String IS_TRANSIENT = "isTransient";

  }

  @JsonCreator
  public EntitySetDefinition(
      @JsonProperty(JsonPropertyName.FILTERS) final ObjectNode filters,
      @JsonProperty(JsonPropertyName.SORT_BY) final String sortBy,
      @JsonProperty(JsonPropertyName.SORT_ORDER) final SortOrder sortOrder,
      @NonNull @JsonProperty(JsonPropertyName.NAME) final String name,
      @JsonProperty(JsonPropertyName.DESCRIPTION) final String description,
      @NonNull @JsonProperty(JsonPropertyName.TYPE) final Type type,
      @JsonProperty(JsonPropertyName.SIZE) final int limit,
      @JsonProperty(JsonPropertyName.IS_TRANSIENT) final boolean isTransient) {
    super(name, description, type, isTransient);

    checkArgument(!isNullOrEmpty(sortBy), "The 'sortBy' argument must contain a valid expression.");

    this.sortBy = sortBy;
    this.filters = filters;
    this.sortOrder = sortOrder;
    this.size = (limit < MIN_SIZE) ? MAX_SIZE : limit;
  }

  public int getLimit(final int cap) {
    return Math.min(cap, size);
  }

  @RequiredArgsConstructor
  @Getter
  public enum SortOrder {

    ASCENDING("asc"),
    DESCENDING("desc");

    private final String name;

  }

}
