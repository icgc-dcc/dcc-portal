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
package org.icgc.dcc.portal.server.repository;

import static org.icgc.dcc.portal.server.model.IndexModel.getFields;
import static org.icgc.dcc.portal.server.util.ElasticsearchRequestUtils.EMPTY_SOURCE_FIELDS;
import static org.icgc.dcc.portal.server.util.ElasticsearchRequestUtils.resolveSourceFields;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.createResponseMap;

import java.util.Map;

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
public class PathwayRepository {

  private final Client client;
  private final String indexName;

  @Autowired
  PathwayRepository(Client client, @Value("#{indexName}") String indexName) {
    this.indexName = indexName;
    this.client = client;
  }

  public Map<String, Object> findOne(String id, Query query) {
    val search = client.prepareGet(indexName, IndexType.PATHWAY.getId(), id);
    search.setFields(getFields(query, EntityType.PATHWAY));
    String[] sourceFields = resolveSourceFields(query, EntityType.PATHWAY);
    if (sourceFields != EMPTY_SOURCE_FIELDS) {
      search.setFetchSource(resolveSourceFields(query, EntityType.PATHWAY), EMPTY_SOURCE_FIELDS);
    }

    val response = search.execute().actionGet();
    checkResponseState(id, response, EntityType.PATHWAY);

    val map = createResponseMap(response, query, EntityType.PATHWAY);
    log.debug("{}", map);

    return map;
  }
}
