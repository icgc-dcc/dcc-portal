/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS AS IS AND ANY                           
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

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "Repository", description = "A representation of an external data resposity housing ICGC data sets")
public class Repository implements Identifiable<String> {

  @ApiModelProperty(value = "The canonical ID of the repository", required = true)
  String id;

  @ApiModelProperty(value = "The repository code. Currently same as `id`", required = true)
  String code;

  @ApiModelProperty(value = "The type of repository", required = true)
  String type;

  @ApiModelProperty(value = "The name of the repository", required = true)
  String name;

  @ApiModelProperty(value = "The source organization of data of the repository", required = true)
  String source;

  @ApiModelProperty(value = "Where the repository geographically resides", required = true)
  String country;

  @ApiModelProperty(value = "The base URL of the repository", required = true)
  String baseUrl;

  @ApiModelProperty(value = "The data path of the repository relative to the `baseUrl`")
  String dataPath;

  @ApiModelProperty(value = "The metadata path of the repository relative to the `baseUrl`")
  String metadataPath;

  @JsonIgnore
  public boolean isGNOS() {
    return type.equals("GNOS");
  }

  @JsonIgnore
  public boolean isS3() {
    return type.equals("S3");
  }

  @JsonIgnore
  public boolean isGDC() {
    return type.equals("GDC");
  }

  @JsonIgnore
  public boolean isPDC() {
    return type.equals("PDC");
  }

  @JsonIgnore
  public boolean isEGA() {
    return type.equals("EGA");
  }

}
