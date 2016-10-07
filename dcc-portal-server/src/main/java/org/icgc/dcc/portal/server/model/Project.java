package org.icgc.dcc.portal.server.model;

import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getLong;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getString;

import java.util.List;
import java.util.Map;

import lombok.Value;
import lombok.val;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Project")
public class Project {

  @ApiModelProperty(value = "Project ID", required = true)
  String id;
  @ApiModelProperty(value = "ICGC Project Node ID", required = true)
  String icgcId;
  @ApiModelProperty(value = "Primary Site", required = true)
  String primarySite;
  @ApiModelProperty(value = "Name", required = true)
  String name;
  @ApiModelProperty(value = "Tumour Type", required = true)
  String tumourType;
  @ApiModelProperty(value = "Tumour Subtype", required = true)
  String tumourSubtype;
  @ApiModelProperty(value = "Pubmed Ids", required = true)
  List<String> pubmedIds;
  @ApiModelProperty(value = "Primary Countries", required = true)
  List<String> primaryCountries;
  @ApiModelProperty(value = "Partner Countries", required = true)
  List<String> partnerCountries;
  @ApiModelProperty(value = "Available Data Types", required = true)
  List<String> availableDataTypes;
  @ApiModelProperty(value = "SSM Tested Donor Count", required = true)
  Long ssmTestedDonorCount;
  @ApiModelProperty(value = "CNSM Tested Donor Count", required = true)
  Long cnsmTestedDonorCount;
  @ApiModelProperty(value = "STSM Tested Donor Count", required = true)
  Long stsmTestedDonorCount;
  @ApiModelProperty(value = "SGV Tested Donor Count", required = true)
  Long sgvTestedDonorCount;
  @ApiModelProperty(value = "METH Seq Tested Donor Count", required = true)
  Long methSeqTestedDonorCount;
  @ApiModelProperty(value = "METH Array Tested Donor Count", required = true)
  Long methArrayTestedDonorCount;
  @ApiModelProperty(value = "EXP Seq Tested Donor Count", required = true)
  Long expSeqTestedDonorCount;
  @ApiModelProperty(value = "EXP Array Tested Donor Count", required = true)
  Long expArrayTestedDonorCount;
  @ApiModelProperty(value = "PEXP Tested Donor Count", required = true)
  Long pexpTestedDonorCount;
  @ApiModelProperty(value = "miRNA Seq Tested Donor Count", required = true)
  Long mirnaSeqTestedDonorCount;
  @ApiModelProperty(value = "JCN Tested Donor Count", required = true)
  Long jcnTestedDonorCount;
  @ApiModelProperty(value = "Total Donor Count", required = true)
  Long totalDonorCount;
  @ApiModelProperty(value = "Total Live Donor Count", required = true)
  Long totalLiveDonorCount;
  @ApiModelProperty(value = "Total Affected Donor Count", required = true)
  Long affectedDonorCount;
  @ApiModelProperty(value = "Map of Donor counts by experimental analysis", required = true)
  Map<String, Integer> experimentalAnalysisPerformedDonorCounts;
  @ApiModelProperty(value = "Map of Sample counts by experimental analysis", required = true)
  Map<String, Integer> experimentalAnalysisPerformedSampleCounts;
  @ApiModelProperty(value = "Repository", required = true)
  List<String> repository;
  @ApiModelProperty(value = "State", required = true)
  String state;

  @SuppressWarnings("unchecked")
  @JsonCreator
  public Project(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(EntityType.PROJECT);
    id = getString(fieldMap.get(fields.get("id")));
    primarySite = getString(fieldMap.get(fields.get("primarySite")));
    name = getString(fieldMap.get(fields.get("name")));
    pubmedIds = (List<String>) fieldMap.get(fields.get("pubmedIds"));
    primaryCountries = (List<String>) fieldMap.get(fields.get("primaryCountries"));
    ssmTestedDonorCount = getLong(fieldMap.get(fields.get("ssmTestedDonorCount")));
    cnsmTestedDonorCount = getLong(fieldMap.get(fields.get("cnsmTestedDonorCount")));
    stsmTestedDonorCount = getLong(fieldMap.get(fields.get("stsmTestedDonorCount")));
    sgvTestedDonorCount = getLong(fieldMap.get(fields.get("sgvTestedDonorCount")));
    methSeqTestedDonorCount = getLong(fieldMap.get(fields.get("methSeqTestedDonorCount")));
    methArrayTestedDonorCount = getLong(fieldMap.get(fields.get("methArrayTestedDonorCount")));
    expSeqTestedDonorCount = getLong(fieldMap.get(fields.get("expSeqTestedDonorCount")));
    expArrayTestedDonorCount = getLong(fieldMap.get(fields.get("expArrayTestedDonorCount")));
    pexpTestedDonorCount = getLong(fieldMap.get(fields.get("pexpTestedDonorCount")));
    mirnaSeqTestedDonorCount = getLong(fieldMap.get(fields.get("mirnaSeqTestedDonorCount")));
    jcnTestedDonorCount = getLong(fieldMap.get(fields.get("jcnTestedDonorCount")));
    totalDonorCount = getLong(fieldMap.get(fields.get("totalDonorCount")));
    totalLiveDonorCount = getLong(fieldMap.get(fields.get("totalLiveDonorCount")));
    affectedDonorCount = getLong(fieldMap.get(fields.get("affectedDonorCount")));
    availableDataTypes = (List<String>) fieldMap.get(fields.get("availableDataTypes"));
    icgcId = getString(fieldMap.get(fields.get("icgcId")));
    tumourType = getString(fieldMap.get(fields.get("tumourType")));
    tumourSubtype = getString(fieldMap.get(fields.get("tumourSubtype")));
    partnerCountries = (List<String>) fieldMap.get(fields.get("partnerCountries"));
    experimentalAnalysisPerformedDonorCounts =
        (Map<String, Integer>) fieldMap.get(fields.get("experimentalAnalysisPerformedDonorCounts"));
    experimentalAnalysisPerformedSampleCounts =
        (Map<String, Integer>) fieldMap.get(fields.get("experimentalAnalysisPerformedSampleCounts"));
    repository = (List<String>) fieldMap.get(fields.get("repository"));

    state = getString(fieldMap.get(fields.get("state")));
  }

}
