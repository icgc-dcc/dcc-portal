/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.model;

import static com.yammer.dropwizard.testing.JsonHelpers.jsonFixture;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import lombok.val;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test suite related to data versioning for EnrichmentAnalysis
 */
public class EnrichmentAnalysisTest {

  private final static Class<EnrichmentAnalysis> TEST_TARGET_CLASS = EnrichmentAnalysis.class;
  private final static ObjectMapper MAPPER = new ObjectMapper();

  private final static String TEST_JSON_DIRECTORY = "fixtures/model/EnrichmentAnalysisTest/";
  private final static String TEST_JSON_FILE_FOR_VERSION_1 = TEST_JSON_DIRECTORY + "version1.json";
  private final static String TEST_JSON_FILE_FOR_VERSION_2 = TEST_JSON_DIRECTORY + "version2.json";

  @Test
  public void testVersion1() throws JsonProcessingException, IOException {
    val expectedJsonFileName = TEST_JSON_FILE_FOR_VERSION_1;

    val version1 = MAPPER.readValue(jsonFixture(expectedJsonFileName), TEST_TARGET_CLASS);
    val enrichmentAnalysis = new EnrichmentAnalysis();

    assertThat(enrichmentAnalysis.getVersion()).isEqualTo(version1.getVersion());
  }

  @Test
  public void testVersion2() throws JsonProcessingException, IOException {
    val expectedJsonFileName = TEST_JSON_FILE_FOR_VERSION_2;

    val version2 = MAPPER.readValue(jsonFixture(expectedJsonFileName), TEST_TARGET_CLASS);
    val enrichmentAnalysis = new EnrichmentAnalysis()
        .setVersion(2);

    assertThat(enrichmentAnalysis.getVersion()).isEqualTo(version2.getVersion());
  }

}
