package org.icgc.dcc.portal.server.model.fields;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.icgc.dcc.common.core.util.Joiners;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 * Represents searchable fields, which contain AnalyzedSubFields.
 * The following is an example Elasticsearch mapping that demonstrates how SearchableField instances map with Elasticsearch terminology.
 *
 * "mapping" : {
 *    "myType" : {
 *      "properties" : {
 *        "mySearchableField" : {
 *          "type" : "keyword",
 *          "index" : false,
 *          "fields" : {
 *            "myAnalyzedSubField1" : {
 *              "type" : "text",
 *              "analyzer" : "id_index"
 *              "search_analyzer" : "id_search"
 *            },
 *            "myAnalyzedSubField2" : {
 *              "type" : "text",
 *              "analyzer" : "id_index"
 *              "search_analyzer" : "id_search"
 *            },
 *            "myAnalyzedSubField3" : {
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
@Getter
public class SearchableField {

  private static final Joiner DEFAULT_JOINER = Joiners.DOT;

  private final String name;

  private final Map<String, Float> map;

  public static SearchableField newSearchableField(@NonNull final String name, @NonNull final Set<AnalyzedSubField> fields){
    return new SearchableField(name, fields);
  }

  public static SearchableField newSearchableField(@NonNull final String name, @NonNull final AnalyzedSubField field){
    return new SearchableField(name, field);
  }

  public static SearchableField newSearchableField(@NonNull final String name, @NonNull final AnalyzedSubField... fields){
    return new SearchableField(name, newHashSet(fields));
  }

  private SearchableField(@NonNull final String name, @NonNull final Set<AnalyzedSubField> fields){
    this.name = name;
    val builder = ImmutableMap.<String, Float>builder();
    for (val field : fields){
      val fieldName = DEFAULT_JOINER.join(name, field.getName());
      builder.put(fieldName, field.getBoostValue());
    }
    this.map = builder.build();
  }

  private SearchableField(@NonNull final String name, @NonNull final AnalyzedSubField field){
    this.name = name;
    val builder = ImmutableMap.<String, Float>builder();
    val fieldName = DEFAULT_JOINER.join(name, field.getName());
    builder.put(fieldName, field.getBoostValue());
    this.map = builder.build();
  }

}
