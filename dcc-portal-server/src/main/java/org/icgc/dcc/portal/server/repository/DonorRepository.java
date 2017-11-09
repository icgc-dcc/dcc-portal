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

import static com.google.common.base.Functions.constant;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.toMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newHashSetWithExpectedSize;
import static java.util.Collections.singletonMap;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.facets;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.dcc.portal.pql.query.PqlParser.parse;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.missing;
import static org.elasticsearch.search.aggregations.AggregationBuilders.stats;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.server.model.IndexModel.MAX_FACET_TERM_COUNT;
import static org.icgc.dcc.portal.server.model.IndexModel.TEXT_PREFIX;
import static org.icgc.dcc.portal.server.model.IndexType.DONOR;
import static org.icgc.dcc.portal.server.model.IndexType.DONOR_TEXT;
import static org.icgc.dcc.portal.server.model.IndexType.FILE_DONOR_TEXT;
import static org.icgc.dcc.portal.server.model.fields.SearchFieldMapper.searchFieldMapper;
import static org.icgc.dcc.portal.server.repository.TermsLookupRepository.createTermsLookupFilter;
import static org.icgc.dcc.portal.server.util.ElasticsearchRequestUtils.isRepositoryDonor;
import static org.icgc.dcc.portal.server.util.ElasticsearchRequestUtils.setFetchSourceOfGetRequest;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.createResponseMap;
import static org.icgc.dcc.portal.server.util.SearchResponses.hasHits;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.icgc.dcc.portal.server.model.EntitySetTermFacet;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.Statistics;
import org.icgc.dcc.portal.server.model.TermFacet.Term;
import org.icgc.dcc.portal.server.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.server.repository.TermsLookupRepository.TermLookupType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DonorRepository implements Repository {

  private static final String[] NO_EXCLUDE = null;
  // These are the raw field names from the 'donor-text' type in the main index.
  public static final Map<String, String> DONOR_ID_SEARCH_FIELDS = transformToTextSearchFieldMap(
      "id", "submittedId", "specimenIds", "sampleIds", "submittedSpecimenIds", "submittedSampleIds");

  // These are the raw field names from the 'file-donor-text' type in the icgc-repository index.
  public static final Map<String, String> FILE_DONOR_ID_SEARCH_FIELDS = transformToTextSearchFieldMap(
      "id", "specimen_id", "sample_id", "tcga_participant_barcode", "tcga_sample_barcode",
      "tcga_aliquot_barcode", "submitted_specimen_id", "submitted_sample_id", "submitted_donor_id");

  private static final class PhenotypeFacetNames {

    private static final String AGE_AT_DIAGNOSIS = "ageAtDiagnosis";
    private static final String AVERAGE_AGE_AT_DIAGNOSIS = "Average" + AGE_AT_DIAGNOSIS;
    private static final String AGE_AT_DIAGNOSIS_GROUP = "ageAtDiagnosisGroup";
    private static final String GENDER = "gender";
    private static final String VITAL_STATUS = "vitalStatus";
  }

  private static final Optional<SimpleImmutableEntry<String, String>> EMPTY_PAIR = Optional.empty();

  private static boolean wantsStatistics(Optional<SimpleImmutableEntry<String, String>> value) {
    return value.isPresent();
  }

  /**
   * A lookup table of facet name (the key) and its corresponding stats facet name pair (the value) for phenotype
   * analyses. Set the value to EMPTY_PAIR to indicate no stats is needed for the main facet. *NOTE: Categorical terms
   * such as gender or vitalStatus can't have stats facets and, if you do that, your elasticsearch query will fail.
   */
  public static final ImmutableMap<String, Optional<SimpleImmutableEntry<String, String>>> FACETS_FOR_PHENOTYPE =
      ImmutableMap.of(
          PhenotypeFacetNames.GENDER, EMPTY_PAIR,
          PhenotypeFacetNames.VITAL_STATUS, EMPTY_PAIR,
          PhenotypeFacetNames.AGE_AT_DIAGNOSIS_GROUP, createPair(PhenotypeFacetNames.AVERAGE_AGE_AT_DIAGNOSIS,
              PhenotypeFacetNames.AGE_AT_DIAGNOSIS));

  private static final ImmutableMap<String, String> DONORS_FIELDS_MAPPING_FOR_PHENOTYPE =
      ImmutableMap.of(
          PhenotypeFacetNames.AGE_AT_DIAGNOSIS, "donor_age_at_diagnosis",
          PhenotypeFacetNames.AGE_AT_DIAGNOSIS_GROUP, "_summary._age_at_diagnosis_group",
          PhenotypeFacetNames.GENDER, "donor_sex",
          PhenotypeFacetNames.VITAL_STATUS, "donor_vital_status");

  @Getter(lazy = true)
  private final Map<String, Map<String, Integer>> baselineTermsAggsOfPhenotype = loadBaselineTermsAggsOfPhenotype();

  private static final int SCAN_BATCH_SIZE = 1000;

  private static final Jql2PqlConverter CONVERTER = Jql2PqlConverter.getInstance();
  private static final TimeValue KEEP_ALIVE = new TimeValue(10000);

  private final Client client;
  private final String indexName;
  private final String repoIndexName;
  private final QueryEngine queryEngine;

  @Autowired
  public DonorRepository(Client client, QueryEngine queryEngine,
      @Value("#{indexName}") String indexName, @Value("#{repoIndexName}") String repoIndexName) {
    this.indexName = indexName;
    this.repoIndexName = repoIndexName;
    this.client = client;
    this.queryEngine = queryEngine;
  }

  @Override
  @NonNull
  public SearchResponse findAllCentric(Query query) {
    val pql = CONVERTER.convert(query, DONOR_CENTRIC);
    log.info("pql of findAllCentric is: {}", pql);

    val request = queryEngine.execute(pql, DONOR_CENTRIC);
    val response = request.getRequestBuilder().execute().actionGet();
    return response;
  }

  @NonNull
  public SearchResponse findAllCentric(StatementNode pqlAst) {
    val request = queryEngine.execute(pqlAst, DONOR_CENTRIC);
    log.trace("{}", request.getRequestBuilder());
    val response = request.getRequestBuilder().get();

    log.debug("{}", response);
    return response;
  }

  private SearchRequestBuilder projectDonorCountSearch(Query query, String facetName) {
    val pqlAst = parse(CONVERTER.convert(query, DONOR_CENTRIC));
    pqlAst.setFacets(facets(facetName));

    val result = queryEngine.execute(pqlAst, DONOR_CENTRIC).getRequestBuilder().setFetchSource(false);

    log.debug("projectDonorCountSearch ES query is: '{}'.", result);
    return result;
  }

  @NonNull
  public MultiSearchResponse projectDonorCount(List<Query> queries, String facetName) {
    val multiSearch = client.prepareMultiSearch();

    for (val query : queries) {
      multiSearch.add(projectDonorCountSearch(query, facetName));
    }

    val response = multiSearch.execute().actionGet();

    log.debug("projectDonorCount ES response is: '{}'.", response);
    return response;
  }

  public MultiSearchResponse calculatePhenotypeStats(@NonNull final List<UUID> setIds) {
    val multiSearch = client.prepareMultiSearch();

    for (val setId : setIds) {
      val search = getPhenotypeAnalysisSearchBuilder();
      search.setQuery(boolQuery().must(matchAllQuery()).filter(getDonorSetIdFilterBuilder(setId)));

      // Adding terms_stats facets
      FACETS_FOR_PHENOTYPE.values().stream()
          .filter(v -> wantsStatistics(v))
          .map(Optional::get)
          .forEach(statsFacetNameFieldPair -> {
            final String actualFieldName = DONORS_FIELDS_MAPPING_FOR_PHENOTYPE.get(statsFacetNameFieldPair.getValue());
            search.addAggregation(buildTermsStatsAggsBuilder(statsFacetNameFieldPair.getKey(), actualFieldName));
          });

      log.debug("Sub-search for DonorSet ID [{}] is: '{}'", setId, search);
      multiSearch.add(search);
    }

    val multiResponse = multiSearch.execute().actionGet();
    log.debug("MultiResponse is: '{}'.", multiResponse);

    return multiResponse;
  }

  public EntitySetTermFacet buildEntitySetTermAggs(final UUID entitySetId, final Terms termsFacet,
      final Map<String, Aggregation> aggsMap,
      final Long total,
      final Long missing,
      final Optional<SimpleImmutableEntry<String, String>> statsFacetConfigMap) {
    val termFacetList = buildTermAggList(termsFacet, getBaselineTermsAggsOfPhenotype());

    val mean = wantsStatistics(statsFacetConfigMap) ? getMeanFromTermsStatsAggs(
        aggsMap.get(statsFacetConfigMap.get().getKey())) : null;
    val summary = new Statistics(total, missing, mean);

    return new EntitySetTermFacet(entitySetId, termFacetList, summary);
  }

  private Map<String, Map<String, Integer>> loadBaselineTermsAggsOfPhenotype() {
    val search = getPhenotypeAnalysisSearchBuilder();
    val response = search.execute().actionGet();

    log.debug("ES query is: '{}'", search);
    log.debug("ES response is: '{}'", response);

    val aggs = response.getAggregations();
    checkNotNull(aggs, "Query response does not contain any Aggregations.");

    val results = ImmutableMap.<String, Map<String, Integer>> builder();

    for (val agg : aggs.asList()) {
      if (agg instanceof Terms) {
        val entries = ((Terms) agg).getBuckets();
        val terms = transform(entries, entry -> entry.getKeyAsString());

        // Map all term values to zero
        results.put(agg.getName(), toMap(terms, constant(0)));
      }
    }

    return results.build();
  }

  private static Optional<SimpleImmutableEntry<String, String>> createPair(final String first, final String second) {
    checkArgument(!isNullOrEmpty(first));
    checkArgument(!isNullOrEmpty(second));

    return Optional.of(new SimpleImmutableEntry<String, String>(first, second));
  }

  private static List<Term> buildTermAggList(@NonNull final Terms termsFacet,
      @NonNull Map<String, Map<String, Integer>> baseline) {
    val results = ImmutableMap.<String, Integer> builder();

    // First we populate with the terms facets from the search response
    termsFacet.getBuckets().stream().forEach(entry -> {
      results.put(entry.getKeyAsString(), (int) entry.getDocCount());
    });

    val facetName = termsFacet.getName();
    // Then augment the result in case of missing terms in the response.
    if (baseline.containsKey(facetName)) {
      val difference = Maps.difference(results.build(), baseline.get(facetName)).entriesOnlyOnRight();

      results.putAll(difference);
    }

    val termFacetList = results.build().entrySet().stream()
        .map(entry -> new Term(entry.getKey(), (long) entry.getValue()))
        .collect(toImmutableList());

    return ImmutableList.copyOf(termFacetList);
  }

  private static Double getMeanFromTermsStatsAggs(final Aggregation facet) {
    if (!(facet instanceof Terms)) {
      return null;
    }

    val terms = (Terms) facet;
    try (val buckets = terms.getBuckets().stream()) {
      val stats = (Stats) buckets.findFirst().get().getAggregations().asList().get(0);
      return stats.getAvg();
    } catch (NoSuchElementException e) {
      return 0.0;
    }

  }

  private static TermsAggregationBuilder buildTermsStatsAggsBuilder(final String aggName, final String aggField) {
    return terms(aggName).field("_type").size(MAX_FACET_TERM_COUNT)
        .subAggregation(stats("stats").field(aggField));
  }

  private SearchRequestBuilder getPhenotypeAnalysisSearchBuilder() {
    val type = IndexType.DONOR_CENTRIC;
    val fieldMap = DONORS_FIELDS_MAPPING_FOR_PHENOTYPE;

    val searchBuilder = client.prepareSearch(indexName)
        .setTypes(type.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setSize(0);

    for (val name : FACETS_FOR_PHENOTYPE.keySet()) {
      val aggsBuilder = terms(name)
          .field(fieldMap.get(name))
          .size(MAX_FACET_TERM_COUNT);

      val missingAggsBuilder = missing(name + "_missing")
          .field(fieldMap.get(name));

      searchBuilder.addAggregation(missingAggsBuilder);
      searchBuilder.addAggregation(aggsBuilder);
    }

    return searchBuilder;
  }

  private static QueryBuilder getDonorSetIdFilterBuilder(final UUID donorId) {
    if (null == donorId) {
      return matchAllQuery();
    }

    val mustFilterFieldName = "_id";
    // Note: We should not reference TermsLookupRepository here but for now we'll wait for the PQL module and see how we
    // can move the createTermsLookupFilter() routine out of TermsLookupRepository.
    val termsLookupFilter = createTermsLookupFilter(mustFilterFieldName, TermLookupType.DONOR_IDS, donorId);

    return boolQuery().must(termsLookupFilter);
  }

  @Override
  public SearchResponse findAll(Query query) {
    throw new UnsupportedOperationException("No longer applicable");
  }

  @Override
  public SearchRequestBuilder buildFindAllRequest(Query query, IndexType type) {
    throw new UnsupportedOperationException("No longer applicable");
  }

  @Override
  public long count(Query query) {
    log.debug("Converting {}", query.getFilters());

    val pql = CONVERTER.convertCount(query, DONOR_CENTRIC);

    val request = queryEngine.execute(pql, DONOR_CENTRIC);
    return request.getRequestBuilder().setSize(0).execute().actionGet().getHits().getTotalHits();
  }

  @Override
  public MultiSearchResponse counts(LinkedHashMap<String, Query> queries) {
    MultiSearchRequestBuilder search = client.prepareMultiSearch();

    for (val query : queries.values()) {
      log.debug("Converting {}", query.getFilters());
      val pql = CONVERTER.convertCount(query, DONOR_CENTRIC);
      val request = queryEngine.execute(pql, DONOR_CENTRIC);
      search.add(request.getRequestBuilder());
    }
    return search.execute().actionGet();
  }

  public SearchResponse donorSearchRequest(final BoolQueryBuilder boolFilter, int maxUnionCount) {
    val query = boolQuery().must(matchAllQuery()).filter(boolFilter);
    val request = client.prepareSearch(repoIndexName, indexName)
        .setTypes(DONOR_TEXT.getId(), FILE_DONOR_TEXT.getId())
        .setQuery(query)
        .setSize(maxUnionCount)
        .setFetchSource(false)
        .setSearchType(SearchType.DEFAULT);

    log.debug("Terms Lookup - Donor Search: {}", request);
    val response = request.execute().actionGet();
    log.debug("ElasticSearch result is: '{}'", response);
    return response;
  }

  /**
   * Special case for Survival Analysis, the fields selected for return are the only ones we currently care about.
   */
  public SearchResponse singleDonorUnion(final String indexTypeName,
      @NonNull final SearchType searchType,
      @NonNull final BoolQueryBuilder boolFilter, final int max,
      @NonNull final String[] fields,
      @NonNull final List<String> sort) {
    val query = boolQuery().must(matchAllQuery()).filter(boolFilter);

    // Donor type is not analyzed but this works due to terms-lookup on _id field.
    // https://github.com/icgc-dcc/dcc-release/blob/develop/dcc-release-resources/src/main/resources/org/icgc/dcc/release/resources/mappings/donor.mapping.json#L12-L13
    val request = client.prepareSearch(indexName)
        .setTypes(DONOR.getId())
        .setSearchType(searchType)
        .setQuery(query)
        .setSize(max)
        .setFetchSource(fields, NO_EXCLUDE);

    sort.forEach(s -> request.addSort(s, ASC));
    log.debug("Union ES Query: {}", request);
    val response = request.execute().actionGet();
    log.debug("ElasticSearch result is: '{}'", response);
    return response;
  }

  @Override
  public MultiSearchResponse nestedCounts(LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    val search = client.prepareMultiSearch();

    for (val nestedQuery : queries.values()) {
      for (val innerQuery : nestedQuery.values()) {
        log.debug("Nested converting {}", innerQuery);

        val pql = CONVERTER.convertCount(innerQuery, DONOR_CENTRIC);
        val request = queryEngine.execute(pql, DONOR_CENTRIC);

        search.add(request.getRequestBuilder());
      }
    }

    log.debug("{}", search);
    return search.execute().actionGet();
  }

  @Override
  public NestedQueryBuilder buildQuery(Query query) {
    throw new UnsupportedOperationException("No longer applicable");
  }

  public Map<String, Object> findOne(String id, Query query) {
    val search = client.prepareGet(indexName, IndexType.DONOR.getId(), id);
    setFetchSourceOfGetRequest(search, query, EntityType.DONOR);

    val response = search.execute().actionGet();

    if (response.isExists()) {
      return createResponseMap(response, query, EntityType.DONOR);
    }

    if (!isRepositoryDonor(client, id, repoIndexName)) {
      // We know this is guaranteed to throw a 404, since the 'id' was not found in the first query.
      checkResponseState(id, response, EntityType.DONOR);
    }

    return singletonMap(FIELDS_MAPPING.get(EntityType.DONOR).get("id"), id);
  }

  /**
   * Retrieves donor-specimen-sample information. Note this searches donor and not donor-centric as centric does not
   * have sample information
   */
  public SearchResponse getDonorSamplesByProject(String projectId) {

    // Only download donor with complete (has submitted molecular data)
    val donorFilters = boolQuery()
        .must(termsQuery("_project_id", projectId));

    String[] includes = { "specimen", "_donor_id", "donor_id", "project._project_id" };

    val search = client.prepareSearch(indexName)
        .setTypes(IndexType.DONOR.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setSize(6000)
        .setFetchSource(includes, NO_EXCLUDE)
        .setPostFilter(donorFilters);
    return search.execute().actionGet();
  }

  public Set<String> findIds(Query query) {
    val pqlString = CONVERTER.convert(query, DONOR_CENTRIC);
    return findIds(pqlString);
  }

    public Set<String> findIds(String pqlString) {
    // TODO: Now assume 5000 ids at least
    Set<String> donorIds = newHashSetWithExpectedSize(5000);
    val request = queryEngine.execute(pqlString, DONOR_CENTRIC);
    val requestBuilder = request.getRequestBuilder()
        .setSize(SCAN_BATCH_SIZE)
        .setScroll(KEEP_ALIVE)
        .setFetchSource(false);

    SearchResponse response = requestBuilder.execute().actionGet();
    while (hasHits(response)) {
      for (SearchHit hit : response.getHits()) {
        donorIds.add(hit.getId());
      }
      response = client.prepareSearchScroll(response.getScrollId())
          .setScroll(KEEP_ALIVE)
          .execute().actionGet();
    }

    return donorIds;
  }

  /**
   * Searches for donors based on the ids provided. It will either search against donor-text or donor-file-text based on
   * a boolean.
   * 
   * @param ids A List of ids that can identify a donor
   * @param isForExternalFile False - donor-text, True - file-donor-text
   * @return Returns the SearchResponse object from the query.
   */
  public SearchResponse validateIdentifiers(@NonNull List<String> ids, boolean isForExternalFile) {
    val maxSize = 5000;
    val fields = isForExternalFile ? FILE_DONOR_ID_SEARCH_FIELDS : DONOR_ID_SEARCH_FIELDS;
    val index = isForExternalFile ? repoIndexName : indexName;
    val indexType = isForExternalFile ? FILE_DONOR_TEXT : DONOR_TEXT;

    val search = client.prepareSearch(index)
        .setTypes(indexType.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setSize(maxSize);

    final Object[] values = ids.toArray();
    val boolQuery = boolQuery();

    val highlight = new HighlightBuilder();
    highlight.preTags("").postTags("");
    for (val searchField : fields.keySet()) {
      boolQuery.should(termsQuery(TEXT_PREFIX + searchField, values));
      highlight.field(TEXT_PREFIX + searchField).forceSource(true);
    }

    // Set tags to empty strings so we do not have to parse out fragments later.
    search.setQuery(boolQuery)
        .highlighter(highlight)
        .setFetchSource(true);
    log.debug("ES query is: '{}'.", search);

    val response = search.execute().actionGet();
    log.debug("ES search result is: '{}'.", response);

    return response;
  }

  /**
   * This transforms a list of raw field names (of 'donor-text' or 'file-donor-text') into a map with the key being the
   * search term (field name plus the '.search' suffix).
   */
  private static Map<String, String> transformToTextSearchFieldMap(@NonNull String... fields) {
    return searchFieldMapper()
        .lowercaseMatchFields(newHashSet(fields))
        .build()
        .toMap();
  }

}
