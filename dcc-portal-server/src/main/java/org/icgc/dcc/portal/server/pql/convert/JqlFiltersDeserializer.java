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
package org.icgc.dcc.portal.server.pql.convert;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.dcc.portal.pql.util.Converters.isString;
import static org.icgc.dcc.portal.server.model.IndexType.DONOR;
import static org.icgc.dcc.portal.server.model.IndexType.DRUG;
import static org.icgc.dcc.portal.server.model.IndexType.FILE;
import static org.icgc.dcc.portal.server.model.IndexType.GENE;
import static org.icgc.dcc.portal.server.model.IndexType.MUTATION;
import static org.icgc.dcc.portal.server.model.IndexType.PROJECT;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.dcc.portal.pql.exception.SemanticException;
import org.icgc.dcc.portal.server.pql.convert.model.JqlArrayValue;
import org.icgc.dcc.portal.server.pql.convert.model.JqlField;
import org.icgc.dcc.portal.server.pql.convert.model.JqlFilters;
import org.icgc.dcc.portal.server.pql.convert.model.JqlSingleValue;
import org.icgc.dcc.portal.server.pql.convert.model.JqlValue;
import org.icgc.dcc.portal.server.pql.convert.model.Operation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JqlFiltersDeserializer extends JsonDeserializer<JqlFilters> {

  private static final List<String> VALID_TYPES = ImmutableList.of(
      DONOR.getId(),
      GENE.getId(),
      DRUG.getId(),
      MUTATION.getId(),
      PROJECT.getId(),
      FILE.getId());

  @Override
  public JqlFilters deserialize(@NonNull JsonParser jp, @NonNull DeserializationContext ctxt) throws IOException,
      JsonProcessingException {
    configureJsonParser(jp);

    JsonNode node = jp.getCodec().readTree(jp);
    log.debug("Deserializing node: {}", node);
    validateTypes(node);
    val elements = new ImmutableMap.Builder<String, List<JqlField>>();

    val nodeFields = node.fields();
    while (nodeFields.hasNext()) {
      elements.putAll(parseType(nodeFields.next()));
    }

    return new JqlFilters(elements.build());
  }

  private static Map<String, List<JqlField>> parseType(Entry<String, JsonNode> entry) {
    val type = entry.getKey();
    log.debug("Parsing fields for type '{}'. Fields: {}", type, entry.getValue());

    val typeFieldsBuilder = new ImmutableList.Builder<JqlField>();
    val nodeFields = entry.getValue().fields();
    while (nodeFields.hasNext()) {
      val jqlField = parseField(type, nodeFields.next());
      if (jqlField.isPresent()) {
        typeFieldsBuilder.add(jqlField.get());
      }

    }

    val typeFields = typeFieldsBuilder.build();

    return typeFields.isEmpty() ? emptyMap() : singletonMap(type, typeFields);
  }

  private static Optional<JqlField> parseField(String type, Entry<String, JsonNode> next) {
    val fieldName = next.getKey();
    val fieldValue = next.getValue();
    log.debug("Parsing field {} - {}", fieldName, fieldValue);

    if (fieldName.startsWith("has")) {
      return parseHasOperationField(type, fieldName, fieldValue);
    }

    /*
     * In the case of an empty value "{}", just ignore.
     */
    if (!fieldValue.fields().hasNext()) {
      return Optional.empty();
    }

    try {
      val value = parseValue(fieldValue);

      return hasValue(value) ? Optional.of(new JqlField(fieldName, parseOperation(fieldValue), value, type)) : Optional
          .empty();
    } catch (NullPointerException e) {
      throw new SemanticException("Invalid input value or structure: %s", fieldValue);
    }

  }

  private static Optional<JqlField> parseHasOperationField(String type, String fieldName, JsonNode fieldValue) {
    val value = parseSingleValue(fieldValue);
    if (hasValue(value)) {
      return Optional.of(new JqlField(fieldName, Operation.HAS, value, type));
    }

    return Optional.empty();
  }

  private static boolean hasValue(JqlValue value) {
    return value.isArray() ? hasArrayValue(value) : hasSingleValue(value);
  }

  private static boolean hasSingleValue(JqlValue value) {
    val _value = value.get();
    if (isString(_value) && isNullOrEmpty((String) _value)) {
      return false;
    }

    return true;
  }

  private static boolean hasArrayValue(JqlValue value) {
    val arrayValue = (JqlArrayValue) value;
    if (arrayValue.get().isEmpty()) {
      return false;
    }

    return true;
  }

  private static JqlValue parseValue(JsonNode fieldValue) {
    log.debug("Parsing value for {}", fieldValue);
    val nodeValue = getFirstValue(fieldValue);
    if (nodeValue.isArray()) {
      return parseArrayValue(nodeValue);
    }

    return parseSingleValue(nodeValue);
  }

  private static JqlValue parseArrayValue(JsonNode node) {
    log.debug("Parsing array value for {}", node);
    val result = new ImmutableList.Builder<Object>();
    val elements = node.elements();
    while (elements.hasNext()) {
      result.add(parseJsonNodeValue(elements.next()));
    }

    return new JqlArrayValue(result.build());
  }

  private static JqlValue parseSingleValue(JsonNode node) {
    log.debug("Parsing single value for {}", node);
    return new JqlSingleValue(parseJsonNodeValue(node));
  }

  private static Operation parseOperation(JsonNode fieldValue) {
    validateOperation(fieldValue);
    log.debug("Parsing operation for {}", fieldValue);

    return Operation.byId(getFirstFieldName(fieldValue));
  }

  private static void configureJsonParser(JsonParser parser) {
    parser.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    parser.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
  }

  private static void validateTypes(JsonNode node) {
    val fieldNames = node.fieldNames();
    while (fieldNames.hasNext()) {
      checkSemantic(VALID_TYPES.contains(fieldNames.next()), "Node has no valid types. %s", node);
    }
  }

  private static void validateOperation(JsonNode fieldValue) {
    log.debug("Validating operation for {}", fieldValue);
    checkSemantic(fieldValue.size() == 1, "More than one operation detected. %s", fieldValue);
    val operation = getFirstFieldName(fieldValue);
    checkSemantic(Operation.operations().contains(operation), "Invalid operation '%s'", operation);
  }

  /**
   * SemanticException thrown to respond with a 400 error to client.
   */
  private static void checkSemantic(boolean expression, String message, Object... args) {
    if (!expression) {
      throw new SemanticException(message, args);
    }
  }

  private static String getFirstFieldName(JsonNode fieldValue) {
    return fieldValue.fieldNames().next();
  }

  private static JsonNode getFirstValue(@NonNull JsonNode jsonNode) {
    val entry = jsonNode.fields().next();

    return entry.getValue();
  }

  private static Object parseJsonNodeValue(JsonNode node) {
    if (node.isInt()) {
      return node.asInt();
    }

    if (node.isLong()) {
      return node.asLong();
    }

    if (node.isDouble()) {
      return node.asDouble();
    }

    if (node.isBoolean()) {
      return node.asBoolean();
    }

    return node.textValue();
  }

}
