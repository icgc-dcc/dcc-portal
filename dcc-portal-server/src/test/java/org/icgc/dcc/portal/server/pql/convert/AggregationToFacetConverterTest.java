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

import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filter;
import static org.elasticsearch.search.aggregations.AggregationBuilders.global;
import static org.elasticsearch.search.aggregations.AggregationBuilders.missing;
import static org.elasticsearch.search.aggregations.AggregationBuilders.nested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.reverseNested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;

import org.assertj.core.api.Assertions;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.repository.BaseElasticsearchTest;
import org.junit.Before;
import org.junit.Test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AggregationToFacetConverterTest extends BaseElasticsearchTest {

  private static final String AGG_NAME = "consequenceTypeNested";
  private static final String MISSING_AGG_NAME = AGG_NAME + "_missing";

  AggregationToFacetConverter converter = new AggregationToFacetConverter();

  @Before
  public void setUpAggregationToFacetConverterTest() {
    prepareIndex(RELEASE_INDEX_NAME, MUTATION_CENTRIC);
    loadData("AggregationToFacetConverterTest.json");
  }

  @Test
  public void convertNestedAggregationTest() {
    val filter = QueryBuilders.existsQuery("chromosome");

    val aggregation =
        global(AGG_NAME).subAggregation(
            filter(AGG_NAME, filter).subAggregation(
                nested(AGG_NAME, "transcript").subAggregation(
                    terms(AGG_NAME).field("transcript.consequence.consequence_type").subAggregation(
                        reverseNested(AGG_NAME)))));

    val missingAggregation = global(MISSING_AGG_NAME).subAggregation(
        filter(MISSING_AGG_NAME, filter).subAggregation(
            nested(MISSING_AGG_NAME, "transcript").subAggregation(
                missing(MISSING_AGG_NAME).field("transcript.consequence.consequence_type").subAggregation(
                    reverseNested(MISSING_AGG_NAME)))));

    val response = converter.convert(executeQuery(aggregation, missingAggregation).getAggregations());
    log.info("{}", response);

    Assertions.assertThat(response).hasSize(1);
    val termFacet = response.get(AGG_NAME);
    Assertions.assertThat(termFacet.getType()).isEqualTo("terms");
    Assertions.assertThat(termFacet.getMissing()).isEqualTo(0L);
    Assertions.assertThat(termFacet.getOther()).isEqualTo(0L);
    Assertions.assertThat(termFacet.getTotal()).isEqualTo(6L);

    val terms = termFacet.getTerms();
    Assertions.assertThat(terms).hasSize(3);
    val missenseTerm = terms.get(0);
    Assertions.assertThat(missenseTerm.getCount()).isEqualTo(3L);
    Assertions.assertThat(missenseTerm.getTerm()).isEqualTo("missense_variant");

    val frameshiftTerm = terms.get(1);
    Assertions.assertThat(frameshiftTerm.getCount()).isEqualTo(2L);
    Assertions.assertThat(frameshiftTerm.getTerm()).isEqualTo("frameshift_variant");

    val intergenicTerm = terms.get(2);
    Assertions.assertThat(intergenicTerm.getCount()).isEqualTo(1L);
    Assertions.assertThat(intergenicTerm.getTerm()).isEqualTo("intergenic_region");
  }

  @Test
  public void convertNonNestedAggregationTest() {
    val field = "platform";
    val aggr = terms(AGG_NAME).field(field);
    val missingAggr = missing(MISSING_AGG_NAME).field(field);

    val response = converter.convert(executeQuery(aggr, missingAggr).getAggregations());
    log.info("{}", response);

    Assertions.assertThat(response).hasSize(1);
    val termFacet = response.get(AGG_NAME);
    Assertions.assertThat(termFacet.getType()).isEqualTo("terms");
    Assertions.assertThat(termFacet.getMissing()).isEqualTo(1L);
    Assertions.assertThat(termFacet.getOther()).isEqualTo(0L);
    Assertions.assertThat(termFacet.getTotal()).isEqualTo(2L);

    val terms = termFacet.getTerms();
    Assertions.assertThat(terms).hasSize(1);

    val illuminaTerm = terms.get(0);
    Assertions.assertThat(illuminaTerm.getCount()).isEqualTo(2L);
    Assertions.assertThat(illuminaTerm.getTerm()).isEqualTo("Illumina HiSeq");
  }

  private SearchResponse executeQuery(AggregationBuilder... builder) {
    val request = client
        .prepareSearch(RELEASE_INDEX_NAME)
        .setTypes(IndexType.MUTATION_CENTRIC.getId())
        .addAggregation(builder[0])
        .addAggregation(builder[1]);
    log.debug("Request - {}", request);

    val response = request.execute().actionGet();
    log.debug("Response = {}", response);

    return response;
  }

}
