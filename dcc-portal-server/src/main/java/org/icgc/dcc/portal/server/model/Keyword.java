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
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getString;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.ImmutableMap;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Value;
import lombok.val;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Keyword")
public class Keyword {

  private static final ImmutableMap<String, String> FIELDS = FIELDS_MAPPING.get(EntityType.KEYWORD);

  @ApiModelProperty(value = "ID", required = true)
  String id;
  @ApiModelProperty(value = "Type", required = true)
  String type;
  @ApiModelProperty(required = false)
  String name;
  @ApiModelProperty(required = false)
  String symbol;
  @ApiModelProperty(required = false)
  String ensemblTranscriptId;
  @ApiModelProperty(required = false)
  String ensemblTranslationId;
  @ApiModelProperty(required = false)
  List<String> synonyms;
  @ApiModelProperty(required = false)
  List<String> uniprotkbSwissprot;
  @ApiModelProperty(required = false)
  List<String> omimGene;
  @ApiModelProperty(required = false)
  List<String> entrezGene;
  @ApiModelProperty(required = false)
  List<String> hgnc;
  @ApiModelProperty(required = false)
  List<String> altIds;

  @ApiModelProperty(required = false)
  String projectId;
  @ApiModelProperty(required = false)
  String submittedId;
  @ApiModelProperty(required = false)
  List<String> specimenIds;
  @ApiModelProperty(required = false)
  List<String> submittedSpecimenIds;
  @ApiModelProperty(required = false)
  List<String> sampleIds;
  @ApiModelProperty(required = false)
  List<String> submittedSampleIds;

  @ApiModelProperty(required = false)
  String primarySite;
  @ApiModelProperty(required = false)
  String tumourType;
  @ApiModelProperty(required = false)
  String tumourSubtype;

  @ApiModelProperty(required = false)
  String mutation;
  @ApiModelProperty(required = false)
  List<String> geneMutations;

  @ApiModelProperty(required = false)
  String url;

  @ApiModelProperty(required = false)
  String source;

  @ApiModelProperty(required = false)
  List<String> donorId;

  @ApiModelProperty(required = false)
  List<String> fileName;

  @ApiModelProperty(required = false)
  String dataType;

  @ApiModelProperty(required = false)
  List<String> projectCode;

  @ApiModelProperty(required = false)
  String fileObjectId;

  @ApiModelProperty(required = false)
  String fileBundleId;

  @ApiModelProperty(required = false)
  List<String> tcgaParticipantBarcode;

  @ApiModelProperty(required = false)
  List<String> tcgaSampleBarcode;

  @ApiModelProperty(required = false)
  List<String> tcgaAliquotBarcode;

  // Drug-text fields
  @ApiModelProperty(required = false)
  String inchikey;
  @ApiModelProperty(required = false)
  String drugClass;
  @ApiModelProperty(required = false)
  List<String> atcCodes;
  @ApiModelProperty(required = false)
  List<String> atcCodeDescriptions;
  @ApiModelProperty(required = false)
  List<String> atcLevel5Codes;
  @ApiModelProperty(required = false)
  List<String> trialDescriptions;
  @ApiModelProperty(required = false)
  List<String> trialConditionNames;
  @ApiModelProperty(required = false)
  List<String> externalReferencesDrugbank;
  @ApiModelProperty(required = false)
  List<String> externalReferencesChembl;

