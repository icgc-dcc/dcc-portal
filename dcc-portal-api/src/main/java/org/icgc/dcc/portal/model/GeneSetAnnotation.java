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

package org.icgc.dcc.portal.model;

import java.util.List;
import java.util.Map;

import lombok.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "GeneSet")
public class GeneSetAnnotation {

  // Common fields
  @ApiModelProperty(value = "ID", required = true)
  String id;

  @ApiModelProperty(value = "Name", required = true)
  String name;

  @ApiModelProperty(value = "Type", required = true)
  String type;

  @ApiModelProperty(value = "Annotation")
  String annotation;

  @ApiModelProperty(value = "GO qualifiers")
  List<String> qualifiers;

  // @SuppressWarnings("unchecked")
  @SuppressWarnings("unchecked")
  @JsonCreator
  public GeneSetAnnotation(Map<String, Object> fieldMap) {
    id = (String) fieldMap.get("id");
    name = (String) fieldMap.get("name");
    type = (String) fieldMap.get("type");
    annotation = (String) fieldMap.get("annotation");
    qualifiers = (List<String>) fieldMap.get("qualifiers");
  }

}
