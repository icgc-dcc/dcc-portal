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
package org.dcc.portal.pql.ast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.ast.builder.PqlSearchBuilder.statement;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.facets;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.select;
import static org.dcc.portal.pql.ast.function.SortNode.builder;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.ast.builder.FilterBuilders;
import org.dcc.portal.pql.ast.function.FunctionBuilders;
import org.junit.Test;

@Slf4j
public class StatementNodeTest {

  @Test
  public void addFacetsTest() {
    val statement = statement().facets("id").build();
    statement.setFacets(facets("gender"));

    val facets = statement.getFacets();
    assertThat(facets.size()).isEqualTo(2);
    assertThat(facets.get(0).getFacets()).containsOnly("id");
    assertThat(facets.get(1).getFacets()).containsOnly("gender");
  }

  @Test
  public void setFiltersTest() {
    val statement = statement().filter(FilterBuilders.eq("id", "I1")).build();
    log.info("{}", statement);
    val newFilters = FilterBuilders.eq("gender", "male").build();
    statement.setFilters(newFilters);
    log.info("{}", statement);
    assertThat(statement.getFilters()).isEqualTo(newFilters);
    assertThat(statement.getChildren()).containsOnly(newFilters);
  }

  @Test
  public void setLimitTest() {
    val statement = statement().limit(10, 20).build();
    val newLimit = FunctionBuilders.limit(30, 40);
    statement.setLimit(newLimit);
    assertThat(statement.getLimit()).isEqualTo(newLimit);
    assertThat(statement.getChildren()).containsOnly(newLimit);
  }

  @Test
  public void addSelectTest() {
    val statement = statement().select("id").build();
    statement.addSelect(select("gender"));

    val select = statement.getSelect();
    assertThat(select.size()).isEqualTo(2);
    assertThat(select.get(0).getFields()).containsOnly("id");
    assertThat(select.get(1).getFields()).containsOnly("gender");
  }

  @Test
  public void setSortTest() {
    val statement = statement().sort(builder().sortAsc("id")).build();
    val newSort = FunctionBuilders.sortBuilder().sortAsc("gender").build();
    statement.setSort(newSort);
    assertThat(statement.getSort()).isEqualTo(newSort);
    assertThat(statement.getChildren()).containsOnly(newSort);
  }

}
