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

import static org.dcc.portal.pql.es.utils.VisitorHelpers.checkOptional;

import java.util.Optional;

import javax.annotation.concurrent.ThreadSafe;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

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
 * Visits filter nodes. And removes those that match provided aggregation field name.
 */
@Slf4j
@ThreadSafe
public class AggregationFiltersVisitor extends NodeVisitor<ExpressionNode, String> {

  private static final ExpressionNode REMOVE_CHILD = null;

  @Override
  public ExpressionNode visitFilter(FilterNode node, @NonNull Optional<String> context) {
    return processCommonCases(node, context);
  }

  @Override
  public ExpressionNode visitBool(BoolNode node, @NonNull Optional<String> context) {
    return processCommonCases(node, context);
  }

  @Override
  public ExpressionNode visitMustBool(MustBoolNode node, @NonNull Optional<String> context) {
    return processCommonCases(node, context);
  }

  @Override
  public ExpressionNode visitShouldBool(ShouldBoolNode node, @NonNull Optional<String> context) {
    return processCommonCases(node, context);
  }

  @Override
  public ExpressionNode visitTerm(TermNode node, @NonNull Optional<String> context) {
    checkOptional(context);

    return node.getNameNode().getValue().equals(context.get()) ? REMOVE_CHILD : node;
  }

  @Override
  public ExpressionNode visitNot(NotNode node, @NonNull Optional<String> context) {
    return processCommonCases(node, context);
  }

  @Override
  public ExpressionNode visitRange(RangeNode node, @NonNull Optional<String> context) {
    checkOptional(context);

    return node.getFieldName().equals(context.get()) ? REMOVE_CHILD : node;
  }

  @Override
  public ExpressionNode visitTerms(TermsNode node, @NonNull Optional<String> context) {
    checkOptional(context);

    return node.getField().equals(context.get()) ? REMOVE_CHILD : node;
  }

  @Override
  public ExpressionNode visitExists(ExistsNode node, @NonNull Optional<String> context) {
    checkOptional(context);

    return node.getField().equals(context.get()) ? REMOVE_CHILD : node;
  }

  @Override
  public ExpressionNode visitMissing(MissingNode node, @NonNull Optional<String> context) {
    checkOptional(context);

    return node.getField().equals(context.get()) ? REMOVE_CHILD : node;
  }

  @Override
  public ExpressionNode visitNested(NestedNode node, @NonNull Optional<String> context) {
    return processCommonCases(node, context);
  }

  private ExpressionNode processCommonCases(ExpressionNode node, Optional<String> context) {
    for (int i = node.childrenCount() - 1; i >= 0; i--) {
      val child = node.getChild(i);
      log.debug("Visiting child: {}", child);
      if (child.accept(this, context) == REMOVE_CHILD) {
        log.debug("Removing the child from the filters.");
        node.removeChild(i);
      }
    }

    return node;
  }

}
