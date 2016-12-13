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

import static com.google.common.collect.Lists.transform;
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.action.search.SearchType.SCAN;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filter;
import static org.elasticsearch.search.aggregations.AggregationBuilders.nested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.server.model.IndexModel.MAX_FACET_TERM_COUNT;
import static org.icgc.dcc.portal.server.model.IndexModel.getFields;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.server.util.Filters.andFilter;
import static org.icgc.dcc.portal.server.util.Filters.geneSetFilter;
import static org.icgc.dcc.portal.server.util.Filters.inputGeneSetFilter;
import static org.icgc.dcc.portal.server.util.SearchResponses.hasHits;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.Universe;
import org.icgc.dcc.portal.server.pql.convert.Jql2PqlConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GeneRepository implements Repository {

  private static final IndexType CENTRIC_TYPE = IndexType.GENE_CENTRIC;
  private static final String GENE_TEXT = IndexType.GENE_TEXT.getId();
  private static final TimeValue KEEP_ALIVE = new TimeValue(10000);
  private static final String GENE_SYMBOL_FIELD_NAME = "symbol";
  private static final String ENSEMBL_ID_FIELD_NAME = "id";
  private static final String[] GENE_SYMBOL_ENSEMBL_ID_FIELDS = { GENE_SYMBOL_FIELD_NAME, ENSEMBL_ID_FIELD_NAME };

  public static final Map<String, String> GENE_ID_SEARCH_FIELDS = ImmutableMap.of(
      "id.search", "_gene_id",
      "symbol.search", "symbol",
      "uniprotkbSwissprot.search", "external_db_ids.uniprotkb_swissprot");

  /**
   * Dependencies.
   */
  private final Client client;
  private final String indexName;

  private final QueryEngine queryEngine;
  private final Jql2PqlConverter converter = Jql2PqlConverter.getInstance();

  @Autowired
  GeneRepository(Client client, QueryEngine queryEngine, @Value("#{indexName}") String indexName) {
    this.indexName = indexName;
    this.client = client;
    this.queryEngine = queryEngine;
  }

  @Override
  @NonNull
  public SearchResponse findAllCentric(Query query) {

    // Converter does not handle limits
    Integer limit = query.getLimit();
    query.setLimit(null);

    val pql = converter.convert(query, GENE_CENTRIC);
    log.info(" find all centric {}", pql);
    val search = queryEngine.execute(pql, GENE_CENTRIC);
    if (limit != null) {
      search.getRequestBuilder().setSize(limit.intValue());
    }

    log.info(" find all centric {}", search);

    return search.getRequestBuilder().execute().actionGet();
  }

  @NonNull
  public SearchResponse findAllCentric(StatementNode pqlAst) {
    val request = queryEngine.execute(pqlAst, GENE_CENTRIC);

    return request.getRequestBuilder().execute().actionGet();
  }

  private Map<String, String> findGeneSymbolsByFilters(@NonNull ObjectNode filters) {
    val maxGenes = 70000;
    val symbolFieldName = "symbol";

    val query = Query.builder().filters(filters).build();
    val pql = converter.convert(query, GENE_CENTRIC);
    val response = queryEngine.execute(pql, GENE_CENTRIC).getRequestBuilder()
        .setSize(maxGenes)
        .addField(symbolFieldName)
        .execute().actionGet();

    val map = Maps.<String, String> newLinkedHashMap();
    for (val hit : response.getHits()) {
      String id = hit.getId();
      String symbol = hit.getFields().get(symbolFieldName).getValue();

      map.put(id, symbol);
    }

    return map;
  }

  public Map<String, String> findGeneSymbolsByGeneListIdAndGeneSetId(@NonNull UUID inputGeneListId,
      @NonNull String geneSetId) {
    val filters = andFilter(geneSetFilter(geneSetId), inputGeneSetFilter(inputGeneListId));
    return findGeneSymbolsByFilters(filters);
  }

  public Map<String, String> findGeneSymbolsByGeneListId(@NonNull UUID inputGeneSetId) {
    val filters = inputGeneSetFilter(inputGeneSetId);
    return findGeneSymbolsByFilters(filters);
  }

  public SearchResponse findGeneSetCounts(Query query) {
    log.info(" My Query {} ", query.getFilters());

    val pql = converter.convert(query, GENE_CENTRIC);
    val search = queryEngine.execute(pql, GENE_CENTRIC);

    for (val universe : Universe.values()) {
      val universeAggName = universe.getGeneSetFacetName();

      search.getRequestBuilder()
          .addAggregation(terms(universeAggName).field(universeAggName).size(50000));
    }

    search.getRequestBuilder().setSearchType(COUNT);
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
    val pql = converter.convertCount(query, GENE_CENTRIC);
    val search = queryEngine.execute(pql, GENE_CENTRIC).getRequestBuilder();

    log.debug("{}", search);
    return search.execute().actionGet().getHits().getTotalHits();
  }

  @Override
  public MultiSearchResponse counts(LinkedHashMap<String, Query> queries) {
    val search = client.prepareMultiSearch();

    for (val query : queries.values()) {
      val pql = converter.convertCount(query, GENE_CENTRIC);
      search.add(queryEngine.execute(pql, GENE_CENTRIC).getRequestBuilder());
    }

    log.debug("{}", search);

    return search.execute().actionGet();
  }

  @Override
  public MultiSearchResponse nestedCounts(LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    val search = client.prepareMultiSearch();

    for (val nestedQuery : queries.values()) {
      for (val innerQuery : nestedQuery.values()) {
        val pql = converter.convertCount(innerQuery, GENE_CENTRIC);
        search.add(queryEngine.execute(pql, GENE_CENTRIC).getRequestBuilder());
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
    val search = client.prepareGet(indexName, IndexType.GENE.getId(), id);

    val sourceFields = prepareSourceFields(query, getFields(query, EntityType.GENE));
    String[] excludeFields = null;
    search.setFetchSource(sourceFields, excludeFields);

    val response = search.execute().actionGet();
    checkResponseState(id, response, EntityType.GENE);

    val result = response.getSource();
    log.debug("{}", result);

    return result;
  }

  private String[] prepareSourceFields(Query query, String[] fields) {
    val typeFieldsMap = FIELDS_MAPPING.get(EntityType.GENE);
    val result = Lists.newArrayList(fields);

    if (!query.hasFields()) {
      result.add(typeFieldsMap.get("externalDbIds"));
      result.add(typeFieldsMap.get("pathways"));
    }

    if (query.getIncludes() != null) {
      result.addAll(query.getIncludes());
    }

    return result.toArray(new String[result.size()]);
  }

  /*
   * Lookup up genes by ensembl gene_id or gene symbol or uniprot
   * 
   * @param input a list of string identifiers of either ensembl id or gene symbol or uniprot
   * 
   * @returns a map of matched identifiers
   */
  public SearchResponse validateIdentifiers(List<String> input) {
    val boolQuery = boolQuery();

    val search = client.prepareSearch(indexName)
        .setTypes("gene-text")
        .setSearchType(QUERY_THEN_FETCH)
        .setSize(5000);

    for (val searchField : GENE_ID_SEARCH_FIELDS.keySet()) {
      boolQuery.should(termsQuery(searchField, input.toArray()));

      search.addHighlightedField(searchField);
      search.addField(searchField);
    }

    search.setQuery(boolQuery);
    log.info("Search is {}", search);

    return search.execute().actionGet();
  }

  /**
   * Find transcripts for a specific gene that have mutations
   * @param geneId
   * @return unique list of transcript ids
   */
  public List<String> getAffectedTranscripts(String geneId) {
    val ssmConsequence = "donor.ssm.consequence";
    val transcriptField = ssmConsequence + ".transcript_affected";
    val geneIdField = ssmConsequence + "._gene_id";
    val rootAgg = "aggs";
    val filteredAgg = "filtered";
    val aggName = "affectedTranscript";

    val response = searchGenes(CENTRIC_TYPE.getId(), "getAffectedTranscripts", request -> {
      request
          .setTypes(CENTRIC_TYPE.getId())
          .setSearchType(QUERY_THEN_FETCH)
          .setSize(0)
          .addAggregation(nested(rootAgg)
              .path(ssmConsequence)
              .subAggregation(filter(filteredAgg)
                  .filter(termFilter(geneIdField, geneId))
                  .subAggregation(terms(aggName)
                      .size(MAX_FACET_TERM_COUNT)
                      .field(transcriptField))));
    });

    val nestedAggs = (Nested) response.getAggregations().get(rootAgg);
    val filteredAggs = (Filter) nestedAggs.getAggregations().get(filteredAgg);
    val aggs = (Terms) filteredAggs.getAggregations().get(aggName);

    val aggsTransform = transform(aggs.getBuckets(), bucket -> bucket.getKeyAsText().toString());

    return aggsTransform;
  }

  public Multimap<String, String> getGeneSymbolEnsemblIdMap() {
    val result = ImmutableMultimap.<String, String> builder();
    String scrollId = prepareScrollSearch(GENE_SYMBOL_ENSEMBL_ID_FIELDS).getScrollId();

    while (true) {
      val response = fetchScrollData(scrollId);

      if (!hasHits(response)) {
        break;
      }

      for (val hit : response.getHits()) {
        val values = hit.getFields();
        val ensemblId = values.get(ENSEMBL_ID_FIELD_NAME).getValue().toString();
        val geneSymbol = values.get(GENE_SYMBOL_FIELD_NAME).getValue().toString();

        result.put(geneSymbol, ensemblId);
      }

      scrollId = response.getScrollId();
    }

    return result.build();
  }

  @NonNull
  private SearchResponse searchGenes(String indexType, String logMessage,
      Consumer<SearchRequestBuilder> customizer) {
    val request = client.prepareSearch(indexName).setTypes(indexType);
    customizer.accept(request);

    log.debug("{}; ES query is: '{}'", logMessage, request);
    return request.execute().actionGet();
  }

  private SearchResponse prepareScrollSearch(String[] fields) {
    val batchSize = 5000;

    return searchGenes(GENE_TEXT, "prepareScrollSearch", request -> {
      request.setSearchType(SCAN)
          .setSize(batchSize)
          .setScroll(KEEP_ALIVE)
          .addFields(fields);
    });
  }

  private SearchResponse fetchScrollData(String scrollId) {
    return client.prepareSearchScroll(scrollId)
        .setScroll(KEEP_ALIVE)
        .execute().actionGet();
  }

}
