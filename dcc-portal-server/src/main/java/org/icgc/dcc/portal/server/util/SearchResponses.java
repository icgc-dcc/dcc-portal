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

import static lombok.AccessLevel.PRIVATE;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.icgc.dcc.portal.server.model.Query;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

/**
 * Elasticsearch {@link SearchResponses} utilities.
 */
@NoArgsConstructor(access = PRIVATE)
public final class SearchResponses {

  public static List<String> getHitIds(@NonNull SearchResponse response) {
    val ids = Lists.<String> newArrayList();
    for (val hit : response.getHits()) {
      ids.add(hit.getId());
    }

    return ids;
  }

  /**
   * Get ids from hits as a set. Useful for when queries can span across types and indices resulting in id collision and
   * enforcing id uniqueness is desirable.
   * 
   * @param response SearchResponse from elasticsearch
   * @return ids in a HashSet
   */
  public static Set<String> getHitIdsSet(@NonNull SearchResponse response) {
    val ids = Sets.<String> newHashSet();
    for (val hit : response.getHits()) {
      ids.add(hit.getId());
    }

    return ids;
  }

  public static long getTotalHitCount(@NonNull SearchResponse response) {
    return response.getHits().totalHits();
  }

  public static boolean hasHits(@NonNull SearchResponse response) {
    return response.getHits().hits().length > 0;
  }

  public static LinkedHashMap<String, Long> getCounts(LinkedHashMap<String, Query> queries,
      MultiSearchResponse sr) {
    val counts = Maps.<String, Long> newLinkedHashMap();
    val ids = queries.keySet().iterator();
    for (val item : sr.getResponses()) {
      SearchResponse r = item.getResponse();
      counts.put(ids.next(), r.getHits().getTotalHits());
    }

    return counts;
  }

  public static LinkedHashMap<String, LinkedHashMap<String, Long>> getNestedCounts(
      LinkedHashMap<String, LinkedHashMap<String, Query>> queries,
      MultiSearchResponse sr) {
    val counts = Maps.<String, LinkedHashMap<String, Long>> newLinkedHashMap();

    val entrySet = queries.entrySet();
    val idSet = entrySet.stream().map(Entry::getKey);
    val subIdSet = entrySet.iterator().next().getValue().keySet();

    val ids = idSet.iterator();
    Iterator<String> subIds = subIdSet.iterator();
    LinkedHashMap<String, Long> subCounts = Maps.<String, Long> newLinkedHashMap();

    for (val item : sr.getResponses()) {
      SearchResponse r = item.getResponse();
      if (!subIds.hasNext()) {
        counts.put(ids.next(), subCounts);
        subIds = subIdSet.iterator();
        subCounts = Maps.<String, Long> newLinkedHashMap();
      }
      subCounts.put(subIds.next(), r.getHits().getTotalHits());
    }

    // catch last set of subCounts
    counts.put(ids.next(), subCounts);

    return counts;
  }

}
