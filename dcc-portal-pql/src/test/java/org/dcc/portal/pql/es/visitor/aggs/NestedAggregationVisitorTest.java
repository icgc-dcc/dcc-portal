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
package org.dcc.portal.pql.es.visitor.aggs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.query.QueryEngine;
import org.dcc.portal.pql.utils.BaseElasticsearchTest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.global.Global;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.ReverseNested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class NestedAggregationVisitorTest extends BaseElasticsearchTest {

  QueryEngine queryEngine;

  @Before
  public void setUp() {
    es.execute(createIndexMappings(MUTATION_CENTRIC).withData(bulkFile(getClass())));
    queryEngine = new QueryEngine(es.client(), INDEX_NAME);
  }

  @Test
  public void nestedAggregationTest() {
    val result = executeQuery("facets(consequenceType)");

    Global global = result.getAggregations().get("consequenceType");
    Nested nested = global.getAggregations().get("consequenceType");
    Terms terms = nested.getAggregations().get("consequenceType");

    val missenseAll = terms.getBucketByKey("missense_variant");
    assertThat(missenseAll.getDocCount()).isEqualTo(5L);
    ReverseNested missenseNested = missenseAll.getAggregations().get("consequenceType");
    assertThat(missenseNested.getDocCount()).isEqualTo(3L);

    val frameshiftAll = terms.getBucketByKey("frameshift_variant");
    assertThat(frameshiftAll.getDocCount()).isEqualTo(2L);
    ReverseNested frameshiftNested = frameshiftAll.getAggregations().get("consequenceType");
    assertThat(frameshiftNested.getDocCount()).isEqualTo(2L);

    val intergenicAll = terms.getBucketByKey("intergenic_region");
    assertThat(intergenicAll.getDocCount()).isEqualTo(2L);
    ReverseNested intergenicNested = intergenicAll.getAggregations().get("consequenceType");
    assertThat(intergenicNested.getDocCount()).isEqualTo(1L);

    // Missing
    Global globalMissing = result.getAggregations().get("consequenceType_missing");
    Nested nestedMissing = globalMissing.getAggregations().get("consequenceType_missing");
    Missing termsMissing = nestedMissing.getAggregations().get("consequenceType_missing");
    ReverseNested reverseMissing = termsMissing.getAggregations().get("consequenceType_missing");
    assertThat(reverseMissing.getDocCount()).isEqualTo(1L);
  }

  @Test
  public void nestedAggregationTest_withFilter() {
    val result = executeQuery("facets(consequenceType), eq(transcriptId, 'T7')");

    Global global = result.getAggregations().get("consequenceType");
    Nested nested = global.getAggregations().get("consequenceType");
    Filter nestedFilter = nested.getAggregations().get("consequenceType");
    Terms terms = nestedFilter.getAggregations().get("consequenceType");

    val intergenicAll = terms.getBucketByKey("intergenic_region");
    assertThat(intergenicAll.getDocCount()).isEqualTo(1L);
    ReverseNested intergenicNested = intergenicAll.getAggregations().get("consequenceType");
    assertThat(intergenicNested.getDocCount()).isEqualTo(1L);
  }

  @Test
  public void nestedAggregationTest_withNestedFilter() {
    val result = executeQuery("facets(platform), eq(donor.projectId, 'ALL-US')");

    Global global = result.getAggregations().get("platform");
    Nested nested = global.getAggregations().get("platform");
    Filter nestedFilter = nested.getAggregations().get("platform");
    nested = nestedFilter.getAggregations().get("platform");
    Terms terms = nested.getAggregations().get("platform");

    val solidSeq = terms.getBucketByKey("SOLiD sequencing");
    assertThat(solidSeq.getDocCount()).isEqualTo(2L);
    ReverseNested solidSeqNested = solidSeq.getAggregations().get("platform");
    assertThat(solidSeqNested.getDocCount()).isEqualTo(1L);

    val illuminaGa = terms.getBucketByKey("SOLiD sequencing");
    assertThat(illuminaGa.getDocCount()).isEqualTo(2L);
    ReverseNested illuminaGaNested = illuminaGa.getAggregations().get("platform");
    assertThat(illuminaGaNested.getDocCount()).isEqualTo(1L);
  }

  private SearchResponse executeQuery(String query) {
    val request = queryEngine.execute(query, MUTATION_CENTRIC);
    log.debug("Request - {}", request);
    val result = request.getRequestBuilder().execute().actionGet();
    log.debug("Result - {}", result);

    return result;
  }

}
