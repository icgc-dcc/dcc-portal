/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.util;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.repeat;
import static com.google.common.base.Throwables.propagate;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@Slf4j
@RequiredArgsConstructor
public class MappingsReader {

  /**
   * Configuration.
   */
  @NonNull
  private final String indexName;

  /**
   * Dependencies.
   */
  @NonNull
  private final MappingsSourceResolver resolver;

  public Mappings read() throws IOException {
    val mappings = ImmutableList.<Mapping> builder();
    val sources = resolveSources();
    for (val entry : sources.entrySet()) {
      val type = entry.getKey();
      val source = entry.getValue();

      val fields = getMappingFields(source);
      mappings.add(new Mapping(type, fields));
    }

    return new Mappings(indexName, mappings.build());
  }

  private Map<String, ObjectNode> resolveSources() throws IOException {
    return resolver.resolve(indexName);
  }

  private List<Field> getMappingFields(ObjectNode source) {
    return processMappingNode(0, source);
  }

  private List<Field> processMappingNode(int level, ObjectNode node) {
    val fields = ImmutableList.<Field> builder();
    val properties = node.path("properties");
    if (properties.isObject()) {
      return processMappingNode(level + 1, (ObjectNode) properties);
    } else {
      val iterator = node.fieldNames();
      while (iterator.hasNext()) {
        val name = iterator.next();
        val definition = node.get(name);
        val type = getFieldType(definition);

        try {
          log.info("{}{}: {}", new Object[] { repeat(" ", level), name, type });
          if (definition.isObject() && definition.has("properties")) {
            fields.add(new Field(name, type, processMappingNode(level + 1, (ObjectNode) definition)));
          } else {
            fields.add(new Field(name, type, Collections.<Field> emptyList()));
          }
        } catch (Exception e) {
          log.error("Error processing field " + name + " " + definition, e);
          propagate(e);
        }
      }

      return fields.build();
    }
  }

  private static FieldType getFieldType(JsonNode definition) {
    val type = definition.path("type");
    val typeName = type.isMissingNode() ? "object" : type.asText();

    if (typeName.equals("object")) {
      return FieldType.OBJECT;
    } else if (typeName.equals("boolean")) {
      return FieldType.BOOLEAN;
    } else if (typeName.equals("double")) {
      return FieldType.DOUBLE;
    } else if (typeName.equals("nested")) {
      return FieldType.NESTED;
    } else if (typeName.equals("string")) {
      return FieldType.STRING;
    } else if (typeName.equals("long")) {
      return FieldType.LONG;
    } else if (typeName.equals("multi_field")) {
      return FieldType.MULTI;
    } else {
      return null;
    }
  }

  @Value
  public static class Mappings implements Iterable<Mapping> {

    private final String indexName;
    private final Map<String, Mapping> mappings;

    public Mappings(@NonNull String indexName, @NonNull Iterable<Mapping> mappings) {
      val builder = ImmutableMap.<String, Mapping> builder();
      for (val mapping : mappings) {
        builder.put(mapping.getType(), mapping);
      }

      this.indexName = indexName;
      this.mappings = builder.build();
    }

    public Mapping getMapping(@NonNull String type) {
      return mappings.get(type);
    }

    public Field getField(@NonNull String type, @NonNull String fieldName) {
      checkState(!fieldName.contains("."), "Field names cannot be paths");

      return mappings.get(type).getField(fieldName);
    }

    public Field getPath(@NonNull String type, @NonNull String path) {
      return mappings.get(type).getPath(path);
    }

    @Override
    public Iterator<Mapping> iterator() {
      return mappings.values().iterator();
    }

  }

  @Value
  public static class Mapping implements Iterable<Field> {

    String type;
    Map<String, Field> fields;

    public Mapping(@NonNull String type, @NonNull Iterable<Field> fields) {
      val builder = ImmutableMap.<String, Field> builder();
      for (val field : fields) {
        builder.put(field.getName(), field);
      }

      this.type = type;
      this.fields = builder.build();
    }

    public Field getField(@NonNull String fieldName) {
      checkState(!fieldName.contains("."), "Field names cannot be paths");

      return fields.get(fieldName);
    }

    public Field getPath(@NonNull String path) {
      String[] parts = path.split("\\.");
      Field field = getField(parts[0]);

      for (int i = 1; i < parts.length; i++) {
        val fieldName = parts[i];
        field = field.getField(fieldName);
      }

      return field;
    }

    @Override
    public Iterator<Field> iterator() {
      return fields.values().iterator();
    }

  }

  @Value
  public static class Field implements Iterable<Field> {

    String name;
    FieldType type;
    Map<String, Field> fields;

    public Field(@NonNull String name, @NonNull FieldType type, @NonNull Iterable<Field> fields) {
      val builder = ImmutableMap.<String, Field> builder();
      for (val field : fields) {
        builder.put(field.getName(), field);
      }

      this.name = name;
      this.type = type;
      this.fields = builder.build();
    }

    public Field getField(@NonNull String fieldName) {
      checkState(!fieldName.contains("."), "Field names cannot be paths");

      return fields.get(fieldName);
    }

    public Field getPath(@NonNull String path) {
      String[] parts = path.split("\\.");
      Field field = getField(parts[0]);
      if (parts.length == 1) {
        return field;
      }

      for (int i = 1; i < parts.length; i++) {
        val fieldName = parts[i];
        field = field.getField(fieldName);
      }

      return field;
    }

    @Override
    public Iterator<Field> iterator() {
      return fields.values().iterator();
    }

  }

  public enum FieldType {

    BOOLEAN,
    STRING,
    DOUBLE,
    LONG,
    OBJECT,
    NESTED,
    MULTI;

    public boolean isBoolean() {
      return this == BOOLEAN;
    }

    public boolean isString() {
      return this == STRING;
    }

    public boolean isDouble() {
      return this == DOUBLE;
    }

    public boolean isLong() {
      return this == LONG;
    }

    public boolean isObject() {
      return this == OBJECT;
    }

    public boolean isNested() {
      return this == NESTED;
    }

    public boolean isMulti() {
      return this == MULTI;
    }

  }

}
