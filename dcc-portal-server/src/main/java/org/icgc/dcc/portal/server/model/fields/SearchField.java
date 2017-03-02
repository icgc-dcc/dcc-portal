package org.icgc.dcc.portal.server.model.fields;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

/**
 * Represents a field in elasticsearch mapping with a boost value.
 */
@Value
@RequiredArgsConstructor(access=PRIVATE)
public class SearchField {

  private static final int DEFAULT_BOOST_VALUE = 1;

  /**
   * These field names are defined and used in our elasticsearch index models.
   */
  public static final String EXACT_MATCH_FIELDNAME = "raw";
  public static final String LOWERCASE_MATCH_FIELDNAME= "search";
  public static final String PARTIAL_MATCH_FIELDNAME= "analyzed";

  @NonNull
  private final String name;

  private final float boostedValue;

  /**
   * Factory methods
   */
  public static SearchField newNoneBoostedSearchField(final String name){
    return newBoostedSearchField(name, DEFAULT_BOOST_VALUE);
  }

  public static SearchField newBoostedSearchField(final String name, final float boostedValue){
    return new SearchField(name, boostedValue);
  }

  public boolean isBoosted(){
    //Explicitly indicating widening conversion from int to float, so that its obvious
    return boostedValue != (float)DEFAULT_BOOST_VALUE;
  }

}