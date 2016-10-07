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

import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.IndexType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DonorRepositoryIntegrationTest extends BaseRepositoryIntegrationTest {

  private static final String DEFAULT_SORT = "ssmAffectedGenes";
  private static final IndexType CENTRIC_TYPE = IndexType.DONOR_CENTRIC;
  @InjectMocks
  DonorRepository repository;
  @InjectMocks
  GeneRepository geneRepository;

  @Test
  public void test_aggs() {
    aggregations(repository, DEFAULT_SORT, CENTRIC_TYPE, EntityType.DONOR);
  }

  @Test
  public void test_score() {
    scores(repository, geneRepository, DEFAULT_SORT, CENTRIC_TYPE, EntityType.DONOR);
  }

  @Test
  public void test_counts() {
    counts(repository, projectIds, PROJECT_FILTER_TEMPLATE, DEFAULT_SORT);
  }

  @Test
  public void test_nestedCounts() {
    nestedCounts(repository, projectIds, mutationIds, MUTATION_PROJECT_FILTER_TEMPLATE, DEFAULT_SORT);
  }
}
