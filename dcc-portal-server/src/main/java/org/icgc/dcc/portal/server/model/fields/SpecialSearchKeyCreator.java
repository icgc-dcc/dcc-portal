package org.icgc.dcc.portal.server.model.fields;

import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import lombok.val;

import java.util.Set;

/**
 * Extends the functionality of SearchKeyCreator by effectivly updating a SearchFields boost value if the expanded Search Field name is in the specialSearchField set.
 */
public class SpecialSearchKeyCreator extends SearchKeyCreator {

  public static SpecialSearchKeyCreator newSpecialSearchKeyCreator(
      @NonNull Set<SearchField> unexpandedSearchFields,
      @NonNull Set<SearchField> specialExpandedSearchFields ) {
    return new SpecialSearchKeyCreator(unexpandedSearchFields, specialExpandedSearchFields);
  }

  private final Set<SearchField> specialExpandedSearchFields;

  public SpecialSearchKeyCreator(Set<SearchField> unexpandedSearchFields, Set<SearchField> specialExpandedSearchFields ) {
    super(unexpandedSearchFields);
    this.specialExpandedSearchFields = specialExpandedSearchFields;
  }

  @Override
  public SearchKey getSearchKey(String searchKeyName) {
    val expandedSearchField = super.getSearchKey(searchKeyName).getExpandedSearchField(searchKeyName);
    val setBuilder = ImmutableSet.<SearchField>builder();
    for (val specialExpandedSearchField : specialExpandedSearchFields){
      if (specialExpandedSearchField.hasSameNameAs(expandedSearchField)){
        setBuilder.add(specialExpandedSearchField);
      }else {
        setBuilder.add( expandedSearchField);
      }
    }
    return SearchKey.newSearchKey(searchKeyName, setBuilder.build());
  }

}
