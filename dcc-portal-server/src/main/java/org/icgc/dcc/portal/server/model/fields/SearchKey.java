package org.icgc.dcc.portal.server.model.fields;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.icgc.dcc.common.core.util.Joiners;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.portal.server.model.fields.SearchField.newBoostedSearchField;

/**
 * Represents a grouping of SearchFields.
 * This class is responsible for expanding its contained SearchFields to include the SearchKey name.
 * For instance, if the SearckKey name is "text", and the unexpanded SearchFields have the names: "raw", "analyzed", "search"
 * with boosts of: 2.0, 1.0, 5.0 respectively,
 * then the expanded SearchKeys would  have the names:  "text.raw", "text.analyzer", "text.search"
 * with boosts of: 2.0, 1.0, 5.0 respectively.
 */
public class SearchKey implements Iterable<SearchField>{

  private static final Joiner DEFAULT_JOINER = Joiners.DOT;

  @Getter
  private final String name;

  /**
   * Map contains mapping of unique unexpanded SearchField names (ie "raw", "analyzed", "search", etc)
   * to expanded SearchField instances
   */
  private final Map<String, SearchField> map;

  /**
   * Factory Methods
   */
  public static SearchKey newSearchKey(@NonNull final String name, @NonNull final Set<SearchField> unexpandedSearchFields){
    return new SearchKey(name, unexpandedSearchFields);
  }

  public static SearchKey newSearchKey(@NonNull final String name, @NonNull final SearchField unexpandedSearchFields){
    return new SearchKey(name, newHashSet(unexpandedSearchFields));
  }

  public static SearchKey newSearchKey(@NonNull final String name, @NonNull final SearchField... unexpandedSearchFields){
    return new SearchKey(name, newHashSet(unexpandedSearchFields));
  }

  /**
   * Creates a map where keys are the child fieldnames, and the values are newly created searchfields,
   * that contain the absolute fieldname (ie parent_fieldname.child_fieldname)
   */
  private static Map<String, SearchField> buildSearchFieldMap(String name, Set<SearchField> unexpandedSearchFields){
    val builder = ImmutableMap.<String, SearchField>builder();
    for (val unexpandedSearchField : unexpandedSearchFields){
      val boostedValue = unexpandedSearchField.getBoostedValue();

      val unexpandedSearchFieldName = unexpandedSearchField.getName();
      val expandedSearchFieldName = DEFAULT_JOINER.join(name, unexpandedSearchFieldName);

      val expandedSearchField = newBoostedSearchField(expandedSearchFieldName, boostedValue);
      builder.put(unexpandedSearchFieldName,  expandedSearchField);
    }
    return builder.build();
  }

  private SearchKey(@NonNull final String name, @NonNull final Set<SearchField> unexpandedSearchFields){
    this.name = name;
    this.map = buildSearchFieldMap(name, unexpandedSearchFields);
  }

  public Set<SearchField> getExpandedSearchFields(){
    return map.values().stream().collect(toImmutableSet());
  }

  public SearchField getExpandedSearchField(final String unexpandedSearchFieldName){
    checkArgument(map.containsKey(unexpandedSearchFieldName));
    return map.get(unexpandedSearchFieldName);
  }

  public SearchField getExpandedSearchField(@NonNull final SearchField unexpandedSearchField){
    return getExpandedSearchField(unexpandedSearchField.getName());
  }

  @Override
  public Iterator<SearchField> iterator() {
    return map.values().iterator();
  }

}
