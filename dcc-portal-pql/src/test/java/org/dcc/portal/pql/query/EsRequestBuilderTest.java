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

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.exception.SemanticException;
import org.dcc.portal.pql.utils.BaseElasticsearchTest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.global.Global;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Contains 3 mutations: MU1, MU2, MU3
 */
@Slf4j
public class EsRequestBuilderTest extends BaseElasticsearchTest {

  private EsRequestBuilder visitor;
  QueryEngine queryEngine;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() {
    es.execute(createIndexMappings(MUTATION_CENTRIC).withData(bulkFile(getClass())));
    visitor = new EsRequestBuilder(es.client());
    queryContext = new QueryContext(INDEX_NAME, MUTATION_CENTRIC);
    queryEngine = new QueryEngine(es.client(), INDEX_NAME);
  }

  @Test
  public void sortTest() {
    val result = executeQuery("select(start),sort(-start)");
    assertThat(getFirstSearchResult(result).getSortValues()[0]).isEqualTo(61020906L);
  }

  @Test
  public void selectTest() {
    val result = executeQuery("select(chromosome)");
    val hit = getFirstSearchResult(result);
    assertThat(hit.fields().size()).isEqualTo(1);
    val value = hit.field("chromosome").getValue();
    assertThat(value).isEqualTo("1");
  }

  @Test
  public void selectTest_withIncludes() {
    val result = executeQuery("select(transcripts)");
    assertTotalHitsCount(result, 3);
    val hit = getFirstSearchResult(result);
    assertThat(hit.fields().size()).isEqualTo(0);
    assertThat(hit.getSource().get("transcript")).isNotNull();
  }

  @Test
  public void countTest() {
    val esAst = createTree("count()");
    val request = visitor.buildSearchRequest(esAst, queryContext);
    val result = request.execute().actionGet();
    assertTotalHitsCount(result, 3);
  }

  @Test
  public void countTest_withFilter() {
    val esAst = createTree("count(), gt(start, 60000000)");
    val request = visitor.buildSearchRequest(esAst, queryContext);
    val result = request.execute().actionGet();
    assertTotalHitsCount(result, 1);
  }

  @Test
  public void inTest() {
    val result = executeQuery("in(chromosome, '1', '2')");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU1", "MU2");
  }

  @Test
  public void inTest_nested() {
    val result = executeQuery("in(sequencingStrategy, 'WGA', 'WGD')");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU1", "MU2");
  }

  @Test
  public void eqTest() {
    val result = executeQuery("eq(id, 'MU2')");
    assertTotalHitsCount(result, 1);
    assertThat(getFirstSearchResult(result).getId()).isEqualTo("MU2");
  }

  @Test
  public void eqTest_nested() {
    val result = executeQuery("eq(functionalImpact, 'Low')");
    assertTotalHitsCount(result, 1);
    assertThat(getFirstSearchResult(result).getId()).isEqualTo("MU1");
  }

  @Test
  public void neTest() {
    val result = executeQuery("ne(id, 'MU1')");
    assertTotalHitsCount(result, 2);
    for (val hit : result.getHits()) {
      assertThat(hit.getId()).isNotEqualTo("MU1");
    }
  }

  @Test
  public void neTest_nested() {
    val result = executeQuery("not(nested(transcript, eq(functionalImpact, 'Low')))");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU2", "MU3");
  }

