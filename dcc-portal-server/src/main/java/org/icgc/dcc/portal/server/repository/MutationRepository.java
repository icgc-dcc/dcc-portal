/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.server.repository;

import static java.lang.String.format;
import static org.apache.lucene.search.join.ScoreMode.Avg;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.icgc.dcc.portal.server.util.ElasticsearchRequestUtils.setFetchSourceOfGetRequest;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.createResponseMap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.pql.convert.Jql2PqlConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MutationRepository implements Repository {

  private static final String[] NO_EXCLUDE = null;
  private static final String[] PROTEIN_FIELDS =
      new String[] { "_mutation_id", "mutation", "transcript.id", "transcript.consequence.aa_mutation", "transcript.functional_impact_prediction_summary", "_summary._affected_donor_count"
      };
  private static final IndexType CENTRIC_TYPE = IndexType.MUTATION_CENTRIC;
  private final QueryEngine queryEngine;
  private final Jql2PqlConverter converter = Jql2PqlConverter.getInstance();

  private final Client client;
  private final String indexName;

  @Autowired
  MutationRepository(Client client, QueryEngine queryEngine, @Value("#{indexName}") String indexName) {
    this.indexName = indexName;
    this.client = client;
    this.queryEngine = queryEngine;
  }

  @Override
  @NonNull
  public SearchResponse findAllCentric(Query query) {
    val pql = converter.convert(query, MUTATION_CENTRIC);
    val search = queryEngine.execute(pql, MUTATION_CENTRIC);

    return search.getRequestBuilder().get();
  }

  @NonNull
  public SearchResponse findAllCentric(StatementNode pqlAst) {
    val search = queryEngine.execute(pqlAst, MUTATION_CENTRIC);
    log.debug("Mutation : {}", search.getRequestBuilder());
    return search.getRequestBuilder().get();
  }

  /**
   * The logic is kind of reversed here. We want to find top mutations of <strong>donorId</strong> while using query as
   * a scoring mechanism. I.e., find top 10 mutations of donor X where mutations are ranked by number of donors affected
   * within the same project as donor X.
   */
  public SearchResponse findMutationsByDonor(@NonNull Query query, @NonNull String donorId) {
    val pql = converter.convert(query, MUTATION_CENTRIC);
    val search = queryEngine.execute(pql, MUTATION_CENTRIC);

    val termFilter = termQuery("ssm_occurrence.donor._donor_id", donorId);
    val nestedFilter = nestedQuery("ssm_occurrence", termFilter, Avg);
    search.getRequestBuilder().setPostFilter(nestedFilter);

    log.debug("Find mutations by donor {}", search.getRequestBuilder());

    return search.getRequestBuilder().execute().actionGet();
  }

  @Override
  public SearchResponse findAll(Query query) {
    throw new UnsupportedOperationException("Not applicable");
  }

  @Override
  public SearchRequestBuilder buildFindAllRequest(Query query, IndexType type) {
    throw new UnsupportedOperationException("Not applicable");
  }

  @Override
  public long count(Query query) {
    log.debug("Count Query {}", query.getFilters());
    val pql = converter.convertCount(query, MUTATION_CENTRIC);
    val search = queryEngine.execute(pql, MUTATION_CENTRIC);
    return search.getRequestBuilder().execute().actionGet().getHits().getTotalHits();
  }

  @Override
  public MultiSearchResponse counts(@NonNull LinkedHashMap<String, Query> queries) {
    val search = client.prepareMultiSearch();

    for (val query : queries.values()) {
      val pql = converter.convertCount(query, MUTATION_CENTRIC);
      search.add(queryEngine.execute(pql, MUTATION_CENTRIC).getRequestBuilder());
    }

    log.debug("{}", search);
    return search.execute().actionGet();
  }

  @NonNull
  public MultiSearchResponse counts(List<String> geneIds) {
    val pqlTemplate = "count(), nested (transcript, in (gene.id, '%s'))";
    val search = client.prepareMultiSearch();

    for (val id : geneIds) {
      val pql = format(pqlTemplate, id);

      search.add(queryEngine.execute(pql, MUTATION_CENTRIC).getRequestBuilder());
    }

    log.debug("{}", search);
    return search.execute().actionGet();
  }

  public MultiSearchResponse countSearches(@NonNull List<QueryBuilder> searches) {
    val search = client.prepareMultiSearch();
    for (val s : searches) {
      search.add(buildCountSearchFromQuery(s, CENTRIC_TYPE));
    }

    log.debug("{}", search);
    return search.execute().actionGet();
  }

  public SearchRequestBuilder buildCountSearchFromQuery(QueryBuilder query, IndexType type) {
    val search = client.prepareSearch(indexName).setTypes(type.getId()).setSize(0);
    search.setQuery(query);

    return search;
  }

  @Override
  public MultiSearchResponse nestedCounts(LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    val search = client.prepareMultiSearch();

    for (val nestedQuery : queries.values()) {
      for (val innerQuery : nestedQuery.values()) {
        val pql = converter.convertCount(innerQuery, MUTATION_CENTRIC);
        search.add(queryEngine.execute(pql, MUTATION_CENTRIC).getRequestBuilder());
      }
    }

    log.debug("{}", search);
    return search.execute().actionGet();
  }

  @Override
  public NestedQueryBuilder buildQuery(Query query) {
    throw new UnsupportedOperationException("Not applicable");
  }

  public Map<String, Object> findOne(String id, Query query) {
    val search = client.prepareGet(indexName, CENTRIC_TYPE.getId(), id);
    setFetchSourceOfGetRequest(search, query, EntityType.MUTATION);
    val response = search.execute().actionGet();
    checkResponseState(id, response, EntityType.MUTATION);

    val map = createResponseMap(response, query, EntityType.MUTATION);
    log.debug("{}", map);

    return map;
  }

  public SearchResponse protein(Query query) {
    val pql = converter.convert(query, MUTATION_CENTRIC);
    val search = queryEngine.execute(pql, MUTATION_CENTRIC)
        .getRequestBuilder();

    search.setFrom(0)
        .setSize(10000)
        .setFetchSource(PROTEIN_FIELDS, NO_EXCLUDE);

    log.debug("!!! {}", search);

    val response = search.execute().actionGet();
    return response;
  }
}
