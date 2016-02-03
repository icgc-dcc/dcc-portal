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
package org.dcc.portal.pql.es.visitor.special;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.SortNode;
import org.dcc.portal.pql.es.model.Order;
import org.junit.Test;

@Slf4j
public class ScoreSortVisitorTest {

  ScoreSortVisitor visitor = new ScoreSortVisitor();

  @Test
  public void noScoreNodeTest() {
    val result = visit(new RootNode());
    val sortNode = assertRoot(result);
    val fields = sortNode.getFields();
    assertThat(fields.size()).isEqualTo(1);
    assertThat(fields.get("_score")).isEqualTo(Order.DESC);
  }

  @Test
  public void hasScoreTest() {
    val sortNode = new SortNode();
    sortNode.addField("_score", Order.DESC);
    val result = visit(new RootNode(sortNode));
    val sortNodeResult = assertRoot(result);
    assertThat(sortNode).isEqualTo(sortNodeResult);
  }

  @Test
  public void sortWithoutScoreTest() {
    val sortNode = new SortNode();
    sortNode.addField("id", Order.DESC);
    val result = visit(new RootNode(sortNode));

    val sortNodeResult = assertRoot(result);
    val fields = sortNodeResult.getFields();
    assertThat(fields.size()).isEqualTo(2);
    assertThat(fields.get("id")).isEqualTo(Order.DESC);
    assertThat(fields.get("_score")).isEqualTo(Order.DESC);
  }

  private SortNode assertRoot(ExpressionNode root) {
    assertThat(root.childrenCount()).isEqualTo(1);

    return (SortNode) root.getFirstChild();
  }

  private ExpressionNode visit(ExpressionNode ast) {
    log.debug("Visiting \n{}", ast);
    val result = ast.accept(visitor, Optional.empty());
    log.debug("Result \n{}", result);

    return result;
  }

}
