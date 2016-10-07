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
package org.icgc.dcc.portal.server.repository;

import static org.dcc.portal.pql.meta.Type.DRUG_CENTRIC;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.sanityCheck;

import java.util.function.Consumer;

import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Data access layer to the drug index type
 */
@Component
@Slf4j
public class DrugRepository {

  private static final String INDEX_TYPE = DRUG_CENTRIC.getId();

  /**
   * Dependencies.
   */
  private final Client client;
  private final String indexName;
  private final QueryEngine queryEngine;

  @Autowired
  public DrugRepository(Client client, QueryEngine queryEngine, @Value("#{indexName}") String indexName) {
    this.client = client;
    this.indexName = indexName;
    this.queryEngine = queryEngine;
  }

  @NonNull
  public SearchResponse findAll(StatementNode pqlAst) {
    val response = pqlSearch(pqlAst, "findAll", request -> {
    });

    log.debug("findAll() - ES response is: '{}'.", response);
    return response;
  }

  @NonNull
  public GetResponse findOne(String id) {
    val search = client.prepareGet(indexName, INDEX_TYPE, id);
    val response = search.execute().actionGet();

    return sanityCheck(response, INDEX_TYPE, id);
  }

  @NonNull
  private SearchResponse search(SearchRequestBuilder request, String logMessage,
      Consumer<SearchRequestBuilder> customizer) {
    customizer.accept(request);

    log.debug("{}; ES query is: '{}'", logMessage, request);
    return request.execute().actionGet();
  }

  @NonNull
  private SearchResponse pqlSearch(StatementNode pqlAst, String logMessage, Consumer<SearchRequestBuilder> customizer) {
    val request = queryEngine.execute(pqlAst, DRUG_CENTRIC).getRequestBuilder();
    return search(request, logMessage, customizer);
  }

}
