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
package org.dcc.portal.pql.query;

import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;

import org.dcc.portal.pql.es.model.Order;
import org.dcc.portal.pql.exception.SemanticException;
import org.junit.Test;

public class PqlParserTest {

  @Test
  public void successfulParseTest() {
    assertThat(PqlParser.parse("select(*)").toString()).isEqualTo("select(*)");
  }

  @Test(expected = SemanticException.class)
  public void malformedPqlTest() {
    PqlParser.parse("select()");
  }

  @Test
  public void selectTest() {
    val statement = PqlParser.parse("select(a),select(b)");
    val select = statement.getSelect();
    assertThat(select.size()).isEqualTo(2);
    assertThat(select.get(0).getFields()).containsOnly("a");
    assertThat(select.get(1).getFields()).containsOnly("b");
  }

  @Test
  public void facetsTest() {
    val statement = PqlParser.parse("facets(*),facets(b)");
    val facets = statement.getFacets();
    assertThat(facets.size()).isEqualTo(2);
    assertThat(facets.get(0).getFacets()).containsOnly("*");
    assertThat(facets.get(1).getFacets()).containsOnly("b");
  }

  @Test
  public void filterTest_single() {
    val statement = PqlParser.parse("eq(gene.id,'G1')");
    val eqFilter = statement.getFilters().toEqNode();
    assertThat(eqFilter.getField()).isEqualTo("gene.id");
    assertThat(eqFilter.getValue()).isEqualTo("G1");
  }

  @Test
  public void filterTest_multi() {
    val statement = PqlParser.parse("eq(id,'G1'),gt(a,10)");
    val filter = statement.getFilters().toAndNode();
    assertThat(filter.childrenCount()).isEqualTo(2);

    val eqFilter = filter.getFirstChild().toEqNode();
    assertThat(eqFilter.getField()).isEqualTo("id");
    assertThat(eqFilter.getValue()).isEqualTo("G1");

    val gtFilter = filter.getChild(1).toGtNode();
    assertThat(gtFilter.getField()).isEqualTo("a");
    assertThat(gtFilter.getValue()).isEqualTo(10);
  }

  @Test
  public void limitTest() {
    val statement = PqlParser.parse("select(*),limit(5,10)");
    assertThat(statement.childrenCount()).isEqualTo(2);
    val limitNode = statement.getLimit();
    assertThat(limitNode.getFrom()).isEqualTo(5);
    assertThat(limitNode.getSize()).isEqualTo(10);
  }

  @Test
  public void sortTest() {
    val statement = PqlParser.parse("select(*),sort(a,-b)");
    val sortFields = statement.getSort().getFields();
    assertThat(sortFields.size()).isEqualTo(2);
    assertThat(sortFields.get("a")).isEqualTo(Order.ASC);
    assertThat(sortFields.get("b")).isEqualTo(Order.DESC);
  }

  @Test
  public void countTest() {
    val statement = PqlParser.parse("count()");
    assertThat(statement.isCount()).isTrue();
  }

  @Test
  public void selectFacetsTest() {
    assertThat(PqlParser.parse("select(*),select(a),facets(*),facets(b)").toString())
        .isEqualTo("select(*),select(a),facets(*),facets(b)");
  }

}
