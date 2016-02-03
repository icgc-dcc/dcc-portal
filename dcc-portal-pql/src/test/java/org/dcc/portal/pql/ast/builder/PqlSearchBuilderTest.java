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
package org.dcc.portal.pql.ast.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import lombok.val;

import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.ast.function.FacetsNode;
import org.dcc.portal.pql.ast.function.LimitNode;
import org.dcc.portal.pql.ast.function.SelectNode;
import org.dcc.portal.pql.ast.function.SortNode;
import org.dcc.portal.pql.es.model.Order;
import org.junit.Test;

public class PqlSearchBuilderTest {

  @Test
  public void selectTest_oneField() {
    val result = PqlSearchBuilder.statement().select("id").build();
    assertThat(result.childrenCount()).isEqualTo(1);
    val selectNode = (SelectNode) result.getFirstChild();
    assertThat(selectNode.getFields()).containsOnly("id");
  }

  @Test
  public void selectTest_multiFields() {
    val result = PqlSearchBuilder.statement().select("id", "mutation").build();
    assertThat(result.childrenCount()).isEqualTo(1);
    val selectNode = (SelectNode) result.getFirstChild();
    assertThat(selectNode.getFields()).containsOnly("id", "mutation");
  }

  @Test
  public void selectAllTest() {
    val result = PqlSearchBuilder.statement().selectAll().build();
    assertThat(result.childrenCount()).isEqualTo(1);
    val selectNode = (SelectNode) result.getFirstChild();
    assertThat(selectNode.getFields()).containsOnly(SelectNode.ALL_FIELDS);
  }

  @Test
  public void limitTest() {
    StatementNode result = PqlSearchBuilder.statement().limit(5).build();
    assertThat(result.childrenCount()).isEqualTo(1);
    LimitNode limitNode = (LimitNode) result.getFirstChild();
    assertThat(limitNode.getFrom()).isEqualTo(0);
    assertThat(limitNode.getSize()).isEqualTo(5);

    result = PqlSearchBuilder.statement().limit(1, 15).build();
    assertThat(result.childrenCount()).isEqualTo(1);
    limitNode = (LimitNode) result.getFirstChild();
    assertThat(limitNode.getFrom()).isEqualTo(1);
    assertThat(limitNode.getSize()).isEqualTo(15);
  }

  @Test
  public void facetsTest() {
    StatementNode result = PqlSearchBuilder.statement().facets("id").build();
    assertThat(result.childrenCount()).isEqualTo(1);
    FacetsNode facetsNode = (FacetsNode) result.getFirstChild();
    assertThat(facetsNode.getFacets()).containsOnly("id");

    result = PqlSearchBuilder.statement().facets("id", "mutation").build();
    assertThat(result.childrenCount()).isEqualTo(1);
    facetsNode = (FacetsNode) result.getFirstChild();
    assertThat(facetsNode.getFacets()).containsOnly("id", "mutation");
  }

  @Test
  public void sortTest() {
    StatementNode result = PqlSearchBuilder.statement().sort(SortNode.builder().sortAsc("id")).build();
    assertThat(result.childrenCount()).isEqualTo(1);
    SortNode sortNode = (SortNode) result.getFirstChild();
    Map<String, Order> fields = sortNode.getFields();
    assertThat(fields.size()).isEqualTo(1);
    assertThat(fields.get("id")).isEqualTo(Order.ASC);

    result = PqlSearchBuilder.statement()
        .sort(SortNode.builder().sortAsc("id").sortDesc("mutation"))
        .build();
    assertThat(result.childrenCount()).isEqualTo(1);
    sortNode = (SortNode) result.getFirstChild();
    fields = sortNode.getFields();
    assertThat(fields.size()).isEqualTo(2);
    assertThat(fields.get("id")).isEqualTo(Order.ASC);
    assertThat(fields.get("mutation")).isEqualTo(Order.DESC);
  }

  @Test
  public void multiActionsTest() {
    val result = PqlSearchBuilder.statement()
        .select("id")
        .facets("mu")
        .filter(FilterBuilders.eq("gene", "G1"))
        .limit(100)
        .sort(SortNode.builder().sortAsc("id"))
        .build();

    val select = result.getSelect();
    assertThat(select.size()).isEqualTo(1);
    assertThat(select.get(0).getFields()).containsOnly("id");

    val facets = result.getFacets();
    assertThat(facets.size()).isEqualTo(1);
    assertThat(facets.get(0).getFacets()).containsOnly("mu");
    assertThat(result.getLimit().getSize()).isEqualTo(100);

    val sortFields = result.getSort().getFields();
    assertThat(sortFields.size()).isEqualTo(1);
    assertThat(sortFields.get("id")).isEqualTo(Order.ASC);

    assertThat(result.getFilters().toEqNode().getField()).isEqualTo("gene");
    assertThat(result.getFilters().toEqNode().getValue()).isEqualTo("G1");
  }

}
