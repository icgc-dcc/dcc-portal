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

public class GeneRepositoryTest extends BaseElasticSearchTest {

  private static final String DEFAULT_SORT = "affectedDonorCountFiltered";
  private static final String DEFAULT_ORDER = "desc";

  private static final String GENEID = "ENSG00000215529";
  private static final String GENE_FILTER = "gene:{chromosome:{is:[\"20\"]}}";
  private static final String GENE_NOT_FILTER = "gene:{chromosome:{not:\"20\"}}";
  private static final String DONOR_FILTER = "donor:{primarySite:{is:\"Brain\"}}";
  private static final String DONOR_FILTER2 = "donor:{primarySite:{is:\"Ovary\"}}";
  private static final String DONOR_NOT_FILTER = "donor:{primarySite:{not:[\"Pancreas\", \"Ovary\"]}}";
  private static final String MUTATION_FILTER =
      "mutation:{platform:{is:\"Nimblegen Human Methylation 2.1M Whole-Genome sets\"}}";
  private static final String MUTATION_NOT_FILTER = "mutation:{platform:{not:\"Illumina GA sequencing\"}}";

  GeneRepository geneRepository;

  ImmutableMap<String, String> FIELDS = FIELDS_MAPPING.get(EntityType.GENE);

  @Before
  public void setUp() throws Exception {
    this.testIndex = TestIndex.RELEASE;
    es.execute(createIndexMappings(IndexType.GENE, IndexType.GENE_CENTRIC).withData(bulkFile(getClass())));
    geneRepository =
        new GeneRepository(es.client(), new QueryEngine(es.client(), testIndex.getName()), TestIndex.RELEASE.getName());
  }

