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
package org.dcc.portal.pql.es.visitor.aggs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.utils.Tests.assertBoolAndGetMustNode;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.meta.IndexModel;
import org.junit.Test;

@Slf4j
public class ResolveNestedFilterVisitorTest {

  private static final String NESTED_PATH = "ssm_occurrence";
  private ResolveNestedFilterVisitor visitor = new ResolveNestedFilterVisitor();

  @Test
  public void noNestedFiltersTest() {
    val esAst = new FilterNode(new TermNode("id", 1));
    val result = esAst.accept(visitor, createContext());
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  public void nestedFiltersTest() {
    val termNode = new TermNode("donor.id", 1);
    val esAst = new FilterNode(termNode);
    val termNodeClone = Nodes.cloneNode(termNode);
    val result = esAst.accept(visitor, createContext()).get();

    assertThat(result).isInstanceOf(FilterNode.class);
    assertThat(result.childrenCount()).isEqualTo(1);
    assertThat(result.getFirstChild()).isEqualTo(termNodeClone);
  }

  @Test
  public void nestedAndNonNestedFiltersTest() {
    val termNode = new TermNode("donor.id", 1);
    val esAst = new FilterNode(new BoolNode(new MustBoolNode(termNode, new TermNode("id", 1))));
    val termNodeClone = Nodes.cloneNode(termNode);
    val result = esAst.accept(visitor, createContext()).get();

    assertThat(result).isInstanceOf(FilterNode.class);
    assertThat(result.childrenCount()).isEqualTo(1);

    val mustNode = assertBoolAndGetMustNode(result.getFirstChild());
    assertThat(mustNode.childrenCount()).isEqualTo(1);

    assertThat(mustNode.getFirstChild()).isEqualTo(termNodeClone);
  }

  @Test
  public void nestedNodeAtNestedPathLevelTest() {
    val termNode = new TermNode("donor.id", 1);
    val esAst = new FilterNode(new NestedNode(NESTED_PATH, termNode));
    val termNodeClone = Nodes.cloneNode(termNode);
    val result = esAst.accept(visitor, createContext()).get();
    log.info("{}", result);
    assertThat(result.getFirstChild()).isEqualTo(termNodeClone);
  }

  @Test
  public void nestedNodeAtDeeperNestedPathLevelTest() {
    val nestedNode = new NestedNode(NESTED_PATH + ".observation", new TermNode("platformNested", 1));
    val esAst = new FilterNode(nestedNode);
    val nestedNodeClone = Nodes.cloneNode(nestedNode);
    val result = esAst.accept(visitor, createContext()).get();
    log.info("{}", result);
    assertThat(result.getFirstChild()).isEqualTo(nestedNodeClone);
  }

  @Test
  public void anotherNestedLevel() {
    val esAst = new FilterNode(new NestedNode("transcript", new TermNode("transcriptId", 1)));
    val result = esAst.accept(visitor, createContext());
    assertThat(result.isPresent()).isFalse();
  }

  private Optional<VisitContext> createContext() {
    return Optional.of(new VisitContext(NESTED_PATH, IndexModel.getMutationCentricTypeModel()));
  }

}
