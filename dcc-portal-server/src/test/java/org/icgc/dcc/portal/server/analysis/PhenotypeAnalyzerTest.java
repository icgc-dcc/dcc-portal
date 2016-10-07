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
package org.icgc.dcc.portal.server.analysis;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.stream.Collectors;

import org.dcc.portal.pql.query.QueryEngine;
import org.icgc.dcc.portal.server.config.ServerProperties;
import org.icgc.dcc.portal.server.model.BaseEntitySet;
import org.icgc.dcc.portal.server.model.EntitySet;
import org.icgc.dcc.portal.server.model.EntitySet.State;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.repository.BaseElasticSearchTest;
import org.icgc.dcc.portal.server.repository.DonorRepository;
import org.icgc.dcc.portal.server.repository.EntitySetRepository;
import org.icgc.dcc.portal.server.repository.TermsLookupRepository;
import org.icgc.dcc.portal.server.test.TestIndex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import lombok.val;

@RunWith(MockitoJUnitRunner.class)
public class PhenotypeAnalyzerTest extends BaseElasticSearchTest {

  PhenotypeAnalyzer phenotypeAnalyzer;
  DonorRepository donorRepository;
  @Mock
  EntitySetRepository entitySetRepository;

  @Before
  public void setUp() throws Exception {
    this.testIndex = TestIndex.RELEASE;

    val set = new EntitySet(UUID.randomUUID(), State.FINISHED, 200L, "test", "test", BaseEntitySet.Type.DONOR, 1);
    when(entitySetRepository.find(any())).thenReturn(set);

    es.execute(createIndexMappings(IndexType.DONOR, IndexType.DONOR_CENTRIC)
        .withData(bulkFile(getClass()))
        // This is needed because the DonorRepository now does a 'secondary' search on icgc-repository index.
        .withData(MANIFEST_TEST_DATA));
    donorRepository =
        new DonorRepository(es.client(), new QueryEngine(es.client(), testIndex.getName()),
            TestIndex.RELEASE.getName(), TestIndex.REPOSITORY.getName());

    phenotypeAnalyzer = new PhenotypeAnalyzer(donorRepository, entitySetRepository);
  }

  @Test
  public void testPhenotypeAnalysis() {
    val id1 = UUID.randomUUID();
    val id2 = UUID.randomUUID();
    setUpTermsLookup(id1, id2);

    val results = phenotypeAnalyzer.getPhenotypeAnalysisResult(newArrayList(id1, id2));

    // Here we are only interested in results that contain stats, which is under "ageAtDiagnosisGroup", and we use
    // flatMap to flatten/unwrap the result to a simple list.
    val data = results.stream()
        .filter(result -> result.getName().equals("ageAtDiagnosisGroup"))
        .flatMap(result -> result.getData().stream())
        .collect(Collectors.toList());

    assertThat(data.size()).isEqualTo(2);

    for (val analysisResult : data) {
      val uuid = analysisResult.getId();
      val mean = analysisResult.getSummary().getMean();

      if (uuid.equals(id1)) {
        assertThat(mean).isEqualTo(109.6);
      } else if (uuid.equals(id2)) {
        assertThat(mean).isEqualTo(135.66666666666666);
      } else {
        fail("Encountered an unexpected UUID in query result.");
      }
    }

  }

  private void setUpTermsLookup(final UUID id1, final UUID id2) {
    val termsLookupRepository =
        new TermsLookupRepository(es.client(), TestIndex.RELEASE.getName(), TestIndex.REPOSITORY.getName(),
            new ServerProperties());
    val lookupType = TermsLookupRepository.TermLookupType.DONOR_IDS;

    val donorSet1 = newArrayList("DO1", "DO3", "DO5", "DO7", "DO9");
    termsLookupRepository.createTermsLookup(lookupType, id1, donorSet1);

    val donorSet2 = newArrayList("DO2", "DO4", "DO5", "DO6", "DO8");
    termsLookupRepository.createTermsLookup(lookupType, id2, donorSet2);
  }

}
