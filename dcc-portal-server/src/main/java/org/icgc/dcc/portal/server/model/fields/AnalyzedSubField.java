package org.icgc.dcc.portal.server.model.fields;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents analyzed subfields for searchable fields, such as raw, analyzed, search, ..., etc.
 * The following is an example Elasticsearch mapping that demonstrates how AnalyzedSubField instances map with Elasticsearch terminology.
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
public class AnalyzedSubField {
  private static final int DEFAULT_BOOST_VALUE = 1;

  public static AnalyzedSubField newNoneBoostedSubField(@NonNull String name){
    return new AnalyzedSubField(name);
  }

  public static AnalyzedSubField newBoostedSubField(@NonNull String name, @NonNull int boostedValue){
    return new AnalyzedSubField(name, boostedValue);
  }

  @NonNull
  @Getter
  private final String name;

  private final int boostedValue;

  private AnalyzedSubField(final String name, final int boostedValue){
    this.name = name;
    this.boostedValue = boostedValue;
  }

  private AnalyzedSubField(final String name){
    this(name, DEFAULT_BOOST_VALUE);
  }

  public boolean isBoosted(){
    return boostedValue != DEFAULT_BOOST_VALUE;
  }

  public float getBoostValue(){
    return (float)boostedValue;
  }

}
