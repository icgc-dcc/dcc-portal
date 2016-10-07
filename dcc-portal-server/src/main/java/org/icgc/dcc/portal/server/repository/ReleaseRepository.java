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

import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.server.model.IndexModel.getFields;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.createResponseMap;

import java.util.Map;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.model.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ReleaseRepository {

  private final Client client;
  private final String indexName;

  @Autowired
  ReleaseRepository(Client client, @Value("#{indexName}") String indexName) {
    this.indexName = indexName;
    this.client = client;
  }

  public SearchResponse findAll(Query query) {
    val search = client
        .prepareSearch(indexName)
        .setTypes(IndexType.RELEASE.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(query.getFrom())
        .setSize(query.getSize())
        .addSort(FIELDS_MAPPING.get(EntityType.RELEASE).get(query.getSort()), query.getOrder());

    search.addFields(getFields(query, EntityType.RELEASE));

    log.debug("{}", search);
    SearchResponse response = search.execute().actionGet();
    log.debug("{}", response);

    return response;
  }

  public long count(Query query) {
    SearchRequestBuilder search =
        client.prepareSearch(indexName).setTypes(IndexType.RELEASE.getId()).setSearchType(COUNT);

    log.debug("{}", search);
    return search.execute().actionGet().getHits().getTotalHits();
  }

  public Map<String, Object> findOne(String id, Query query) {
    val search = client.prepareGet(indexName, IndexType.RELEASE.getId(), id);
    search.setFields(getFields(query, EntityType.RELEASE));

    val response = search.execute().actionGet();
    checkResponseState(id, response, EntityType.RELEASE);

    val map = createResponseMap(response, query, EntityType.RELEASE);
    log.debug("{}", map);

    return map;
  }
}
