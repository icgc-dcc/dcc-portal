/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.server.manifest.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;

import java.util.List;
import java.util.UUID;

import org.icgc.dcc.portal.server.model.Identifiable;
import org.icgc.dcc.portal.server.util.ObjectNodeDeserializer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * The representation of an abstract file manifest.
 */
@Data
@Accessors(chain = true)
@JsonInclude(NON_NULL)
@ApiModel(value = "Manifest")
public class Manifest implements Identifiable<UUID> {

  /**
   * Resource identifier
   */
  @ApiModelProperty(value = "The ID of the manifest")
  UUID id;

  /**
   * Used for schema evolution.
   * <p>
   * This is the default value and is used for migration when this version field is introduced.
   */
  Integer version = 1;

  /**
   * When created.
   */
  long timestamp = System.currentTimeMillis();

  /**
   * Specification
   */
  @JsonDeserialize(using = ObjectNodeDeserializer.class)
  @ApiModelProperty(value = "The filters to apply to the repository files", required = true)
  ObjectNode filters = DEFAULT.createObjectNode();

  List<String> repos = newArrayList();

  List<ManifestField> fields = newArrayList();

  @ApiModelProperty(value = "The output format", required = true)
  ManifestFormat format = ManifestFormat.JSON;

  boolean unique;

  boolean multipart;
  List<ManifestEntry> entries;

}
