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
 * Represents a key term, which contain subfields. The class can generate SearchFields RELATIVE to the key name, using a collection of previously defined SearchFields. The purpose of this class is to partially model the Elasticsearch mapping below:
 *
 * "mapping" : {
 *    "myType" : {
 *      "properties" : {
 *        "mySearchableKey" : {
 *          "type" : "keyword",
 *          "index" : false,
 *          "fields" : {
 *            "mySearchableField1" : {
 *              "type" : "text",
 *              "analyzer" : "id_index"
 *              "search_analyzer" : "id_search"
 *            },
 *            "mySearchableField2" : {
 *              "type" : "text",
 *              "analyzer" : "id_index"
 *              "search_analyzer" : "id_search"
 *            },
 *            "mySearchableField3" : {
 *              "type" : "text",
 *              "analyzer" : "id_index"
 *              "search_analyzer" : "id_search"
 *            }
 *          }
 *        }
 *      }
 *    }
 *
 *
 * }
 */
public class SearchKey implements Iterable<SearchField>{

  private static final Joiner DEFAULT_JOINER = Joiners.DOT;

  /**
   * Factory Methods
   */
  public static SearchKey newSearchKey(@NonNull final String name, @NonNull final Set<SearchField> fields){
    return new SearchKey(name, fields);
  }

  public static SearchKey newSearchKey(@NonNull final String name, @NonNull final SearchField field){
    return new SearchKey(name, field);
  }

  public static SearchKey newSearchKey(@NonNull final String name, @NonNull final SearchField... fields){
    return new SearchKey(name, newHashSet(fields));
  }

  @Getter
  private final String name;

  /**
   * Map contains mapping of unique subFieldNames (ie "raw", "analyzed", "search", etc)
   * to absoluteFields which contain the key name (this.name)
   */
  private final Map<String, SearchField> map;


  private SearchKey(@NonNull final String name, @NonNull final Set<SearchField> fields){
    this.name = name;
    val builder = ImmutableMap.<String, SearchField>builder();
    for (val field : fields){
      val absoluteFieldName = DEFAULT_JOINER.join(name, field.getName());
      val boostedValue = field.getBoostValue();
      val absoluteField = newBoostedSearchField(absoluteFieldName, boostedValue);
      val subFieldName = field.getName();
      builder.put(subFieldName,  absoluteField);
    }
    this.map = builder.build();
  }

  // update this constructor then,
  // update comments,
  // implement iterator,
  // rename this class to SearchKey since a Key contains SearchField objects. When you create a key with SearchField instance, those instances become children of the parent key, and new SearchField instance are created, where the name changes to the absolute form (parent.child) and inherits the child boostValue
  private SearchKey(@NonNull final String name, @NonNull final SearchField field){
    this.name = name;
    val builder = ImmutableMap.<String, SearchField>builder();
    val absoluteFieldName = DEFAULT_JOINER.join(name, field.getName());
    val boostedValue = field.getBoostValue();
    val absoluteField = newBoostedSearchField(absoluteFieldName, boostedValue);
    val subFieldName = field.getName();
    builder.put(subFieldName, field);
    this.map = builder.build();
  }

  public Set<SearchField> getFields(){
    return map.values().stream().collect(toImmutableSet());
  }

  public SearchField getField(final String subFieldName){
    checkArgument(map.containsKey(subFieldName));
    return map.get(subFieldName);
  }

  public SearchField getField(@NonNull final SearchField field){
    return getField(field.getName());
  }

  @Override
  public Iterator<SearchField> iterator() {
    return map.values().iterator();
  }
}
