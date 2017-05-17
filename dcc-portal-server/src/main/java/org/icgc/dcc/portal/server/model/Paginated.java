package org.icgc.dcc.portal.server.model;

import java.util.Map;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Setter
@Getter
public abstract class Paginated {

  @ApiModelProperty(value = "A collection of aggregation counts on pre-defined dimensions of a target entity")
  private Map<String, TermFacet> facets;

  @ApiModelProperty(value = "Pagination information of the search result set", required = true)
  private Pagination pagination;

  public void addFacets(@NonNull Map<String, TermFacet> facets) {
    this.facets = facets;
  }

}
