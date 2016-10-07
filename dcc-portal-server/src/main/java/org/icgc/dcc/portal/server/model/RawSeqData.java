package org.icgc.dcc.portal.server.model;

import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;

import java.util.Map;

import lombok.Value;
import lombok.val;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "RawSeqData")
public class RawSeqData {

  @ApiModelProperty(value = "Raw Sequence Data ID", required = true)
  String id;
  String platform;
  String state;
  String type;
  String libraryStrategy;
  String analyteCode;
  String dataUri;
  String repository;
  String filename;
  String rawDataAccession;

  public RawSeqData(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(EntityType.SEQ_DATA);

    id = (String) fieldMap.get(fields.get("id"));
    platform = (String) fieldMap.get(fields.get("platform"));
    state = (String) fieldMap.get(fields.get("state"));
    type = (String) fieldMap.get(fields.get("type"));
    libraryStrategy = (String) fieldMap.get(fields.get("libraryStrategy"));
    analyteCode = (String) fieldMap.get(fields.get("analyteCode"));
    dataUri = (String) fieldMap.get(fields.get("dataUri"));
    repository = (String) fieldMap.get(fields.get("repository"));
    filename = (String) fieldMap.get(fields.get("filename"));
    rawDataAccession = (String) fieldMap.get(fields.get("rawDataAccession"));
  }

}
