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
package org.dcc.portal.pql.es.visitor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.dcc.portal.pql.es.utils.Nodes.getValues;
import static org.dcc.portal.pql.es.utils.ScoreModes.resolveScoreMode;
import static org.dcc.portal.pql.es.utils.VisitorHelpers.checkOptional;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;

import java.util.List;
import java.util.Optional;
import java.util.Stack;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.lucene.search.join.ScoreMode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.aggs.FilterAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.NestedAggregationNode;
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
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.utils.Visitors;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.meta.TypeModel;
import org.dcc.portal.pql.query.QueryContext;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.indices.TermsLookup;

import com.google.common.collect.Lists;

/**
 * Visits {@link FilterNode} and builds FilterBuilders
 */
@Slf4j
@NoArgsConstructor
public class FilterBuilderVisitor extends NodeVisitor<QueryBuilder, QueryContext> {

  private final Stack<QueryBuilder> stack = new Stack<QueryBuilder>();

  @Override
  public QueryBuilder visitFilter(@NonNull FilterNode node, @NonNull Optional<QueryContext> context) {
    return node.getFirstChild().accept(this, context);
  }

  @Override
  public QueryBuilder visitQuery(@NonNull QueryNode node, @NonNull Optional<QueryContext> context) {
    val queryBuilder = node.accept(Visitors.createQueryBuilderVisitor(), context);

    return queryBuilder;
  }

  @Override
  public QueryBuilder visitBool(@NonNull BoolNode node, @NonNull Optional<QueryContext> context) {
    val resultBuilder = QueryBuilders.boolQuery();

    for (val child : node.getChildren()) {
      val childrenResult = visitChildren(child, context);
      if (child instanceof MustBoolNode) {
        childrenResult.forEach(result -> resultBuilder.must(result));
      } else if (child instanceof ShouldBoolNode) {
        childrenResult.forEach(result -> resultBuilder.should(result));
      } else {
        throw new IllegalStateException(format("Operation type %s is not supported", child.getNodeName()));
      }
    }

    return resultBuilder;
  }

  @Override
  public QueryBuilder visitGreaterEqual(@NonNull GreaterEqualNode node, @NonNull Optional<QueryContext> context) {
    val rangeFilter = (RangeQueryBuilder) stack.peek();
    checkNotNull(rangeFilter, "Could not find the RangeFilter on the stack");
    rangeFilter.gte(node.getValue());

    return rangeFilter;
  }

  @Override
  public QueryBuilder visitGreaterThan(@NonNull GreaterThanNode node, @NonNull Optional<QueryContext> context) {
    val rangeFilter = (RangeQueryBuilder) stack.peek();
    checkNotNull(rangeFilter, "Could not find the RangeFilter on the stack");
    rangeFilter.gt(node.getValue());

    return rangeFilter;
  }

  @Override
  public QueryBuilder visitLessEqual(@NonNull LessEqualNode node, @NonNull Optional<QueryContext> context) {
    val rangeFilter = (RangeQueryBuilder) stack.peek();
    checkNotNull(rangeFilter, "Could not find the RangeFilter on the stack");
    rangeFilter.lte(node.getValue());

    return rangeFilter;
  }

  @Override
  public QueryBuilder visitLessThan(@NonNull LessThanNode node, @NonNull Optional<QueryContext> context) {
    val rangeFilter = (RangeQueryBuilder) stack.peek();
    checkNotNull(rangeFilter, "Could not find the RangeFilter on the stack");
    rangeFilter.lt(node.getValue());

    return rangeFilter;
  }

  @Override
  public QueryBuilder visitNested(@NonNull NestedNode node, @NonNull Optional<QueryContext> context) {
    log.debug("Visiting Nested: {}", node);
    val scoreMode = resolveScoreMode(node.getScoreMode());
    val childQuery = node.getFirstChild().accept(this, context);

    return nestedQuery(node.getPath(), childQuery, scoreMode);
  }

  @Override
  public QueryBuilder visitNot(@NonNull NotNode node, @NonNull Optional<QueryContext> context) {
    val childrenCount = node.childrenCount();
    checkState(childrenCount == 1, "NotNode can have only one child. Found %s", childrenCount);

    // TODO: Rework AST to eliminate NotNode
    return QueryBuilders.boolQuery().mustNot(node.getFirstChild().accept(this, context));
  }

