/*
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
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

package org.icgc.dcc.portal.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.icgc.dcc.portal.server.model.AlleleMutation;
import org.icgc.dcc.portal.server.model.Beacon;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.repository.BaseElasticSearchTest;
import org.icgc.dcc.portal.server.test.TestIndex;
import org.junit.Before;
import org.junit.Test;

public class BeaconServiceTest extends BaseElasticSearchTest {

  private BeaconService service;

  @Before
  public void setUp() throws Exception {
    this.testIndex = TestIndex.RELEASE;
    es.execute(
        createIndexMappings(IndexType.MUTATION_CENTRIC)
            .withData(bulkFile("BeaconServiceTest.json")));
    service = new BeaconService(es.client(), testIndex.getName());
  }

  @Test
  public void testSuccesfullyFound() {
    Beacon result = service.query("19", 1207014, "GRCh37", new AlleleMutation("-", "T", "T"), "");
    assertThat(result.getResponse().exists).isEqualTo("true");
  }

  @Test
  public void testWrongAlleleFound() {
    Beacon result = service.query("19", 1207014, "GRCh37", new AlleleMutation("-", "C", "C"), "");
    assertThat(result.getResponse().exists).isEqualTo("false");
  }

  @Test
  public void testNothingFound() {
    Beacon result = service.query("11", 11111, "GRCh37", new AlleleMutation("-", "T", "T"), "");
    assertThat(result.getResponse().exists).isEqualTo("null");
  }

  @Test
  public void testSpecificDataset() {
    Beacon result = service.query("11", 11111, "GRCh37", new AlleleMutation("-", "T", "T"), "53049.0");
    assertThat(result.getResponse().exists).isEqualTo("null");

    result = service.query("11", 1207014, "GRCh37", new AlleleMutation("-", "T", "T"), "MADE-UP");
    assertThat(result.getResponse().exists).isEqualTo("null");
  }

}
