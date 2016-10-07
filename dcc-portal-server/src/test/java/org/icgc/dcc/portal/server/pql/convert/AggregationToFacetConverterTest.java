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
package org.icgc.dcc.portal.server.pql.convert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filter;
import static org.elasticsearch.search.aggregations.AggregationBuilders.global;
import static org.elasticsearch.search.aggregations.AggregationBuilders.missing;
import static org.elasticsearch.search.aggregations.AggregationBuilders.nested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.reverseNested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.repository.BaseElasticSearchTest;
import org.icgc.dcc.portal.server.test.TestIndex;
import org.junit.Before;
import org.junit.Test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AggregationToFacetConverterTest extends BaseElasticSearchTest {

  private static final String AGG_NAME = "consequenceTypeNested";
  private static final String MISSING_AGG_NAME = AGG_NAME + "_missing";

  AggregationToFacetConverter converter = new AggregationToFacetConverter();

  @Before
  public void setUp() {
    this.testIndex = TestIndex.RELEASE;
    es.execute(createIndexMappings(IndexType.MUTATION_CENTRIC).withData(bulkFile(getClass())));
  }

  @Test
  public void convertNestedAggregationTest() {
    val filter = FilterBuilders.existsFilter("chromosome");

    val aggregation =
        global(AGG_NAME).subAggregation(
            filter(AGG_NAME).filter(filter).subAggregation(
                nested(AGG_NAME).path("transcript").subAggregation(
                    terms(AGG_NAME).field("transcript.consequence.consequence_type").subAggregation(
                        reverseNested(AGG_NAME)))));

    val missingAggregation = global(MISSING_AGG_NAME).subAggregation(
        filter(MISSING_AGG_NAME).filter(filter).subAggregation(
            nested(MISSING_AGG_NAME).path("transcript").subAggregation(
                missing(MISSING_AGG_NAME).field("transcript.consequence.consequence_type").subAggregation(
                    reverseNested(MISSING_AGG_NAME)))));

    val response = converter.convert(executeQuery(aggregation, missingAggregation).getAggregations());
    log.info("{}", response);

    assertThat(response).hasSize(1);
    val termFacet = response.get(AGG_NAME);
    assertThat(termFacet.getType()).isEqualTo("terms");
    assertThat(termFacet.getMissing()).isEqualTo(0L);
    assertThat(termFacet.getOther()).isEqualTo(0L);
    assertThat(termFacet.getTotal()).isEqualTo(6L);

    val terms = termFacet.getTerms();
    assertThat(terms).hasSize(3);
    val missenseTerm = terms.get(0);
    assertThat(missenseTerm.getCount()).isEqualTo(3L);
    assertThat(missenseTerm.getTerm()).isEqualTo("missense_variant");

    val frameshiftTerm = terms.get(1);
    assertThat(frameshiftTerm.getCount()).isEqualTo(2L);
    assertThat(frameshiftTerm.getTerm()).isEqualTo("frameshift_variant");

    val intergenicTerm = terms.get(2);
    assertThat(intergenicTerm.getCount()).isEqualTo(1L);
    assertThat(intergenicTerm.getTerm()).isEqualTo("intergenic_region");
  }

  @Test
  public void convertNonNestedAggregationTest() {
    val field = "platform";
    val aggr = terms(AGG_NAME).field(field);
    val missingAggr = missing(MISSING_AGG_NAME).field(field);

    val response = converter.convert(executeQuery(aggr, missingAggr).getAggregations());
    log.info("{}", response);

    assertThat(response).hasSize(1);
    val termFacet = response.get(AGG_NAME);
    assertThat(termFacet.getType()).isEqualTo("terms");
    assertThat(termFacet.getMissing()).isEqualTo(1L);
    assertThat(termFacet.getOther()).isEqualTo(0L);
    assertThat(termFacet.getTotal()).isEqualTo(2L);

    val terms = termFacet.getTerms();
    assertThat(terms).hasSize(1);

    val illuminaTerm = terms.get(0);
    assertThat(illuminaTerm.getCount()).isEqualTo(2L);
    assertThat(illuminaTerm.getTerm()).isEqualTo("Illumina HiSeq");
  }

  private SearchResponse executeQuery(AggregationBuilder<?>... builder) {
    val request = es.client()
        .prepareSearch(testIndex.getName())
        .setTypes(IndexType.MUTATION_CENTRIC.getId())
        .addAggregation(builder[0])
        .addAggregation(builder[1]);
    log.debug("Request - {}", request);

    val response = request.execute().actionGet();
    log.debug("Response = {}", response);

    return response;
  }

}
