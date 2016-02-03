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

package org.icgc.dcc.portal.repository;

import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.model.IndexModel.getFields;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;

import java.util.Map;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ReleaseRepository {

  private static final Type TYPE = Type.RELEASE;
  private static final Kind KIND = Kind.RELEASE;

  private final Client client;

  private final String index;

  @Autowired
  ReleaseRepository(Client client, IndexModel indexModel) {
    this.index = indexModel.getIndex();
    this.client = client;
  }

  public SearchResponse findAll(Query query) {
    val search = client
        .prepareSearch(index)
        .setTypes(TYPE.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(query.getFrom())
        .setSize(query.getSize())
        .addSort(FIELDS_MAPPING.get(KIND).get(query.getSort()), query.getOrder());

    search.addFields(getFields(query, KIND));

    log.debug("{}", search);
    SearchResponse response = search.execute().actionGet();
    log.debug("{}", response);

    return response;
  }

  public long count(Query query) {
    SearchRequestBuilder search = client.prepareSearch(index).setTypes(TYPE.getId()).setSearchType(COUNT);

    log.debug("{}", search);
    return search.execute().actionGet().getHits().getTotalHits();
  }

  public Map<String, Object> findOne(String id, Query query) {
    val search = client.prepareGet(index, TYPE.getId(), id);
    search.setFields(getFields(query, KIND));

    val response = search.execute().actionGet();
    checkResponseState(id, response, KIND);

    val map = createResponseMap(response, query, KIND);
    log.debug("{}", map);

    return map;
  }
}
