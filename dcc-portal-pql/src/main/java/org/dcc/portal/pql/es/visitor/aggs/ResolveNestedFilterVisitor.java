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

import static com.google.common.base.Preconditions.checkState;
import static org.dcc.portal.pql.es.utils.VisitorHelpers.checkOptional;

import java.util.Optional;

import lombok.NonNull;
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
import org.dcc.portal.pql.es.visitor.NodeVisitor;

/**
 * Returns a {@link FilterNode} that contain only children nested under the nested path. The visitor is used to resolve
 * aggregations on nested fields.
 */
public class ResolveNestedFilterVisitor extends NodeVisitor<Optional<ExpressionNode>, VisitContext> {

  @Override
  public Optional<ExpressionNode> visitFilter(@NonNull FilterNode node, @NonNull Optional<VisitContext> context) {
    checkState(node.childrenCount() == 1, "FilterNode has an incorrect children count: '%s'", node.childrenCount());

    return visitChildren(node, context);
  }

  @Override
  public Optional<ExpressionNode> visitBool(@NonNull BoolNode node, @NonNull Optional<VisitContext> context) {
    return visitChildren(node, context);
  }

  @Override
  public Optional<ExpressionNode> visitMustBool(@NonNull MustBoolNode node, @NonNull Optional<VisitContext> context) {
    return visitChildren(node, context);
  }

  @Override
  public Optional<ExpressionNode> visitShouldBool(ShouldBoolNode node, Optional<VisitContext> context) {
    return visitChildren(node, context);
  }

  @Override
  public Optional<ExpressionNode> visitTerm(TermNode node, Optional<VisitContext> context) {
    return processCommonCases(node.getNameNode().getValueAsString(), node, context);
  }

  @Override
  public Optional<ExpressionNode> visitNot(NotNode node, Optional<VisitContext> context) {
    return visitChildren(node, context);
  }

  @Override
  public Optional<ExpressionNode> visitRange(RangeNode node, Optional<VisitContext> context) {
    return processCommonCases(node.getFieldName(), node, context);
  }

  @Override
  public Optional<ExpressionNode> visitTerms(TermsNode node, Optional<VisitContext> context) {
    return processCommonCases(node.getField(), node, context);
  }

  @Override
  public Optional<ExpressionNode> visitExists(ExistsNode node, Optional<VisitContext> context) {
    return processCommonCases(node.getField(), node, context);
  }

  @Override
  public Optional<ExpressionNode> visitMissing(MissingNode node, Optional<VisitContext> context) {
    return processCommonCases(node.getField(), node, context);
  }

  @Override
  public Optional<ExpressionNode> visitNested(NestedNode node, Optional<VisitContext> context) {
    checkOptional(context);
    checkState(node.childrenCount() == 1, "NestedNode has an incorrect children count: '%s'", node.childrenCount());
    val nestedPath = context.get().getPath();
    val nodePath = node.getPath();

    // Remove nested node on the same level
    if (nestedPath.equals(nodePath)) {
      return Optional.of(node.getFirstChild());
    } else if (nodePath.startsWith(nestedPath)) {
      return Optional.of(node);
    }

    // Another nested level
    return Optional.empty();
  }

  private Optional<ExpressionNode> visitChildren(ExpressionNode node, Optional<VisitContext> context) {
    for (int i = node.childrenCount() - 1; i >= 0; i--) {
      val child = node.getChild(i);
      val visitResult = child.accept(this, context);
      if (visitResult.isPresent()) {
        node.setChild(i, visitResult.get());
      } else {
        node.removeChild(i);
      }
    }

    return node.hasChildren() ? Optional.of(node) : Optional.empty();
  }

  private Optional<ExpressionNode> processCommonCases(String field, ExpressionNode node, Optional<VisitContext> context) {
    checkOptional(context);
    val typeModel = context.get().getTypeModel();
    val nestedPath = context.get().getPath();

    return typeModel.isNested(field, nestedPath) ? Optional.of(node) : Optional.empty();
  }

}
