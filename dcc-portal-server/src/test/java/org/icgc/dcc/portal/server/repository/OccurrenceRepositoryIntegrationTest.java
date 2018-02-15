/*
 * Copyright 2014(c) The Ontario Institute for Cancer Research. All rights reserved.
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

import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.icgc.dcc.portal.server.model.Query.QueryBuilder;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OccurrenceRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

  private static final String DEFAULT_SORT = "donorId";
  private static final IndexType CENTRIC_TYPE = IndexType.OCCURRENCE_CENTRIC;

  @InjectMocks
  OccurrenceRepository repository;

  MultiSearchResponse setup(OccurrenceRepository repo, QueryBuilder qb, IndexType type) {
    MultiSearchRequestBuilder search = client.prepareMultiSearch();

    for (val f : FILTERS) {
      val filter = new FiltersParam(f).get();
      val query = qb.filters(filter).build();
      val request = repo.buildFindAllRequest(query);

      search.add(request);
    }

    return search.execute().actionGet();
  }

  @Test
  // Just check that the response is not empty
  public void test_response() {
    val sr = setup(repository, score(DEFAULT_SORT), CENTRIC_TYPE);

    // Setup MultiSearch request for all filter combinations
    for (val res : sr.getResponses()) { // MultiSearchResponses
      val response = res.getResponse();
      checkEmpty(response);
    }
  }
}
