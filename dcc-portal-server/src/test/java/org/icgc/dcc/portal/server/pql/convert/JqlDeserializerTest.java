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

import static org.assertj.core.api.Assertions.assertThat;

import org.dcc.portal.pql.exception.SemanticException;
import org.icgc.dcc.portal.server.pql.convert.model.JqlArrayValue;
import org.icgc.dcc.portal.server.pql.convert.model.JqlFilters;
import org.icgc.dcc.portal.server.pql.convert.model.Operation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Iterables;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JqlDeserializerTest {

  ObjectMapper mapper = createObjectMapper();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void multiTypeTest() {
    val result = convert("{donor:{age:{not:100}}, gene:{id:{is:'GE1'}}}");

    assertThat(result.getEntityValues().size()).isEqualTo(2);

    val donorType = result.getEntityValues().get("donor");
    assertThat(donorType.size()).isEqualTo(1);
    val age = Iterables.get(donorType, 0);
    assertThat(age.getName()).isEqualTo("age");
    assertThat(age.getOperation()).isEqualTo(Operation.NOT);
    assertThat(age.getValue().isArray()).isFalse();
    assertThat(age.getValue().get()).isEqualTo(100);

    val geneType = result.getEntityValues().get("gene");
    assertThat(geneType.size()).isEqualTo(1);
    val id = Iterables.get(geneType, 0);
    assertThat(id.getName()).isEqualTo("id");
    assertThat(id.getOperation()).isEqualTo(Operation.IS);
    assertThat(id.getValue().isArray()).isFalse();
    assertThat(id.getValue().get()).isEqualTo("GE1");
  }

  @Test
  public void convertTest() {
    val result = convert("{donor:{id:{is:'DO1'}, age:{not:100}}}");
    assertThat(result.getEntityValues().size()).isEqualTo(1);

    val donorValues = result.getEntityValues().get("donor");
    assertThat(donorValues.size()).isEqualTo(2);

    val idValue = Iterables.get(donorValues, 0);
    assertThat(idValue.getName()).isEqualTo("id");
    assertThat(idValue.getOperation()).isEqualTo(Operation.IS);
    assertThat(idValue.getValue().isArray()).isFalse();
    assertThat(idValue.getValue().get()).isEqualTo("DO1");

    val ageValue = Iterables.get(donorValues, 1);
    assertThat(ageValue.getName()).isEqualTo("age");
    assertThat(ageValue.getOperation()).isEqualTo(Operation.NOT);
    assertThat(ageValue.getValue().isArray()).isFalse();
    assertThat(ageValue.getValue().get()).isEqualTo(100);
  }

  @Test
  public void arrayValueTest() {
    val result = convert("{donor:{id:{is:['DO1','DO2']}}}");
    assertThat(result.getEntityValues().size()).isEqualTo(1);

    val donorValues = result.getEntityValues().get("donor");
    assertThat(donorValues.size()).isEqualTo(1);
    val idValue = Iterables.get(donorValues, 0).getValue();

    assertThat(idValue.isArray()).isTrue();
    val values = (JqlArrayValue) idValue;
    assertThat(values.get()).containsExactly("DO1", "DO2");
  }

  @Test
  public void removeEmptyFilterValueTest() {
    assertThat(convert("{donor:{id:{is:[]}}}").getEntityValues()).isEmpty();
    assertThat(convert("{donor:{id:{is:''}}}").getEntityValues()).isEmpty();
  }

  @Test
  public void invalidTypesTest() {
    thrown.expect(SemanticException.class);
    thrown.expectMessage("Node has no valid types.");

    convert("{test:1}");
  }

  @Test
  public void validTypesTest() {
    convert("{donor:{}, gene:{}, mutation:{}}");
  }

  @Test
  public void hasTest() {
    val result = convert("{donor:{hasPathway:true}}").getEntityValues().get("donor");
    assertThat(result.size()).isEqualTo(1);

    val pathway = Iterables.get(result, 0);
    assertThat(pathway.getName()).isEqualTo("hasPathway");
    assertThat(pathway.getOperation()).isEqualTo(Operation.HAS);
    assertThat(pathway.getValue().get()).isEqualTo(Boolean.TRUE);
  }

  @SneakyThrows
  private JqlFilters convert(String jql) {
    val result = mapper.readValue(jql, JqlFilters.class);
    log.debug("{}", result);

    return result;
  }

  private ObjectMapper createObjectMapper() {
    return registerJqlDeserializer(new ObjectMapper());
  }

  private ObjectMapper registerJqlDeserializer(ObjectMapper mapper) {
    val module = new SimpleModule();
    module.addDeserializer(JqlFilters.class, new JqlFiltersDeserializer());
    mapper.registerModule(module);

    return configureMapper(mapper);
  }

  private ObjectMapper configureMapper(ObjectMapper mapper) {
    return mapper;
  }

}
