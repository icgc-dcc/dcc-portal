/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.server.repository;

import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.common.core.util.Joiners.COMMA;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import org.dcc.portal.pql.meta.Type;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.painless.PainlessPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.test.ESIntegTestCase.Scope;
import org.icgc.dcc.common.es.security.SecurityManagerWorkaroundSeedDecorator;
import org.junit.Before;

import com.carrotsearch.randomizedtesting.annotations.SeedDecorators;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SeedDecorators(value = SecurityManagerWorkaroundSeedDecorator.class)
@ClusterScope(scope = Scope.TEST, numDataNodes = 1, maxNumDataNodes = 1, supportsDedicatedMasters = false, transportClientRatio = 0.0)
public class BaseElasticSearchTest extends ESIntegTestCase {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final ObjectReader READER = MAPPER.readerFor(ObjectNode.class);

  /**
   * Test configuration.
   */
  protected static final String MISSING_ID = "@@@@@@@@@@@";
  protected static final String RELEASE_INDEX_NAME = "test-icgc-release";
  protected static final String REPOSITORY_INDEX_NAME = "test-icgc-repository";
  protected static final String SETTINGS_FILE_NAME = "index.settings.json";
  protected static final String JSON_DIR = "org/icgc/dcc/release/resources/mappings";
  protected static final String REPO_JSON_DIR = "org/icgc/dcc/repository/resources/mappings";
  protected static final String FIXTURES_DIR = "src/test/resources/fixtures";
  protected static final URL SETTINGS_FILE = getMappingFileUrl(SETTINGS_FILE_NAME);

  protected Client client;

  @Before
  public void setUpBaseElasticsearchTest() {
    client = client();
  }

  @Override
  protected Collection<Class<? extends Plugin>> nodePlugins() {
    return ImmutableList.of(PainlessPlugin.class);
  }

  protected void prepareIndex(String indexName, Type... typeNames) {
    createIndexMappings(indexName, typeNames);
    loadData(getClass());
  }

  protected void prepareIndex(String indexName, String file, Type... typeNames) {
    createIndexMappings(indexName, typeNames);
    loadData(file);
  }

  protected void createIndexMappings(String indexName, String... typeNames) {
    val settingsContents = settingsSource(SETTINGS_FILE);
    val settings = Settings.builder()
        .loadFromSource(settingsContents);

    val createBuilder = prepareCreate(indexName, 1, settings);
    for (val typeName : typeNames) {
      log.debug("Creating mapping for type: {}", typeName);
      createBuilder.addMapping(typeName, mappingSource(typeName));
    }

    val created = createBuilder.execute().actionGet().isAcknowledged();
    checkState(created, "Failed to create index");
  }

  protected void createIndexMappings(String indexName, Type... typeNames) {
    val settingsContents = settingsSource(SETTINGS_FILE);
    val settings = Settings.builder()
        .loadFromSource(settingsContents);

    val createBuilder = prepareCreate(indexName, 1, settings);
    for (val typeName : typeNames) {
      log.debug("Creating mapping for type: {}", typeName);
      createBuilder.addMapping(typeName.getId(),
          indexName.equals(REPOSITORY_INDEX_NAME) ? repoMappingSource(typeName) : mappingSource(typeName));
    }

    val created = createBuilder.execute().actionGet().isAcknowledged();
    checkState(created, "Failed to create index");
  }

  @SneakyThrows
  protected void loadData(Class<?> testClass) {
    val fileName = testClass.getSimpleName() + ".json";
    loadData(fileName);
  }

  @SneakyThrows
  protected void loadData(String fileName) {
    val dataFile = new File(FIXTURES_DIR, fileName);
    val iterator = READER.readValues(dataFile);

    while (iterator.hasNext()) {
      val docMetadata = (ObjectNode) iterator.next();
      val indexMetadata = docMetadata.get("index");
      val indexName = indexMetadata.get("_index").textValue();
      val indexType = indexMetadata.get("_type").textValue();
      val indexId = indexMetadata.get("_id").textValue();
      checkState(iterator.hasNext(), "Incorrect format of input test data file. Expected data after document metadata");
      val doc = (ObjectNode) iterator.next();
      val indexRequest = client.prepareIndex(indexName, indexType, indexId).setSource(doc.toString());
      indexRandom(true, indexRequest);
    }
  }

  protected String joinFilters(String... filters) {
    return "{" + COMMA.join(filters) + "}";
  }

  protected Object cast(Object object) {
    return object;
  }

  @SneakyThrows
  private static String settingsSource(URL settingsFile) {
    // Override production values that would introduce test timing delays / issues
    return objectNode(settingsFile)
        .put("index.number_of_shards", 1)
        .put("index.number_of_replicas", 0)
        .toString();
  }

  private static URL getMappingFileUrl(String fileName) {
    return Resources.getResource(JSON_DIR + "/" + fileName);
  }

  private static URL getRepoMappingFileUrl(String fileName) {
    return Resources.getResource(REPO_JSON_DIR + "/" + fileName);
  }

  private static String repoMappingSource(Type typeName) {
    return mappingSource(getRepoMappingFileUrl(typeName.getId() + ".mapping.json"));
  }

  private static String mappingSource(Type typeName) {
    return mappingSource(mappingFile(typeName));
  }

  private static String mappingSource(String typeName) {
    return mappingSource(mappingFile(typeName));
  }

  @SneakyThrows
  private static String mappingSource(URL mappingFile) {
    return json(mappingFile);
  }

  private static URL mappingFile(Type typeName) {
    return mappingFile(typeName.getId());
  }

  private static URL mappingFile(String typeName) {
    String mappingFileName = typeName + ".mapping.json";
    return getMappingFileUrl(mappingFileName);
  }

  private static String json(URL url) throws IOException, JsonProcessingException {
    return objectNode(url).toString();
  }

  private static ObjectNode objectNode(URL url) throws IOException, JsonProcessingException {
    return (ObjectNode) MAPPER.readTree(url);
  }

}
