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

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * TODO
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "UnionUnitWithCount")
public class UnionUnitWithCount extends UnionUnit {

  @ApiModelProperty(value = "todo", required = true)
  private final long count;

  private final static class JsonPropertyName {

    final static String intersection = "intersection";
    final static String exclusions = "exclusions";
    final static String count = "count";
  }

  @JsonCreator
  public UnionUnitWithCount(
      @JsonProperty(JsonPropertyName.intersection) final Set<UUID> intersection,
      @JsonProperty(JsonPropertyName.exclusions) final Set<UUID> exclusions,
      @JsonProperty(JsonPropertyName.count) final long count) {

    super(intersection, exclusions);

    if (count < 0) {
      throw new IllegalArgumentException("The count argument must not be a negative number.");
    }
    this.count = count;
  }

  // static constructors
  public static UnionUnitWithCount copyOf(
      @NonNull final UnionUnit unionUnit,
      final long count) {

    return new UnionUnitWithCount(
        unionUnit.getIntersection(),
        unionUnit.getExclusions(),
        count);
  }

  public static UnionUnitWithCount noExclusionInstance(
      final Set<UUID> intersection,
      final int count) {

    return new UnionUnitWithCount(
        intersection,
        Collections.<UUID> emptySet(),
        count);
  }

  // shadow the superclass's own implementation
  public static UnionUnitWithCount noExclusionInstance(
      final Set<UUID> intersection) {

    return UnionUnitWithCount.noExclusionInstance(intersection, 0);
  }
}
