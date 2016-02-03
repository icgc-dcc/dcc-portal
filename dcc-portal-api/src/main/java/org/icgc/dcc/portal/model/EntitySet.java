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
package org.icgc.dcc.portal.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.UUID;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Represents an entity set.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "EntitySet")
@JsonInclude(NON_NULL)
public class EntitySet extends BaseEntitySet implements Identifiable<UUID> {

  @NonNull
  @ApiModelProperty(value = "ID of this entity set.", required = true)
  private final UUID id;

  @NonNull
  @ApiModelProperty(value = "The processing state for this entity set.", required = true)
  private State state;

  @ApiModelProperty(value = "Number of elements in this entity set.")
  private Long count;

  /*
   * This is the default value and is used for migration when this version field is introduced.
   */
  private int version = 1;

  @NonNull
  @ApiModelProperty(value = "Sub-type for this entity set.", required = true)
  private SubType subtype = SubType.NORMAL;

  public EntitySet updateStateToInProgress() {
    this.state = State.IN_PROGRESS;
    return this;
  }

  public EntitySet updateStateToFinished(final long count) {
    checkArgument(count >= 0, "The 'count' argument must be a positive integer.");

    this.state = State.FINISHED;
    this.count = count;
    return this;
  }

  public EntitySet updateStateToError() {
    this.state = State.ERROR;
    return this;
  }

  private final static class JsonPropertyName {

    final static String ID = "id";
    final static String STATE = "state";
    final static String COUNT = "count";
    final static String VERSION = "version";
    final static String NAME = "name";
    final static String DESCRIPTION = "description";
    final static String TYPE = "type";
  }

  @JsonCreator
  public EntitySet(
      @JsonProperty(JsonPropertyName.ID) final UUID id,
      @JsonProperty(JsonPropertyName.STATE) final State state,
      @JsonProperty(JsonPropertyName.COUNT) final Long count,
      @JsonProperty(JsonPropertyName.NAME) @NonNull final String name,
      @JsonProperty(JsonPropertyName.DESCRIPTION) final String description,
      @JsonProperty(JsonPropertyName.TYPE) @NonNull final Type type,
      @JsonProperty(JsonPropertyName.VERSION) final Integer dataVersion) {
    super(name, description, type);

    this.id = id;
    this.state = state;
    this.count = count;

    if (null != dataVersion && dataVersion > 1) {
      /*
       * Use the default value 1 if it is null. This is used to migrate the JSON when the version field is introduced.
       */
      this.version = dataVersion;
    }
  }

  // static constructors
  /*
   * This constructor is used for creating a brand-new entity set.
   */
  public static EntitySet createFromDefinition(@NonNull final BaseEntitySet definition, final int dataVersion) {
    return new EntitySet(
        UUID.randomUUID(),
        State.PENDING,
        null,
        definition.getName(),
        definition.getDescription(),
        definition.getType(),
        dataVersion);
  }

  /*
   * This constructor is used for instantiating an existing entity set and setting the status to State.FINISHED.
   */
  public static EntitySet createForStatusFinished(final UUID id, final String name, final String description,
      final Type type, final long count, final int dataVersion) {
    checkArgument(count >= 0, "The 'count' argument must be a positive integer.");

    return new EntitySet(id, State.FINISHED, count, name, description, type, dataVersion);
  }

  @RequiredArgsConstructor
  @Getter
  @ApiModel(value = "State")
  public enum State {
    PENDING("pending"),
    IN_PROGRESS("in progess"),
    FINISHED("finished"),
    ERROR("error");

    @NonNull
    private final String name;
  }

  @RequiredArgsConstructor
  @Getter
  @ApiModel(value = "Subtype")
  public enum SubType {
    NORMAL("normal"),
    UPLOAD("upload"),
    ENRICHMENT("enrichment"),
    TRANSIENT("transient");

    @NonNull
    private final String name;
  }
}
