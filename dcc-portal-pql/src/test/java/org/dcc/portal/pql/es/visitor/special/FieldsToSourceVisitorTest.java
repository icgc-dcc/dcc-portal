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
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.utils.Tests.createEsAst;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.FieldsNode;
import org.dcc.portal.pql.es.ast.SourceNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.query.QueryContext;
import org.junit.Test;

@Slf4j
public class FieldsToSourceVisitorTest {

  FieldsToSourceVisitor visitor = new FieldsToSourceVisitor();

  @Test
  public void visitFields_noIncludes() {
    val root = createEsAst("select(*)", MUTATION_CENTRIC);
    val result = root.accept(visitor, Optional.of(new QueryContext("", MUTATION_CENTRIC))).get();
    log.debug("After visitor: {}", result);
    val sourceNodeOpt = Nodes.getOptionalChild(result, SourceNode.class);
    assertThat(sourceNodeOpt.isPresent()).isFalse();
  }

  @Test
  public void visitFields_withIncludes() {
    val root = createEsAst("select(transcripts)", GENE_CENTRIC);
    val result = root.accept(visitor, Optional.of(new QueryContext("", GENE_CENTRIC))).get();
    log.debug("After visitor: {}", result);

    val sourceNode = Nodes.getOptionalChild(result, SourceNode.class).get();
    assertThat(sourceNode.childrenCount()).isEqualTo(1);
    val sourceTerminalNode = (TerminalNode) sourceNode.getFirstChild();
    assertThat(sourceTerminalNode.getValue()).isEqualTo("transcripts");

    val fieldsNode = Nodes.getOptionalChild(result, FieldsNode.class).get();
    assertThat(fieldsNode.childrenCount()).isEqualTo(0);
  }

  @Test
  public void visitFields_withIncludes_mutation() {
    val root = createEsAst("select(transcripts)", MUTATION_CENTRIC);
    val result = root.accept(visitor, Optional.of(new QueryContext("", MUTATION_CENTRIC))).get();
    log.debug("After visitor: {}", result);

    val sourceNode = Nodes.getOptionalChild(result, SourceNode.class).get();
    assertThat(sourceNode.childrenCount()).isEqualTo(1);
    val sourceTerminalNode = (TerminalNode) sourceNode.getFirstChild();
    assertThat(sourceTerminalNode.getValue()).isEqualTo("transcript");

    val fieldsNode = Nodes.getOptionalChild(result, FieldsNode.class).get();
    assertThat(fieldsNode.childrenCount()).isEqualTo(0);
  }

}