  @SuppressWarnings("unchecked")
  public Keyword(Map<String, Object> fieldMap) {
    type = getString(fieldMap.get(FIELDS.get("type")));
    name = getString(fieldMap.get(FIELDS.get("name")));

    symbol = getString(fieldMap.get(FIELDS.get("symbol")));
    ensemblTranscriptId = getString(fieldMap.get(FIELDS.get("ensemblTranscriptId")));
    ensemblTranslationId = getString(fieldMap.get(FIELDS.get("ensemblTranslationId")));
    synonyms = (List<String>) fieldMap.get(FIELDS.get("synonyms"));
    uniprotkbSwissprot = (List<String>) fieldMap.get(FIELDS.get("uniprotkbSwissprot"));
    omimGene = (List<String>) fieldMap.get(FIELDS.get("omimGene"));
    entrezGene = (List<String>) fieldMap.get(FIELDS.get("entrezGene"));
    hgnc = (List<String>) fieldMap.get(FIELDS.get("hgnc"));
    altIds = (List<String>) fieldMap.get(FIELDS.get("altIds"));

    projectId = getString(fieldMap.get(FIELDS.get("projectId")));
    submittedId = getString(fieldMap.get(FIELDS.get("submittedId")));
    specimenIds = (List<String>) fieldMap.get(FIELDS.get("specimenIds"));
    submittedSpecimenIds = (List<String>) fieldMap.get(FIELDS.get("submittedSpecimenIds"));
    sampleIds = (List<String>) fieldMap.get(FIELDS.get("sampleIds"));
    submittedSampleIds = (List<String>) fieldMap.get(FIELDS.get("submittedSampleIds"));

    primarySite = getString(fieldMap.get(FIELDS.get("primarySite")));
    tumourType = getString(fieldMap.get(FIELDS.get("tumourType")));
    tumourSubtype = getString(fieldMap.get(FIELDS.get("tumourSubtype")));

    mutation = getString(fieldMap.get(FIELDS.get("mutation")));
    geneMutations = (List<String>) fieldMap.get(FIELDS.get("geneMutations"));

    url = getString(fieldMap.get(FIELDS.get("url")));
    source = getString(fieldMap.get(FIELDS.get("source")));

    // File
    fileObjectId = getString(fieldMap.get(FIELDS.get("object_id")));
    fileBundleId = getString(fieldMap.get(FIELDS.get("data_bundle_id")));
    dataType = getString(fieldMap.get(FIELDS.get("data_type")));

    fileName = (List<String>) fieldMap.get(FIELDS.get("file_name"));
    donorId = (List<String>) fieldMap.get(FIELDS.get("donor_id"));
    projectCode = (List<String>) fieldMap.get(FIELDS.get("project_code"));

    tcgaParticipantBarcode = (List<String>) fieldMap.get(FIELDS.get("TCGAParticipantBarcode"));
    tcgaAliquotBarcode = (List<String>) fieldMap.get(FIELDS.get("TCGAAliquotBarcode"));
    tcgaSampleBarcode = (List<String>) fieldMap.get(FIELDS.get("TCGASampleBarcode"));

    // Drug-text
    val drugTextPrefix = "drug-text.";
    inchikey = string(fieldMap, drugTextPrefix + "inchikey");
    drugClass = string(fieldMap, drugTextPrefix + "drug_class");
    atcCodes = strings(fieldMap, drugTextPrefix + "atc_codes_code");
    atcCodeDescriptions = strings(fieldMap, drugTextPrefix + "atc_codes_description");
    atcLevel5Codes = strings(fieldMap, drugTextPrefix + "atc_level5_codes");
    trialDescriptions = strings(fieldMap, drugTextPrefix + "trials_description");
    trialConditionNames = strings(fieldMap, drugTextPrefix + "trials_conditions_name");
    externalReferencesDrugbank = strings(fieldMap, drugTextPrefix + "external_references_drugbank");
    externalReferencesChembl = strings(fieldMap, drugTextPrefix + "external_references_chembl");

    // Generic id
    id = string(fieldMap, "id");
  }

  private static String string(Map<String, Object> fieldMap, String fieldName) {
    return getString(fieldMap.get(FIELDS.get(fieldName)));
  }

  @SuppressWarnings("unchecked")
  private static List<String> strings(Map<String, Object> fieldMap, String fieldName) {
    return (List<String>) fieldMap.get(FIELDS.get(fieldName));
  }

}
