/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.server.util;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.index.get.GetField;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.Query;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;

import lombok.NoArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides methods to retrieve values from SearchHit
 */
@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class ElasticsearchResponseUtils {

  /**
   * Returns first value of the list as a String
   */
  public static String getString(Object values) {
    if (values == null) {
      return null;
    }

    if (values instanceof Iterable<?>) {
      val iterable = (Iterable<?>) values;
      return Iterables.isEmpty(iterable) ? null : Iterables.get(iterable, 0).toString();
    }

    if (values instanceof String) {
      return values.toString();
    }

    return null;
  }

  public static List<String> toStringList(Object value) {
    if (null == value) {
      return null;
    }

    if (value instanceof List<?>) {
      // There might be a performance issue here. In the event that we already know 'value' is a List<String>, maybe we
      // should just cast it to List<String> directly. Alternatively, we could check the size of the list then,
      // depending on the size, either directly cast it for a big collection or transform it for a small collection.
      return newArrayList(transform((List<?>) value, v -> v.toString()));
    }

    if (value instanceof Iterable<?>) {
      return newArrayList(transform((Iterable<?>) value, v -> v.toString()));
    }

    return newArrayList(value.toString());
  }

  public static Long getLong(Object value) {
    if (null == value) {
      return null;
    }

    if (value instanceof Long) {
      return (Long) value;
    }

    if (value instanceof Float) {
      return ((Float) value).longValue();
    }

    if (value instanceof Integer) {
      return Long.valueOf((Integer) value);
    }

    if (value instanceof Iterable<?>) {
      val iterable = (Iterable<?>) value;
      return Iterables.isEmpty(iterable) ? null : Longs.tryParse(Iterables.get(iterable, 0).toString());
    }

    return Longs.tryParse(value.toString());
  }

  public static Boolean getBoolean(Object values, Boolean defaultValue) {
    if (values == null) {
      return defaultValue;
    }

    if (values instanceof Boolean) {
      return (Boolean) values;
    }

    if (values instanceof Iterable<?>) {
      val iterable = (Iterable<?>) values;
      if (Iterables.isEmpty(iterable)) {
        return defaultValue;
      } else {
        return Boolean.TRUE.equals(Iterables.get(iterable, 0));
      }
    }

    return defaultValue;
  }

  private static void processConsequences(Map<String, Object> map, Collection<String> includes) {
    if (includes != null && includes.contains("consequences")) {
      log.debug("Copying transcripts to consequences...");
      map.put("consequences", map.get("transcript"));
      if (includes == null || !includes.contains("transcripts")) {
        log.debug("Removing transcripts...");
        map.remove("transcript");
      }
    }
  }

  public static Map<String, Object> createMapFromSearchFields(Map<String, SearchHitField> fields) {
    val result = Maps.<String, Object> newHashMap();
    for (val field : fields.entrySet()) {
      result.put(field.getKey(), field.getValue().getValues());
    }

    return result;
  }

  public static Map<String, Object> createMapFromGetFields(Map<String, GetField> fields) {
    val result = Maps.<String, Object> newHashMap();
    for (val field : fields.entrySet()) {
      result.put(field.getKey(), field.getValue().getValues());
    }

    return result;
  }

  public static Map<String, Object> createResponseMap(GetResponse response, Query query, EntityType entityType) {
    val map = processSource(response.getSource(), query.getIncludes(), entityType);
    return map;
  }

  public static Map<String, Object> createResponseMap(SearchHit response, Query query, EntityType entityType) {
    return createResponseMap(response, query.getIncludes(), entityType);
  }

  public static Map<String, Object> createResponseMap(SearchHit response,
      Collection<String> includes, EntityType entityType) {
      val map = createMapFromSearchFields(response.getFields());
      map.putAll(processSource(response.getSource(),includes, entityType));

      return map;
  }

  public static void checkResponseState(String id, GetResponse response, EntityType entityType) {
    if (!response.isExists()) {
      val type = entityType.getId().substring(0, 1).toUpperCase() + entityType.getId().substring(1);
      log.info("{} {} not found.", type, id);

      val message = format("{\"code\": 404, \"message\":\"%s %s not found.\"}", type, id);
      throw new WebApplicationException(status(NOT_FOUND).entity(message).build());
    }
  }

  public static GetResponse sanityCheck(GetResponse response, String indexType, String id) {
    if (response.isExists()) {
      return response;
    }

    log.info("ID {} was NOT found in {} index type.", id, indexType);

    val message = format("{\"code\": 404, \"message\":\"ID %s was NOT found.\"}", id);
    throw new WebApplicationException(status(NOT_FOUND).entity(message).build());
  }

  private static Map<String, Object> processSource(Map<String, Object> source,
      Collection<String> includes, EntityType entityType) {
    if (source == null) {
      return emptyMap();
    }

    val result = flattenMap(source, includes, entityType);
    processConsequences(result, includes);

    return result;
  }

  public static Map<String, Object> flattenMap(Map<String, Object> source) {
    if (source == null) {
      return emptyMap();
    }

    return flattenMap(Optional.empty(), source, null, null);
  }

  public static Map<String, Object> flattenMap(Map<String, Object> source,
      Collection<String> includes, EntityType entityType) {
    if (source == null) {
      return emptyMap();
    }

    return flattenMap(Optional.empty(), source, entityType, includes);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> flattenMap(Optional<String> prefix, Map<String, Object> source,
      EntityType entityType,
      Collection<String> includes) {
    val result = Maps.<String, Object> newHashMap();

    for (val entry : source.entrySet()) {
      val fieldName = resolvePrefix(prefix, entry.getKey());
      if (entry.getValue() instanceof Map && !isSkip(fieldName, includes, entityType)) {
        result.putAll(
            flattenMap(Optional.of(entry.getKey()), (Map<String, Object>) entry.getValue(), entityType, includes));
      } else {
        result.put(fieldName, entry.getValue());
      }
    }

    return result;
  }

  /**
   * Some fields are maps and the client expects them to be a map.
   * 
   * This method returns true if fieldName refers to a map that should not be flattened.
   */
  private static boolean isSkip(String fieldName, Collection<String> includes,
      EntityType entityType) {
    if (entityType == null || includes == null) {
      return false;
    }

    return (FIELDS_MAPPING.get(entityType).containsValue(fieldName) || includes.contains(fieldName));
  }

  private static String resolvePrefix(Optional<String> prefix, String field) {
    return prefix.isPresent() ? format("%s.%s", prefix.get(), field) : field;
  }
}
