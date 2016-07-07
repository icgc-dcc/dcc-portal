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

package org.icgc.dcc.portal.util;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES;
import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.google.common.base.Charsets.UTF_8;
import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.portal.service.BadRequestException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

@NoArgsConstructor(access = PRIVATE)
public final class JsonUtils {

  // @formatter:off
  public static final TypeReference<ArrayList<String>> LIST_TYPE_REFERENCE = new TypeReference<ArrayList<String>>() {{}};
  // @formatter:on

  public static final ObjectMapper MAPPER = new ObjectMapper()
      .enable(INDENT_OUTPUT)
      .configure(ALLOW_UNQUOTED_FIELD_NAMES, true)
      .configure(ALLOW_SINGLE_QUOTES, true)
      .configure(ALLOW_COMMENTS, true);

  public static void moveField(ObjectNode objectNode, String fromFieldName, String toFieldName) {
    if (objectNode.has(fromFieldName)) {
      objectNode.set(toFieldName, objectNode.remove(fromFieldName));
    }
  }

  @SneakyThrows
  public static List<String> parseDownloadDataTypeNames(@NonNull String json) {
    val arrayNode = MAPPER.readValue(json, ArrayNode.class);
    val dataTypeNames = ImmutableList.<String> builder();
    for (val element : arrayNode) {
      val typeName = element.get("key").textValue();
      dataTypeNames.add(typeName);
    }

    return dataTypeNames.build();
  }

  @SneakyThrows
  public static List<String> parseList(String json) {
    return MAPPER.readValue(json, LIST_TYPE_REFERENCE);
  }

  public static JsonNode parseFilters(String filters) {
    String wrappedFilters = filters.replaceFirst("^\\{?", "{").replaceFirst("}?$", "}");
    try {
      String json = URLDecoder.decode(wrappedFilters, UTF_8.name());

      return MAPPER.readTree(json);
    } catch (IOException e) {
      throw new BadRequestException("Bad filter expression: " + filters);
    }
  }

  public static String join(List<String> items) {
    return Joiner.on("','").join(items);
  }

  public static String join(List<String> items, String separator) {
    return Joiner.on(separator).join(items);
  }

  public static ObjectNode merge(JsonNode mainNode, JsonNode updateNode) {
    val node = mainNode.deepCopy();

    Iterator<String> fieldNames = updateNode.fieldNames();
    while (fieldNames.hasNext()) {
      val fieldName = fieldNames.next();
      val jsonNode = node.get(fieldName);
      // if field doesn't exist or is an embedded object
      if (jsonNode != null && jsonNode.isObject()) {
        val updated = merge(jsonNode, updateNode.get(fieldName));
        ((ObjectNode) node).set(fieldName, updated);
      } else {
        if (node instanceof ObjectNode) {
          // Overwrite field
          val value = updateNode.get(fieldName);
          ((ObjectNode) node).set(fieldName, value);
        }
      }
    }

    return (ObjectNode) node;
  }

  public static ObjectNode merge(JsonNode... nodes) {
    JsonNode left = nodes[0];
    for (int i = 1; i < nodes.length; i++) {
      val right = nodes[i];

      left = merge(left, right);
    }

    return (ObjectNode) left;
  }

}
