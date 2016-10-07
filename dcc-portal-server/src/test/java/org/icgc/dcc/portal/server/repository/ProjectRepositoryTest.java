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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ProjectRepositoryTest extends BaseElasticSearchTest {

  private static final String DEFAULT_SORT = "totalDonorCount";
  private static final String DEFAULT_ORDER = "desc";
  private static final String FILTER = "{project:{primaryCountries:{is:\"Australia\"}}}";
  private static final String NOT_FILTER = "{project:{primaryCountries:{not:\"Australia\"}}}";

  ProjectRepository projectRepository;

  ImmutableMap<String, String> FIELDS = FIELDS_MAPPING.get(EntityType.PROJECT);

  @Before
  public void setUp() throws Exception {
    this.testIndex = TestIndex.RELEASE;
    es.execute(createIndexMapping(IndexType.PROJECT)
        .withData(bulkFile(getClass()))
        // This is needed because the ProjectRepository now does a 'secondary' search on icgc-repository index.
        .withData(MANIFEST_TEST_DATA));

    projectRepository =
        new ProjectRepository(es.client(), new QueryEngine(es.client(), testIndex.getName()),
            TestIndex.RELEASE.getName(), TestIndex.REPOSITORY.getName());
  }

  @Test
  public void testFindAll() throws Exception {
    Query query = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).build();
    SearchResponse response = projectRepository.findAll(query);
    assertThat(response.getHits().getTotalHits()).isEqualTo(3);
  }

  @Test
  public void testFindAllWithFields() throws Exception {
    Query query = Query.builder()
        .from(1)
        .size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER)
        .fields(Lists.newArrayList("name", "primarySite"))
        .build();
    SearchResponse response = projectRepository.findAll(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(3);
    assertThat(hits.getHits().length).isEqualTo(3);

    for (SearchHit hit : hits) {
      assertThat(hit.fields().keySet().size()).isEqualTo(2);
      assertThat(hit.fields().keySet()).isEqualTo(Sets.newHashSet(FIELDS.get("name"), FIELDS.get("primarySite")));
    }
  }

  @Test
  public void testFindAllWithIsFilters() throws Exception {
    FiltersParam filter = new FiltersParam(FILTER);
    Query query = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get())
        .build();
    SearchResponse response = projectRepository.findAll(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(2);
    assertThat(hits.getHits().length).isEqualTo(2);

    for (SearchHit hit : hits) {
      assertThat(cast(hit.field(FIELDS.get("id")).getValue())).isIn(Lists.newArrayList("OV-AU", "PACA-AU"));
    }
  }

  @Test
  public void testFindAllWithNotFilters() throws Exception {
    FiltersParam filter = new FiltersParam(NOT_FILTER);
    Query query = Query.builder().from(1).size(10).sort(DEFAULT_SORT).order(DEFAULT_ORDER).filters(filter.get())
        .build();
    SearchResponse response = projectRepository.findAll(query);
    SearchHits hits = response.getHits();

    assertThat(hits.getTotalHits()).isEqualTo(1);
    assertThat(hits.getHits().length).isEqualTo(1);

    for (SearchHit hit : hits) {
      assertThat(cast(hit.field(FIELDS.get("id")).getValue())).isNotIn(Lists.newArrayList("OV-AU", "PACA-AU"));
    }
  }

  @Test
  public void testCount() throws Exception {
    Query query = Query.builder().build();
    long response = projectRepository.count(query);
    assertThat(response).isEqualTo(3);
  }

  @Test
  public void testCountWithFilters() throws Exception {
    FiltersParam filter = new FiltersParam(FILTER);
    Query query = Query.builder().filters(filter.get()).build();
    long response = projectRepository.count(query);
    assertThat(response).isEqualTo(2);
  }

  @Test
  public void testFind() throws Exception {
    String id = "PEME-CA";
    Query query = Query.builder().build();
    Map<String, Object> response = projectRepository.findOne(id, query);
    assertThat(getString(response.get(FIELDS.get("id")))).isEqualTo(id);
  }

  @Test
  public void testFindWithFields() throws Exception {
    String id = "PEME-CA";
    Query query = Query.builder().fields(Lists.newArrayList("id", "name")).build();
    Map<String, Object> response = projectRepository.findOne(id, query);
    assertThat(getString(response.get(FIELDS.get("id")))).isEqualTo(id);
    assertThat(response.keySet()).isEqualTo(Sets.newHashSet(FIELDS.get("id"), FIELDS.get("name")));
  }

  // FIXME
  // @Test(expected = WebApplicationException.class)
  public void testFind404() throws Exception {
    Query query = Query.builder().build();
    projectRepository.findOne(MISSING_ID, query);
  }

  @Override
  protected Object cast(Object object) {
    return object;
  }
}