  @Test
  public void gtTest() {
    val result = executeQuery("gt(testedDonorCount, 200)");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "MU3");
  }

  @Test
  public void gtTest_nested() {
    val result = executeQuery("gt(transcriptId, 'T2')");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "MU3");
  }

  @Test
  public void geTest() {
    val result = executeQuery("ge(testedDonorCount, 200)");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU2", "MU3");
  }

  @Test
  public void geTest_nested() {
    val result = executeQuery("ge(transcriptId, 'T2')");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU2", "MU3");
  }

  @Test
  public void leTest() {
    val result = executeQuery("le(id, 'MU2')");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU1", "MU2");
  }

  @Test
  public void leTest_nested() {
    val result = executeQuery("le(transcriptId, 'T2')");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU1", "MU2");
  }

  @Test
  public void ltTest() {
    val result = executeQuery("lt(id, 'MU2')");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "MU1");
  }

  @Test
  public void ltTest_nested() {
    val result = executeQuery("lt(transcriptId, 'T2')");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "MU1");
  }

  @Test
  public void andTest() {
    val result =
        executeQuery("and(eq(verificationStatus, 'tested'), eq(sequencingStrategy, 'WGE'))");
    assertTotalHitsCount(result, 1);
    assertThat(getFirstSearchResult(result).getId()).isEqualTo("MU2");
  }

  @Test
  public void andTest_rootLevel() {
    val result = executeQuery("eq(verificationStatus, 'tested'), eq(sequencingStrategy, 'WGE')");
    assertTotalHitsCount(result, 1);
    assertThat(getFirstSearchResult(result).getId()).isEqualTo("MU2");
  }

  @Test
  public void orTest() {
    val result =
        executeQuery("or(eq(verificationStatus, 'tested'), eq(sequencingStrategy, 'WGA')))");
    assertTotalHitsCount(result, 3);
    containsOnlyIds(result, "MU1", "MU2", "MU3");
  }

  @Test
  public void nestedTest() {
    val result = executeQuery("nested(ssm_occurrence.observation, " +
        "eq(verificationStatus, 'tested'), lt(sequencingStrategy, 'WGE'))");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "MU2");
  }

  @Test
  public void facetsTest_noFilters() {
    val result = executeQuery("facets(verificationStatus)");
    assertTotalHitsCount(result, 3);
    Global global = result.getAggregations().get("verificationStatus");
    Nested nested = global.getAggregations().get("verificationStatus");
    Terms terms = nested.getAggregations().get("verificationStatus");

    for (val bucket : terms.getBuckets()) {
      if (bucket.getKey().equals("tested")) {
        assertThat(bucket.getDocCount()).isEqualTo(2);
      } else {
        assertThat(bucket.getDocCount()).isEqualTo(4);
      }
    }

    Global globalMissing = result.getAggregations().get("verificationStatus_missing");
    Nested nestedMissing = globalMissing.getAggregations().get("verificationStatus_missing");
    Missing missing = nestedMissing.getAggregations().get("verificationStatus_missing");
    assertThat(missing.getDocCount()).isEqualTo(0L);
  }

  @Test
  public void facetsTest_missing() {
    val result = executeQuery("facets(verificationStatus)");
    assertTotalHitsCount(result, 3);

    Global globaldMissing = result.getAggregations().get("verificationStatus_missing");
    Nested nestedMissing = globaldMissing.getAggregations().get("verificationStatus_missing");
    Missing missing = nestedMissing.getAggregations().get("verificationStatus_missing");
    assertThat(missing.getDocCount()).isEqualTo(0L);
  }

  @Test
  public void facetsTest_noMatchFilter() {
    val result = executeQuery("facets(verificationStatus), in(transcriptId, 'T1', 'T2')");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU1", "MU2");

    Global globalAgg = result.getAggregations().get("verificationStatus");
    Filter filterAgg = globalAgg.getAggregations().get("verificationStatus");
    Nested nested = filterAgg.getAggregations().get("verificationStatus");
    Terms terms = nested.getAggregations().get("verificationStatus");

    for (val bucket : terms.getBuckets()) {
      if (bucket.getKey().equals("tested")) {
        assertThat(bucket.getDocCount()).isEqualTo(1);
      } else {
        assertThat(bucket.getDocCount()).isEqualTo(3);
      }
    }
  }

  @Test
  public void facetsTest_matchFilter() {
    val result = executeQuery("facets(verificationStatus), eq(verificationStatus, 'tested')");

    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU2", "MU3");

    Global global = result.getAggregations().get("verificationStatus");
    Nested nested = global.getAggregations().get("verificationStatus");
    Terms terms = nested.getAggregations().get("verificationStatus");

    for (val bucket : terms.getBuckets()) {
      if (bucket.getKey().equals("tested")) {
        assertThat(bucket.getDocCount()).isEqualTo(2);
      } else {
        assertThat(bucket.getDocCount()).isEqualTo(4);
      }
    }
  }

  @Test
  public void facetsCountQueryTest_matchFilter() {
    val result = executeQuery("count(),facets(verificationStatus), eq(verificationStatus, 'tested')");

    assertTotalHitsCount(result, 2);

    Global global = result.getAggregations().get("verificationStatus");
    Nested nested = global.getAggregations().get("verificationStatus");
    Terms terms = nested.getAggregations().get("verificationStatus");

    for (val bucket : terms.getBuckets()) {
      if (bucket.getKey().equals("tested")) {
        assertThat(bucket.getDocCount()).isEqualTo(2);
      } else {
        assertThat(bucket.getDocCount()).isEqualTo(4);
      }
    }
  }

  @Test
  public void countAndSelectShouldNotAppearTogetherTest() {
    thrown.expect(SemanticException.class);
    executeQuery("count(),select(verificationStatus), eq(verificationStatus, 'tested')");
  }

  @Test
  public void facetTest_multiFilters() {
    val result =
        executeQuery("facets(verificationStatus), eq(verificationStatus, 'tested'), in(transcriptId, 'T1', 'T2')");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "MU2");

    Global globalAgg = result.getAggregations().get("verificationStatus");
    Filter filterAgg = globalAgg.getAggregations().get("verificationStatus");
    Nested nested = filterAgg.getAggregations().get("verificationStatus");
    Terms terms = nested.getAggregations().get("verificationStatus");

    for (val bucket : terms.getBuckets()) {
      if (bucket.getKey().equals("tested")) {
        assertThat(bucket.getDocCount()).isEqualTo(1);
      } else {
        assertThat(bucket.getDocCount()).isEqualTo(3);
      }
    }
  }

  @Test
  public void notTest() {
    val result = executeQuery("not(in(id, 'MU1', 'MU2'))");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "MU3");
  }

  @Test
  public void existsTest() {
    val result = executeQuery("exists(assemblyVersion)");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "MU3");
  }

  @Test
  public void missingTest() {
    val result = executeQuery("missing(assemblyVersion)");
    assertTotalHitsCount(result, 2);
    containsOnlyIds(result, "MU1", "MU2");
  }

  // We need these goTermId and geneSetId queries to be consumable by the PQL engine.
  // As long as no exception occurs in these tests, we're fine.
  @Test
  public void singleGoTermId_InSimpleTest() {
    val pql = "in(gene.goTermId,'GO:0003674')";
    queryEngine.execute(pql, GENE_CENTRIC);
  }

  @Test
  public void singleGoTermId_EqSimpleTest() {
    val pql = "eq(gene.goTermId,'GO:0003674')";
    queryEngine.execute(pql, GENE_CENTRIC);
  }

  @Test
  public void multipleGoTermId_InTest() {
    val pql =
        "select(*),and(in(gene.goTermId,'GO:0003674','GO:0003673'),in(gene.goTermId,'GO:0003672','GO:0003671')),"
            + "in(gene.id,'b9b06e98-351a-4fd2-a86e-5071c78c66eb')";
    queryEngine.execute(pql, GENE_CENTRIC);
  }

  @Test
  public void multipleGoTermId_EqTest() {
    val pql =
        "select(*),and(eq(gene.goTermId,'GO:0003674'),eq(gene.goTermId,'GO:0003674')),"
            + "in(gene.id,'b9b06e98-351a-4fd2-a86e-5071c78c66eb')";
    queryEngine.execute(pql, GENE_CENTRIC);
  }

  @Test
  public void singleGeneSetId_InSimpleTest() {
    val pql = "in(gene.geneSetId,'GO:0003674')";
    queryEngine.execute(pql, GENE_CENTRIC);
  }

  @Test
  public void singleGeneSetId_EqSimpleTest() {
    val pql = "eq(gene.geneSetId,'GO:0003674')";
    queryEngine.execute(pql, GENE_CENTRIC);
  }

  @Test
  public void multipleGeneSetId_InTest() {
    val pql =
        "select(*),and(in(gene.geneSetId,'GO:0003674','GO:0003673'),in(gene.geneSetId,'GO:0003672','GO:0003671')),"
            + "in(gene.id,'b9b06e98-351a-4fd2-a86e-5071c78c66eb')";
    queryEngine.execute(pql, GENE_CENTRIC);
  }

  @Test
  public void multipleGeneSetId_EqTest() {
    val pql =
        "select(*),and(eq(gene.geneSetId,'GO:0003674'),eq(gene.geneSetId,'GO:0003674')),"
            + "in(gene.id,'b9b06e98-351a-4fd2-a86e-5071c78c66eb')";
    queryEngine.execute(pql, GENE_CENTRIC);
  }

  // Location tests
  @Test
  public void locationTest() {
    val result = executeQuery("eq(mutation.location, 'chr1:1-41020906')");
    assertTotalHitsCount(result, 1);
    containsOnlyIds(result, "MU1");
  }

  @Test
  public void hasCompoundTest() {
    val result = executeQuery("exists(gene.compoundId)");
    containsOnlyIds(result, "MU1");
  }

  @Test
  public void missingCompoundTest() {
    val result = executeQuery("missing(gene.compoundId)");
    containsOnlyIds(result, "MU2", "MU3");
  }

  private SearchResponse executeQuery(String query) {
    val request = queryEngine.execute(query, MUTATION_CENTRIC);
    log.debug("Request - {}", request);
    val result = request.getRequestBuilder().execute().actionGet();
    log.debug("Result - {}", result);

    return result;
  }

  private static SearchHit getFirstSearchResult(SearchResponse response) {
    return response.getHits().getAt(0);
  }

}
