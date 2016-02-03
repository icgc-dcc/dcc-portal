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

import static com.yammer.dropwizard.testing.JsonHelpers.asJson;
import static com.yammer.dropwizard.testing.JsonHelpers.jsonFixture;
import static org.assertj.core.api.Assertions.assertThat;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test suite for RepositoryFile class
 */
@Slf4j
public class RepositoryFileTest {

  private static final Class<RepositoryFile> TEST_TARGET_CLASS = RepositoryFile.class;
  private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();
  private static final ObjectMapper CLASS_MAPPER = RepositoryFile.createMapper(); // configured with strategy

  private static final String TEST_JSON_DIRECTORY = "fixtures/model/";
  private static final String TEST_JSON_FILE =
      TEST_JSON_DIRECTORY + RepositoryFileTest.class.getSimpleName() + ".json";

  @Test
  @SneakyThrows
  public void test() {
    val jsonFixture = jsonFixture(TEST_JSON_FILE);
    val testPojo = RepositoryFile.parse(jsonFixture);

    log.info("JSON read from fixture file: '{}'", jsonFixture);
    log.info("JSON serialized from Pojo: '{}'", asJson(testPojo));

    runTestWith(testPojo, TEST_JSON_FILE);
  }

  @SneakyThrows
  private static void runTestWith(RepositoryFile testPojo, String jsonFixtureFilePath) {
    val jsonNode = DEFAULT_MAPPER.readTree(jsonFixture(jsonFixtureFilePath));
    val expectedPojo = CLASS_MAPPER.readValue(asJson(jsonNode), TEST_TARGET_CLASS);

    assertThat(toJsonNode(testPojo))
        .isEqualTo(toJsonNode(expectedPojo));
  }

  @SneakyThrows
  private static JsonNode toJsonNode(RepositoryFile pojo) {
    return DEFAULT_MAPPER.readTree(asJson(pojo));
  }
}
