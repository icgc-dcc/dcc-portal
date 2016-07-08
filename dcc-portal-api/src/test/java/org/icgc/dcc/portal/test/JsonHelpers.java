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
package org.icgc.dcc.portal.test;

import static org.icgc.dcc.portal.test.FixtureHelpers.fixture;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A set of helper methods for testing the serialization and deserialization of classes to and from JSON.
 * <p>
 * For example, a test for reading and writing a {@code Person} object as JSON:
 * </p>
 * 
 * <pre>
 * <code>
 * assertThat("writing a person as JSON produces the appropriate JSON object",
 *            asJson(person),
 *            is(jsonFixture("fixtures/person.json"));
 *
 * assertThat("reading a JSON object as a person produces the appropriate person",
 *            fromJson(jsonFixture("fixtures/person.json"), Person.class),
 *            is(person));
 * </code>
 * </pre>
 */
public class JsonHelpers {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private JsonHelpers() {
    /* singleton */ }

  /**
   * Converts the given object into a canonical JSON string.
   *
   * @param object an object
   * @return {@code object} as a JSON string
   * @throws IllegalArgumentException if there is an error encoding {@code object}
   */
  public static String asJson(Object object) throws IOException {
    return MAPPER.writeValueAsString(object);
  }

  /**
   * Converts the given JSON string into an object of the given type.
   *
   * @param json a JSON string
   * @param klass the class of the type that {@code json} should be converted to
   * @param <T> the type that {@code json} should be converted to
   * @return {@code json} as an instance of {@code T}
   * @throws IOException if there is an error reading {@code json} as an instance of {@code T}
   */
  public static <T> T fromJson(String json, Class<T> klass) throws IOException {
    return MAPPER.readValue(json, klass);
  }

  /**
   * Converts the given JSON string into an object of the given type.
   *
   * @param json a JSON string
   * @param reference a reference of the type that {@code json} should be converted to
   * @param <T> the type that {@code json} should be converted to
   * @return {@code json} as an instance of {@code T}
   * @throws IOException if there is an error reading {@code json} as an instance of {@code T}
   */
  public static <T> T fromJson(String json, TypeReference<T> reference) throws IOException {
    return MAPPER.readValue(json, reference);
  }

  /**
   * Loads the given fixture resource as a normalized JSON string.
   *
   * @param filename the filename of the fixture
   * @return the contents of {@code filename} as a normalized JSON string
   * @throws IOException if there is an error parsing {@code filename}
   */
  public static String jsonFixture(String filename) throws IOException {
    return MAPPER.writeValueAsString(MAPPER.readValue(fixture(filename), JsonNode.class));
  }
}
