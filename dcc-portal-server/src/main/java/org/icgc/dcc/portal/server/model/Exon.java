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

import java.util.Map;

import lombok.Value;
import lombok.val;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Exon {

  @ApiModelProperty(value = "Start Position", required = true)
  Long start;
  @ApiModelProperty(value = "End Position", required = true)
  Long end;
  @ApiModelProperty(value = "CDNA Coding Start Position", required = true)
  Long cdnaCodingStart;
  @ApiModelProperty(value = "CDNA Coding End Position", required = true)
  Long cdnaCodingEnd;
  @ApiModelProperty(value = "Genomic Coding Start Position", required = true)
  Long genomicCodingStart;
  @ApiModelProperty(value = "Genomic Coding End Position", required = true)
  Long genomicCodingEnd;
  @ApiModelProperty(value = "CDNA Start Position", required = true)
  Long cdnaStart;
  @ApiModelProperty(value = "CDNA End Position", required = true)
  Long cdnaEnd;
  @ApiModelProperty(value = "Start Phase", required = true)
  Long startPhase;
  @ApiModelProperty(value = "End Phase", required = true)
  Long endPhase;

  public Exon(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(EntityType.EXON);
    start = getLong(fieldMap.get(fields.get("start")));
    end = getLong(fieldMap.get(fields.get("end")));
    cdnaCodingStart = getLong(fieldMap.get(fields.get("cdnaCodingStart")));
    cdnaCodingEnd = getLong(fieldMap.get(fields.get("cdnaCodingEnd")));
    genomicCodingStart = getLong(fieldMap.get(fields.get("genomicCodingStart")));
    genomicCodingEnd = getLong(fieldMap.get(fields.get("genomicCodingEnd")));
    cdnaStart = getLong(fieldMap.get(fields.get("cdnaStart")));
    cdnaEnd = getLong(fieldMap.get(fields.get("cdnaEnd")));
    startPhase = getLong(fieldMap.get(fields.get("startPhase")));
    endPhase = getLong(fieldMap.get(fields.get("endPhase")));
  }

  private Long getLong(Object field) {
    if (field instanceof Long) return (Long) field;
    if (field instanceof Integer) return (long) (Integer) field;
    else
      return null;
  }
}
