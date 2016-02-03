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

import static com.fasterxml.jackson.databind.PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES;

import java.util.List;

import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Models a drug document from the Drug index type
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "Drug")
public class Drug {

  private static final Class<Drug> MY_CLASS = Drug.class;
  private static final ObjectMapper MAPPER = createMapper();

  @SneakyThrows
  public static Drug parse(@NonNull String json) {
    return MAPPER.readValue(json, MY_CLASS);
  }

  /*
   * Fields
   */
  @ApiModelProperty(value = "ZINC ID")
  String zincId;

  @ApiModelProperty(value = "Name of the drug")
  String name;

  @ApiModelProperty(value = "Small version of the drug's molecule image")
  String smallImageUrl;

  @ApiModelProperty(value = "Large version of the drug's molecule image")
  String largeImageUrl;

  @ApiModelProperty(value = "InChIKey of the drug")
  String inchikey;

  @ApiModelProperty(value = "Class of the drug")
  String drugClass;

  @ApiModelProperty(value = "Number of cancer trials related to the drug")
  long cancerTrialCount;

  @ApiModelProperty(value = "Synonyms of the drug")
  List<String> synonyms;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class ExternalReferences {

    @ApiModelProperty(value = "ChEMBL reference of the drug")
    List<String> chembl;

    @ApiModelProperty(value = "DrugBank reference of the drug")
    List<String> drugbank;

  }

  @ApiModelProperty(value = "External references of the drug (e.g. ChEMBL and DrugBank)")
  ExternalReferences externalReferences;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class AtcCode {

    @ApiModelProperty(value = "ATC code of the drug")
    String code;

    @ApiModelProperty(value = "ATC description of the drug")
    String description;

    @ApiModelProperty(value = "ATC Fifth level information of the drug")
    String atcLevel5Codes;

  }

  @ApiModelProperty(value = "Anatomical Therapeutic Chemical (ATC) information of the drug")
  List<AtcCode> atcCodes;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class Gene {

    @ApiModelProperty(value = "UniProt of the gene")
    String uniprot;

    @ApiModelProperty(value = "Ensembl Gene ID")
    String ensemblGeneId;

  }

  @ApiModelProperty(value = "Gene information related to the drug")
  List<Gene> genes;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class Trial {

    @ApiModelProperty(value = "Trial code")
    String code;

    @ApiModelProperty(value = "Trial description")
    String description;

    @ApiModelProperty(value = "Phase of the trial")
    String phaseName;

    @ApiModelProperty(value = "Start date of the trial")
    String startDate;

    @ApiModelProperty(value = "Status of the trial")
    String statusName;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Condition {

      @ApiModelProperty(value = "Condition name of the trial")
      String name;

      @ApiModelProperty(value = "Short name of the trial condition")
      String shortName;

    }

    @ApiModelProperty(value = "A list of conditions associated with the trial")
    List<Condition> conditions;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class DrugMapping {

      @ApiModelProperty(value = "Description of ")
      String description;

      @ApiModelProperty(value = "IDs of ")
      List<String> ids;

    }

    @ApiModelProperty(value = "")
    List<DrugMapping> drugMappings;

  }

  @ApiModelProperty(value = "Clinical trials related to the drug")
  List<Trial> trials;

  // Helpers
  static final ObjectMapper createMapper() {
    val strategy = CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES;

    return new ObjectMapper()
        .setPropertyNamingStrategy(strategy);
  }
}