  @Test
  public void testFindAll() throws Exception {
    Query query = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).build();
    SearchResponse response = geneRepository.findAllCentric(query);
    assertThat(response.getHits().getTotalHits()).isEqualTo(3);
  }

  @Test
  public void testFindAllWithFields() throws Exception {
    Query query =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).fields(Lists.newArrayList("id",
            "symbol")).build();
    SearchResponse response = geneRepository.findAllCentric(query);
    SearchHits hits = response.getHits();

    assertThat(response.getHits().getTotalHits()).isEqualTo(3);

    for (SearchHit hit : hits) {
      assertThat(hit.fields().keySet().size()).isEqualTo(2);
      assertThat(hit.fields().keySet()).isEqualTo(Sets.newHashSet(FIELDS.get("id"), FIELDS.get("symbol")));
    }
  }

  @Test
  public void testFindAllWithIsFilters() throws Exception {
    FiltersParam filter = new FiltersParam(joinFilters(GENE_FILTER));
    Query query =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get()).build();
    SearchResponse response = geneRepository.findAllCentric(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(1);
    assertThat(hits.getHits().length).isEqualTo(1);

    assertThat(cast(hits.getAt(0).field(FIELDS.get("id")).getValue())).isEqualTo(GENEID);

  }

  @Test
  public void testFindAllWithNotFilters() throws Exception {
    FiltersParam filter = new FiltersParam(joinFilters(GENE_NOT_FILTER));
    Query query =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get()).build();
    SearchResponse response = geneRepository.findAllCentric(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(2);
    assertThat(hits.getHits().length).isEqualTo(2);
  }

  @Test
  public void testFindAllWithNestedDonorFilters() throws Exception {
    FiltersParam filterIs = new FiltersParam(joinFilters(GENE_FILTER, DONOR_FILTER));
    Query queryIs =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filterIs.get()).build();
    SearchResponse responseIs = geneRepository.findAllCentric(queryIs);
    SearchHits hitsIs = responseIs.getHits();

    assertThat(hitsIs.getTotalHits()).isEqualTo(1);
    assertThat(hitsIs.getHits().length).isEqualTo(1);

    // assertThat(hitsIs.getAt(0).field(FIELDS.get("id")).getValue()).isEqualTo(GENEID);
    assertThat(cast(hitsIs.getAt(0).field(FIELDS.get("id")).getValue())).isIn(
        Lists.newArrayList("ENSG00000215529"));

    FiltersParam filterNot = new FiltersParam(joinFilters(DONOR_NOT_FILTER));
    Query queryNot =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filterNot.get()).build();
    SearchResponse responseNot = geneRepository.findAllCentric(queryNot);
    SearchHits hitsNot = responseNot.getHits();

    // Every single gene contains at least one ovary or pancreas donor, hence we get no results back.
    // This behavior is new after: DCC-5113
    assertThat(hitsNot.getTotalHits()).isEqualTo(0);
  }

  @Test
  public void testFindAllWithNestedMutationFilters() throws Exception {
    FiltersParam filterIs = new FiltersParam(joinFilters(GENE_FILTER, MUTATION_FILTER));
    Query queryIs =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filterIs.get()).build();
    SearchResponse responseIs = geneRepository.findAllCentric(queryIs);
    SearchHits hitsIs = responseIs.getHits();

    assertThat(hitsIs.getTotalHits()).isEqualTo(1);
    assertThat(hitsIs.getHits().length).isEqualTo(1);

    assertThat(cast(hitsIs.getAt(0).field(FIELDS.get("id")).getValue())).isEqualTo(GENEID);

    FiltersParam filterNot = new FiltersParam(joinFilters(GENE_FILTER, MUTATION_NOT_FILTER));
    Query queryNot =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filterNot.get()).build();
    SearchResponse responseNot = geneRepository.findAllCentric(queryNot);
    SearchHits hitsNot = responseNot.getHits();

    assertThat(hitsNot.getTotalHits()).isEqualTo(1);
  }

  @Test
  public void testFindAllWithNestedDonorMutationFilters() throws Exception {
    FiltersParam filter = new FiltersParam(joinFilters(GENE_FILTER, DONOR_FILTER2, MUTATION_FILTER));
    Query query =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get()).build();
    SearchResponse response = geneRepository.findAllCentric(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(1);
    assertThat(hits.getHits().length).isEqualTo(1);

    assertThat(cast(hits.getAt(0).field(FIELDS.get("id")).getValue())).isEqualTo(GENEID);
  }

  @Test
  public void testCountIntersection() throws Exception {
    assertThat(geneRepository.count(Query.builder().build())).isEqualTo(3);
  }

  @Test
  public void testCountIntersectionWithFilters() throws Exception {
    assertThat(
        geneRepository.count(Query.builder().filters(new FiltersParam(joinFilters(GENE_FILTER)).get()).build()))
            .isEqualTo(1);
    assertThat(
        geneRepository.count(Query.builder().filters(new FiltersParam(joinFilters(GENE_NOT_FILTER)).get())
            .build())).isEqualTo(2);
  }

  @Test
  public void testFind() throws Exception {
    Query query = Query.builder().build();
    Map<String, Object> response = geneRepository.findOne(GENEID, query);
    assertThat(getString(response.get(FIELDS.get("id")))).isEqualTo(GENEID);
  }

  @Test
  public void testFindWithFields() throws Exception {
    Query query = Query.builder().fields(Lists.newArrayList("id", "symbol")).build();
    Map<String, Object> response = geneRepository.findOne(GENEID, query);
    assertThat(getString(response.get(FIELDS.get("id")))).isEqualTo(GENEID);
    assertThat(response.keySet()).isEqualTo(Sets.newHashSet(FIELDS.get("id"), FIELDS.get("symbol")));
  }

  @Test
  public void testFindWithIncludes() throws Exception {
    Query query = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).build();

    Map<String, Object> response = geneRepository.findOne(GENEID, query);

    assertThat(getString(response.get(FIELDS.get("id")))).isEqualTo(GENEID);
    assertThat(response.containsKey("transcripts")).isFalse();

    Query queryInclude =
        Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER)
            .includes(Lists.newArrayList("transcripts")).build();

    Map<String, Object> responseInclude = geneRepository.findOne(GENEID, queryInclude);

    assertThat(getString(responseInclude.get(FIELDS.get("id")))).isEqualTo(GENEID);
    assertThat(responseInclude.containsKey("transcripts")).isTrue();
  }

  @Test(expected = WebApplicationException.class)
  public void testFind404() throws Exception {
    Query query = Query.builder().build();
    geneRepository.findOne(MISSING_ID, query);
  }

  @Override
  protected Object cast(Object object) {
    return object;
  }
}