  @Override
  public QueryBuilder visitRange(@NonNull RangeNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);
    checkState(node.childrenCount() > 0, "RangeNode has no children");

    stack.push(QueryBuilders.rangeQuery(node.getFieldName()));
    for (val child : node.getChildren()) {
      child.accept(this, context);
    }

    return createNestedFilter(node, node.getFieldName(), stack.pop(), context.get().getTypeModel());
  }

  @Override
  public QueryBuilder visitTerm(@NonNull TermNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);

    QueryBuilder filter;
    val field = node.getField();
    val lookupInfo = node.getLookup();
    if (lookupInfo.isDefine()) {
      val type = lookupInfo.getType();
      val lookup = new TermsLookup(lookupInfo.getIndex(), type, lookupInfo.getId(), lookupInfo.getPath());
      filter = QueryBuilders.termsLookupQuery(type + "-lookup", lookup);
    } else {
      val value = node.getValueNode().getValue();
      log.debug("[visitTerm] Name: {}, Value: {}", field, value);
      filter = QueryBuilders.termQuery(field, value);
    }

    return createNestedFilter(node, field, filter, context.get().getTypeModel());
  }

  @Override
  public QueryBuilder visitExists(@NonNull ExistsNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);
    val field = node.getField();
    log.debug("[visitExists] Field: {}", field);
    val result = QueryBuilders.existsQuery(field);

    return createNestedFilter(node, field, result, context.get().getTypeModel());
  }

  @Override
  public QueryBuilder visitMissing(@NonNull MissingNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);
    val field = node.getField();
    log.debug("[visitMissing] Field: {}", field);

    // TODO: Include in some common bool
    val missing = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(field));

    QueryBuilder result;
    // It is possible for genes to have no donors aside from a placeholder value.
    // In order to exclude these genes from filters with donors, we require a check for this case.
    // JIRA: DCC-3914 for more information.
    if (context.get().getType() == Type.GENE_CENTRIC && field.startsWith("donor.")) {
      result = QueryBuilders.boolQuery()
          .must(missing)
          .mustNot(QueryBuilders.termQuery("placeholder", true));
      ;
    } else {
      result = missing;
    }

    return createNestedFilter(node, field, result, context.get().getTypeModel());
  }

  @Override
  public QueryBuilder visitTerms(@NonNull TermsNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);
    val termsFilter = QueryBuilders.termsQuery(node.getField(), getValues(node));

    return createNestedFilter(node, node.getField(), termsFilter, context.get().getTypeModel());
  }

  private List<QueryBuilder> visitChildren(ExpressionNode node, Optional<QueryContext> context) {
    log.debug("Visiting Bool child: {}", node);
    val result = Lists.<QueryBuilder> newArrayList();
    for (val child : node.getChildren()) {
      log.debug("Sub-child: {}", child);
      result.add(child.accept(this, context));
    }

    return result;
  }

  /**
   * Wraps {@code field} in a {@code nested} query if the {@code node} which contains the {@code field} does not have a
   * {@link NestedNode} parent.
   */
  private QueryBuilder createNestedFilter(ExpressionNode node, String field, QueryBuilder sourceFilter,
      TypeModel typeModel) {
    if (typeModel.isNested(field) && !node.hasNestedParent() && !isNestedAggregationFilter(node)) {
      val nestedPath = typeModel.getNestedPath(field);
      log.debug("[visitTerm] Node '{}' does not have a nested parent. Nesting at path '{}'", node, nestedPath);

      // FIXME: Properly resolve score
      return QueryBuilders.nestedQuery(nestedPath, sourceFilter, ScoreMode.Avg);
    }

    return sourceFilter;
  }

  /**
   * If the {@code node} is a filter in a {@link FilterAggregationNode} which has a {@link NestedAggregationNode} parent
   * skip nesting the filter in a {@link NestedNode} because the AST structure is already correct
   */
  private boolean isNestedAggregationFilter(ExpressionNode node) {
    return Nodes.findParent(node, NestedAggregationNode.class).isPresent();
  }

}
