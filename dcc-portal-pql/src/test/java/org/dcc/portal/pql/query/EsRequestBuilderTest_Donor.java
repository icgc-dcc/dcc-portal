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
package org.dcc.portal.pql.query;

import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;

import org.dcc.portal.pql.utils.BaseElasticsearchTest;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Before;
import org.junit.Test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EsRequestBuilderTest_Donor extends BaseElasticsearchTest {

  QueryEngine queryEngine;

  @Before
  public void setUp() {
    es.execute(createIndexMappings(DONOR_CENTRIC).withData(bulkFile(getClass())));
    queryContext = new QueryContext(INDEX_NAME, DONOR_CENTRIC);
    queryEngine = new QueryEngine(es.client(), INDEX_NAME);
  }

  @Test
  public void geneLocationTest() {
    val result = executeQuery("in(gene.location,'chr20:31446730-31549006')");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "DO9");
  }

  @Test
  public void mutationLocationTest() {
    val result = executeQuery("in(mutation.location,'chrY:13463924-13463924')");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "DO2");
  }

  @Test
  public void nonNestedTest() {
    val result = executeQuery("select(id),facets(gender),eq(gene.chromosome, '2')");
    containsOnlyIds(result, "DO2", "DO4", "DO8");
  }

  private SearchResponse executeQuery(String query) {
    val request = queryEngine.execute(query, DONOR_CENTRIC);
    log.debug("Request - {}", request);
    val result = request.getRequestBuilder().execute().actionGet();
    log.debug("Result - {}", result);

    return result;
  }

}
