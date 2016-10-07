/* Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
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

import org.dcc.portal.pql.query.QueryEngine;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.test.TestIndex;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import lombok.val;

public class DiagramRepositoryTest extends BaseElasticSearchTest {

  DiagramRepository diagramRepository;

  ImmutableMap<String, String> FIELDS = FIELDS_MAPPING.get(EntityType.DIAGRAM);

  @Before
  public void setUp() throws Exception {
    this.testIndex = TestIndex.RELEASE;
    es.execute(
        createIndexMapping(IndexType.DIAGRAM)
            .withData(bulkFile(getClass())));
    diagramRepository =
        new DiagramRepository(es.client(), testIndex.getName(), new QueryEngine(es.client(), testIndex.getName()));
  }

  @Test
  public void testFindAll() throws Exception {
    val query = Query.builder().from(1).size(10).build();
    val response = diagramRepository.findAll(query);
    assertThat(response.getHits().getTotalHits()).isEqualTo(1);
  }

  @Test
  public void testFind() throws Exception {
    val id = "REACT_163914";
    val query = Query.builder().build();
    val response = diagramRepository.findOne(id, query);
    assertThat(response.get(FIELDS.get("id"))).isEqualTo(id);
  }

}
