package org.icgc.dcc.portal.model;

import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableMap;

import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.Facets;
import org.elasticsearch.search.facet.terms.TermsFacet;

import com.wordnik.swagger.annotations.ApiModelProperty;

@Setter
@Getter
public abstract class Paginated {

  @ApiModelProperty(value = "A collection of aggregation counts on pre-defined dimensions of a target entity")
  private Map<String, TermFacet> facets;

  @ApiModelProperty(value = "Pagination information of the search result set", required = true)
  private Pagination pagination;

  public void setFacets(Facets facets) {
    if (facets != null) {
      this.facets = facets.facets().stream().collect(
          toImmutableMap(Facet::getName, facet -> TermFacet.of((TermsFacet) facet)));
    }
  }

  public void addFacets(@NonNull Map<String, TermFacet> facets) {
    this.facets = facets;
  }

}
