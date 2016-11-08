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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.Lists;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

/**
 * Models a file from external repositories such as CGHub
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "File")
public class File {

  private static final ObjectMapper MAPPER = createMapper();

  @SneakyThrows
  public static File parse(@NonNull String json) {
    return MAPPER.readValue(json, File.class);
  }

  @SneakyThrows
  public static File parse(@NonNull Map<String, Object> fieldMap) {
    return MAPPER.convertValue(fieldMap, File.class);
  }

  /*
   * Fields
   */
  @ApiModelProperty(value = "ID of a repository file")
  String id;
  @ApiModelProperty(value = "Object ID of a repository file")
  String objectId;
  @ApiModelProperty(value = "Access type of a repository file")
  String access;
  @ApiModelProperty(value = "Study type of a repository file")
  List<String> study;
  @ApiModelProperty(value = "Data categorization of a repository file")
  DataCategorization dataCategorization;
  @ApiModelProperty(value = "Data bundle info of a repository file")
  DataBundle dataBundle;
  @ApiModelProperty(value = "Copies of a repository file")
  List<FileCopy> fileCopies;
  @ApiModelProperty(value = "Donors info of a repository file")
  List<Donor> donors;
  @ApiModelProperty(value = "Reference genome of a repository file")
  ReferenceGenome referenceGenome;
  @ApiModelProperty(value = "Analysis method of a repository file")
  AnalysisMethod analysisMethod;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class DataCategorization {

    @ApiModelProperty(value = "Data type of a repository file")
    String dataType;

    @ApiModelProperty(value = "Experimental strategy of a repository file")
    String experimentalStrategy;

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class DataBundle {

    @ApiModelProperty(value = "Data bundle ID of a repository file")
    String dataBundleId;

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class FileCopy {

    @ApiModelProperty(value = "Repository specific bundle id of a file copy")
    String repoDataBundleId;

    @ApiModelProperty(value = "Repository specific file id of a file copy")
    String repoFileId;

    @ApiModelProperty(value = "Repository specific data set ids of a file copy")
    List<String> repoDataSetIds = Lists.newArrayList();

    @ApiModelProperty(value = "Repository code of a file copy")
    String repoCode;

    @ApiModelProperty(value = "Repository organization of a file copy")
    String repoOrg;

    @ApiModelProperty(value = "Repository name of a file copy")
    String repoName;

    @ApiModelProperty(value = "Repository type of a file copy")
    String repoType;

    @ApiModelProperty(value = "Repository country of a file copy")
    String repoCountry;

    @ApiModelProperty(value = "Repository's base URL of a file copy")
    String repoBaseUrl;

    @ApiModelProperty(value = "Repository's data path of a file copy")
    String repoDataPath;

    @ApiModelProperty(value = "Repository's meta-data path of a file copy")
    String repoMetadataPath;

    @ApiModelProperty(value = "Repository's index info of a file copy")
    IndexFile indexFile;

    @ApiModelProperty(value = "File name of a file copy")
    String fileName;

    @ApiModelProperty(value = "File format of a file copy")
    String fileFormat;

    @ApiModelProperty(value = "File MD5 sum of a file copy")
    String fileMd5sum;

    @ApiModelProperty(value = "File size of a file copy")
    Long fileSize;

    @ApiModelProperty(value = "Last modification timestamp of a file copy")
    Long lastModified;

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class IndexFile {

    @ApiModelProperty(value = "ID of a repository index file")
    String id;

    @ApiModelProperty(value = "Object UUID of a repository index file")
    String objectId;

    @ApiModelProperty(value = "Name of a repository index file")
    String fileName;

    @ApiModelProperty(value = "Format of a repository index file")
    String fileFormat;

    @ApiModelProperty(value = "MD5 sum of a repository index file")
    String fileMd5sum;

    @ApiModelProperty(value = "Size of a repository index file")
    Long fileSize;

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class Donor {

    @ApiModelProperty(value = "Donor ID of a repository file")
    String donorId;

    @ApiModelProperty(value = "Program of a repository file")
    String program;

    @ApiModelProperty(value = "Primary site of a repository file")
    String primarySite;

    @ApiModelProperty(value = "Project ID of a repository file")
    String projectCode;

    @ApiModelProperty(value = "Donor study of a repository file")
    String study;

    @ApiModelProperty(value = "Sample ID of a repository file")
    List<String> sampleId = Lists.newArrayList();

    @ApiModelProperty(value = "Specimen ID of a repository file")
    List<String> specimenId = Lists.newArrayList();

    @ApiModelProperty(value = "Specimen type of a repository file")
    List<String> specimenType = Lists.newArrayList();

    @ApiModelProperty(value = "Donor submitter ID of a repository file")
    String submittedDonorId;

    @ApiModelProperty(value = "Sample submitter ID of a repository file")
    List<String> submittedSampleId = Lists.newArrayList();

    @ApiModelProperty(value = "Specimen submitter ID of a repository file")
    List<String> submittedSpecimenId = Lists.newArrayList();

    @ApiModelProperty(value = "Matched control sample ID of a repository file")
    String matchedControlSampleId;

    @ApiModelProperty(value = "Other identifiers of a repository file")
    OtherIdentifiers otherIdentifiers;

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class OtherIdentifiers {

    @ApiModelProperty(value = "TCGA sample barcode of a repository file")
    List<String> tcgaSampleBarcode = Lists.newArrayList();

    @ApiModelProperty(value = "TCGA aliquot barcode of a repository file")
    List<String> tcgaAliquotBarcode = Lists.newArrayList();

    @ApiModelProperty(value = "TCGA participant barcode of a repository file")
    String tcgaParticipantBarcode;

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class AnalysisMethod {

    @ApiModelProperty(value = "Analysis type of a repository file")
    String analysisType;

    @ApiModelProperty(value = "Analysis software of a repository file")
    String software;

  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class ReferenceGenome {

    @ApiModelProperty(value = "Genome build of  reference genome")
    String genomeBuild;

    @ApiModelProperty(value = "Reference Name of reference genome")
    String referenceName;

    @ApiModelProperty(value = "Download URL of reference genome")
    String downloadUrl;

  }

  // Helpers
  static final ObjectMapper createMapper() {
    /*
     * We read fields in snake case from an ES response into fields in camel case in Java. Note: Due to this, the serde
     * process is one-way only (deserializing from snake case but serializing in camel case). Don't expect a serialized
     * JSON to be deserialized back into an instance. However, for our current use case, this is okay as we don't expect
     * to consume (JSON with field names in camel case) but produce only.
     */
    val strategy = PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES;
    val result = new ObjectMapper();
    result.setPropertyNamingStrategy(strategy);
    return result;
  }

}
