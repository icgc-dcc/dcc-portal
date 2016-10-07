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

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getString;

import java.util.Map;

import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.test.TestIndex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public class DonorRepositoryTest extends BaseElasticSearchTest {

  private static final String DEFAULT_SORT = "ssmAffectedGenes";
  private static final String DEFAULT_ORDER = "desc";

  private static final String DONOR_FILTER = "donor:{primarySite:{is:\"Ovary\"}}";
  private static final String DONOR_NOT_FILTER = "donor:{primarySite:{not:\"Ovary\"}}";
  private static final String GENE_FILTER = "gene:{type:{is:\"protein_coding\"}}";
  private static final String MUTATION_FILTER =
      "mutation:{platform:{is:\"Nimblegen Human Methylation 2.1M Whole-Genome sets\"}}";

  DonorRepository donorRepository;
  ImmutableMap<String, String> FIELDS = FIELDS_MAPPING.get(EntityType.DONOR);

  @Before
  public void setUp() throws Exception {
    this.testIndex = TestIndex.RELEASE;

    es.execute(createIndexMappings(IndexType.DONOR, IndexType.DONOR_CENTRIC)
        .withData(bulkFile(getClass()))
        // This is needed because the DonorRepository now does a 'secondary' search on icgc-repository index.
        .withData(MANIFEST_TEST_DATA));
    donorRepository =
        new DonorRepository(es.client(), new QueryEngine(es.client(), testIndex.getName()), TestIndex.RELEASE.getName(),
            TestIndex.REPOSITORY.getName());
  }

  @Test
  public void testFindAllCentric() throws Exception {
    Query query = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).build();
    SearchResponse response = donorRepository.findAllCentric(query);
    assertThat(response.getHits().getTotalHits()).isEqualTo(9);
  }

  @Test
  public void testFindAllCentricWithFields() throws Exception {
    Query query = Query.builder()
        .from(1)
        .size(10)
        .sort(DEFAULT_SORT).order(DEFAULT_ORDER)
        .fields(Lists.newArrayList("id", "primarySite"))
        .build();
    SearchResponse response = donorRepository.findAllCentric(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(9);
    assertThat(hits.getHits().length).isEqualTo(9);

    for (SearchHit hit : hits) {
      assertThat(hit.fields().keySet().size()).isEqualTo(2);
      assertThat(hit.fields().keySet()).isEqualTo(Sets.newHashSet(FIELDS.get("id"), FIELDS.get("primarySite")));
    }
  }

  @Test
  public void testFindAllCentricWithIsFilters() throws Exception {
    FiltersParam filter = new FiltersParam(joinFilters(DONOR_FILTER));
    Query query =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get()).build();
    SearchResponse response = donorRepository.findAllCentric(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(3);
    assertThat(hits.getHits().length).isEqualTo(3);

    for (SearchHit hit : hits) {
      assertThat(cast(hit.field(FIELDS.get("id")).getValue())).isIn(Lists.newArrayList("DO2", "DO6", "DO7"));
    }
  }

  @Test
  public void testFindAllCentricWithNotFilters() throws Exception {
    FiltersParam filter = new FiltersParam(joinFilters(DONOR_NOT_FILTER));
    Query query =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get()).build();
    SearchResponse response = donorRepository.findAllCentric(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(6);
    assertThat(hits.getHits().length).isEqualTo(6);

    for (SearchHit hit : hits) {
      assertThat(cast(hit.field(FIELDS.get("id")).getValue())).isNotIn(Lists.newArrayList("DO2", "DO6", "DO7"));
    }
  }

  @Test
  public void testFindAllWithNestedGeneFilters() throws Exception {
    FiltersParam filter = new FiltersParam(joinFilters(DONOR_NOT_FILTER, GENE_FILTER));
    Query query =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get()).build();
    SearchResponse response = donorRepository.findAllCentric(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(5);
    assertThat(hits.getHits().length).isEqualTo(5);

    for (SearchHit hit : hits) {
      assertThat(cast(hit.field(FIELDS.get("id")).getValue())).isIn(
          Lists.newArrayList("DO2", "DO2", "DO9", "DO1", "DO4", "DO8", "DO5"));
    }
  }

  @Test
  public void testFindAllWithNestedMutationFilters() throws Exception {
    FiltersParam filter = new FiltersParam(joinFilters(DONOR_NOT_FILTER, MUTATION_FILTER));
    Query query =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get()).build();
    SearchResponse response = donorRepository.findAllCentric(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(1);
    assertThat(hits.getHits().length).isEqualTo(1);

    for (SearchHit hit : hits) {
      assertThat(cast(hit.field(FIELDS.get("id")).getValue())).isIn(Lists.newArrayList("DO9"));
    }
  }

  @Test
  public void testFindAllWithNestedGeneMutationFilters() throws Exception {
    FiltersParam filter = new FiltersParam(joinFilters(DONOR_FILTER, GENE_FILTER, MUTATION_FILTER));
    Query query =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get()).build();
    SearchResponse response = donorRepository.findAllCentric(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(1);
    assertThat(hits.getHits().length).isEqualTo(1);

    for (SearchHit hit : hits) {
      assertThat(cast(hit.field(FIELDS.get("id")).getValue())).isIn(Lists.newArrayList("DO2"));
    }
  }

  @Test
  public void testCountIntersection() throws Exception {
    Query query = Query.builder().build();
    long response = donorRepository.count(query);
    assertThat(response).isEqualTo(9);
  }

  @Test
  public void testCountIntersectionWithNestedGeneFilters() throws Exception {
    FiltersParam filter = new FiltersParam(joinFilters(DONOR_NOT_FILTER, GENE_FILTER));
    Query query =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get()).build();
    long response = donorRepository.count(query);

    assertThat(response).isEqualTo(5);
  }

  @Test
  public void testCountIntersectionWithNestedMutationFilters() throws Exception {
    FiltersParam filter = new FiltersParam(joinFilters(DONOR_NOT_FILTER, MUTATION_FILTER));
    Query query =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get()).build();
    long response = donorRepository.count(query);

    assertThat(response).isEqualTo(1);
  }

  @Test
  public void testCountIntersectionWithNestedGeneMutationFilters() throws Exception {
    FiltersParam filter = new FiltersParam(joinFilters(DONOR_FILTER, GENE_FILTER, MUTATION_FILTER));
    Query query =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get()).build();
    long response = donorRepository.count(query);

    assertThat(response).isEqualTo(1);
  }

  @Test
  public void testFind() throws Exception {
    String id = "DO1";
    Query query = Query.builder().build();
    Map<String, Object> response = donorRepository.findOne(id, query);
    assertThat(getString(response.get(FIELDS.get("id")))).isEqualTo(id);
  }

  @Test
  public void testFindWithFields() throws Exception {
    String id = "DO1";
    Query query = Query.builder().fields(Lists.newArrayList("id", "primarySite")).build();
    Map<String, Object> response = donorRepository.findOne(id, query);
    assertThat(getString(response.get(FIELDS.get("id")))).isEqualTo(id);
    assertThat(response.keySet()).isEqualTo(Sets.newHashSet(FIELDS.get("id"), FIELDS.get("primarySite")));
  }

  @Test
  public void testFindWithIncludes() throws Exception {
    String id = "DO1";
    Query query = Query.builder().from(1).size(10).build();

    Map<String, Object> response = donorRepository.findOne(id, query);

    assertThat(getString(response.get(FIELDS.get("id")))).isEqualTo(id);
    assertThat(response.containsKey("specimen")).isFalse();

    Query queryInclude =
        Query.builder().from(1).size(10).includes(Lists.newArrayList("specimen", "notarealthing")).build();

    Map<String, Object> responseInclude = donorRepository.findOne(id, queryInclude);

    assertThat(getString(responseInclude.get(FIELDS.get("id")))).isEqualTo(id);
    assertThat(responseInclude.containsKey("specimen")).isTrue();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testUnsupported() throws Exception {
    Query query = Query.builder().build();
    donorRepository.findAll(query);
  }

  @Override
  protected Object cast(Object object) {
    return object;
  }

}
