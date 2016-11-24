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

import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Ignore
@Slf4j
public class RepositoryTest extends BaseElasticsearchTest {

  @Before
  public void setUpRepositoryTest() throws Exception {
    prepareIndex(RELEASE_INDEX_NAME, GENE_CENTRIC);
    loadData("GeneRepositoryTest.json");
  }

  @Test
  public void test() throws Exception {
    val result = client.prepareSearch(RELEASE_INDEX_NAME).setTypes("gene-centric").execute().actionGet();
    log.info("Result: {}", result);
  }

}
