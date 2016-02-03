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

import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.NestedNode.ScoreMode;
import org.dcc.portal.pql.es.ast.query.FunctionScoreNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;

/**
 * Helps to test ScoreVisitors. Assumes that all the tests are performed with MutationScoreQueryVisitor
 */
public class ScoreVisitorHelpers {

  public static void assertQueryNode(QueryNode queryNode) {
    assertThat(queryNode.childrenCount()).isEqualTo(1);
  }

  public static void assertNestedNode(NestedNode nestedNode) {
    assertThat(nestedNode.childrenCount()).isEqualTo(1);
    assertThat(nestedNode.getPath()).isEqualTo(MutationScoreQueryVisitor.PATH);
    assertThat(nestedNode.getScoreMode()).isEqualTo(ScoreMode.TOTAL);
  }

  public static void assertFunctionScoreNode(FunctionScoreNode node, boolean hasChildren) {
    assertThat(node.getScript()).isEqualTo(MutationScoreQueryVisitor.SCRIPT);
    if (hasChildren) {
      assertThat(node.childrenCount()).isEqualTo(1);
    } else {
      assertThat(node.childrenCount()).isEqualTo(0);
    }
  }

}
