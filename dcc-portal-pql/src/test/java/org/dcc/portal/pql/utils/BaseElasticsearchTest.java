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

package org.dcc.portal.pql.utils;

import static com.github.tlrx.elasticsearch.test.EsSetup.createIndex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.utils.Tests.createEsAst;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.utils.EsAstTransformer;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.query.QueryContext;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.junit.After;
import org.junit.Before;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tlrx.elasticsearch.test.EsSetup;
import com.github.tlrx.elasticsearch.test.provider.JSONProvider;
import com.github.tlrx.elasticsearch.test.request.CreateIndex;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

public class BaseElasticsearchTest {

  /**
   * Test configuration.
   */
  protected static final String INDEX_NAME = "dcc-release-etl-cli";
  protected static final String SETTINGS_FILE_NAME = "index.settings.json";
  protected static final String JSON_DIR = "org/icgc/dcc/etl/resources/mappings";
  protected static final String FIXTURES_DIR = "src/test/resources/fixtures";
  protected static final URL SETTINGS_FILE = getMappingFileUrl(SETTINGS_FILE_NAME);

  @SneakyThrows
  private static URL getMappingFileUrl(String fileName) {
    return Resources.getResource(JSON_DIR + "/" + fileName);
  }

  /**
   * Test data.
   */
  protected static final String MISSING_ID = "@@@@@@@@@@@";

  /**
   * ES facade.
   */
  protected EsSetup es;

  /**
   * Parser's setup
   */
  protected QueryContext queryContext;
  protected EsAstTransformer esAstTransformator = new EsAstTransformer();

  @Before
  public void before() {
    val settings = ImmutableSettings.settingsBuilder().put("script.groovy.sandbox.enabled", true).build();
    es = new EsSetup(settings);
  }

  @After
  public void after() {
    es.terminate();
  }

  protected static CreateIndex createIndexMappings(Type... typeNames) {
    CreateIndex request = createIndex(INDEX_NAME)
        .withSettings(settingsSource(SETTINGS_FILE));

    for (Type typeName : typeNames) {
      request = request.withMapping(typeName.getId(), mappingSource(typeName));
    }

    return request;
  }

  protected static BulkJSONProvider bulkFile(Class<?> testClass) {
    return new BulkJSONProvider(new File(FIXTURES_DIR, testClass.getSimpleName() + ".txt"));
  }

  @SneakyThrows
  private static String settingsSource(URL settingsFile) {
    // Override production values that would introduce test timing delays / issues
    return objectNode(settingsFile)
        .put("index.number_of_shards", 1)
        .put("index.number_of_replicas", 0)
        .toString();
  }

  private static String mappingSource(Type typeName) {
    return mappingSource(mappingFile(typeName));
  }

  @SneakyThrows
  private static String mappingSource(URL mappingFile) {
    return json(mappingFile);
  }

  private static URL mappingFile(Type typeName) {
    String mappingFileName = typeName.getId() + ".mapping.json";
    return getMappingFileUrl(mappingFileName);
  }

  private static String json(URL url) throws IOException, JsonProcessingException {
    return objectNode(url).toString();
  }

  private static ObjectNode objectNode(URL url) throws IOException, JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return (ObjectNode) mapper.readTree(url);
  }

  /**
   * {@link JSONProvider} implementation that can read formatted concatenated Elasticsearch bulk load files as specified
   * in http://www.elasticsearch.org/guide/reference/api/bulk/.
   */
  @RequiredArgsConstructor
  protected static class BulkJSONProvider implements JSONProvider {

    @NonNull
    private final File file;

    @Override
    @SneakyThrows
    public String toJson() {
      // Normalize to non-pretty printed in memory representation
      ObjectReader reader = new ObjectMapper().reader(JsonNode.class);
      MappingIterator<JsonNode> iterator = reader.readValues(file);

      StringBuilder builder = new StringBuilder();
      while (iterator.hasNext()) {
        // Write non-pretty printed
        JsonNode jsonNode = iterator.nextValue();
        builder.append(jsonNode);
        builder.append("\n");
      }

      return builder.toString();
    }
  }

  protected ExpressionNode createTree(String query) {
    return createEsAst(query, queryContext);
  }

  public static void containsOnlyIds(SearchResponse response, String... ids) {
    val resopnseIds = Lists.<String> newArrayList();

    for (val hit : response.getHits()) {
      resopnseIds.add(hit.getId());
    }
    assertThat(resopnseIds).containsOnly(ids);
  }

  public static void assertTotalHitsCount(SearchResponse response, int expectedCount) {
    assertThat(response.getHits().getTotalHits()).isEqualTo(expectedCount);
  }

}
