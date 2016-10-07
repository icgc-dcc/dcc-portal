/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not), see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES), INCLUDING), BUT NOT LIMITED TO), THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT), INDIRECT), INCIDENTAL), SPECIAL), EXEMPLARY), OR CONSEQUENTIAL
 * DAMAGES (INCLUDING), BUT NOT LIMITED TO), PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE),
 * DATA), OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY),
 * WHETHER IN CONTRACT), STRICT LIABILITY), OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE), EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.server.model;

import static org.icgc.dcc.portal.server.test.JsonHelpers.asJson;
import static org.icgc.dcc.portal.server.test.JsonHelpers.jsonFixture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class ProjectTest {

  private static final ImmutableMap<String, String> FIELDS = FIELDS_MAPPING.get(EntityType.PROJECT);

  private final ObjectMapper MAPPER = new ObjectMapper();

  public static final ImmutableMap<String, Object> OVAU = new ImmutableMap.Builder<String, Object>()
      .put(FIELDS.get("id"), "OV-AU")
      .put(FIELDS.get("icgcId"), "809")
      .put(FIELDS.get("primarySite"), "Ovary")
      .put(FIELDS.get("name"), "Ovarian Cancer - AU")
      .put(FIELDS.get("tumourType"), "Ovarian cancer")
      .put(FIELDS.get("tumourSubtype"), "Serous cystadenocarcinoma")
      .put(FIELDS.get("pubmedIds"), Lists.newArrayList("222222", "333333"))
      .put(FIELDS.get("primaryCountries"), Lists.newArrayList("Canada", "Australia"))
      .put(FIELDS.get("partnerCountries"), Lists.newArrayList("Canada", "Australia"))
      .put(FIELDS.get("ssmTestedDonorCount"), 3)
      .put(FIELDS.get("cnsmTestedDonorCount"), 0)
      .put(FIELDS.get("stsmTestedDonorCount"), 0)
      .put(FIELDS.get("sgvTestedDonorCount"), 0)
      .put(FIELDS.get("methSeqTestedDonorCount"), 0)
      .put(FIELDS.get("methArrayTestedDonorCount"), 0)
      .put(FIELDS.get("expSeqTestedDonorCount"), 0)
      .put(FIELDS.get("expArrayTestedDonorCount"), 0)
      .put(FIELDS.get("pexpTestedDonorCount"), 0)
      .put(FIELDS.get("mirnaSeqTestedDonorCount"), 0)
      .put(FIELDS.get("jcnTestedDonorCount"), 0)
      .put(FIELDS.get("totalDonorCount"), 3)
      .put(FIELDS.get("availableDataTypes"), Lists.newArrayList("ssm", "cnsm"))
      .put(FIELDS.get("experimentalAnalysisPerformedDonorCounts"), ImmutableMap.of("WXS", 333, "WGS", 222))
      .put(FIELDS.get("experimentalAnalysisPerformedSampleCounts"), ImmutableMap.of("WXS", 333, "WGS", 222))
      .put(FIELDS.get("repository"), Lists.newArrayList())
      .build();

  public static final ImmutableMap<String, Object> PEMECA = new ImmutableMap.Builder<String, Object>()
      .put(FIELDS.get("id"), "PEME-CA")
      .put(FIELDS.get("icgcId"), "71589")
      .put(FIELDS.get("primarySite"), "Brain")
      .put(FIELDS.get("name"), "Pediatric Medulloblastoma - CA")
      .put(FIELDS.get("tumourType"), "Brain cancer")
      .put(FIELDS.get("tumourSubtype"), "Pediatric medulloblastoma")
      .put(FIELDS.get("pubmedIds"), Lists.newArrayList("222222", "333333"))
      .put(FIELDS.get("primaryCountries"), Lists.newArrayList("Canada", "Australia"))
      .put(FIELDS.get("partnerCountries"), Lists.newArrayList("Canada", "Australia"))
      .put(FIELDS.get("ssmTestedDonorCount"), 3)
      .put(FIELDS.get("cnsmTestedDonorCount"), 0)
      .put(FIELDS.get("stsmTestedDonorCount"), 0)
      .put(FIELDS.get("sgvTestedDonorCount"), 0)
      .put(FIELDS.get("methSeqTestedDonorCount"), 0)
      .put(FIELDS.get("methArrayTestedDonorCount"), 0)
      .put(FIELDS.get("expSeqTestedDonorCount"), 0)
      .put(FIELDS.get("expArrayTestedDonorCount"), 0)
      .put(FIELDS.get("pexpTestedDonorCount"), 0)
      .put(FIELDS.get("mirnaSeqTestedDonorCount"), 0)
      .put(FIELDS.get("jcnTestedDonorCount"), 0)
      .put(FIELDS.get("totalDonorCount"), 3)
      .put(FIELDS.get("availableDataTypes"), Lists.newArrayList("ssm", "cnsm"))
      .put(FIELDS.get("experimentalAnalysisPerformedDonorCounts"), ImmutableMap.of("WXS", 333, "WGS", 222))
      .put(FIELDS.get("experimentalAnalysisPerformedSampleCounts"), ImmutableMap.of("WXS", 333, "WGS", 222))
      .put(FIELDS.get("repository"), Lists.newArrayList())
      .build();

  final Project project = new Project(OVAU);

  @Test
  public void serializesToJSON() throws Exception {
    // readTree turns the JSON strings into ObjectNode
    // used to ignore field order
    assertThat(MAPPER.readTree(asJson(project)))
        .isEqualTo(MAPPER.readTree(jsonFixture("fixtures/model/ProjectOVAU.json")));
  }
}