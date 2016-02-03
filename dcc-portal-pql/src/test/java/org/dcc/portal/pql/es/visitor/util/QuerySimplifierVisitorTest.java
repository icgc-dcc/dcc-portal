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
package org.dcc.portal.pql.es.visitor.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.NotNode;
import org.dcc.portal.pql.es.ast.filter.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.junit.Test;

@Slf4j
public class QuerySimplifierVisitorTest {

  QuerySimplifierVisitor visitor = new QuerySimplifierVisitor();

  @Test
  public void shouldAndMustTest() {
    val esAst = new BoolNode(new ShouldBoolNode(), new MustBoolNode());
    val result = visit(esAst);
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  public void noChildrenTest() {
    val esAst = new BoolNode();
    val result = visit(esAst);
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  public void oneChildWithMultipleChildrenTest() {
    val esAst = new BoolNode(new ShouldBoolNode(new NotNode(), new NotNode()));
    val result = visit(esAst);
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  public void oneChildWithSingeChildTest() {
    val subChild = new TermNode("name", "value");
    val esAst = new BoolNode(new ShouldBoolNode(subChild));
    val result = visit(esAst).get();
    assertThat(result).isEqualTo(subChild);
  }

  private Optional<ExpressionNode> visit(ExpressionNode esAst) {
    log.debug("Visiting - \n{}", esAst);
    val result = esAst.accept(visitor, Optional.empty());
    log.debug("Visit result - \n{}", result);

    return result;
  }

}
