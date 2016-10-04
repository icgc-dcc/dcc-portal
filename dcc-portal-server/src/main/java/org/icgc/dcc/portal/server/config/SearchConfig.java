/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.server.config;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.icgc.dcc.portal.server.config.ServerProperties.ElasticSearchProperties.SNIFF_MODE_KEY;
import static org.icgc.dcc.portal.server.util.VersionUtils.getApiVersion;
import static org.icgc.dcc.portal.server.util.VersionUtils.getApplicationVersion;
import static org.icgc.dcc.portal.server.util.VersionUtils.getCommitId;

import java.net.InetAddress;
import java.util.Map;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.Index;
import org.elasticsearch.indices.IndexMissingException;
import org.icgc.dcc.portal.server.config.ServerProperties.ElasticSearchProperties;
import org.icgc.dcc.portal.server.model.Versions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Lazy
@Configuration
public class SearchConfig {

  /**
   * Dependencies
   */
  @Autowired
  private ElasticSearchProperties elastic;

  @SneakyThrows
  @Bean(destroyMethod = "close")
  public Client client() {
    // TransportClient is thread-safe
    val client = createTransportClient(elastic.getClient());
    for (val nodeAddress : elastic.getNodeAddresses()) {
      log.info("Configuring ES node address: {}", nodeAddress);
      checkState(InetAddress.getByName(nodeAddress.getHost()).isReachable((int) SECONDS.toMillis(5)));

      client.addTransportAddress(new InetSocketTransportAddress(
          nodeAddress.getHost(),
          nodeAddress.getPort()));
    }

    return client;
  }

  @Bean
  public String indexName() {
    String indexName = elastic.getIndexName();
    return resolveIndexName(indexName);
  }

  @Bean
  public String repoIndexName() {
    return elastic.getRepoIndexName();
  }

  @Bean
  public Map<String, String> releaseIndexMetadata() {
    String indexStr = resolveIndexName(elastic.getIndexName());
    return indexMetadata(indexStr);
  }

  @Bean
  public Versions versions() {
    return new Versions(
        getApiVersion(),
        getApplicationVersion(),
        getCommitId(),
        firstNonNull(releaseIndexMetadata().get("git.commit.id.abbrev"), "unknown"),
        indexName());
  }

  private String resolveIndexName(String indexName) {
    // Get cluster state
    ClusterState clusterState = client().admin().cluster()
        .prepareState()
        .execute()
        .actionGet()
        .getState();

    val aliases = clusterState.getMetaData().aliases().get(indexName);
    if (aliases != null) {
      val indexNames = newHashSet(aliases.keys().toArray(String.class));
      if (indexNames.size() != 1) throw new ElasticsearchException(
          String.format("Expected alias to point to a single index but instead it points to '%s'", indexNames));

      val realIndexName = getFirst(indexNames, null);
      log.warn("Redirecting configured index alias of '{}' with real index name '{}'", indexName, realIndexName);
      indexName = realIndexName;
    }

    return indexName;
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private Map<String, String> indexMetadata(String indexName) {
    // Get cluster state
    val clusterState = client().admin().cluster()
        .prepareState()
        .setIndices(indexName)
        .execute()
        .actionGet()
        .getState();

    val indexMetaData = clusterState.getMetaData().index(indexName);
    if (indexMetaData == null) throw new IndexMissingException(new Index(indexName));

    // Get the first mappings. This is arbitrary since they all contain the same metadata
    MappingMetaData mappingMetaData = indexMetaData.getMappings().values().iterator().next().value;

    Map<String, Object> source = mappingMetaData.sourceAsMap();
    Map<String, String> meta = (Map<String, String>) source.get("_meta");
    if (meta == null) {
      meta = newHashMap();
    }

    return meta;
  }

  private static TransportClient createTransportClient(Map<String, String> clientSettings) {
    logClientSettings(clientSettings);
    val settingsBuilder = ImmutableSettings.settingsBuilder();
    if (!isSniffModeSet(clientSettings)) {
      settingsBuilder.put(SNIFF_MODE_KEY, true);
    }

    clientSettings.entrySet().stream()
        .forEach(s -> settingsBuilder.put(s.getKey(), s.getValue()));

    return new TransportClient(settingsBuilder.build());
  }

  private static boolean isSniffModeSet(Map<String, String> clientSettings) {
    return clientSettings.entrySet().stream()
        .anyMatch(e -> e.getKey().equals(SNIFF_MODE_KEY));
  }

  private static void logClientSettings(Map<String, String> clientSettings) {
    val settings = newHashMap(clientSettings);
    if (!isSniffModeSet(clientSettings)) {
      settings.put(SNIFF_MODE_KEY, String.valueOf(true));
    }
    log.info("Initializing Elasticsearch Transport Client with settings: {}", settings);
  }

}