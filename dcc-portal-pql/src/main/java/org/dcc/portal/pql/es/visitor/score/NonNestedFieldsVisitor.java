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

import java.util.Optional;

import lombok.val;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.ExistsNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MissingNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.NotNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.es.ast.filter.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.es.utils.VisitorHelpers;
import org.dcc.portal.pql.es.visitor.NodeVisitor;

/**
 * Returns an AST with filters that are not nested under the nesting path. All methods beneath visitFilter return nodes
 * which must be removed because they are nested.
 */
public class NonNestedFieldsVisitor extends NodeVisitor<Optional<ExpressionNode>, ScoreQueryContext> {

  @Override
  public Optional<ExpressionNode> visitFilter(FilterNode node, Optional<ScoreQueryContext> context) {
    VisitorHelpers.checkOptional(context);
    processChildren(node, context);

    return Optional.of(node);
  }

  @Override
  public Optional<ExpressionNode> visitBool(BoolNode node, Optional<ScoreQueryContext> context) {
    processChildren(node, context);

    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitMustBool(MustBoolNode node, Optional<ScoreQueryContext> context) {
    processChildren(node, context);

    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitShouldBool(ShouldBoolNode node, Optional<ScoreQueryContext> context) {
    processChildren(node, context);

    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitNot(NotNode node, Optional<ScoreQueryContext> context) {
    processChildren(node, context);

    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitNested(NestedNode node, Optional<ScoreQueryContext> context) {
    val nestingPath = context.get().getNestingPath();
    if (node.getPath().startsWith(nestingPath)) {
      return Optional.of(node);
    }

    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitExists(ExistsNode node, Optional<ScoreQueryContext> context) {
    return isRemove(node.getField(), context) ? Optional.of(node) : Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitMissing(MissingNode node, Optional<ScoreQueryContext> context) {
    return isRemove(node.getField(), context) ? Optional.of(node) : Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitRange(RangeNode node, Optional<ScoreQueryContext> context) {
    return isRemove(node.getFieldName(), context) ? Optional.of(node) : Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitTerm(TermNode node, Optional<ScoreQueryContext> context) {
    return isRemove(node.getNameNode().getValueAsString(), context) ? Optional.of(node) : Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitTerms(TermsNode node, Optional<ScoreQueryContext> context) {
    return isRemove(node.getField(), context) ? Optional.of(node) : Optional.empty();
  }

  private static boolean isRemove(String fieldName, Optional<ScoreQueryContext> context) {
    val nestingPath = context.get().getNestingPath();
    val typeModel = context.get().getTypeModel();
    if (typeModel.isNested(fieldName, nestingPath)) {
      return true;
    }

    return false;
  }

  private void processChildren(ExpressionNode parent, Optional<ScoreQueryContext> context) {
    for (int i = parent.childrenCount() - 1; i >= 0; i--) {
      val child = parent.getChild(i);
      val visitResult = child.accept(this, context);
      if (visitResult.isPresent()) {
        parent.removeChild(i);
      }
    }
  }

}
