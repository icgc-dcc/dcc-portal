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
import lombok.val;

import org.dcc.portal.pql.ast.PqlNode;
import org.dcc.portal.pql.es.utils.ParseTrees;
import org.junit.Test;

public class CreatePqlAstVisitorTest {

  CreatePqlAstVisitor visitor = new CreatePqlAstVisitor();

  @Test
  public void visitSelectTest() {
    assertThat(visit("select(id,gene)")).isEqualTo("select(id,gene)");
    assertThat(visit("select(*)")).isEqualTo("select(*)");
  }

  @Test
  public void visitFacetsTest() {
    assertThat(visit("facets(id,gene)")).isEqualTo("facets(id,gene)");
    assertThat(visit("facets(*)")).isEqualTo("facets(*)");
  }

  @Test
  public void visitLimitTest() {
    assertThat(visit("select(i),limit(5)")).isEqualTo("select(i),limit(0,5)");
    assertThat(visit("select(i),limit(1,5)")).isEqualTo("select(i),limit(1,5)");
  }

  @Test
  public void visitSortTest() {
    assertThat(visit("select(i),sort(+id,-gene)")).isEqualTo("select(i),sort(+id,-gene)");
  }

  @Test
  public void visitCountTest() {
    assertThat(visit("count()")).isEqualTo("count()");
  }

  @Test
  public void visitEqTest() {
    assertThat(visit("eq(id,1)")).isEqualTo("eq(id,1)");
  }

  @Test
  public void visitNeTest() {
    assertThat(visit("ne(id,1)")).isEqualTo("ne(id,1)");
  }

  @Test
  public void visitGeTest() {
    assertThat(visit("ge(id,1)")).isEqualTo("ge(id,1)");
  }

  @Test
  public void visitGtTest() {
    assertThat(visit("gt(id,1)")).isEqualTo("gt(id,1)");
  }

  @Test
  public void visitLeTest() {
    assertThat(visit("le(id,1)")).isEqualTo("le(id,1)");
  }

  @Test
  public void visitLtTest() {
    assertThat(visit("lt(id,1)")).isEqualTo("lt(id,1)");
  }

  @Test
  public void visitNotTest() {
    assertThat(visit("not(lt(id,1))")).isEqualTo("not(lt(id,1))");
  }

  @Test
  public void visitExistsTest() {
    assertThat(visit("exists(id)")).isEqualTo("exists(id)");
  }

  @Test
  public void visitMissingTest() {
    assertThat(visit("missing(id)")).isEqualTo("missing(id)");
  }

  @Test
  public void visitInTest() {
    assertThat(visit("in(id,5,10)")).isEqualTo("in(id,5,10)");
  }

  @Test
  public void visitAndTest() {
    assertThat(visit("and(eq(id,10),missing(gene))")).isEqualTo("and(eq(id,10),missing(gene))");
    assertThat(visit("eq(id,10),missing(gene)")).isEqualTo("and(eq(id,10),missing(gene))");
  }

  @Test
  public void visitOrTest() {
    assertThat(visit("or(eq(id,10),missing(gene))")).isEqualTo("or(eq(id,10),missing(gene))");
  }

  @Test
  public void visitNestedTest() {
    assertThat(visit("nested(gene,in(id,5,10))")).isEqualTo("nested(gene,in(id,5,10))");
  }

  private PqlNode createPqlAst(String query) {
    val statement = ParseTrees.getParser(query).statement();

    return statement.accept(visitor);
  }

  private String visit(String query) {
    return createPqlAst(query).toString();
  }

}
