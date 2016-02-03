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
import static org.dcc.portal.pql.es.visitor.score.MutationScoreQueryVisitor.SCRIPT;
import static org.dcc.portal.pql.es.visitor.score.ScoreVisitorHelpers.assertFunctionScoreNode;
import static org.dcc.portal.pql.es.visitor.score.ScoreVisitorHelpers.assertNestedNode;
import static org.dcc.portal.pql.utils.Tests.assertBoolAndGetMustNode;
import static org.dcc.portal.pql.utils.Tests.assertTermNode;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.NestedNode.ScoreMode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.query.FunctionScoreNode;
import org.dcc.portal.pql.meta.IndexModel;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Slf4j
public class NestedFieldsVisitorTest {

  NestedFieldsVisitor visitor = new NestedFieldsVisitor();
  NestedNode nestedNode = ScoreQueryVisitor.createdFunctionScoreNestedNode(SCRIPT, PATH);

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void malformedFilterNode() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("FilterNode has a FilterNode child.");

    val esAst = new FilterNode(new BoolNode(new MustBoolNode(new FilterNode())));
    visit(esAst);
  }

  @Test
  public void otherNestingLevelTest() {
    val esAst = new FilterNode(new NestedNode("transcript", new TermNode("transcript.id", "1")));
    val nestedNode = visit(esAst);
    assertNestedNode(nestedNode);
    assertFunctionScoreNode((FunctionScoreNode) nestedNode.getFirstChild(), false);
  }

  @Test
  public void noNestedFilterTest() {
    val esAst = new FilterNode(new TermNode("id", "1"));
    val nestedNode = visit(esAst);
    assertNestedNode(nestedNode);
    assertFunctionScoreNode((FunctionScoreNode) nestedNode.getFirstChild(), false);
  }

  @Test
  public void twoNestedNodesTest() {
    val deepestNestedNode = new NestedNode("ssm_occurrence.observation", new TermNode("platformNested", "1"));
    val esAst = new FilterNode(new NestedNode(PATH, new BoolNode(new MustBoolNode(
        deepestNestedNode, new TermNode("donor.id", "2")))));

    val nestedNode = visit(esAst);
    assertNestedNode(nestedNode);

    val scoreNode = (FunctionScoreNode) nestedNode.getFirstChild();
    assertFunctionScoreNode(scoreNode, true);

    // FunctionScoreNode - FilterNode - BoolNode - MustBoolNode
    val mustNode = (MustBoolNode) scoreNode.getFirstChild().getFirstChild().getFirstChild();
    assertThat(mustNode.childrenCount()).isEqualTo(2);

    val level2NesteNode = (NestedNode) mustNode.getFirstChild();
    assertThat(level2NesteNode.getPath()).isEqualTo("ssm_occurrence.observation");
    assertThat(level2NesteNode.getScoreMode()).isEqualTo(ScoreMode.AVG);

    assertTermNode(level2NesteNode.getFirstChild(), "platformNested", "1");
    assertTermNode(mustNode.getChild(1), "donor.id", "2");
  }

  @Test
  public void singleNestedNodeTest_sameLevel() {
    val esAst = new FilterNode(new NestedNode(PATH,
        new BoolNode(new MustBoolNode(
            new TermNode("donor.id", "1"),
            new TermNode("donor.gender", "male")))));
    val nestedNode = visit(esAst);
    assertNestedNode(nestedNode);

    val scoreNode = (FunctionScoreNode) nestedNode.getFirstChild();
    assertFunctionScoreNode(scoreNode, true);

    // ScoreNode - FilterNode - BoolNode - MustBoolNode
    val mustNode = (MustBoolNode) scoreNode.getFirstChild().getFirstChild().getFirstChild();
    assertThat(mustNode.childrenCount()).isEqualTo(2);

    assertTermNode(mustNode.getFirstChild(), "donor.id", "1");
    assertTermNode(mustNode.getChild(1), "donor.gender", "male");
  }

  @Test
  public void singleNestedNodeTest_nestedBelowThePath() {
    val esAst = new FilterNode(new NestedNode("ssm_occurrence.observation", new TermNode("platformNested", "1")));
    val nestedNode = visit(esAst);

    assertNestedNode(nestedNode);

    val scoreNode = (FunctionScoreNode) nestedNode.getFirstChild();
    assertFunctionScoreNode(scoreNode, true);

    // ScoreNode - FilterNode - TermNode
    val filterNode = (FilterNode) scoreNode.getFirstChild();
    assertThat(filterNode.childrenCount()).isEqualTo(1);

    val ssmNestedNode = (NestedNode) filterNode.getFirstChild();
    assertThat(ssmNestedNode.childrenCount()).isEqualTo(1);
    assertThat(ssmNestedNode.getPath()).isEqualTo("ssm_occurrence.observation");
    assertThat(ssmNestedNode.getScoreMode()).isEqualTo(NestedNode.ScoreMode.AVG);

    assertTermNode(ssmNestedNode.getFirstChild(), "platformNested", "1");
  }

  @Test
  public void andTest_empty() {
    val esAst = new MustBoolNode(new TermNode("id", "1"));
    val requestContext = new NestedFieldsVisitor.RequestContext(IndexModel.getMutationCentricTypeModel(), nestedNode);
    val result = esAst.accept(visitor, Optional.of(requestContext));
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  public void andTest() {
    val esAst = new BoolNode(new MustBoolNode(new TermNode("platform", "1")));
    val requestContext = new NestedFieldsVisitor.RequestContext(IndexModel.getMutationCentricTypeModel(), nestedNode);
    val result = esAst.accept(visitor, Optional.of(requestContext)).get();
    log.debug("{}", result);

    val mustNode = assertBoolAndGetMustNode(result);
    assertThat(mustNode.hasChildren()).isTrue();
  }

  @Test
  public void orTest_empty() {
    val esAst = new ShouldBoolNode(new TermNode("id", "1"));
    val requestContext = new NestedFieldsVisitor.RequestContext(IndexModel.getMutationCentricTypeModel(), nestedNode);
    val result = esAst.accept(visitor, Optional.of(requestContext));
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  public void orTest() {
    val esAst = new BoolNode(new ShouldBoolNode(new TermNode("platform", "1")));
    val requestContext = new NestedFieldsVisitor.RequestContext(IndexModel.getMutationCentricTypeModel(), nestedNode);
    val result = esAst.accept(visitor, Optional.of(requestContext)).get();
    log.debug("{}", result);

    val boolNode = (BoolNode) result;
    assertThat(boolNode.childrenCount()).isEqualTo(1);

    val shouldNode = (ShouldBoolNode) boolNode.getFirstChild();
    assertThat(shouldNode.hasChildren()).isTrue();
  }

  private NestedNode visit(ExpressionNode nodeToVisit) {
    log.debug("Visiting node: \n{}", nodeToVisit);
    val requestContext = new NestedFieldsVisitor.RequestContext(IndexModel.getMutationCentricTypeModel(), nestedNode);
    val result = nodeToVisit.accept(visitor, Optional.of(requestContext)).get();
    log.debug("Result: \n{}", result);

    return (NestedNode) result;
  }

}
