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

import static com.github.tlrx.elasticsearch.test.EsSetup.createIndex;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.common.core.util.Joiners.COMMA;
import static org.icgc.dcc.portal.server.util.JsonUtils.parseFilters;

import java.io.File;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.test.TestIndex;
import org.junit.After;
import org.junit.Before;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tlrx.elasticsearch.test.EsSetup;
import com.github.tlrx.elasticsearch.test.provider.JSONProvider;
import com.github.tlrx.elasticsearch.test.request.CreateIndex;
import com.google.common.io.Files;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@RequiredArgsConstructor
public abstract class BaseElasticSearchTest {

  /**
   * Test configuration.
   */
  protected static final String FIXTURES_DIR = "src/test/resources/fixtures";

  /**
   * Test data.
   */
  protected static final String MISSING_ID = "@@@@@@@@@@@";
  protected static final BulkJSONProvider MANIFEST_TEST_DATA = bulkFile("ManifestServiceTest.json");

  /**
   * The index to test.
   */
  protected TestIndex testIndex;

  /**
   * ES facade.
   */
  protected EsSetup es;

  @Before
  public void before() {
    val settings = ImmutableSettings.settingsBuilder().put("script.groovy.sandbox.enabled", true).build();
    es = new EsSetup(settings);
  }

  @After
  public void after() {
    es.terminate();
  }

  protected String joinFilters(String... filters) {
    return "{" + COMMA.join(filters) + "}";
  }

  @SneakyThrows
  public void bulkInsert(JSONProvider provider) {
    byte[] content = provider.toJson().getBytes("UTF-8");
    checkState(!es.client()
        .prepareBulk()
        .add(content, 0, content.length, true)
        .setRefresh(true)
        .execute()
        .actionGet()
        .hasFailures());
  }

  @SneakyThrows
  public void bulkInsert() {
    bulkInsert(bulkFile(getClass()));
  }

  /**
   * Creates the index, settings and {@code TypeName} mapping
   * 
   * @param type - the index type to create
   * @return
   */
  protected CreateIndex createIndexMapping(IndexType type) {
    return createIndexMappings(type);
  }

  protected CreateIndex createIndexMappings(IndexType... types) {
    CreateIndex request = createIndex(testIndex.getName())
        .withSettings(testIndex.getSettings());

    for (IndexType type : types) {
      request = request.withMapping(type.getId(), testIndex.getMapping(type));
    }

    return request;
  }

  protected static FileJSONProvider jsonFile(File file) {
    return new FileJSONProvider(file);
  }

  protected static BulkJSONProvider bulkFile(File file) {
    return new BulkJSONProvider(file);
  }

  protected static BulkJSONProvider bulkFile(String fileName) {
    return new BulkJSONProvider(new File(FIXTURES_DIR, fileName));
  }

  protected static BulkJSONProvider bulkFile(Class<?> testClass) {
    return new BulkJSONProvider(new File(FIXTURES_DIR, testClass.getSimpleName() + ".json"));
  }

  protected static ObjectNode filters(String jsonish) {
    return (ObjectNode) parseFilters(jsonish);
  }

  /**
   * {@link JSONProvider} implementation that can read files from the local file system.
   */
  @RequiredArgsConstructor
  private static class FileJSONProvider implements JSONProvider {

    @NonNull
    private final File file;

    @Override
    @SneakyThrows
    public String toJson() {
      return Files.toString(file, UTF_8);
    }

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
      ObjectReader reader = new ObjectMapper().readerFor(JsonNode.class);
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

  protected Object cast(Object object) {
    return object;
  }

}
