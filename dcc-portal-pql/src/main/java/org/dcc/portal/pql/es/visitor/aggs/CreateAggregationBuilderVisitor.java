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

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.FilterAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.MissingAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.NestedAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.ReverseNestedAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.utils.Visitors;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.query.QueryContext;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

@Slf4j
public class CreateAggregationBuilderVisitor extends NodeVisitor<AbstractAggregationBuilder, QueryContext> {

  private static int DEFAULT_FACETS_SIZE = 1000;

  @Override
  public AbstractAggregationBuilder visitTermsAggregation(TermsAggregationNode node, Optional<QueryContext> context) {
    checkOptional(context);

    val fieldName = node.getFieldName();
    val result = AggregationBuilders
        .terms(node.getAggregationName())
        .size(DEFAULT_FACETS_SIZE)
        .field(fieldName);

    if (node.hasChildren()) {
      result.subAggregation(node.getFirstChild().accept(this, context));
    }

    return resolveGlobal(result, node, node.getAggregationName());
  }

  @Override
  public AbstractAggregationBuilder visitMissingAggregation(MissingAggregationNode node, Optional<QueryContext> context) {
    checkOptional(context);
    val fieldName = node.getFieldName();
    val result = AggregationBuilders
        .missing(node.getAggregationName())
        .field(fieldName);

    if (node.hasChildren()) {
      result.subAggregation(node.getFirstChild().accept(this, context));
    }

    return resolveGlobal(result, node, node.getAggregationName());
  }

  @Override
  public AbstractAggregationBuilder visitFilterAggregation(FilterAggregationNode node, Optional<QueryContext> context) {
    checkOptional(context);
    log.debug("Visiting FilterAggregationNode: \n{}", node);
    log.debug("Filters: {}", node.getFilters());

    val result = AggregationBuilders.filter(node.getAggregationName())
        .filter(resolveFilters(node, context))
        .subAggregation(node.getFirstChild().accept(this, context));

    return resolveGlobal(result, node, node.getAggregationName());
  }

  @Override
  public AbstractAggregationBuilder visitNestedAggregation(NestedAggregationNode node, Optional<QueryContext> context) {
    log.debug("Visiting NestedAggregationNode: \n{}", node);
    val subAggregation = node.getFirstChild().accept(this, context);

    val result = AggregationBuilders.nested(node.getAggregationName())
        .path(node.getPath())
        .subAggregation(subAggregation);

    return resolveGlobal(result, node, node.getAggregationName());
  }

  @Override
  public AbstractAggregationBuilder visitReverseNestedAggregation(ReverseNestedAggregationNode node,
      Optional<QueryContext> context) {

    return AggregationBuilders.reverseNested(node.getAggregationName());
  }

  private static boolean isGlobal(ExpressionNode node) {
    return node.getParent() instanceof AggregationsNode;
  }

  private static FilterBuilder resolveFilters(FilterAggregationNode parent, Optional<QueryContext> context) {
    return parent.getFilters().accept(Visitors.filterBuilderVisitor(), context);
  }

  private static AbstractAggregationBuilder resolveGlobal(AbstractAggregationBuilder builder, ExpressionNode node,
      String aggregationName) {
    if (isGlobal(node)) {
      return AggregationBuilders.global(aggregationName)
          .subAggregation(builder);
    }

    return builder;
  }

}
