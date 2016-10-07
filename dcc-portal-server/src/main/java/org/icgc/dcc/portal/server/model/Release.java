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
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getLong;

import java.util.Map;

import lombok.Value;
import lombok.val;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Release")
public class Release {

  @ApiModelProperty(value = "Release ID", required = true)
  String id;
  @ApiModelProperty(value = "Release Name", required = true)
  String name;
  @ApiModelProperty(value = "Date of release", required = true)
  String releasedOn;
  @ApiModelProperty(value = "Donor Count", required = true)
  Long donorCount;
  @ApiModelProperty(value = "Mutation Count", required = true)
  Long mutationCount;
  @ApiModelProperty(value = "Sample Count", required = true)
  Long sampleCount;
  @ApiModelProperty(value = "Project Count", required = true)
  Long projectCount;
  @ApiModelProperty(value = "Specimen Count", required = true)
  Long specimenCount;
  @ApiModelProperty(value = "SSM Count", required = true)
  Long ssmCount;
  @ApiModelProperty(value = "Primary Site Count", required = true)
  Long primarySiteCount;
  @ApiModelProperty(value = "Mutated Gene Count", required = true)
  Long mutatedGeneCount;
  @ApiModelProperty(value = "Release Number", required = true)
  Integer releaseNumber;
  @ApiModelProperty(value = "Donors with molecular data", required = true)
  Long liveDonorCount;
  @ApiModelProperty(value = "Projects with molecular data", required = true)
  Long liveProjectCount;
  @ApiModelProperty(value = "Primary Site Count with molecular data", required = true)
  Long livePrimarySiteCount;

  public Release(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(EntityType.RELEASE);
    id = (String) fieldMap.get(fields.get("id"));
    name = (String) fieldMap.get(fields.get("name"));
    releasedOn = (String) fieldMap.get(fields.get("releasedOn"));
    donorCount = getLong(fieldMap.get(fields.get("donorCount")));
    mutationCount = getLong(fieldMap.get(fields.get("mutationCount")));
    sampleCount = getLong(fieldMap.get(fields.get("sampleCount")));
    projectCount = getLong(fieldMap.get(fields.get("projectCount")));
    specimenCount = getLong(fieldMap.get(fields.get("specimenCount")));
    ssmCount = getLong(fieldMap.get(fields.get("ssmCount")));
    primarySiteCount = getLong(fieldMap.get(fields.get("primarySiteCount")));
    mutatedGeneCount = getLong(fieldMap.get(fields.get("mutatedGeneCount")));
    releaseNumber = (Integer) (fieldMap.get(fields.get("releaseNumber")));
    liveDonorCount = getLong(fieldMap.get(fields.get("liveDonorCount")));
    liveProjectCount = getLong(fieldMap.get(fields.get("liveProjectCount")));
    livePrimarySiteCount = getLong(fieldMap.get(fields.get("livePrimarySiteCount")));
  }

}
