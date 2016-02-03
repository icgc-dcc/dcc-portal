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
package org.dcc.portal.pql.es.visitor.score;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.es.visitor.score.ScoreVisitorHelpers.assertFunctionScoreNode;
import static org.dcc.portal.pql.es.visitor.score.ScoreVisitorHelpers.assertNestedNode;
import static org.dcc.portal.pql.es.visitor.score.ScoreVisitorHelpers.assertQueryNode;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.query.FunctionScoreNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Slf4j
public class ScoreQueryVisitorTest {

  ScoreQueryVisitor visitor = new MutationScoreQueryVisitor();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void noQueryTest() {
    val result = visit(new RootNode());

    assertThat(result.childrenCount()).isEqualTo(1);
    val queryNode = (QueryNode) result.getFirstChild();
    assertQueryNode(queryNode);
    val nestedNode = (NestedNode) queryNode.getFirstChild();
    assertNestedNode(nestedNode);
    assertFunctionScoreNode((FunctionScoreNode) nestedNode.getFirstChild(), false);
  }

  @Test()
  public void malformedQueryTest() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Malformed QueryNode");
    visit(new RootNode(new QueryNode()));
  }

  @Test()
  public void withQueryTest() {
    val result = visit(new RootNode(new QueryNode(new FilterNode())));
    assertThat(result.childrenCount()).isEqualTo(1);

    // Query - Bool - MustBool
    val queryNode = (QueryNode) result.getFirstChild();
    assertThat(queryNode.childrenCount()).isEqualTo(1);

    val boolNode = (BoolNode) queryNode.getFirstChild();
    assertThat(boolNode.childrenCount()).isEqualTo(1);

    val mustNode = (MustBoolNode) boolNode.getFirstChild();
    assertThat(mustNode.childrenCount()).isEqualTo(2);

    val filterNode = (FilterNode) mustNode.getFirstChild();
    assertThat(filterNode.hasChildren()).isFalse();

    val nestedNode = (NestedNode) mustNode.getChild(1);
    assertNestedNode(nestedNode);
    assertFunctionScoreNode((FunctionScoreNode) nestedNode.getFirstChild(), false);
  }

  private ExpressionNode visit(ExpressionNode node) {
    log.debug("Visiting: \n{}", node);
    val result = node.accept(visitor, Optional.empty());
    log.debug("Result - \n{}", result);

    return result;
  }

}
