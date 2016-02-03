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
package org.dcc.portal.pql.query;

import static org.dcc.portal.pql.es.utils.Visitors.createAggregationBuilderVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createQueryBuilderVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.filterBuilderVisitor;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.elasticsearch.action.search.SearchType.COUNT;

import java.util.Collection;
import java.util.Optional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.CountNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.FieldsNode;
import org.dcc.portal.pql.es.ast.LimitNode;
import org.dcc.portal.pql.es.ast.SortNode;
import org.dcc.portal.pql.es.ast.SourceNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.sort.SortOrder;

@Slf4j
@RequiredArgsConstructor
public class EsRequestBuilder {

  private static final String[] NO_EXCLUDE = null;

  @NonNull
  private final Client client;

  public SearchRequestBuilder buildSearchRequest(@NonNull ExpressionNode esAst, @NonNull QueryContext queryContext) {
    boolean containsCount = false;
    val result = client
        .prepareSearch(queryContext.getIndex())
        .setTypes(queryContext.getType().getId());

    for (val child : esAst.getChildren()) {
      if (child instanceof FilterNode) {
        result.setPostFilter(toBuilder(child, filterBuilderVisitor(), queryContext));
      } else if (child instanceof QueryNode) {
        result.setQuery(toBuilder(child, createQueryBuilderVisitor(), queryContext));
      } else if (child instanceof AggregationsNode) {
        addAggregations(result, child, queryContext);
      } else if (child instanceof FieldsNode) {
        val fields = ((FieldsNode) child).getFields();
        result.addFields(toStringArray(fields));
      } else if (child instanceof SourceNode) {
        val includeFields = ((SourceNode) child).getFields();
        result.setFetchSource(toStringArray(includeFields), NO_EXCLUDE);
      } else if (child instanceof LimitNode) {
        val limit = (LimitNode) child;
        result.setFrom(limit.getFrom())
            .setSize(limit.getSize());
      } else if (child instanceof SortNode) {
        addSorts(result, (SortNode) child, queryContext);
      } else if (child instanceof CountNode) {
        containsCount = true;
      }
    }

    if (containsCount) {
      log.debug("Setting search type to COUNT");
      result.setSearchType(COUNT);
    }

    return result;
  }

  private static void addAggregations(SearchRequestBuilder result, ExpressionNode aggregations, QueryContext context) {
    log.debug("Adding aggregations for AggregationsNode\n{}", aggregations);

    for (val child : aggregations.getChildren()) {
      result.addAggregation(toBuilder(child, createAggregationBuilderVisitor(), context));
    }
  }

  private static String[] toStringArray(Collection<String> source) {
    return source.stream().toArray(String[]::new);
  }

  private static <T> T toBuilder(ExpressionNode child, NodeVisitor<T, QueryContext> visitor, QueryContext context) {
    return child.accept(visitor, Optional.of(context));
  }

  private static void addSorts(SearchRequestBuilder builder, SortNode sorts, QueryContext context) {
    // TODO: Temporary solution to the sorting issue on the donor tab when sorting by primary site or project.
    // JIRA: DCC-3791
    val type = context.getType();
    val isDonorCentric = (type == DONOR_CENTRIC);
    val prefix = type.getId();

    for (val sort : sorts.getFields().entrySet()) {
      val fieldName = sort.getKey();
      val qualifiedName = (isDonorCentric && fieldName.startsWith("project.")) ? (prefix + "." + fieldName) : fieldName;
      val sortOrder = SortOrder.valueOf(sort.getValue().toString());

      builder.addSort(qualifiedName, sortOrder);
    }
  }

}
