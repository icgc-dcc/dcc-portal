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

import lombok.Value;
import lombok.val;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Sample")
public class Sample {

  @ApiModelProperty(value = "Sample ID", required = true)
  String id;
  @ApiModelProperty(value = "Analyzed ID", required = true)
  String analyzedId;
  @ApiModelProperty(value = "Analyzed Interval", required = true)
  Integer analyzedInterval;
  @ApiModelProperty(value = "Study", required = true)
  String study;

  @ApiModelProperty(value = "Available Raw Sequence Data", required = true)
  List<RawSeqData> availableRawSequenceData;

  @SuppressWarnings("unchecked")
  public Sample(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(EntityType.SAMPLE);

    id = (String) fieldMap.get(fields.get("id"));
    analyzedId = (String) fieldMap.get(fields.get("analyzedId"));
    analyzedInterval = (Integer) fieldMap.get(fields.get("analyzedInterval"));
    study = (String) fieldMap.get(fields.get("study"));
    availableRawSequenceData =
        buildRawSeqData((List<Map<String, Object>>) fieldMap.get(fields.get("availableRawSequenceData")));
  }

  private List<RawSeqData> buildRawSeqData(List<Map<String, Object>> field) {
    if (field == null) return null;
    val lst = Lists.<RawSeqData> newArrayList();
    for (val item : field) {
      lst.add(new RawSeqData(item));
    }
    return lst;
  }
}
