package org.icgc.dcc.portal.server.model.fields;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents a field in elasticsearch mapping with a boost value.
 */
public class SearchField {

  /**
   * These field names are defined and used in our elasticsearch index models.
   */
  public static final String EXACT_MATCH_FIELDNAME = "raw";
  public static final String LOWERCASE_MATCH_FIELDNAME= "search";
  public static final String PARTIAL_MATCH_FIELDNAME= "analyzed";

  private static final int DEFAULT_BOOST_VALUE = 1;

  /**
   * Factory methods
   */
  public static SearchField newNoneBoostedSearchField(@NonNull String name){
    return new SearchField(name);
  }

  public static SearchField newBoostedSearchField(@NonNull String name, @NonNull float boostedValue){
    return new SearchField(name, boostedValue);
  }

  public static void main(String[] args){
    SearchField f10 = newBoostedSearchField("hey", 1.0f);
    System.out.println("f10 boosted: "+f10.isBoosted());
    SearchField f13 = newBoostedSearchField("hey", 1.3f);
    System.out.println("f13 boosted: "+f13.isBoosted());
  }

  @NonNull
  @Getter
  private final String name;

  private final float boostedValue;

  private SearchField(final String name, final float boostedValue){
    this.name = name;
    this.boostedValue = boostedValue;
  }

  private SearchField(final String name){
    this(name, DEFAULT_BOOST_VALUE);
  }

  public boolean isBoosted(){
    return boostedValue != (float)DEFAULT_BOOST_VALUE; //Explicitly widening conversion from int to float, even though automatically done
  }

  public float getBoostValue(){
    return boostedValue;
  }



}
