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
package org.dcc.portal.pql.es.visitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.utils.Tests.createEsAst;

import java.util.Optional;

import org.dcc.portal.pql.es.ast.SortNode;
import org.dcc.portal.pql.es.model.Order;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.visitor.special.ScoreSortVisitor;
import org.junit.Test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScoreSortVisitorTest {

  ScoreSortVisitor visitor = new ScoreSortVisitor();

  @Test
  public void containsScore() {
    val root = createEsAst("select(id), sort(id, +_score)");
    log.debug("Before visitor: {}", root);
    val result = root.accept(visitor, Optional.empty());
    log.debug("After visitor: {}", result);

    val sortNode = Nodes.getOptionalChild(result, SortNode.class).get();
    assertThat(sortNode.getFields().size()).isEqualTo(2);
    assertThat(sortNode.getFields().get("_score")).isEqualTo(Order.ASC);
    assertThat(sortNode.getFields().get("_donor_id")).isEqualTo(Order.ASC);
  }

  @Test
  public void noScore() {
    val root = createEsAst("select(id), sort(id)");
    val result = root.accept(visitor, Optional.empty());
    log.debug("After visitor: {}", result);

    val sortNode = Nodes.getOptionalChild(result, SortNode.class).get();
    assertThat(sortNode.getFields().size()).isEqualTo(2);
    assertThat(sortNode.getFields().get("_score")).isEqualTo(Order.DESC);
    assertThat(sortNode.getFields().get("_donor_id")).isEqualTo(Order.ASC);
  }

  @Test
  public void noSorting() {
    val root = createEsAst("select(id)");
    val result = root.accept(visitor, Optional.empty());
    log.debug("After visitor: {}", result);

    val sortNode = Nodes.getOptionalChild(result, SortNode.class).get();
    assertThat(sortNode.getFields().size()).isEqualTo(1);
    assertThat(sortNode.getFields().get("_score")).isEqualTo(Order.DESC);
  }

}
