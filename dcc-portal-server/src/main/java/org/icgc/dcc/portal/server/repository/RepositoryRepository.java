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

package org.icgc.dcc.portal.server.repository;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.checkResponseState;

import java.util.List;
import java.util.stream.Stream;

import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.Repository;
import org.icgc.dcc.portal.server.model.IndexType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RepositoryRepository {

  private static final ObjectMapper MAPPER = DEFAULT.disable(FAIL_ON_UNKNOWN_PROPERTIES); // Just in case we add more
                                                                                          // fields upstream

  /**
   * Dependencies.
   */
  private final Client client;
  private final String index;

  @Autowired
  RepositoryRepository(@NonNull Client client, @Value("#{repoIndexName}") String repoIndexName) {
    this.index = repoIndexName;
    this.client = client;
  }

  @Cacheable("repository")
  public Repository findOne(String id) {
    val search = client.prepareGet(index, IndexType.REPOSITORY.getId(), id);

    log.debug("{}", search);
    val response = search.execute().actionGet();
    checkResponseState(id, response, EntityType.REPOSITORY);

    return convertSource(response.getSourceAsString());
  }

  @Cacheable("repositories")
  public List<Repository> findAll() {
    val search = client.prepareSearch(index).setTypes(IndexType.REPOSITORY.getId()).setSize(100).setSearchType(QUERY_THEN_FETCH);

    log.debug("{}", search);
    val response = search.execute().actionGet();
    log.debug("{}", response);

    return convertHits(response.getHits().getHits());
  }

  private static List<Repository> convertHits(SearchHit[] hits) {
    return Stream.of(hits).map(hit -> convertHit(hit)).collect(toImmutableList());
  }

  private static Repository convertHit(SearchHit hit) {
    return convertSource(hit.getSourceAsString());
  }

  @SneakyThrows
  private static Repository convertSource(String source) {
    return MAPPER.readValue(source, Repository.class);
  }

}
