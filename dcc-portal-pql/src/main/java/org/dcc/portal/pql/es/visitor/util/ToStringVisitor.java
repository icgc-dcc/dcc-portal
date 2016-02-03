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

import static java.lang.String.format;

import java.util.Optional;

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
 * Creates a String representation of the ES AST.
 */
public class ToStringVisitor extends NodeVisitor<String, Void> {

  private static final String NEWLINE = System.getProperty("line.separator");
  private static final String DEFAULT_INDENT = "  ";

  @Override
  public String visitCount(CountNode node, Optional<Void> context) {
    return buildToString(node);
  }

  @Override
  public String visitFilter(FilterNode node, Optional<Void> context) {
    return buildToString(node);
  }

  @Override
  public String visitNested(NestedNode node, Optional<Void> context) {
    val header = format("%s [path: %s, score_mode: %s]", getCommonHeader(node), node.getPath(),
        node.getScoreMode().getId());

    return buildToString(node, Optional.of(header));
  }

  @Override
  public String visitBool(BoolNode node, Optional<Void> context) {
    return buildToString(node);
  }

  @Override
  public String visitMustBool(MustBoolNode node, Optional<Void> context) {
    return buildToString(node);
  }

  @Override
  public String visitShouldBool(ShouldBoolNode node, Optional<Void> context) {
    return buildToString(node);
  }

  @Override
  public String visitTerm(TermNode node, Optional<Void> context) {
    return format("%s (%s = %s)", getCommonHeader(node), node.getNameNode().getValue(), node.getValueNode().getValue());
  }

  @Override
  public String visitTerms(TermsNode node, Optional<Void> context) {
    String header = "";
    if (node.getLookup().getIndex().isEmpty()) {
      header = format("%s [%s]", getCommonHeader(node), node.getField());
    } else {
      header = format("%s [%s %s]", getCommonHeader(node), node.getField(), node.getLookup());
    }

    return buildToString(node, Optional.of(header));
  }

  @Override
  public String visitNot(NotNode node, Optional<Void> context) {
    return buildToString(node);
  }

  @Override
  public String visitRoot(RootNode node, Optional<Void> context) {
    val builder = new StringBuilder();
    builder.append("{");
    builder.append(NEWLINE);

    for (val child : node.getChildren()) {
      builder.append(child.accept(this, Optional.<Void> empty()));
      builder.append(",");
      builder.append(NEWLINE);
    }

    builder.append("}");

    return builder.toString();
  }

  @Override
  public String visitSort(SortNode node, Optional<Void> context) {
    val builder = new StringBuilder();
    builder.append(getCommonHeader(node));
    builder.append(" [");

    for (val entry : node.getFields().entrySet()) {
      builder.append(format("%s(%s) ", entry.getKey(), entry.getValue()));
    }

    builder.append("]");

    return builder.toString();
  }

  @Override
  public String visitTerminal(TerminalNode node, Optional<Void> context) {
    return format("%s = %s", getCommonHeader(node), node.getValue());
  }

  @Override
  public String visitRange(RangeNode node, Optional<Void> context) {
    val header = format("%s [%s]", getCommonHeader(node), node.getFieldName());

    return buildToString(node, Optional.of(header));
  }

  @Override
  public String visitGreaterEqual(GreaterEqualNode node, Optional<Void> context) {
    return buildToString(node);
  }

  @Override
  public String visitGreaterThan(GreaterThanNode node, Optional<Void> context) {
    return buildToString(node);
  }

  @Override
  public String visitLessEqual(LessEqualNode node, Optional<Void> context) {
    return buildToString(node);
  }

  @Override
  public String visitLessThan(LessThanNode node, Optional<Void> context) {
    return buildToString(node);
  }

  @Override
  public String visitLimit(LimitNode node, Optional<Void> context) {
    return format("%s [from: %d, size: %d]", getCommonHeader(node), node.getFrom(), node.getSize());
  }

  @Override
  public String visitFields(FieldsNode node, Optional<Void> context) {
    val builder = new StringBuilder();
    builder.append(getCommonHeader(node));
    builder.append(" [");
    for (val field : node.getFields()) {
      builder.append(field + ", ");
    }
    builder.append("]");

    return builder.toString();
  }

