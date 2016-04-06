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

package org.icgc.dcc.portal.repository;

import static com.google.common.base.Functions.constant;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Maps.toMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newHashSetWithExpectedSize;
import static java.util.Collections.singletonMap;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.facets;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.dcc.portal.pql.query.PqlParser.parse;
import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.action.search.SearchType.SCAN;
import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.elasticsearch.index.query.FilterBuilders.matchAllFilter;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.missing;
import static org.elasticsearch.search.aggregations.AggregationBuilders.stats;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.model.IndexModel.MAX_FACET_TERM_COUNT;
import static org.icgc.dcc.portal.model.IndexModel.getFields;
import static org.icgc.dcc.portal.model.SearchFieldMapper.searchFieldMapper;
import static org.icgc.dcc.portal.repository.TermsLookupRepository.createTermsLookupFilter;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.isRepositoryDonor;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.setFetchSourceOfGetRequest;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.createResponseMap;
import static org.icgc.dcc.portal.util.SearchResponses.hasHits;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.icgc.dcc.portal.model.EntitySetTermFacet;
import org.icgc.dcc.portal.model.IndexModel;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.IndexModel.Type;
import org.icgc.dcc.portal.model.PhenotypeResult;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.Statistics;
import org.icgc.dcc.portal.model.TermFacet.Term;
import org.icgc.dcc.portal.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.repository.TermsLookupRepository.TermLookupType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DonorRepository implements Repository {

  private static final Type TYPE = Type.DONOR;
  private static final Kind KIND = Kind.DONOR;

  @NonNull
  private final EntityListRepository entityListRepository;

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
  private static final ImmutableMap<String, Optional<SimpleImmutableEntry<String, String>>> FACETS_FOR_PHENOTYPE =
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

  @Getter(lazy = true, value = PRIVATE)
  private final Map<String, Map<String, Integer>> baselineTermsAggsOfPhenotype = loadBaselineTermsAggsOfPhenotype();

  private static final int SCAN_BATCH_SIZE = 1000;

  private static final Jql2PqlConverter CONVERTER = Jql2PqlConverter.getInstance();
  private static final TimeValue KEEP_ALIVE = new TimeValue(10000);

  private final Client client;
  private final String index;
  private final String repoIndexName;
  private final QueryEngine queryEngine;

  @Autowired
  DonorRepository(Client client, IndexModel indexModel, QueryEngine queryEngine,
      EntityListRepository entityListRepository) {
    this.index = indexModel.getIndex();
    this.repoIndexName = indexModel.getRepoIndex();
    this.client = client;
    this.queryEngine = queryEngine;
    this.entityListRepository = entityListRepository;
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
    val response = request.getRequestBuilder().execute().actionGet();

    return response;
  }

  private SearchRequestBuilder projectDonorCountSearch(Query query, String facetName) {
    val pqlAst = parse(CONVERTER.convert(query, DONOR_CENTRIC));
    pqlAst.setFacets(facets(facetName));

    val result = queryEngine.execute(pqlAst, DONOR_CENTRIC).getRequestBuilder().setNoFields();

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

  public List<PhenotypeResult> getPhenotypeAnalysisResult(@NonNull final Collection<UUID> entitySetIds) {
    // Here we eliminate duplicates and impose ordering (needed for reading the response items).
    val setIds = ImmutableSet.copyOf(entitySetIds).asList();

    val multiResponse = performPhenotypeAnalysisMultiSearch(setIds);
    val responseItems = multiResponse.getResponses();
    val responseItemCount = responseItems.length;
    checkState(responseItemCount == setIds.size(),
        "The number of queries does not match the number of responses in a multi-search.");

    val facetKeyValuePairs = FACETS_FOR_PHENOTYPE.entrySet();

    // Building a map with facet name as the key and a list builder as the value.
    val results = facetKeyValuePairs.stream().collect(
        Collectors.toMap(entry -> entry.getKey(), notUsed -> new ImmutableList.Builder<EntitySetTermFacet>()));

    // Here we enumerate the response collection by entity-set UUIDs (the same grouping as in the multi-search).
    for (int i = 0; i < responseItemCount; i++) {
      val aggregations = responseItems[i].getResponse().getAggregations();

      if (null == aggregations) continue;

      val aggsMap = aggregations.asMap();
      val entitySetId = setIds.get(i);

      val entitySetCount = entityListRepository.find(entitySetId).getCount();

      // We go through the main Results map for each facet and build the inner list by populating it with instances of
      // EntitySetTermFacet.
      for (val facetKv : facetKeyValuePairs) {
        val facetName = facetKv.getKey();
        Aggregation aggs = aggsMap.get(facetName);

        Long total = 0L;
        Long missing = 0L;
        // We want to include the number of missing donor documents in our final missing count
        if (aggs instanceof Terms) {
          val termsAggs = (Terms) aggs;

          total = termsAggs.getBuckets()
              .stream()
              .mapToLong(Bucket::getDocCount)
              .sum();

          missing = entitySetCount - total;
        }

        results.get(facetName).add(
            buildEntitySetTermAggs(entitySetId, (Terms) aggs, aggsMap, total, missing, facetKv.getValue()));
      }

    }

    val finalResult = transform(results.entrySet(),
        entry -> new PhenotypeResult(entry.getKey(), entry.getValue().build()));

    return ImmutableList.copyOf(finalResult);
  }

  private EntitySetTermFacet buildEntitySetTermAggs(final UUID entitySetId, final Terms termsFacet,
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
        val terms = transform(entries, entry -> entry.getKey());

        // Map all term values to zero
        results.put(agg.getName(), toMap(terms, constant(0)));
      }
    }

    return results.build();
  }

  private static Optional<SimpleImmutableEntry<String, String>> createPair(final String first, final String second) {
    checkArgument(isNotBlank(first));
    checkArgument(isNotBlank(second));

    return Optional.of(new SimpleImmutableEntry<String, String>(first, second));
  }

  private static List<Term> buildTermAggList(@NonNull final Terms termsFacet,
      @NonNull Map<String, Map<String, Integer>> baseline) {
    val results = ImmutableMap.<String, Integer> builder();

    // First we populate with the terms facets from the search response
    termsFacet.getBuckets().stream().forEach(entry -> {
      results.put(entry.getKey(), (int) entry.getDocCount());
    });

    val facetName = termsFacet.getName();
    // Then augment the result in case of missing terms in the response.
    if (baseline.containsKey(facetName)) {
      val difference = Maps.difference(results.build(), baseline.get(facetName)).entriesOnlyOnRight();

      results.putAll(difference);
    }

    val termFacetList = transform(results.build().entrySet(),
        entry -> new Term(entry.getKey(), (long) entry.getValue()));

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

  private MultiSearchResponse performPhenotypeAnalysisMultiSearch(@NonNull final List<UUID> setIds) {
    val multiSearch = client.prepareMultiSearch();
    val matchAll = matchAllQuery();

    for (val setId : setIds) {
      val search = getPhenotypeAnalysisSearchBuilder();
      search.setQuery(filteredQuery(matchAll, getDonorSetIdFilterBuilder(setId)));

      // Adding terms_stats facets
      FACETS_FOR_PHENOTYPE.values().stream()
          .filter(v -> wantsStatistics(v))
          .map(Optional::get)
          .forEach(statsFacetNameFieldPair -> {
            final String actualFieldName = DONORS_FIELDS_MAPPING_FOR_PHENOTYPE.get(statsFacetNameFieldPair.getValue());
            search.addAggregation(buildTermsStatsAggsBuilder(statsFacetNameFieldPair.getKey(), actualFieldName));
          });

      log.info("Sub-search for DonorSet ID [{}] is: '{}'", setId, search);
      multiSearch.add(search);
    }

    val multiResponse = multiSearch.execute().actionGet();
    log.info("MultiResponse is: '{}'.", multiResponse);

    return multiResponse;
  }

  private static TermsBuilder buildTermsStatsAggsBuilder(final String aggName, final String aggField) {
    return terms(aggName).field("_type").size(MAX_FACET_TERM_COUNT)
        .subAggregation(stats("stats").field(aggField));
  }

  private SearchRequestBuilder getPhenotypeAnalysisSearchBuilder() {
    val type = Type.DONOR_CENTRIC;
    val fieldMap = DONORS_FIELDS_MAPPING_FOR_PHENOTYPE;

    val searchBuilder = client.prepareSearch(index)
        .setTypes(type.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(0)
        .addFields(fieldMap.values().stream().toArray(String[]::new));

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

  private static FilterBuilder getDonorSetIdFilterBuilder(final UUID donorId) {
    if (null == donorId) {
      return matchAllFilter();
    }

    val mustFilterFieldName = "_id";
    // Note: We should not reference TermsLookupRepository here but for now we'll wait for the PQL module and see how we
    // can move the createTermsLookupFilter() routine out of TermsLookupRepository.
    val termsLookupFilter = createTermsLookupFilter(mustFilterFieldName, TermLookupType.DONOR_IDS, donorId);

    return boolFilter().must(termsLookupFilter);
  }

  @Override
  public SearchResponse findAll(Query query) {
    throw new UnsupportedOperationException("No longer applicable");
  }

  @Override
  public SearchRequestBuilder buildFindAllRequest(Query query, Type type) {
    throw new UnsupportedOperationException("No longer applicable");
  }

  @Override
  public long count(Query query) {
    log.info("Converting {}", query.getFilters());

    val pql = CONVERTER.convertCount(query, DONOR_CENTRIC);

    val request = queryEngine.execute(pql, DONOR_CENTRIC);
    return request.getRequestBuilder().setSearchType(COUNT).execute().actionGet().getHits().getTotalHits();
  }

  @Override
  public MultiSearchResponse counts(LinkedHashMap<String, Query> queries) {
    MultiSearchRequestBuilder search = client.prepareMultiSearch();

    for (val query : queries.values()) {
      log.info("Converting {}", query.getFilters());
      val pql = CONVERTER.convertCount(query, DONOR_CENTRIC);
      val request = queryEngine.execute(pql, DONOR_CENTRIC);
      search.add(request.getRequestBuilder());
    }
    return search.execute().actionGet();
  }

  @Override
  public MultiSearchResponse nestedCounts(LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    val search = client.prepareMultiSearch();

    for (val nestedQuery : queries.values()) {
      for (val innerQuery : nestedQuery.values()) {
        log.info("Nested converting {}", innerQuery);

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
    val search = client.prepareGet(index, TYPE.getId(), id)
        .setFields(getFields(query, KIND));
    setFetchSourceOfGetRequest(search, query, KIND);

    val response = search.execute().actionGet();

    if (response.isExists()) {
      return createResponseMap(response, query, KIND);
    }

    if (!isRepositoryDonor(client, id, repoIndexName)) {
      // We know this is guaranteed to throw a 404, since the 'id' was not found in the first query.
      checkResponseState(id, response, KIND);
    }

    return singletonMap(FIELDS_MAPPING.get(KIND).get("id"), id);
  }

  /**
   * Retrieves donor-specimen-sample information. Note this searches donor and not donor-centric as centric does not
   * have sample information
   */
  public SearchResponse getDonorSamplesByProject(String projectId) {

    // Only download donor with complete (has submitted molecular data)
    val donorFilters = FilterBuilders.boolFilter()
        .must(FilterBuilders.termFilter("_project_id", projectId));

    val search = client.prepareSearch(index)
        .setTypes(TYPE.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setSize(6000)
        .setFetchSource("specimen", null)
        .addFields("_donor_id", "donor_id", "project._project_id")
        .setPostFilter(donorFilters);
    return search.execute().actionGet();
  }

  public Set<String> findIds(Query query) {

    // TODO: Now assume 5000 ids at least
    Set<String> donorIds = newHashSetWithExpectedSize(5000);

    val pql = CONVERTER.convert(query, DONOR_CENTRIC);
    val request = queryEngine.execute(pql, DONOR_CENTRIC);
    val requestBuilder = request.getRequestBuilder()
        .setSearchType(SCAN)
        .setSize(SCAN_BATCH_SIZE)
        .setScroll(KEEP_ALIVE)
        .setNoFields();

    SearchResponse response = requestBuilder.execute().actionGet();
    while (true) {
      response = client.prepareSearchScroll(response.getScrollId())
          .setScroll(KEEP_ALIVE)
          .execute().actionGet();

      for (SearchHit hit : response.getHits()) {
        donorIds.add(hit.getId());
      }

      val finished = !hasHits(response);
      if (finished) {
        break;
      }
    }

    return donorIds;
  }

  /**
   * Searches for donors based on the ids provided. It will either search against donor-text or donor-file-text based on
   * a boolean.
   * 
   * @param ids A List of ids that can identify a donor
   * @param False - donor-text, True - file-donor-text
   * @return Returns the SearchResponse object from the query.
   */
  public SearchResponse validateIdentifiers(@NonNull List<String> ids, boolean isForExternalFile) {
    val maxSize = 5000;
    val fields = isForExternalFile ? FILE_DONOR_ID_SEARCH_FIELDS : DONOR_ID_SEARCH_FIELDS;
    val indexName = isForExternalFile ? repoIndexName : index;
    val indexType = isForExternalFile ? Type.REPOSITORY_FILE_DONOR_TEXT : Type.DONOR_TEXT;

    val search = client.prepareSearch(indexName)
        .setTypes(indexType.getId())
        .setSearchType(QUERY_THEN_FETCH)
        .setSize(maxSize);

    final Object[] values = ids.toArray();
    val boolQuery = boolQuery();

    for (val searchField : fields.keySet()) {
      boolQuery.should(termsQuery(searchField, values));
      search.addHighlightedField(searchField);
    }

    // Set tags to empty strings so we do not have to parse out fragments later.
    search.setQuery(boolQuery)
        .setHighlighterPreTags("")
        .setHighlighterPostTags("")
        .setNoFields();
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
