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

import static org.dcc.portal.pql.meta.Type.DIAGRAM;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.createResponseMap;

import java.util.Map;

import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.pql.convert.Jql2PqlConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DiagramRepository {

  private static final Jql2PqlConverter CONVERTER = Jql2PqlConverter.getInstance();

  /**
   * Dependencies.
   */
  private final Client client;
  private final String indexName;
  private final QueryEngine queryEngine;

  @Autowired
  public DiagramRepository(@NonNull Client client, @Value("#{indexName}") String indexName,
      @NonNull QueryEngine queryEngine) {
    this.indexName = indexName;
    this.client = client;
    this.queryEngine = queryEngine;
  }

  public SearchResponse findAll(Query query) {
    val pql = CONVERTER.convert(query, DIAGRAM);
    log.info("pql of findAll is: {}", pql);

    val request = queryEngine.execute(pql, DIAGRAM);
    val response = request.getRequestBuilder().execute().actionGet();

    return response;
  }

  public Map<String, Object> findOne(@NonNull String id, @NonNull Query query) {
    val search = client.prepareGet(indexName, IndexType.DIAGRAM.getId(), id);

    val response = search.execute().actionGet();
    checkResponseState(id, response, EntityType.DIAGRAM);

    val map = createResponseMap(response, query, EntityType.DIAGRAM);
    log.debug("{}", map);

    return map;
  }

}
