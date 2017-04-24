/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static org.dcc.portal.pql.meta.Type.DRUG_CENTRIC;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.utils.BaseElasticsearchTest;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class EsRequestBuilderTest_Drug extends BaseElasticsearchTest {

  QueryEngine queryEngine;

  @Before
  public void setUpEsRequestBuilderTestDrug() throws Exception {
    createIndexMappings(DRUG_CENTRIC);
    createTermsLookupType();
    loadData(getClass());
    queryContext = new QueryContext(INDEX_NAME, DRUG_CENTRIC);
    queryEngine = new QueryEngine(client, INDEX_NAME);
  }

  @Test
  public void lookupTest() {
    val result = executeQuery("or(eq(drug.id, 'ZINC123'),"
        + "in(gene.id, 'ENS123','ES:6d66b2bd-daed-431e-9a8d-b1d99be0bc18'))");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "ZINC000000000905");
  }

  private SearchResponse executeQuery(String query) {
    val request = queryEngine.execute(query, DRUG_CENTRIC);
    log.debug("Request - {}", request);
    val result = request.getRequestBuilder().execute().actionGet();
    log.debug("Result - {}", result);

    return result;
  }

}
