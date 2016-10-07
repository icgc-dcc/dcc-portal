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

import javax.ws.rs.WebApplicationException;

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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class MutationRepositoryTest extends BaseElasticSearchTest {

  private static final String DEFAULT_SORT = "affectedDonorCountFiltered";
  private static final String DEFAULT_ORDER = "desc";

  private static final String ID = "MU498";
  private static final String DONOR_FILTER = "donor:{primarySite:{is:\"Blood\"}}";
  private static final String DONOR_NOT_FILTER = "donor:{primarySite:{not:\"Blood\"}}";
  private static final String MUTATION_FILTER = "mutation:{chromosome:{is:\"19\"}}";
  private static final String MUTATION_NOT_FILTER = "mutation:{chromosome:{not:\"19\"}}";
  private static final String GENES_FILTER = "gene:{symbol:{is:[\"STK11\",\"HMGB2P1\"]}}";
  private static final String GENES_NOT_FILTER = "gene:{symbol:{not:[\"STK11\",\"HMGB2P1\"]}}";
  private static final String GENE_FILTER = "gene:{symbol:{is:\"STK11\"}}";

  MutationRepository mutationRepository;

  ImmutableMap<String, String> FIELDS = FIELDS_MAPPING.get(EntityType.MUTATION);

  @Before
  public void setUp() throws Exception {
    this.testIndex = TestIndex.RELEASE;
    es.execute(
        createIndexMappings(IndexType.MUTATION_CENTRIC)
            .withData(bulkFile(getClass())));
    mutationRepository =
        new MutationRepository(es.client(), new QueryEngine(es.client(), testIndex.getName()), testIndex.getName());
  }

  @Test
  public void testFindAll() throws Exception {
    Query query = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).build();
    SearchResponse response = mutationRepository.findAllCentric(query);
    assertThat(response.getHits().getTotalHits()).isEqualTo(1);
  }

  @Test
  public void testFindAllWithFields() throws Exception {
    Query query = Query.builder()
        .from(1)
        .size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER)
        .fields(Lists.newArrayList("id", "mutation"))
        .build();
    SearchResponse response = mutationRepository.findAllCentric(query);
    SearchHits hits = response.getHits();

    assertThat(response.getHits().getTotalHits()).isEqualTo(1);

    for (SearchHit hit : hits) {
      assertThat(hit.fields().keySet().size()).isEqualTo(2);
      assertThat(hit.fields().keySet()).isEqualTo(Sets.newHashSet(FIELDS.get("id"), FIELDS.get("mutation")));
    }
  }

  @Test
  public void testFindAllWithIsFilters() throws Exception {
    FiltersParam filter = new FiltersParam(joinFilters(MUTATION_FILTER));
    Query query = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get())
        .build();
    SearchResponse response = mutationRepository.findAllCentric(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(1);
    assertThat(hits.getHits().length).isEqualTo(1);

    assertThat(cast(hits.getAt(0).field(FIELDS.get("id")).getValue())).isEqualTo(ID);

  }

  @Test
  public void testFindAllWithNotFilters() throws Exception {
    FiltersParam filter = new FiltersParam(joinFilters(MUTATION_NOT_FILTER));
    Query query = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get())
        .build();
    SearchResponse response = mutationRepository.findAllCentric(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(0);
    assertThat(hits.getHits().length).isEqualTo(0);
  }

  @Test
  public void testFindAllWithNestedDonorFilters() throws Exception {
    FiltersParam filterIs = new FiltersParam(joinFilters(MUTATION_FILTER, DONOR_FILTER));
    Query queryIs =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filterIs.get()).build();
    SearchResponse responseIs = mutationRepository.findAllCentric(queryIs);
    SearchHits hitsIs = responseIs.getHits();

    assertThat(hitsIs.getTotalHits()).isEqualTo(1);
    assertThat(hitsIs.getHits().length).isEqualTo(1);

    assertThat(cast(hitsIs.getAt(0).field(FIELDS.get("id")).getValue())).isEqualTo(ID);

    FiltersParam filterNot = new FiltersParam(joinFilters(MUTATION_FILTER, DONOR_NOT_FILTER));
    Query queryNot = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filterNot
        .get()).build();
    SearchResponse responseNot = mutationRepository.findAllCentric(queryNot);
    SearchHits hitsNot = responseNot.getHits();

    assertThat(hitsNot.getTotalHits()).isEqualTo(0);
  }

  @Test
  public void testFindAllWithNestedGeneFilters() throws Exception {
    FiltersParam filterIs = new FiltersParam(joinFilters(MUTATION_FILTER, GENES_FILTER));
    Query queryIs =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filterIs.get()).build();
    SearchResponse responseIs = mutationRepository.findAllCentric(queryIs);
    SearchHits hitsIs = responseIs.getHits();

    assertThat(hitsIs.getTotalHits()).isEqualTo(1);
    assertThat(hitsIs.getHits().length).isEqualTo(1);

    assertThat(cast(hitsIs.getAt(0).field(FIELDS.get("id")).getValue())).isEqualTo(ID);

    FiltersParam filterNot = new FiltersParam(joinFilters(MUTATION_FILTER, GENES_NOT_FILTER));
    Query queryNot = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filterNot
        .get()).build();
    SearchResponse responseNot = mutationRepository.findAllCentric(queryNot);
    SearchHits hitsNot = responseNot.getHits();

    assertThat(hitsNot.getTotalHits()).isEqualTo(0);
  }

  @Test
  public void testFindAllWithNestedGeneMutationFilters() throws Exception {
    FiltersParam filter = new FiltersParam(joinFilters(MUTATION_FILTER, DONOR_FILTER, GENE_FILTER));
    Query query = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get())
        .build();
    SearchResponse response = mutationRepository.findAllCentric(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(1);
    assertThat(hits.getHits().length).isEqualTo(1);

    assertThat(cast(hits.getAt(0).field(FIELDS.get("id")).getValue())).isEqualTo(ID);
  }

  @Test
  public void testCountIntersection() throws Exception {
    assertThat(mutationRepository.count(Query.builder().build())).isEqualTo(1);
  }

  @Test
  public void testCountIntersectionWithFilters() throws Exception {
    assertThat(mutationRepository.count(
        Query.builder().filters(new FiltersParam(joinFilters(MUTATION_FILTER)).get()).build())).isEqualTo(1);
    assertThat(mutationRepository.count(
        Query.builder().filters(new FiltersParam(joinFilters(MUTATION_NOT_FILTER)).get()).build())).isEqualTo(0);
  }

  @Test
  public void testFind() throws Exception {

    Query query = Query.builder().build();
    Map<String, Object> response = mutationRepository.findOne(ID, query);
    assertThat(getString(response.get(FIELDS.get("id")))).isEqualTo(ID);
  }

  @Test
  public void testFindWithFields() throws Exception {

    Query query = Query.builder().fields(Lists.newArrayList("id", "mutation")).build();
    Map<String, Object> response = mutationRepository.findOne(ID, query);
    assertThat(getString(response.get(FIELDS.get("id")))).isEqualTo(ID);
    assertThat(response.keySet()).isEqualTo(Sets.newHashSet(FIELDS.get("id"), FIELDS.get("mutation")));
  }

  @Test
  public void testFindWithoutIncludes() throws Exception {

    Query query = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).build();

    Map<String, Object> response = mutationRepository.findOne(ID, query);

    assertThat(getString(response.get(FIELDS.get("id")))).isEqualTo(ID);
    assertThat(response.containsKey("transcripts")).isFalse();
    assertThat(response.containsKey("consequence")).isFalse();
  }

  @Test
  public void testFindWithIncludesTranscript() throws Exception {

    Query query = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).includes(Lists
        .newArrayList("transcripts")).build();

    Map<String, Object> responseInclude = mutationRepository.findOne(ID, query);

    assertThat(getString(responseInclude.get(FIELDS.get("id")))).isEqualTo(ID);
    assertThat(responseInclude.containsKey("transcript")).isTrue();
    assertThat(responseInclude.containsKey("consequence")).isFalse();
  }

  @Test
  public void testFindWithIncludesConsequence() throws Exception {

    Query query = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).includes(Lists
        .newArrayList("consequences")).build();

    Map<String, Object> responseInclude = mutationRepository.findOne(ID, query);

    assertThat(getString(responseInclude.get(FIELDS.get("id")))).isEqualTo(ID);
    assertThat(responseInclude.containsKey("transcript")).isFalse();
    assertThat(responseInclude.containsKey("consequences")).isTrue();
  }

  @Test
  public void testFindWithIncludesTranscriptAndConsequence() throws Exception {

    Query query = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).includes(Lists
        .newArrayList("transcripts", "consequences")).build();

    Map<String, Object> responseInclude = mutationRepository.findOne(ID, query);

    assertThat(getString(responseInclude.get(FIELDS.get("id")))).isEqualTo(ID);
    assertThat(responseInclude.containsKey("transcript")).isTrue();
    assertThat(responseInclude.containsKey("consequences")).isTrue();
  }

  @Test(expected = WebApplicationException.class)
  public void testFind404() throws Exception {
    Query query = Query.builder().build();
    mutationRepository.findOne(MISSING_ID, query);
  }

}
