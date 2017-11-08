/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.portal.server.repository;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Lists.newArrayList;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.icgc.dcc.portal.server.util.ElasticsearchRequestUtils.setFetchSourceOfGetRequest;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.createResponseMap;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.flattenMap;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getLong;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getString;

import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.GeneSetType;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.model.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GeneSetRepository {

  /**
   * Constants.
   */
  private static final String INDEX_GENE_COUNT_FIELD_NAME = "_summary._gene_count";
  private static final String INDEX_GENE_SETS_NAME_FIELD_NAME = "name";
  public static final Map<String, String> SOURCE_FIELDS = ImmutableMap.of(
      "hierarchy", "gene_set_pathway.hierarchy",
      "inferredTree", "go_term.inferred_tree",
      "altIds", "go_term.alt_ids",
      "synonyms", "go_term.synonyms");

  private final Client client;
  private final String indexName;

  @Autowired
  public GeneSetRepository(@NonNull Client client, @Value("#{indexName}") String indexName) {
    this.indexName = indexName;
    this.client = client;
  }

  public int countGenes(@NonNull String id) {
    val geneSet = findOne(id, "geneCount");

    return Ints.saturatedCast(getLong(geneSet.get(INDEX_GENE_COUNT_FIELD_NAME)));
  }

  public Map<String, Integer> countGenes(@NonNull Iterable<String> ids) {
    val fieldName = INDEX_GENE_COUNT_FIELD_NAME;
    val response = findField(ids, fieldName);

    val map = Maps.<String, Integer> newLinkedHashMap();
    for (val hit : response.getHits()) {
      val id = hit.getId();
      val sourceMap = flattenMap(hit.getSource());
      val count = (Integer) sourceMap.get(fieldName);

      map.put(id, count);
    }

    return map;
  }

  public int countDecendants(@NonNull GeneSetType type, @NonNull Optional<String> id) {
    QueryBuilder query;
    QueryBuilder filter;

    switch (type) {
    case GO_TERM:
      filter = id.isPresent() ? termQuery("go_term.inferred_tree.id", id.get()) : termQuery("type", "go_term");
      query = boolQuery().must(matchAllQuery()).filter(filter);
      break;
    case PATHWAY:
      filter = id.isPresent() ? termQuery("pathway.hierarchy.id", id.get()) : termQuery("type", "pathway");
      query = boolQuery().must(matchAllQuery()).filter(filter);
      break;
    case CURATED_SET:
      filter = id.isPresent() ? termQuery("curated_set.id", id.get()) : termQuery("type", "curated_set");
      query = boolQuery().must(matchAllQuery()).filter(filter);
      break;
    default:
      checkState(false, "Unexpected type %s", type);
      return -1;
    }

    val result = client.prepareSearch(indexName)
        .setTypes(IndexType.GENE_SET.getId())
        .setQuery(query)
        .setSize(0)
        .get()
        .getHits()
        .getTotalHits();

    return Ints.saturatedCast(result);
  }

  public Map<String, String> findName(@NonNull Iterable<String> ids) {
    val fieldName = INDEX_GENE_SETS_NAME_FIELD_NAME;
    val response = findField(ids, fieldName);

    val map = Maps.<String, String> newLinkedHashMap();
    for (val hit : response.getHits()) {
      val id = hit.getId();
      val count = getString(hit.getSource().get(fieldName));

      map.put(id, count);
    }

    return map;
  }

  public Map<String, Object> findOne(@NonNull String id, String... fieldNames) {
    return findOne(id, ImmutableList.copyOf(fieldNames));
  }

  public Map<String, Object> findOne(String id, Iterable<String> fieldNames) {
    val query = Query.builder().fields(Lists.newArrayList(fieldNames)).build();
    val search = client.prepareGet(indexName, IndexType.GENE_SET.getId(), id);
    setFetchSourceOfGetRequest(search, query, EntityType.GENE_SET);

    GetResponse response = search.execute().actionGet();
    checkResponseState(id, response, EntityType.GENE_SET);

    val map = createResponseMap(response, query, EntityType.GENE_SET);
    log.debug("{}", map);

    return map;
  }

  private SearchResponse findField(Iterable<String> ids, String fieldName) {
    // The ArrayList is needed because Elasticsearch cannot serialize an ImmutableMapKeySet, or Iterables for that
    // matter.
    val filters = termsQuery("_id", newArrayList(ids));

    val search = client.prepareSearch(indexName)
        .setTypes(IndexType.GENE_SET.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(size(ids))
        .setPostFilter(filters)
        .setFetchSource(fieldName, null);

    return search.get();
  }

}
