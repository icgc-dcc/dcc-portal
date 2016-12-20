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
import static org.dcc.portal.pql.es.utils.VisitorHelpers.checkOptional;
import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.elasticsearch.index.query.FilterBuilders.existsFilter;
import static org.elasticsearch.index.query.FilterBuilders.missingFilter;
import static org.elasticsearch.index.query.FilterBuilders.nestedFilter;
import static org.elasticsearch.index.query.FilterBuilders.notFilter;
import static org.elasticsearch.index.query.FilterBuilders.rangeFilter;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.index.query.FilterBuilders.termsFilter;

import java.util.Optional;
import java.util.Stack;

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
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.index.query.TermFilterBuilder;

import com.google.common.collect.Lists;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Visits {@link FilterNode} and builds FilterBuilders
 */
@Slf4j
@NoArgsConstructor
public class FilterBuilderVisitor extends NodeVisitor<FilterBuilder, QueryContext> {

  private final Stack<FilterBuilder> stack = new Stack<FilterBuilder>();

  @Override
  public FilterBuilder visitFilter(@NonNull FilterNode node, @NonNull Optional<QueryContext> context) {
    return node.getFirstChild().accept(this, context);
  }

  @Override
  public FilterBuilder visitQuery(@NonNull QueryNode node, @NonNull Optional<QueryContext> context) {
    val queryBuilder = node.accept(Visitors.createQueryBuilderVisitor(), context);

    return FilterBuilders.queryFilter(queryBuilder);
  }

  @Override
  public FilterBuilder visitBool(@NonNull BoolNode node, @NonNull Optional<QueryContext> context) {
    BoolFilterBuilder resultBuilder = boolFilter();
    for (val child : node.getChildren()) {
      val childrenResult = visitChildren(child, context);
      if (child instanceof MustBoolNode) {
        resultBuilder.must(childrenResult);
      } else if (child instanceof ShouldBoolNode) {
        resultBuilder.should(childrenResult);
      } else {
        throw new IllegalStateException(format("Operation type %s is not supported", child.getNodeName()));
      }
    }

    return resultBuilder;
  }

  @Override
  public FilterBuilder visitGreaterEqual(@NonNull GreaterEqualNode node, @NonNull Optional<QueryContext> context) {
    val rangeFilter = (RangeFilterBuilder) stack.peek();
    checkNotNull(rangeFilter, "Could not find the RangeFilter on the stack");
    rangeFilter.gte(node.getValue());

    return rangeFilter;
  }

  @Override
  public FilterBuilder visitGreaterThan(@NonNull GreaterThanNode node, @NonNull Optional<QueryContext> context) {
    val rangeFilter = (RangeFilterBuilder) stack.peek();
    checkNotNull(rangeFilter, "Could not find the RangeFilter on the stack");
    rangeFilter.gt(node.getValue());

    return rangeFilter;
  }

  @Override
  public FilterBuilder visitLessEqual(@NonNull LessEqualNode node, @NonNull Optional<QueryContext> context) {
    val rangeFilter = (RangeFilterBuilder) stack.peek();
    checkNotNull(rangeFilter, "Could not find the RangeFilter on the stack");
    rangeFilter.lte(node.getValue());

    return rangeFilter;
  }

  @Override
  public FilterBuilder visitLessThan(@NonNull LessThanNode node, @NonNull Optional<QueryContext> context) {
    val rangeFilter = (RangeFilterBuilder) stack.peek();
    checkNotNull(rangeFilter, "Could not find the RangeFilter on the stack");
    rangeFilter.lt(node.getValue());

    return rangeFilter;
  }

  @Override
  public FilterBuilder visitNested(@NonNull NestedNode node, @NonNull Optional<QueryContext> context) {
    log.debug("Visiting Nested: {}", node);

    return nestedFilter(node.getPath(), node.getFirstChild().accept(this, context));
  }

  @Override
  public FilterBuilder visitNot(@NonNull NotNode node, @NonNull Optional<QueryContext> context) {
    val childrenCount = node.childrenCount();
    checkState(childrenCount == 1, "NotNode can have only one child. Found %s", childrenCount);

    return notFilter(node.getFirstChild().accept(this, context));
  }

  @Override
  public FilterBuilder visitRange(@NonNull RangeNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);
    checkState(node.childrenCount() > 0, "RangeNode has no children");

    stack.push(rangeFilter(node.getFieldName()));
    for (val child : node.getChildren()) {
      child.accept(this, context);
    }

    return createNestedFilter(node, node.getFieldName(), stack.pop(), context.get().getTypeModel());
  }

  @Override
  public FilterBuilder visitTerm(@NonNull TermNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);

    FilterBuilder filter;
    val field = node.getField();
    val lookupInfo = node.getLookup();
    if (lookupInfo.isDefine()) {
      filter = FilterBuilders.termsLookupFilter(field)
          .lookupCache(false)
          .lookupIndex(lookupInfo.getIndex())
          .lookupType(lookupInfo.getType())
          .lookupPath(lookupInfo.getPath())
          .lookupId(lookupInfo.getId());
    } else {
      val value = node.getValueNode().getValue();
      log.debug("[visitTerm] Name: {}, Value: {}", field, value);
      filter = termFilter(field, value);
    }

    return createNestedFilter(node, field, filter, context.get().getTypeModel());
  }

  @Override
  public FilterBuilder visitExists(@NonNull ExistsNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);
    val field = node.getField();
    log.debug("[visitExists] Field: {}", field);
    val result = existsFilter(field);

    return createNestedFilter(node, field, result, context.get().getTypeModel());
  }

  @Override
  public FilterBuilder visitMissing(@NonNull MissingNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);
    val field = node.getField();
    log.debug("[visitMissing] Field: {}", field);
    val missing = missingFilter(field);

    FilterBuilder result;
    // It is possible for genes to have no donors aside from a placeholder value.
    // In order to exclude these genes from filters with donors, we require a check for this case.
    // JIRA: DCC-3914 for more information.
    if (context.get().getType() == Type.GENE_CENTRIC && field.startsWith("donor.")) {
      result = new BoolFilterBuilder()
          .must(missing)
          .mustNot(new TermFilterBuilder("placeholder", true));
    } else {
      result = missing;
    }

    return createNestedFilter(node, field, result, context.get().getTypeModel());
  }

  @Override
  public FilterBuilder visitTerms(@NonNull TermsNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);
    val termsFilter = termsFilter(node.getField(), getValues(node));

    return createNestedFilter(node, node.getField(), termsFilter, context.get().getTypeModel());
  }

  private FilterBuilder[] visitChildren(ExpressionNode node, Optional<QueryContext> context) {
    log.debug("Visiting Bool child: {}", node);
    val result = Lists.<FilterBuilder> newArrayList();
    for (val child : node.getChildren()) {
      log.debug("Sub-child: {}", child);
      result.add(child.accept(this, context));
    }

    return result.toArray(new FilterBuilder[result.size()]);
  }

  /**
   * Wraps {@code field} in a {@code nested} query if the {@code node} which contains the {@code field} does not have a
   * {@link NestedNode} parent.
   */
  private FilterBuilder createNestedFilter(ExpressionNode node, String field, FilterBuilder sourceFilter,
      TypeModel typeModel) {
    if (typeModel.isNested(field) && !node.hasNestedParent() && !isNestedAggregationFilter(node)) {
      val nestedPath = typeModel.getNestedPath(field);
      log.debug("[visitTerm] Node '{}' does not have a nested parent. Nesting at path '{}'", node, nestedPath);

      return nestedFilter(nestedPath, sourceFilter);
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
