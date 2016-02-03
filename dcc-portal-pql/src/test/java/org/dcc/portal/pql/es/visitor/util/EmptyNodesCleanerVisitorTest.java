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
import static org.dcc.portal.pql.es.visitor.util.EmptyNodesCleanerVisitor.REMOVE_NODE;

import java.util.Optional;

import lombok.val;

import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.visitor.util.EmptyNodesCleanerVisitor;
import org.junit.Before;
import org.junit.Test;

public class EmptyNodesCleanerVisitorTest {

  EmptyNodesCleanerVisitor visitor;

  @Before
  public void setUp() {
    visitor = new EmptyNodesCleanerVisitor();
  }

  @Test
  public void visitRootTest() {
    val result = visitor.visitRoot(new RootNode(new FilterNode(new BoolNode(new MustBoolNode()))), Optional.empty());
    assertThat(result.childrenCount()).isEqualTo(0);
  }

  @Test
  public void visitMustBoolTest() {
    assertThat(visitor.visitMustBool(new MustBoolNode(), Optional.empty())).isEqualTo(REMOVE_NODE);
  }

}
