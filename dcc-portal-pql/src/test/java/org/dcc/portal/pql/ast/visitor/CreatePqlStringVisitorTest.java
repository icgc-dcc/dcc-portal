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
package org.dcc.portal.pql.ast.visitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.facets;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.select;

import java.util.Optional;

import lombok.val;

import org.dcc.portal.pql.ast.PqlNode;
import org.dcc.portal.pql.ast.filter.AndNode;
import org.dcc.portal.pql.ast.filter.EqNode;
import org.dcc.portal.pql.ast.filter.GtNode;
import org.dcc.portal.pql.ast.filter.InNode;
import org.dcc.portal.pql.ast.filter.NestedNode;
import org.dcc.portal.pql.ast.function.SortNode;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class CreatePqlStringVisitorTest {

  CreatePqlStringVisitor visitor = new CreatePqlStringVisitor();

  @Test
  public void visitEqTest() {
    val node = new EqNode("id", 10);
    assertThat(visit(node)).isEqualTo("eq(id,10)");
    assertThat(visit(new EqNode("id", "ID1"))).isEqualTo("eq(id,'ID1')");
  }

  @Test
  public void visitAndTest() {
    val node = new AndNode(new EqNode("id", 10), new GtNode("age", 100));
    assertThat(visit(node)).isEqualTo("and(eq(id,10),gt(age,100))");
  }

  @Test
  public void visitNestedTest() {
    val node = new NestedNode("gene", new AndNode(new EqNode("id", 10), new GtNode("age", 100)));
    assertThat(visit(node)).isEqualTo("nested(gene,and(eq(id,10),gt(age,100)))");
  }

  @Test
  public void visitInTest() {
    val node = new InNode("gene", ImmutableList.of(20, 30));
    assertThat(visit(node)).isEqualTo("in(gene,20,30)");
    assertThat(visit(new InNode("gene", ImmutableList.of("G1", "G2")))).isEqualTo("in(gene,'G1','G2')");
  }

  @Test
  public void visitFacetsTest() {
    val node = facets("id");
    assertThat(visit(node)).isEqualTo("facets(id)");
  }

  @Test
  public void visitSortTest() {
    val node = SortNode.builder();
    node.sortDesc("gene");
    node.sortAsc("id");

    assertThat(visit(node.build())).isEqualTo("sort(-gene,+id)");
  }

  @Test
  public void visitSelectTest() {
    assertThat(visit(select("id"))).isEqualTo("select(id)");
  }

  private String visit(PqlNode node) {
    return node.accept(visitor, Optional.empty());
  }

}
