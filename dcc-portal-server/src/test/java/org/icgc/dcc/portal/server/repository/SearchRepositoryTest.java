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

import org.assertj.core.api.Assertions;
import org.dcc.portal.pql.meta.Type;
import org.elasticsearch.action.search.SearchResponse;
import org.icgc.dcc.portal.server.model.Query;
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

  private SearchRepository searchRepository = null;
  private static final String DEFAULT_TYPE = "";
  private static final String DEFAULT_SORT = "_score";
  private static final String DEFAULT_ORDER = "desc";
  private static final Type[] KEYWORD_TYPES_FOR_RELEASE_INDEX = {
      Type.DONOR_CENTRIC,
      Type.GENE_CENTRIC,
      Type.MUTATION_CENTRIC,
      Type.OBSERVATION_CENTRIC,
      Type.PROJECT,
      Type.GENE_SET,
      Type.DRUG_CENTRIC,
      Type.DIAGRAM
  };
  private static final Type[] KEYWORD_TYPES_FOR_REPOSITORY_INDEX = {
    Type.FILE
  };

  private static final String REPOSITORY_FIXTURE_FILENAME = DOT.join(SearchRepositoryTest.class.getSimpleName(), "repository.json");
  private static final String RELEASE_FIXTURE_FILENAME = DOT.join(SearchRepositoryTest.class.getSimpleName(), "release.json");

  @Before
  public void setUpProjectRepositoryTest() throws Exception {
    if (searchRepository == null) {
      prepareIndex(REPOSITORY_INDEX_NAME, REPOSITORY_FIXTURE_FILENAME , KEYWORD_TYPES_FOR_REPOSITORY_INDEX);
      prepareIndex(RELEASE_INDEX_NAME, RELEASE_FIXTURE_FILENAME, KEYWORD_TYPES_FOR_RELEASE_INDEX);
      searchRepository = new SearchRepository(client, RELEASE_INDEX_NAME, REPOSITORY_INDEX_NAME);
    }
  }

  @Test
  public void testFindAll() throws Exception {
    Query query = Query.builder()
        .query("4ba8b9ec-5c52-58bd-838e-e2b4cae6a434")
        .from(1)
        .size(10)
        .sort(DEFAULT_SORT)
        .order(DEFAULT_ORDER)
        .build();
    SearchResponse response = searchRepository.findAll(query, DEFAULT_TYPE);
    Assertions.assertThat(response.getHits().getTotalHits()).isLessThanOrEqualTo(5);
  }
}
