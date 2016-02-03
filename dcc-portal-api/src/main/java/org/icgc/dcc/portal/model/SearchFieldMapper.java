/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.model;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Maps.uniqueIndex;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.common.core.util.stream.Streams.stream;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.val;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * This represents a collection of raw field names grouped by the type of search analyzer (or lack of) defined by our
 * elasticsearch data model/mappings.
 */
@Value
@Builder
public class SearchFieldMapper {

  /**
   * These suffixes are defined and used in our elasticsearch index models.
   */
  public static final String EXACT_MATCH_SUFFIX = ".raw";
  public static final String PARTIAL_MATCH_SUFFIX = ".analyzed";
  public static final String LOWERCASE_MATCH_SUFFIX = ".search";

  Set<String> exactMatchFields;
  Set<String> partialMatchFields;
  Set<String> lowercaseMatchFields;

  public static SearchFieldMapperBuilder searchFieldMapper() {
    return builder();
  }

  public Iterable<String> map(@NonNull Iterable<String> fields) {
    return stream(fields)
        .flatMap(field -> getSearchSuffixes(field).stream()
            .map((String suffix) -> field + suffix))
        .collect(toImmutableSet());
  }

  public Iterable<String> map() {
    val fields = concat(ensureSet(exactMatchFields), ensureSet(partialMatchFields), ensureSet(lowercaseMatchFields));
    return map(fields);
  }

  @NonNull
  public static String boost(String suffix, int boost) {
    return format("%s^%d", suffix, boost);
  }

  public Map<String, String> toMap() {
    return ImmutableMap.<String, String> builder()
        .putAll(toMap(exactMatchFields, EXACT_MATCH_SUFFIX))
        .putAll(toMap(partialMatchFields, PARTIAL_MATCH_SUFFIX))
        .putAll(toMap(lowercaseMatchFields, LOWERCASE_MATCH_SUFFIX))
        .build();
  }

  private static Map<String, String> toMap(Iterable<String> matchFields, String suffix) {
    return (null == matchFields) ? emptyMap() : uniqueIndex(matchFields, field -> field + suffix);
  }

  private static Set<String> ensureSet(Set<String> set) {
    return (null == set) ? emptySet() : set;
  }

  private static boolean contains(Set<String> matchFields, String fieldName) {
    return matchFields != null && matchFields.contains(fieldName);
  }

  private List<String> getSearchSuffixes(String fieldName) {
    if (null == fieldName) {
      return emptyList();
    }

    val result = ImmutableList.<String> builder();

    if (contains(exactMatchFields, fieldName)) {
      result.add(EXACT_MATCH_SUFFIX);
    }

    if (contains(partialMatchFields, fieldName)) {
      result.add(PARTIAL_MATCH_SUFFIX);
    }

    if (contains(lowercaseMatchFields, fieldName)) {
      result.add(LOWERCASE_MATCH_SUFFIX);
    }

    return result.build();
  }

}
