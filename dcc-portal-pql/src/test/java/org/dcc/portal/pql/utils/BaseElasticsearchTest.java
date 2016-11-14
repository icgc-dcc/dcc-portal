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

import static com.google.common.base.Preconditions.checkState;
import static org.dcc.portal.pql.utils.Tests.createEsAst;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.assertj.core.api.Assertions;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.utils.EsAstTransformer;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.query.QueryContext;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.painless.PainlessPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.test.ESIntegTestCase.Scope;
import org.junit.Before;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

@Slf4j
@ClusterScope(scope = Scope.TEST, numDataNodes = 1, maxNumDataNodes = 1)
public class BaseElasticsearchTest extends ESIntegTestCase {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ObjectReader READER = MAPPER.readerFor(ObjectNode.class);

  /**
   * Test configuration.
   */
  protected static final String INDEX_NAME = "dcc-release-etl-cli";
  protected static final String SETTINGS_FILE_NAME = "test-index.settings.json";
  protected static final String JSON_DIR = "org/icgc/dcc/release/resources/mappings";
  protected static final String FIXTURES_DIR = "src/test/resources/fixtures";
  protected static final String SETTINGS_FILE = getIndexFileUrl();

  /**
   * Parser's setup
   */
  protected QueryContext queryContext;
  protected EsAstTransformer esAstTransformator = new EsAstTransformer();

  protected Client client;

  @Before
  public void setUpBaseElasticsearchTest() {
    client = client();
  }

  @Override
  protected Collection<Class<? extends Plugin>> nodePlugins() {
    return ImmutableList.of(PainlessPlugin.class);
  }

  protected void prepareIndex(Type... typeNames) {
    createIndexMappings(typeNames);
    loadData(getClass());
  }

  protected void createIndexMappings(Type... typeNames) {
    // TODO: pull settings from file
    val settings = Settings.builder()
        .put("index.number_of_shards", 1)
        .put("index.number_of_replicas", 0);

    val createBuilder = prepareCreate(INDEX_NAME, 1, settings);
    for (val typeName : typeNames) {
      log.debug("Creating mapping for type: {}", typeName);
      createBuilder.addMapping(typeName.getId(), mappingSource(typeName));

    }

    val created = createBuilder.execute().actionGet().isAcknowledged();
    checkState(created, "Failed to create index");
  }

  @SneakyThrows
  protected void loadData(Class<?> testClass) {
    val dataFile = new File(FIXTURES_DIR, testClass.getSimpleName() + ".txt");
    val iterator = READER.readValues(dataFile);

    while (iterator.hasNext()) {
      val docMetadata = (ObjectNode) iterator.next();
      val indexMetadata = docMetadata.get("index");
      val indexType = indexMetadata.get("_type").textValue();
      val indexId = indexMetadata.get("_id").textValue();
      checkState(iterator.hasNext(), "Incorrect format of input test data file. Expected data after document metadata");
      val doc = (ObjectNode) iterator.next();
      val indexRequest = client.prepareIndex(INDEX_NAME, indexType, indexId).setSource(doc.toString());
      indexRandom(true, indexRequest);
    }
  }

  private static URL getMappingFileUrl(String fileName) {
    return Resources.getResource(JSON_DIR + "/" + fileName);
  }

  private static String getIndexFileUrl() {
    return new File(FIXTURES_DIR, SETTINGS_FILE_NAME).getAbsolutePath();
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
    return (ObjectNode) MAPPER.readTree(url);
  }

  protected ExpressionNode createTree(String query) {
    return createEsAst(query, queryContext);
  }

  public static void containsOnlyIds(SearchResponse response, String... ids) {
    val resopnseIds = Lists.<String> newArrayList();

    for (val hit : response.getHits()) {
      resopnseIds.add(hit.getId());
    }
    Assertions.assertThat(resopnseIds).containsOnly(ids);
  }

  public static void assertTotalHitsCount(SearchResponse response, int expectedCount) {
    Assertions.assertThat(response.getHits().getTotalHits()).isEqualTo(expectedCount);
  }

}
