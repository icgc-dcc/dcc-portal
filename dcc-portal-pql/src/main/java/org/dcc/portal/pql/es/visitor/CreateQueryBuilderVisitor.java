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
import static java.util.Optional.empty;
import static org.elasticsearch.index.query.FilterBuilders.termsFilter;
import static org.elasticsearch.index.query.FilterBuilders.termsLookupFilter;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

import java.util.Optional;

import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.NotNode;
import org.dcc.portal.pql.es.ast.filter.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
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
    val nestedOpt = nestedSoloTerm(node);
    if (nestedOpt.isPresent()) {
      return nestedOpt.get();
    }

    val query = node.getFirstChild().accept(this, context);

    return QueryBuilders
        .nestedQuery(node.getPath(), query)
        .scoreMode(node.getScoreMode().getId());
  }

  /**
   * We will only visit this in the case of a NOT node.
   */
  @Override
  public QueryBuilder visitTerm(@NonNull TermNode node, @NonNull Optional<QueryContext> context) {
    val lookup = node.getLookup();
    if (lookup != null && !lookup.getId().isEmpty()) {
      val termsLookup = termsLookupFilter(node.getField())
          .lookupCache(false)
          .lookupPath(lookup.getPath())
          .lookupIndex(lookup.getIndex())
          .lookupType(lookup.getType())
          .lookupId(lookup.getId());

      return filteredQuery(matchAllQuery(), termsLookup);
    }
    return termQuery(node.getField(), node.getValueNode().getValue());
  }

  @Override
  public QueryBuilder visitTerms(@NonNull TermsNode termsNode, @NonNull Optional<QueryContext> context) {
    val terms =
        termsNode.getChildren().stream()
            .map(child -> ((TerminalNode) child).getValue())
            .collect(toImmutableList());

    return termsQuery(termsNode.getField(), terms);
  }

  @Override
  public QueryBuilder visitNot(@NonNull NotNode node, @NonNull Optional<QueryContext> context) {
    val innerQuery = node.getFirstChild().accept(this, context);

    return boolQuery().mustNot(innerQuery);
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

  /**
   * This helper gets called for a nested Node while inside of a 'NOT'. The goal is to prevent scoring on a not(nested)
   * node. We also use filtered queries rather than regular queries in order to allow for caching of the term filters.
   * 
   * @param node - NestedNode representing the Nested query inside of a 'NOT'
   * @return Optional with NestedQueryBuilder if we have a lone term or terms node in the nested, otherwrise empty
   */
  private static Optional<QueryBuilder> nestedSoloTerm(NestedNode node) {
    if (node.getFirstChild() instanceof TermsNode) {
      val termsNode = (TermsNode) node.getFirstChild();
      val values =
          termsNode.getChildren().stream()
              .map(child -> ((TerminalNode) child).getValue())
              .collect(toImmutableList());
      val query = nestedQuery(node.getPath(), filteredQuery(QueryBuilders.matchAllQuery(),
          termsFilter(termsNode.getField(), values)));
      return Optional.of(query);
    } else if (node.getFirstChild() instanceof TermNode) {
      val termNode = (TermNode) node.getFirstChild();
      val query = nestedQuery(node.getPath(), filteredQuery(QueryBuilders.matchAllQuery(),
          termsFilter(termNode.getField(), termNode.getValueNode().getValue())));
      return Optional.of(query);
    }

    return empty();
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
