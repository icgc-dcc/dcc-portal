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
package org.dcc.portal.pql.es.visitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.query.EsRequestBuilder;
import org.dcc.portal.pql.query.QueryContext;
import org.dcc.portal.pql.utils.BaseElasticsearchTest;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Before;
import org.junit.Test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DonorScoreQueryTest extends BaseElasticsearchTest {

  private EsRequestBuilder visitor;

  @Before
  public void setUp() {
    es.execute(createIndexMappings(DONOR_CENTRIC).withData(bulkFile(getClass())));
    visitor = new EsRequestBuilder(es.client());
    queryContext = new QueryContext(INDEX_NAME, DONOR_CENTRIC);
  }

  @Test
  public void scoredQueryTest() {
    val result = executeQuery("select(ageAtDiagnosis)");
    val hits = result.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(9);
    assertThat(hits.getMaxScore()).isEqualTo(3f);

    // 3.0 - 1 hit 1.0 - 5 hits 0.0 - 3 hits
    val topHit = hits.getAt(0);
    assertThat(topHit.getId()).isEqualTo("DO2");
    assertThat(topHit.getScore()).isEqualTo(3f);

    val lastOneScoreHit = hits.getAt(5);
    assertThat(lastOneScoreHit.getScore()).isEqualTo(1f);

    val firstZeroScoreHit = hits.getAt(6);
    assertThat(firstZeroScoreHit.getScore()).isEqualTo(0f);
  }

  private SearchResponse executeQuery(String query) {
    ExpressionNode esAst = createTree(query);
    esAst = esAstTransformator.process(esAst, queryContext);
    log.debug("ES AST: {}", esAst);
    val request = visitor.buildSearchRequest(esAst, queryContext);
    log.debug("Request - {}", request);
    val result = request.execute().actionGet();
    log.debug("Result - {}", result);

    return result;
  }

}
