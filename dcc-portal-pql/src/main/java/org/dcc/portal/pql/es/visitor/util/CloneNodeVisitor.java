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

import lombok.NonNull;
import lombok.val;

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

import com.google.common.collect.Lists;

public class CloneNodeVisitor extends NodeVisitor<ExpressionNode, Void> {

  @Override
  public ExpressionNode visitFunctionScore(FunctionScoreNode node, Optional<Void> context) {
    return new FunctionScoreNode(node.getScript(), visitChildren(node));
  }

  @Override
  public ExpressionNode visitCount(CountNode node, Optional<Void> context) {
    return node;
  }

  @Override
  public ExpressionNode visitFilter(FilterNode node, Optional<Void> context) {
    return new FilterNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitNested(NestedNode node, Optional<Void> context) {
    return new NestedNode(node.getPath(), node.getScoreMode(), visitChildren(node));
  }

  @Override
  public ExpressionNode visitBool(BoolNode node, Optional<Void> context) {
    return new BoolNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitMustBool(MustBoolNode node, Optional<Void> context) {
    return new MustBoolNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitShouldBool(ShouldBoolNode node, Optional<Void> context) {
    return new ShouldBoolNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitTerm(TermNode node, Optional<Void> context) {
    val nameNode = new TerminalNode(node.getNameNode().getValue());
    val valueNode = new TerminalNode(node.getValueNode().getValue());

    return new TermNode(nameNode, valueNode);
  }

  @Override
  public ExpressionNode visitTerms(TermsNode node, Optional<Void> context) {
    val result = new TermsNode(node.getField(), node.getLookup());
    result.addChildren(visitChildren(node));

    return result;
  }

  @Override
  public ExpressionNode visitNot(NotNode node, Optional<Void> context) {
    return new NotNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitRoot(RootNode node, Optional<Void> context) {
    return new RootNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitSort(SortNode node, Optional<Void> context) {
    val result = new SortNode();
    for (val entry : node.getFields().entrySet()) {
      result.addField(entry.getKey(), entry.getValue());
    }

    return result;
  }

  @Override
  public ExpressionNode visitTerminal(TerminalNode node, Optional<Void> context) {
    return new TerminalNode(node.getValue());
  }

  @Override
  public ExpressionNode visitRange(RangeNode node, Optional<Void> context) {
    return new RangeNode(node.getFieldName(), visitChildren(node));
  }

  @Override
  public ExpressionNode visitGreaterEqual(GreaterEqualNode node, Optional<Void> context) {
    return new GreaterEqualNode(visitChildren(node)[0]);
  }

  @Override
  public ExpressionNode visitGreaterThan(GreaterThanNode node, Optional<Void> context) {
    return new GreaterThanNode(visitChildren(node)[0]);
  }

  @Override
  public ExpressionNode visitLessEqual(LessEqualNode node, Optional<Void> context) {
    return new LessEqualNode(visitChildren(node)[0]);
  }

  @Override
  public ExpressionNode visitLessThan(LessThanNode node, Optional<Void> context) {
    return new LessThanNode(visitChildren(node)[0]);
  }

  @Override
  public ExpressionNode visitLimit(LimitNode node, Optional<Void> context) {
    return new LimitNode(node.getFrom(), node.getSize());
  }

  @Override
  public ExpressionNode visitAggregations(AggregationsNode node, Optional<Void> context) {
    return new AggregationsNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitFields(FieldsNode node, Optional<Void> context) {
    return new FieldsNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitSource(SourceNode node, Optional<Void> context) {
    return new SourceNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitQuery(QueryNode node, Optional<Void> context) {
    return new QueryNode(visitChildren(node));
  }

  @Override
  public ExpressionNode visitTermsAggregation(TermsAggregationNode node, Optional<Void> context) {
    val result = new TermsAggregationNode(node.getAggregationName(), node.getFieldName());
    result.addChildren(visitChildren(node));

    return result;
  }

  @Override
  public ExpressionNode visitMissingAggregation(MissingAggregationNode node, Optional<Void> context) {
    val result = new MissingAggregationNode(node.getAggregationName(), node.getFieldName());
    result.addChildren(visitChildren(node));

    return result;
  }

  @Override
  public ExpressionNode visitReverseNestedAggregation(ReverseNestedAggregationNode node, Optional<Void> context) {
    val result = new ReverseNestedAggregationNode(node.getAggregationName());
    result.addChildren(visitChildren(node));

    return result;
  }

  @Override
  public ExpressionNode visitFilterAggregation(FilterAggregationNode node, Optional<Void> context) {
    val result = new FilterAggregationNode(node.getAggregationName(), node.getFilters());
    result.addChildren(visitChildren(node));

    return result;
  }

  @Override
  public ExpressionNode visitExists(@NonNull ExistsNode node, Optional<Void> context) {
    return new ExistsNode(node.getField());
  }

  @Override
  public ExpressionNode visitMissing(@NonNull MissingNode node, Optional<Void> context) {
    return new MissingNode(node.getField());
  }

  private ExpressionNode[] visitChildren(ExpressionNode parent) {
    val result = Lists.<ExpressionNode> newArrayList();
    for (val child : parent.getChildren()) {
      result.add(child.accept(this, Optional.empty()));
    }

    return result.toArray(new ExpressionNode[result.size()]);
  }

}