  @Override
  public String visitSource(SourceNode node, Optional<Void> context) {
    val builder = new StringBuilder();
    builder.append(getCommonHeader(node));
    builder.append(" [");
    for (val field : node.getFields()) {
      builder.append(field + " ");
    }
    builder.append("]");

    return builder.toString();
  }

  @Override
  public String visitQuery(QueryNode node, Optional<Void> context) {
    return buildToString(node);
  }

  @Override
  public String visitAggregations(AggregationsNode node, Optional<Void> context) {
    return buildToString(node);
  }

  @Override
  public String visitTermsAggregation(TermsAggregationNode node, Optional<Void> context) {
    val header =
        format("%s%s [%s(%s)] (", calcIndent(node), node.getNodeName(), node.getAggregationName(), node.getFieldName());

    return buildToString(node, Optional.of(header));
  }

  @Override
  public String visitReverseNestedAggregation(ReverseNestedAggregationNode node, Optional<Void> context) {
    val header = format("%s%s [%s] (", calcIndent(node), node.getNodeName(), node.getAggregationName());

    return buildToString(node, Optional.of(header));
  }

  @Override
  public String visitNestedAggregation(NestedAggregationNode node, Optional<Void> context) {
    val header = format("%s%s [%s(%s)] (", calcIndent(node), node.getNodeName(), node.getAggregationName(),
        node.getPath());

    return buildToString(node, Optional.of(header));
  }

  @Override
  public String visitMissingAggregation(MissingAggregationNode node, Optional<Void> context) {
    val header =
        format("%s%s [%s(%s)] (", calcIndent(node), node.getNodeName(), node.getAggregationName(), node.getFieldName());

    return buildToString(node, Optional.of(header));
  }

  @Override
  public String visitFilterAggregation(FilterAggregationNode node, Optional<Void> context) {
    val builder = new StringBuilder();
    builder.append(format("%s%s [%s] (", calcIndent(node), node.getNodeName(), node.getAggregationName()));
    builder.append(NEWLINE);
    builder.append(node.getFilters().accept(this, Optional.empty()));

    return buildToString(node, Optional.of(builder.toString()));
  }

  @Override
  public String visitFunctionScore(FunctionScoreNode node, Optional<Void> context) {
    val header = format("%s [script: %s]", getCommonHeader(node), node.getScript());

    return buildToString(node, Optional.of(header));
  }

  @Override
  public String visitExists(ExistsNode node, Optional<Void> context) {
    val header = format("%s [%s]", getCommonHeader(node), node.getField());

    return buildToString(node, Optional.of(header));
  }

  @Override
  public String visitMissing(MissingNode node, Optional<Void> context) {
    val header = format("%s [%s]", getCommonHeader(node), node.getField());

    return buildToString(node, Optional.of(header));
  }

  private String buildToString(ExpressionNode node) {
    return buildToString(node, Optional.<String> empty());
  }

  private String buildToString(ExpressionNode node, Optional<String> header) {
    val indent = calcIndent(node);
    val builder = new StringBuilder();

    if (header.isPresent()) {
      builder.append(header.get());
    } else {
      builder.append(format("%s (", getCommonHeader(node)));
    }

    for (val child : node.getChildren()) {
      builder.append(NEWLINE);
      builder.append(child.accept(this, Optional.empty()));
    }

    builder.append(NEWLINE);
    builder.append(indent + ")");

    return builder.toString();
  }

  private static String calcIndent(ExpressionNode node) {
    val builder = new StringBuilder();
    for (int i = 0; i < parentsCount(node); i++) {
      builder.append(DEFAULT_INDENT);
    }

    return builder.toString();
  }

  private static int parentsCount(ExpressionNode node) {
    int count = 0;
    while (node != null) {
      node = node.getParent();
      count++;
    }

    return count;
  }

  private static String getCommonHeader(ExpressionNode node) {
    return format("%s%s", calcIndent(node), node.getNodeName());
  }

}
