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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.val;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Represents the definition of a derived set.
 */
@Value
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "DerivedEntitySetDefinition")
public class DerivedEntitySetDefinition extends BaseEntitySetDefinition {

  @NonNull
  @ApiModelProperty(value = "A list of UnionUnits that will form the union of the resulting set.", required = true)
  private final List<UnionUnit> union;

  private final static class JsonPropertyName {

    final static String UNION = "union";
    final static String NAME = "name";
    final static String DESCRIPTION = "description";
    final static String TYPE = "type";
    final static String IS_TRANSIENT = "isTransient";

  }

  @JsonCreator
  public DerivedEntitySetDefinition(
      @NonNull @JsonProperty(JsonPropertyName.UNION) final List<UnionUnit> union,
      @NonNull @JsonProperty(JsonPropertyName.NAME) final String name,
      @JsonProperty(JsonPropertyName.DESCRIPTION) final String description,
      @NonNull @JsonProperty(JsonPropertyName.TYPE) final Type type,
      @JsonProperty(JsonPropertyName.IS_TRANSIENT) final boolean isTransient) {
    super(name, description, type, isTransient);

    validateUnion(union);
    this.union = union;
  }

  /**
   * @param union
   */
  private void validateUnion(final List<UnionUnit> union) {
    checkArgument(!union.isEmpty(), "The union argument must not be empty.");

    for (val unionUnit : union) {
      if (!unionUnit.isValid()) {
        throw new IllegalArgumentException("Not all union units in the union argument are valid.");
      }
    }

  }

}
