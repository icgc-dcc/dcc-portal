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

import java.util.Optional;

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

/**
 * Removes nodes that do not have children. E.g. MustBoolNode without children must be removed as it breaks an ES search
 * request.
 */
@Slf4j
public class EmptyNodesCleanerVisitor extends NodeVisitor<ExpressionNode, Void> {

  public static final ExpressionNode REMOVE_NODE = null;

  @Override
  public ExpressionNode visitNested(NestedNode node, Optional<Void> context) {
    return processCommonCases(node);
  }

  @Override
  public ExpressionNode visitCount(CountNode node, Optional<Void> context) {
    return node;
  }

  @Override
  public ExpressionNode visitTerm(TermNode node, Optional<Void> context) {
    return node;
  }

  @Override
  public ExpressionNode visitTerms(TermsNode node, Optional<Void> context) {
    return node;
  }

  @Override
  public ExpressionNode visitNot(NotNode node, Optional<Void> context) {
    return processCommonCases(node);
  }

  @Override
  public ExpressionNode visitSort(SortNode node, Optional<Void> context) {
    return node;
  }

  @Override
  public ExpressionNode visitTerminal(TerminalNode node, Optional<Void> context) {
    return node;
  }

  @Override
  public ExpressionNode visitRange(RangeNode node, Optional<Void> context) {
    return processCommonCases(node);
  }

  @Override
  public ExpressionNode visitGreaterEqual(GreaterEqualNode node, Optional<Void> context) {
    return node;
  }

  @Override
  public ExpressionNode visitGreaterThan(GreaterThanNode node, Optional<Void> context) {
    return node;
  }

  @Override
  public ExpressionNode visitLessEqual(LessEqualNode node, Optional<Void> context) {
    return node;
  }

  @Override
  public ExpressionNode visitLessThan(LessThanNode node, Optional<Void> context) {
    return node;
  }

  @Override
  public ExpressionNode visitLimit(LimitNode node, Optional<Void> context) {
    return node;
  }

  @Override
  public ExpressionNode visitFunctionScore(FunctionScoreNode node, Optional<Void> context) {
    return node;
  }

  @Override
  public ExpressionNode visitFields(FieldsNode node, Optional<Void> context) {
    return processCommonCases(node);
  }

  @Override
  public ExpressionNode visitSource(SourceNode node, Optional<Void> context) {
    return node;
  }

  @Override
  public ExpressionNode visitQuery(QueryNode node, Optional<Void> context) {
    return processCommonCases(node);
  }

  @Override
  public ExpressionNode visitRoot(RootNode node, Optional<Void> context) {
    return processChildren(node);
  }

  @Override
  public ExpressionNode visitFilter(FilterNode node, Optional<Void> context) {
    return processCommonCases(node);
  }

  @Override
  public ExpressionNode visitBool(BoolNode node, Optional<Void> context) {
    return processCommonCases(node);
  }

  @Override
  public ExpressionNode visitMustBool(MustBoolNode node, Optional<Void> context) {
    return processCommonCases(node);
  }

  @Override
  public ExpressionNode visitShouldBool(ShouldBoolNode node, Optional<Void> context) {
    return processCommonCases(node);
  }

  @Override
  public ExpressionNode visitAggregations(AggregationsNode node, Optional<Void> context) {
    return processCommonCases(node);
  }

  @Override
  public ExpressionNode visitNestedAggregation(NestedAggregationNode node, Optional<Void> context) {
    return processCommonCases(node);
  }

  @Override
  public ExpressionNode visitTermsAggregation(TermsAggregationNode node, Optional<Void> context) {
    return processChildren(node);
  }

  @Override
  public ExpressionNode visitReverseNestedAggregation(ReverseNestedAggregationNode node, Optional<Void> context) {
    return processChildren(node);
  }

  @Override
  public ExpressionNode visitMissingAggregation(MissingAggregationNode node, Optional<Void> context) {
    return processChildren(node);
  }

  @Override
  public ExpressionNode visitFilterAggregation(FilterAggregationNode node, Optional<Void> context) {
    // Clean filters
    node.getFilters().accept(this, Optional.empty());

    return processChildren(node);
  }

  @Override
  public ExpressionNode visitExists(ExistsNode node, Optional<Void> context) {
    return node;
  }

  @Override
  public ExpressionNode visitMissing(MissingNode node, Optional<Void> context) {
    return node;
  }

  private ExpressionNode processCommonCases(ExpressionNode node) {
    log.debug("Processing \n{}", node);
    node = processChildren(node);
    if (!node.hasChildren()) {
      log.debug("Requesting to remove empty node \n{}", node);

      return REMOVE_NODE;
    }

    return node;
  }

  /**
   * Removes children if necessary, but do not remove the {@code node} ifself.
   */
  private ExpressionNode processChildren(ExpressionNode node) {
    for (int i = node.childrenCount() - 1; i >= 0; i--) {
      if (node.getChild(i).accept(this, Optional.empty()) == REMOVE_NODE) {
        node.removeChild(i);
      }
    }

    return node;
  }

}
