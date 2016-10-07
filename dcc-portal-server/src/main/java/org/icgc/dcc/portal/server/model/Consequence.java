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

import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.val;

import org.icgc.dcc.common.core.fi.CompositeImpactCategory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.swagger.annotations.ApiModelProperty;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Consequence {

  @ApiModelProperty(value = "Affected Gene ID", required = true)
  String geneAffectedId;
  @ApiModelProperty(value = "Affected Gene Symbol", required = true)
  String geneAffectedSymbol;
  @ApiModelProperty(value = "Strand", required = true)
  Long geneStrand;
  @ApiModelProperty(value = "AA Mutation", required = true)
  String aaMutation;
  @ApiModelProperty(value = "CDS Mutation", required = true)
  String cdsMutation;
  @ApiModelProperty(value = "Consequence Type", required = true)
  String type;
  @ApiModelProperty(value = "Transcripts Affected", required = true)
  List<Map<String, String>> transcriptsAffected;
  @ApiModelProperty(value = "Functional Impact", required = false)
  String functionalImpact;

  public Consequence(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(EntityType.CONSEQUENCE);
    geneAffectedId = (String) fieldMap.get(fields.get("geneAffectedId"));
    geneAffectedSymbol = (String) fieldMap.get(fields.get("geneAffectedSymbol"));
    geneStrand = getLong(fieldMap.get(fields.get("geneStrand")));
    aaMutation = (String) fieldMap.get(fields.get("aaMutation"));
    cdsMutation = (String) fieldMap.get(fields.get("cdsMutation"));
    type = (String) fieldMap.get(fields.get("type"));

    // Just a placeholder, calculated later as it is a unique value
    functionalImpact = CompositeImpactCategory.UNKNOWN.getId();
  }

  private Long getLong(Object field) {
    if (field instanceof Long) return (Long) field;
    else if (field instanceof Integer) return (long) (Integer) field;
    else if (field instanceof Float) return ((Float) field).longValue();
    else
      return null;
  }

  // Keeps the highest one
  public void addFunctionalImpact(String fiStr) {
    if (CompositeImpactCategory.byId(functionalImpact).getPriority() < CompositeImpactCategory.byId(fiStr)
        .getPriority()) {
      functionalImpact = fiStr;
    }
  }

  public void addTranscript(Map<String, Object> map) {
    transcriptsAffected =
        transcriptsAffected == null ? Lists.<Map<String, String>> newArrayList() : transcriptsAffected;
    val transcript = Maps.<String, String> newHashMap();
    transcript.put("id", (String) map.get("id"));
    transcript.put("name", (String) map.get("name"));
    transcriptsAffected.add(transcript);
  }
}
