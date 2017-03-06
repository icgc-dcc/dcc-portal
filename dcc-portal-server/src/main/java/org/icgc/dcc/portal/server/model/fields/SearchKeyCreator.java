package org.icgc.dcc.portal.server.model.fields;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

import java.util.Set;

import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.common.core.util.stream.Streams.stream;
import static org.icgc.dcc.portal.server.model.fields.SearchKey.newSearchKey;

@Builder
public class SearchKeyCreator {

  @NonNull
  @Singular
  private final Set<SearchField> searchFields;

  public SearchKey getSearchKey(String searchKeyName){
    return newSearchKey(searchKeyName, searchFields);
  }

  public final Set<SearchKey> getSearchKeys(Iterable<String> searchKeyNames){
    return stream(searchKeyNames)
        .map(this::getSearchKey)
        .collect(toImmutableSet());
  }

}
