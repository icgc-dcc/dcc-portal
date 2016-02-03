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
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
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
 * Checks if a filter has at least one field which is not nested under the path
 */
public class SearchNonNestedFieldsVisitor extends NodeVisitor<Boolean, VisitContext> {

  @Override
  public Boolean visitFilter(@NonNull FilterNode node, @NonNull Optional<VisitContext> context) {
    checkState(node.childrenCount() == 1);

    return node.getFirstChild().accept(this, context);
  }

  @Override
  public Boolean visitBool(@NonNull BoolNode node, @NonNull Optional<VisitContext> context) {
    return visitChildren(node, context);
  }

  @Override
  public Boolean visitMustBool(@NonNull MustBoolNode node, @NonNull Optional<VisitContext> context) {
    return visitChildren(node, context);
  }

  @Override
  public Boolean visitShouldBool(ShouldBoolNode node, Optional<VisitContext> context) {
    return visitChildren(node, context);
  }

  @Override
  public Boolean visitTerm(TermNode node, Optional<VisitContext> context) {
    return processCommonCases(node.getNameNode().getValueAsString(), context);
  }

  @Override
  public Boolean visitNot(NotNode node, Optional<VisitContext> context) {
    return visitChildren(node, context);
  }

  @Override
  public Boolean visitRange(RangeNode node, Optional<VisitContext> context) {
    return processCommonCases(node.getFieldName(), context);
  }

  @Override
  public Boolean visitTerms(TermsNode node, Optional<VisitContext> context) {
    return processCommonCases(node.getField(), context);
  }

  @Override
  public Boolean visitExists(ExistsNode node, Optional<VisitContext> context) {
    return processCommonCases(node.getField(), context);
  }

  @Override
  public Boolean visitMissing(MissingNode node, Optional<VisitContext> context) {
    return processCommonCases(node.getField(), context);
  }

  @Override
  public Boolean visitNested(NestedNode node, Optional<VisitContext> context) {
    return visitChildren(node, context);
  }

  private Boolean processCommonCases(String field, Optional<VisitContext> context) {
    checkOptional(context);
    val typeModel = context.get().getTypeModel();
    val path = context.get().getPath();

    return !typeModel.isNested(field, path);
  }

  private Boolean visitChildren(ExpressionNode node, Optional<VisitContext> context) {
    for (val child : node.getChildren()) {
      if (child.accept(this, context)) {
        return TRUE;
      }
    }

    return FALSE;
  }

}
