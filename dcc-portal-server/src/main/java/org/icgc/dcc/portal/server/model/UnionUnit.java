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

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NonNull;

/**
 * Represents a unit of a set union comprised of a list of sets that form the intersection and a list of sets that need
 * to be excluded.
 */
@Data
@ApiModel(value = "UnionUnit")
public class UnionUnit {

  @ApiModelProperty(value = "Array of entity lists (represented in UUIDs) which form the intersection", required = true)
  private final Set<UUID> intersection;

  @ApiModelProperty(value = "Array of entity lists (represented in UUIDs) which are NOT part of the intersection", required = true)
  private final Set<UUID> exclusions;

  private final static class JsonPropertyName {

    final static String intersection = "intersection";
    final static String exclusions = "exclusions";
  }

  @JsonCreator
  public UnionUnit(
      @NonNull @JsonProperty(JsonPropertyName.intersection) final Set<UUID> intersection,
      @NonNull @JsonProperty(JsonPropertyName.exclusions) final Set<UUID> exclusions) {
    /*
     * the intersection collection must NOT be empty - it wouldn't make sense to have an empty intersection to represent
     * an union, especially when this data structure is meant to be immutable - no setter.
     */
    Preconditions.checkArgument(!intersection.isEmpty(), "The 'intersection' argument must not be empty.");

    this.intersection = intersection;
    this.exclusions = exclusions;
  }

  @JsonIgnore
  public boolean isValid() {
    /*
     * simple sanity check - the intersection should NOT have any element in the exclusion set.
     */
    return (exclusions.isEmpty()) ? true : Sets.intersection(intersection, exclusions).isEmpty();
  }

  // static constructors
  public static UnionUnit noExclusionInstance(final Set<UUID> intersection) {
    return new UnionUnit(intersection, Collections.<UUID> emptySet());
  }
}
