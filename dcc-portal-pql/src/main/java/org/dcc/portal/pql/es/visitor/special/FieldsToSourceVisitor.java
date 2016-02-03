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

import static org.dcc.portal.pql.es.utils.VisitorHelpers.checkOptional;

import java.util.Optional;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.FieldsNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.SourceNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.query.QueryContext;

/**
 * Visits {@link FieldsNode} subtree. It if has fields that are objects (e.g. transcripts) moves them to a sourceNode
 * which will be turned into a source filtering request.<br>
 * <br>
 * <b>NB:</b> Deprecate fields. Use source filtering.
 */
@Slf4j
public class FieldsToSourceVisitor extends NodeVisitor<Optional<ExpressionNode>, QueryContext> {

  @Override
  public Optional<ExpressionNode> visitRoot(@NonNull RootNode node, @NonNull Optional<QueryContext> context) {
    val fieldsNodeOpt = Nodes.getOptionalChild(node, FieldsNode.class);
    if (fieldsNodeOpt.isPresent()) {
      checkOptional(context);
      val sourceNodeOpt = fieldsNodeOpt.get().accept(this, context);
      if (sourceNodeOpt.isPresent()) {
        node.addChildren(sourceNodeOpt.get());
      }
    }

    return Optional.of(node);
  }

  @Override
  public Optional<ExpressionNode> visitFields(@NonNull FieldsNode node, @NonNull Optional<QueryContext> context) {
    log.debug("Visiting {}", node);
    val result = new SourceNode();
    val typeModel = context.get().getTypeModel();
    val includeFields = typeModel.getIncludeFields();

    for (int i = node.childrenCount() - 1; i >= 0; i--) {
      val terminalNode = (TerminalNode) node.getChild(i);
      if (includeFields.contains(terminalNode.getValue())) {
        log.debug("Moving {} to the SourceNode", terminalNode);
        result.addChildren(terminalNode);
        node.removeChild(i);
      }
    }

    return result.childrenCount() == 0 ? Optional.empty() : Optional.of(result);
  }

}
