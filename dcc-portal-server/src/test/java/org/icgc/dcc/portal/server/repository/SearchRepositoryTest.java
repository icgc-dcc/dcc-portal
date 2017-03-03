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

import lombok.val;
import org.assertj.core.api.Assertions;
import org.elasticsearch.action.search.SearchResponse;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.util.Strings;
import org.junit.Before;
import org.junit.Test;

import static org.icgc.dcc.common.core.util.Joiners.DOT;

/**
 * Notes:
 *
 * - for each "type" (which can be found in definition for KEYWORD_FIELDS_MAPPING) across all indices, need to create data that stimulates each attribute.
 * - need a repeatable search criteria for each attribute. Make this well encapsulated, so that it can change easily
 *    - after talking to dusan, for now create a sanity test, and a "TTN and TTN3234" test (make sure TTN is first). This should be enough to protect against ES upgrades and stuff
 * - make sure to test this against legacy code and make sure it fails
 *
 */

//@RunWith(MockitoJUnitRunner.class)
public class SearchRepositoryTest extends BaseElasticsearchTest {

  protected static final String RELEASE_SCHEMA_JSON_DIR = "org/icgc/dcc/repository/resources/mappings";
  private static final String DEFAULT_TYPE = "";
  private static final String DEFAULT_SORT = "_score";
  private static final String DEFAULT_ORDER = "desc";

  private static final IndexType[] RELEASE_INDEX_TYPES = {
    IndexType.DONOR_TEXT,
    IndexType.GENE_TEXT,
    IndexType.MUTATION_TEXT,
    IndexType.GENESET_TEXT,
    IndexType.DRUG_TEXT,
    IndexType.PROJECT_TEXT
  };

  private static final IndexType[] REPO_INDEX_TYPES= {
      IndexType.FILE_DONOR_TEXT,
      IndexType.FILE_TEXT
  };

  private static final IndexType[] ALL_KEYWORDS_SEARCH_INDEX_TYPES = {
      IndexType.DONOR_TEXT,
      IndexType.GENE_TEXT,
      IndexType.MUTATION_TEXT,
      IndexType.GENESET_TEXT,
      IndexType.DRUG_TEXT,
      IndexType.PROJECT_TEXT,
      IndexType.FILE_DONOR_TEXT,
      IndexType.FILE_TEXT
  };

  private static final IndexType[] DEFAULT_KEYWORDS_SEARCH_INDEX_TYPES = {
      IndexType.DONOR_TEXT,
      IndexType.GENE_TEXT,
      IndexType.MUTATION_TEXT,
      IndexType.GENESET_TEXT,
      IndexType.DRUG_TEXT,
      IndexType.PROJECT_TEXT,
      IndexType.FILE_TEXT
  };

  private static final String REPOSITORY_FIXTURE_FILENAME = DOT.join(SearchRepositoryTest.class.getSimpleName(), "repository.json");
  private static final String RELEASE_FIXTURE_FILENAME = DOT.join(SearchRepositoryTest.class.getSimpleName(), "release.json");

  private SearchRepository searchRepository = null;

  protected void prepareIndex(String indexName, String file, IndexType... indexTypes) {
    val indexTypeNames = Strings.toStringArray(indexTypes, IndexType::getId);
    createIndexMappings(indexName, indexTypeNames);
    loadData(file);
  }

  @Before
  public void setUpProjectRepositoryTest() throws Exception {
    if (searchRepository == null) {
      prepareIndex(REPOSITORY_INDEX_NAME, REPOSITORY_FIXTURE_FILENAME , REPO_INDEX_TYPES );
      prepareIndex(RELEASE_INDEX_NAME, RELEASE_FIXTURE_FILENAME, RELEASE_INDEX_TYPES);
      searchRepository = new SearchRepository(client, RELEASE_INDEX_NAME, REPOSITORY_INDEX_NAME);
    }
  }

  @Test
  public void testFindAllFileKeyword() throws Exception {
    Query query = Query.builder()
        .query("305c46a3-a315-5087-ac7b-359350265f62")
        .from(0)
        .size(10)
        .sort(DEFAULT_SORT)
        .order(DEFAULT_ORDER)
        .build();
    SearchResponse response = searchRepository.findAll(query, DEFAULT_TYPE);
    Assertions.assertThat(response.getHits().getTotalHits()).isLessThanOrEqualTo(5);
    Assertions.assertThat(response.getHits().getTotalHits()).isGreaterThan(0);
  }

  @Test
  public void testFindAllGeneKeyword() throws Exception {
    Query query = Query.builder()
        .query("LINC")
        .from(1)
        .size(10)
        .sort(DEFAULT_SORT)
        .order(DEFAULT_ORDER)
        .build();
    SearchResponse response = searchRepository.findAll(query, DEFAULT_TYPE);
    Assertions.assertThat(response.getHits().getTotalHits()).isLessThanOrEqualTo(5);
    Assertions.assertThat(response.getHits().getTotalHits()).isGreaterThan(0);
  }
}
