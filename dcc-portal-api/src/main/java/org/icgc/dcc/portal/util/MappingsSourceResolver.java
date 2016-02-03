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
package org.icgc.dcc.portal.util;

import static org.icgc.dcc.portal.util.JsonUtils.MAPPER;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class MappingsSourceResolver {

  /**
   * Dependencies.
   */
  @NonNull
  private final Client client;

  public Map<String, ObjectNode> resolve(@NonNull String indexName) throws IOException {
    val mappings = ImmutableMap.<String, ObjectNode> builder();
    for (val entry : findMappings(indexName).entrySet()) {
      val type = entry.getKey();
      val metadata = entry.getValue();
      val mapping = convertMapping(type, metadata);

      mappings.put(type, mapping);
    }

    return mappings.build();
  }

  private Map<String, MappingMetaData> findMappings(String indexName) {
    // Get cluster state
    val clusterState = client.admin().cluster()
        .prepareState()
        .setIndices(indexName)
        .execute()
        .actionGet()
        .getState();

    val indexMetaData = clusterState.getMetaData().index(indexName);
    val mappings = indexMetaData.getMappings();

    return convertToMap(mappings);
  }

  private Map<String, MappingMetaData> convertToMap(ImmutableOpenMap<String, MappingMetaData> sourceMap) {
    val result = new ImmutableMap.Builder<String, MappingMetaData>();
    for (val entry : sourceMap) {
      result.put(entry.key, entry.value);
    }

    return result.build();
  }

  private ObjectNode convertMapping(String type, MappingMetaData metadata) throws IOException {
    return (ObjectNode) MAPPER.readTree(metadata.source().toString()).get(type);
  }

}
