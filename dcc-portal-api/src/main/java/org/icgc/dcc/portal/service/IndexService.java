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
package org.icgc.dcc.portal.service;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.HOURS;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class IndexService {

  /**
   * Constants.
   */
  private static final int CACHE_TTL_HOURS = 1;

  /**
   * Dependencies.
   */
  private final ConcurrentMap<Object, Object> cache;

  public IndexService() {
    this.cache = createStore();
  }

  private ConcurrentMap<Object, Object> createStore() {
    return CacheBuilder
        .newBuilder()
        .expireAfterWrite(CACHE_TTL_HOURS, HOURS)
        .maximumSize(100)
        .build()
        .asMap();
  }

  public void clearCache() {
    log.info("Clearing index name cache.");
    this.cache.clear();
  }

  @SneakyThrows
  public Map<String, String> getIndexMetaData(Client client, String indexName) {

    IndexMetaData indexMetaData = getIndexMappings(client, indexName);
    if (indexMetaData == null) {
      clearCache();
      indexMetaData = getIndexMappings(client, indexName);
    }

    val mappings = indexMetaData.mappings();
    log.info("Size of index meta data mappings: {}", mappings.values().size());
    val mappingIterator = mappings.values().iterator();
    val mappingMetaData = mappingIterator.next().value;
    val source = mappingMetaData.sourceAsMap();

    @SuppressWarnings("unchecked")
    val meta = (Map<String, String>) source.get("_meta");

    return (meta == null) ? emptyMap() : meta;
  }

  private IndexMetaData getIndexMappings(Client client, String indexName) {
    val state = client.admin().cluster().prepareState().setIndices(indexName).execute().actionGet().getState();

    String realIndex;
    if (cache.containsKey(indexName)) {
      realIndex = (String) cache.get(indexName);
      log.info("Cache hit for index name: '{}' with value: '{}'", indexName, realIndex);
    } else {
      log.info("Cache miss for index name: {}", indexName);
      val aliases = state.getMetaData().getAliases().get(indexName);

      if (aliases != null) {
        realIndex = aliases.iterator().next().key;
      } else {
        realIndex = indexName;
      }
      cache.put(indexName, realIndex);
    }

    val stateMetaData = state.getMetaData();
    return stateMetaData.index(realIndex);
  }

}
