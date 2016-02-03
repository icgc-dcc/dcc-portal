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

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import java.util.Optional;

import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.query.FunctionScoreNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.utils.Visitors;
import org.dcc.portal.pql.query.QueryContext;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;

import lombok.NonNull;
import lombok.val;

/**
 * Creates QueryBuilder by visiting {@link QueryNode}
 */
public class CreateQueryBuilderVisitor extends NodeVisitor<QueryBuilder, QueryContext> {

  @Override
  public QueryBuilder visitQuery(@NonNull QueryNode node, @NonNull Optional<QueryContext> context) {
    // There are no cases yet where a QueryNode has 2 children. But we should enforce the correct structure.
    verifyQueryChildren(node);

    return node.getFirstChild().accept(this, context);
  }

  @Override
  public QueryBuilder visitNested(@NonNull NestedNode node, @NonNull Optional<QueryContext> context) {
    val scoreQuery = node.getFirstChild().accept(this, context);

    return QueryBuilders
        .nestedQuery(node.getPath(), scoreQuery)
        .scoreMode(node.getScoreMode().getId());
  }

  @Override
  public QueryBuilder visitFunctionScore(@NonNull FunctionScoreNode node, @NonNull Optional<QueryContext> context) {
    FunctionScoreQueryBuilder result = null;
    val filterNode = Nodes.getOptionalChild(node, FilterNode.class);

    if (filterNode.isPresent()) {
      val filteredQuery = filterNode.get().accept(this, context);
      result = QueryBuilders.functionScoreQuery(filteredQuery).boostMode(CombineFunction.REPLACE);
    } else {
      result = QueryBuilders.functionScoreQuery().boostMode(CombineFunction.REPLACE);
    }

    return result.add(ScoreFunctionBuilders.scriptFunction(node.getScript()));
  }

  @Override
  public QueryBuilder visitFilter(@NonNull FilterNode node, @NonNull Optional<QueryContext> context) {
    val filterBuilder = node.accept(Visitors.filterBuilderVisitor(), context);

    return QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filterBuilder);
  }

  @Override
  public QueryBuilder visitBool(@NonNull BoolNode node, @NonNull Optional<QueryContext> context) {
    val boolQueryBuilder = QueryBuilders.boolQuery();
    for (val child : node.getChildren()) {
      if (child instanceof MustBoolNode) {
        for (val subChild : child.getChildren()) {
          boolQueryBuilder.must(subChild.accept(this, context));
        }
      } else if (child instanceof ShouldBoolNode) {
        for (val subChild : child.getChildren()) {
          boolQueryBuilder.should(subChild.accept(this, context));
        }
      } else {
        throw new IllegalStateException(format("Operation type %s is not supported", child.getNodeName()));
      }

    }

    return boolQueryBuilder;
  }

  private static void verifyQueryChildren(QueryNode node) {
    checkState(node.childrenCount() < 3, format("The QueryNode has more than 2 children. Valid children: "
        + "QueryNode, FilterNode. The QueryNode: %n%s", node));

    if (node.childrenCount() > 1) {
      verifyProperChildren(node);
    }
  }

  private static void verifyProperChildren(QueryNode node) {
    for (val child : node.getChildren()) {
      checkState(child instanceof QueryNode || child instanceof FilterNode,
          format("Found non QueryNode or FilterNode child. The QueryNode: %n%s", node));
    }
  }

}
