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

import static org.dcc.portal.pql.es.utils.VisitorHelpers.visitChildren;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.CountNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.FieldsNode;
import org.dcc.portal.pql.es.ast.LimitNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.SortNode;
import org.dcc.portal.pql.es.ast.SourceNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.FilterAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.MissingAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.NestedAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.ReverseNestedAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.ExistsNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.GreaterEqualNode;
import org.dcc.portal.pql.es.ast.filter.GreaterThanNode;
import org.dcc.portal.pql.es.ast.filter.LessEqualNode;
import org.dcc.portal.pql.es.ast.filter.LessThanNode;
import org.dcc.portal.pql.es.ast.filter.MissingNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.NotNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.es.ast.filter.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.es.ast.query.FunctionScoreNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.es.visitor.NodeVisitor;

@Slf4j
public class QuerySimplifierVisitor extends NodeVisitor<Optional<ExpressionNode>, Void> {

  @Override
  public Optional<ExpressionNode> visitCount(CountNode node, Optional<Void> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitFilter(FilterNode node, Optional<Void> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitNested(NestedNode node, Optional<Void> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitBool(BoolNode node, Optional<Void> context) {
    log.debug("[visitBool] Processing node - \n{}", node);
    visitChildren(this, node, context);
    if (node.childrenCount() != 1 || node.getFirstChild().childrenCount() != 1) {
      log.debug("Children count not equal to one or the child has multiple children. Skipping processing.");

      return Optional.empty();
    }

    val child = node.getFirstChild();
    log.debug("The BoolNode has one child which also has one child. Moving its contents one level upper");

    return Optional.of(child.getFirstChild());
  }

  @Override
  public Optional<ExpressionNode> visitMustBool(MustBoolNode node, Optional<Void> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitShouldBool(ShouldBoolNode node, Optional<Void> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitTerm(TermNode node, Optional<Void> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitTerms(TermsNode node, Optional<Void> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitNot(NotNode node, Optional<Void> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitRoot(RootNode node, Optional<Void> context) {
    for (val child : node.getChildren()) {
      child.accept(this, context);
    }

    return Optional.of(node);
  }

  @Override
  public Optional<ExpressionNode> visitSort(SortNode node, Optional<Void> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitTerminal(TerminalNode node, Optional<Void> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitRange(RangeNode node, Optional<Void> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitGreaterEqual(GreaterEqualNode node, Optional<Void> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitGreaterThan(GreaterThanNode node, Optional<Void> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitLessEqual(LessEqualNode node, Optional<Void> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitLessThan(LessThanNode node, Optional<Void> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitLimit(LimitNode node, Optional<Void> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitFields(FieldsNode node, Optional<Void> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitSource(SourceNode node, Optional<Void> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitQuery(QueryNode node, Optional<Void> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitAggregations(AggregationsNode node, Optional<Void> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitTermsAggregation(TermsAggregationNode node, Optional<Void> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitReverseNestedAggregation(ReverseNestedAggregationNode node,
      Optional<Void> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitMissingAggregation(MissingAggregationNode node, Optional<Void> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitFilterAggregation(FilterAggregationNode node, Optional<Void> context) {
    visitChildren(this, node.getFilters(), context);

    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitNestedAggregation(NestedAggregationNode node, Optional<Void> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitFunctionScore(FunctionScoreNode node, Optional<Void> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitExists(ExistsNode node, Optional<Void> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitMissing(MissingNode node, Optional<Void> context) {
    return Optional.empty();
  }

}
