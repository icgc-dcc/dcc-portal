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
import static org.dcc.portal.pql.es.visitor.score.MutationScoreQueryVisitor.PATH;
import static org.dcc.portal.pql.meta.IndexModel.getMutationCentricTypeModel;
import static org.dcc.portal.pql.utils.Tests.assertTermNode;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.NestedNode.ScoreMode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.junit.Test;

@Slf4j
public class NonNestedFieldsVisitorTest {

  NonNestedFieldsVisitor visitor = new NonNestedFieldsVisitor();

  @Test
  public void nonNestedTest() {
    val esAst = new FilterNode(new TermNode("id", "1"));
    val result = visit(esAst);
    assertThat(result.childrenCount()).isEqualTo(1);
    assertTermNode(result.getFirstChild(), "id", "1");
  }

  @Test
  public void nestedSameLevelTest() {
    val esAst = new FilterNode(new NestedNode(PATH, new TermNode("donor.id", "1")));
    val result = visit(esAst);
    assertThat(result.hasChildren()).isFalse();
  }

  @Test
  public void nestedDeeperTest() {
    val esAst = new FilterNode(new NestedNode(PATH + ".observation", new TermNode("platformNested", "1")));
    val result = visit(esAst);
    assertThat(result.hasChildren()).isFalse();
  }

  @Test
  public void otherNestingPathTest() {
    val esAst = new FilterNode(new NestedNode("transcript", new TermNode("platformNested", "1")));
    val result = visit(esAst);
    assertThat(result.childrenCount()).isEqualTo(1);

    val nestedNode = (NestedNode) result.getFirstChild();
    assertThat(nestedNode.childrenCount()).isEqualTo(1);
    assertThat(nestedNode.getPath()).isEqualTo("transcript");
    assertThat(nestedNode.getScoreMode()).isEqualTo(ScoreMode.AVG);

    assertTermNode(nestedNode.getFirstChild(), "platformNested", "1");
  }

  private FilterNode visit(ExpressionNode nodeToVisit) {
    log.debug("Visiting node: \n{}", nodeToVisit);
    val scoreQueryContext = new ScoreQueryContext(PATH, getMutationCentricTypeModel());
    val result = nodeToVisit.accept(visitor, Optional.of(scoreQueryContext)).get();
    log.debug("Result: \n{}", result);

    return (FilterNode) result;
  }

}
