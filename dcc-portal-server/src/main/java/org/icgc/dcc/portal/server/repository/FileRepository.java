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
package org.icgc.dcc.portal.server.repository;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.math.LongMath.divide;
import static java.lang.String.format;
import static java.math.RoundingMode.CEILING;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.range;
import static org.icgc.dcc.portal.server.util.Collections.isEmpty;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.limit;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.select;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.sortBuilder;
import static org.dcc.portal.pql.meta.FileTypeModel.AVAILABLE_FACETS;
import static org.dcc.portal.pql.meta.Type.FILE;
import static org.dcc.portal.pql.query.PqlParser.parse;
import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.elasticsearch.action.search.SearchType.SCAN;
import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.elasticsearch.index.query.FilterBuilders.matchAllFilter;
import static org.elasticsearch.index.query.FilterBuilders.missingFilter;
import static org.elasticsearch.index.query.FilterBuilders.nestedFilter;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.index.query.FilterBuilders.termsFilter;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.avg;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filter;
import static org.elasticsearch.search.aggregations.AggregationBuilders.missing;
import static org.elasticsearch.search.aggregations.AggregationBuilders.nested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.reverseNested;
import static org.elasticsearch.search.aggregations.AggregationBuilders.sum;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.server.model.IndexModel.IS;
import static org.icgc.dcc.portal.server.model.IndexModel.MAX_FACET_TERM_COUNT;
import static org.icgc.dcc.portal.server.model.IndexModel.MISSING;
import static org.icgc.dcc.portal.server.model.SearchFieldMapper.searchFieldMapper;
import static org.icgc.dcc.portal.server.model.TermFacet.repoTermFacet;
import static org.icgc.dcc.portal.server.pql.convert.FiltersConverter.ENTITY_SET_ID;
import static org.icgc.dcc.portal.server.pql.convert.FiltersConverter.ENTITY_SET_PREFIX;
import static org.icgc.dcc.portal.server.repository.TermsLookupRepository.createTermsLookupFilter;
import static org.icgc.dcc.portal.server.repository.TermsLookupRepository.TermLookupType.DONOR_IDS;
import static org.icgc.dcc.portal.server.repository.TermsLookupRepository.TermLookupType.FILE_IDS;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.server.util.JsonUtils.merge;
import static org.icgc.dcc.portal.server.util.SearchResponses.getHitIds;
import static org.icgc.dcc.portal.server.util.SearchResponses.getTotalHitCount;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.ast.function.SelectNode;
import org.dcc.portal.pql.ast.function.SortNode;
import org.dcc.portal.pql.meta.IndexModel;
import org.dcc.portal.pql.meta.FileTypeModel.EsFields;
import org.dcc.portal.pql.meta.FileTypeModel.Fields;
import org.dcc.portal.pql.meta.TypeModel;
import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.MultiSearchResponse.Item;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.NestedFilterBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.SingleBucketAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.elasticsearch.search.aggregations.bucket.missing.MissingBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.NestedBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.icgc.dcc.portal.server.model.IndexModel.Kind;
import org.icgc.dcc.portal.server.model.IndexModel.Type;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.SearchFieldMapper;
import org.icgc.dcc.portal.server.model.TermFacet;
import org.icgc.dcc.portal.server.model.TermFacet.Term;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.server.repository.TermsLookupRepository.TermLookupType;
import org.icgc.dcc.portal.server.service.BadRequestException;
import org.icgc.dcc.portal.server.service.IndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FileRepository {

  /**
   * Constants
   */
  private static final Set<String> FILE_DONOR_FIELDS = newHashSet(
      "specimen_id", "sample_id", "submitted_specimen_id", "submitted_sample_id",
      "id", "submitted_donor_id",
      "tcga_participant_barcode", "tcga_sample_barcode", "tcga_aliquot_barcode");
  private static final SearchFieldMapper FILE_DONOR_TEXT_FIELDS = searchFieldMapper()
      .partialMatchFields(FILE_DONOR_FIELDS)
      .lowercaseMatchFields(FILE_DONOR_FIELDS)
      .build();
  private static final SelectNode FILE_INFO_FIELDS = select(ImmutableList.of(
      Fields.FILE_UUID,
      Fields.FILE_ID,
      Fields.STUDY,
      Fields.DATA_BUNDLE_ID,
      Fields.FILE_COPIES,
      Fields.DONORS));
  private static final SortNode FILE_INFO_SORT = sortBuilder()
      .sortAsc(Fields.REPO_TYPE).build();

  private static final Kind KIND = Kind.FILE;
  private static final TypeModel TYPE_MODEL = IndexModel.getFileTypeModel();
  private static final String PREFIX = TYPE_MODEL.prefix();
  private static final String FILE_INDEX_TYPE = FILE.getId();
  private static final String FILE_DONOR_TEXT_INDEX_TYPE = Type.FILE_DONOR_TEXT.getId();

  private static final String DONOR_ID_RAW_FIELD_NAME = toRawFieldName(Fields.DONOR_ID);
  private static final Jql2PqlConverter PQL_CONVERTER = Jql2PqlConverter.getInstance();
  private static final Map<String, String> JQL_FIELD_NAME_MAPPING = FIELDS_MAPPING.get(KIND);

  private static final TimeValue KEEP_ALIVE = new TimeValue(10000);

  /**
   * Dependencies.
   */
  private final Client client;
  private final String repoIndexName;
  private final QueryEngine queryEngine;
  private final IndexService indexService;

  @Autowired
  public FileRepository(Client client,
      @NonNull @org.springframework.beans.factory.annotation.Value("#{repoIndexName}") String repoIndexName,
      IndexService indexService) {
    this.client = client;
    this.repoIndexName = repoIndexName;
    this.queryEngine = new QueryEngine(client, repoIndexName);
    this.indexService = indexService;
  }

  public Map<String, String> findRepos() {
    // TODO: Return a list of {@link Repository}s instead?
    val repoName = toRawFieldName(Fields.REPO_NAME);
    val repoCode = toRawFieldName(Fields.REPO_CODE);
    val repoNameSubAgg = terms(repoName).field(repoName);
    val repoCodeSubAgg = nestedAgg(repoCode, EsFields.FILE_COPIES,
        terms(repoCode).size(100).field(repoCode).subAggregation(repoNameSubAgg));
    val response = searchFileCentric("findRepos", request -> request
        .setSearchType(COUNT)
        .addAggregation(repoCodeSubAgg));
    val terms = (Terms) getSubAggResultFromNested(response.getAggregations(), repoCode).get(repoCode);

    return terms.getBuckets().stream().collect(toMap(
        bucket -> bucket.getKey(),
        bucket -> {
          final List<Bucket> repoNameBuckets = termsBuckets(bucket.getAggregations(), repoName);

          return isEmpty(repoNameBuckets) ? "" : repoNameBuckets.get(0).getKey();
        }));
  }

  /**
   * @param fields - A list of field names that form the search query.
   * @param queryString - User input - could be any value out of one of the fields.
   * @return
   */
  public SearchResponse findRepoDonor(Iterable<String> fields, String queryString) {
    val maxNumberOfDocs = 5;
    String[] fieldNames = Iterables.toArray(FILE_DONOR_TEXT_FIELDS.map(fields), String.class);
    val result = searchFileDonorText("findRepoDonor", request -> request
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(0)
        .setSize(maxNumberOfDocs)
        .setQuery(multiMatchQuery(queryString, fieldNames)));

    log.debug("findRepoDonor - ES search result is: '{}'.", result);
    return result;
  }

  public GetResponse findOne(@NonNull String id) {
    val search = client.prepareGet(repoIndexName, FILE_INDEX_TYPE, id);
    val response = search.execute().actionGet();
    // This check is important as it validates if there is any document at all in the GET response.
    checkResponseState(id, response, KIND);

    return response;
  }

  public SearchResponse findAll(@NonNull Query query) {
    val queryFilter = query.getFilters();
    val filters = repoFilters(queryFilter);

    val response = searchFileCentric("findAll()", request -> {
      request.setSearchType(QUERY_THEN_FETCH)
          .setFrom(query.getFrom())
          .setSize(query.getSize())
          .addSort(JQL_FIELD_NAME_MAPPING.get(query.getSort()), query.getOrder())
          .setPostFilter(filters);

      aggs(queryFilter).stream().forEach(
          agg -> request.addAggregation(agg));
    });

    log.debug("findAll() - ES response is: '{}'.", response);
    return response;
  }

  public SearchResponse findAll(Query query, final String[] fields) {
    val filters = repoFilters(query.getFilters());
  
    return searchFileCentric(request -> request.setPostFilter(filters).setQuery(matchAllQuery()), fields);
  }

  // FIXME: Support terms lookup on files as part of the filter builder so we don't need an extra method.
  public SearchResponse findAll(String setId, final String[] fields) {
    val query = fileSetIdQuery(setId);
  
    return searchFileCentric(request -> request.setQuery(query), fields);
  }

  public Set<String> findAllDonorIds(@NonNull Query query, final int setLimit) {
    val pqlAst = parse(PQL_CONVERTER.convert(query, FILE));
    val size = query.getSize();
    int pageNumber = 0;

    SearchResponse response = findDonorIdsPQL(pqlAst, pageNumber, size);

    val result = Sets.<String> newHashSet();
    val pageCount = divide(getTotalHitCount(response), size, CEILING);

    // Number of files > max limit, so we must page files in order to ensure we get all donors.
    while (pageNumber <= pageCount) {
      for (val hit : response.getHits()) {
        val donorIdField = hit.field(DONOR_ID_RAW_FIELD_NAME);

        if (null == donorIdField) {
          // Skips when donorId doesn't appear in the fields.
          log.warn("The Donors array in this document (id: {}) is empty, which is not valid.", hit.getId());
          continue;
        }

        val donorIds = donorIdField.getValues();
        result.addAll(transform(donorIds, id -> id.toString()));

        if (result.size() >= setLimit) {
          return result;
        }
      }

      response = findDonorIdsPQL(pqlAst, ++pageNumber, size);
    }

    return result;
  }

  public List<String> findAllFileIds(Query query) {
    val queryFilter = query.getFilters();
    val filters = repoFilters(queryFilter);

    val response = searchFileCentric("Files Ids from Query", (request) -> request
        .setSearchType(QUERY_THEN_FETCH)
        .setFrom(query.getFrom())
        .setSize(query.getSize())
        .addSort(JQL_FIELD_NAME_MAPPING.get(query.getSort()), query.getOrder())
        .setPostFilter(filters));

    return getHitIds(response);
  }

  public SearchResponse findFileInfo(String setId) {
    val pqlAst = parse("select(*)");
    pqlAst.setSelect(FILE_INFO_FIELDS);

    val response = searchFileCentricPQL("Donor Info From Set Id", pqlAst, request -> request
        .setFrom(0)
        .setSize(20000)
        .setQuery(fileSetIdQuery(setId)));

    log.debug("ES response is: {}", response);
    return response;
  }

  public SearchResponse findFileInfoPQL(@NonNull final String pql) {
    val pqlAst = parse(pql);
    pqlAst.setSelect(FILE_INFO_FIELDS);
    pqlAst.setSort(FILE_INFO_SORT);

    log.debug("PQL for download is: '{}'.", pqlAst.toString());

    // Get the total count first.
    val count = getTotalHitCount(findFileInfoPQL(pqlAst, COUNT, 0));
    log.debug("A total of {} files will be returned from this query.", count);

    return findFileInfoPQL(pqlAst, QUERY_THEN_FETCH, Ints.saturatedCast(count));
  }

  /**
   * Get total file size, total donor count and total number of files based on query
   */
  public Map<String, Long> getSummary(Query query) {
    val donorSubAggs = donorIdAgg(SummaryAggregationKeys.DONOR)
        .subAggregation(
            terms(SummaryAggregationKeys.PROJECT).size(1000).field(toRawFieldName(Fields.PROJECT_CODE)))
        .subAggregation(
            terms(SummaryAggregationKeys.PRIMARY_SITE).size(1000).field(toRawFieldName(Fields.PRIMARY_SITE)));

    val fileSizeSubAgg = averageFileSizePerFileCopyAgg(SummaryAggregationKeys.FILE);
    val filters = repoFilters(query.getFilters());

    val response = searchFileCentric("Summary aggregation", request -> request
        .setSearchType(COUNT)
        .setQuery(filteredQuery(filters))
        .addAggregation(fileSizeSubAgg)
        .addAggregation(donorSubAggs));

    log.debug("getSummary aggregation result is: '{}'.", response);

    val aggResult = response.getAggregations();
    val totalFileSize = sumFileCopySize(termsBuckets(aggResult, SummaryAggregationKeys.FILE),
        SummaryAggregationKeys.FILE);
    val donorAggResult = getSubAggResultFromNested(aggResult, SummaryAggregationKeys.DONOR);

    return ImmutableMap.<String, Long> of(
        "fileCount", getTotalHitCount(response),
        "totalFileSize", (long) totalFileSize,
        "donorCount", (long) bucketSize(donorAggResult, SummaryAggregationKeys.DONOR),
        "projectCount", (long) bucketSize(donorAggResult, SummaryAggregationKeys.PROJECT),
        "primarySiteCount", (long) bucketSize(donorAggResult, SummaryAggregationKeys.PRIMARY_SITE));
  }

  /**
   * Returns the unique donor count across repositories Note we are counting the bucket size of a term aggregation. It
   * appears that using cardinality aggregation yields imprecise result.
   */
  public long getDonorCount(Query query) {
    val aggKey = "donorCount";
    val filters = repoFilters(query.getFilters());

    val response = searchFileCentric("Donor Count aggregation", request -> request
        .setSearchType(COUNT)
        .setQuery(filteredQuery(filters))
        .addAggregation(donorIdAgg(aggKey)));

    log.debug("getDonorCount aggregation result is: '{}'.", response);

    return bucketSize(getSubAggResultFromNested(response.getAggregations(), aggKey), aggKey);
  }

  public Map<String, Map<String, Map<String, Object>>> getRepoStats(String repoName) {
    val aggsFilter = nestedFilter(EsFields.FILE_COPIES,
        termFilter(toRawFieldName(Fields.REPO_CODE), repoName));
    val response = findFileStats(aggsFilter, repoName);

    return convertStats(response.getAggregations(), repoName);
  }

  public Map<String, Map<String, Map<String, Object>>> getStudyStats(String study) {
    val aggsFilter = termFilter(toRawFieldName(Fields.STUDY), study);
    val response = findFileStats(aggsFilter, study);

    return convertStats(response.getAggregations(), study);
  }

  public Map<String, TermFacet> getAggregationFacets(Query query, Aggregations aggs) {
    val result = Maps.<String, TermFacet> newHashMap();

    for (val agg : aggs) {
      val name = agg.getName();
      val aggregations = ((Filter) agg).getAggregations();

      if (name.equals(CustomAggregationKeys.REPO_SIZE)) {
        val nestedAgg = getSubAggResultFromNested(aggregations, name);
        val buckets = ((Terms) nestedAgg.get(name)).getBuckets();

        result.put(CustomAggregationKeys.REPO_SIZE, convertRepoSizeAggregation(buckets));
        result.put(CustomAggregationKeys.REPO_DONOR_COUNT, searchGroupByRepoNameDonorId(buckets, query));
      } else if (name.equals(CustomAggregationKeys.REPO_NAME)) {
        val nestedAgg = getSubAggResultFromNested(aggregations, name);

        result.put(name, convertNormalAggregation(nestedAgg, name));
      } else if (name.equals(Fields.FILE_FORMAT)) {
        result.put(name, convertFileFormatAggregation(aggregations, name));
      } else {
        result.put(name, convertNormalAggregation(aggregations, name));
      }
    }

    log.debug("Result of convertAggregationsToFacets is: '{}'.", result);
    return result;
  }

  @SneakyThrows
  public Map<String, String> getIndexMetaData() {
    // TODO: Should all of this really be returned? What is acutally required and by whom?
    return indexService.getIndexMetaData(client, repoIndexName);
  }

  public SearchResponse prepareSearchScroll(@NonNull String scrollId) {
    return client.prepareSearchScroll(scrollId)
        .setScroll(KEEP_ALIVE)
        .execute().actionGet();
  }

  private static String toRawFieldName(@NonNull String alias) {
    return TYPE_MODEL.getField(alias);
  }

  private SearchResponse findFileStats(FilterBuilder filter, String aggName) {
    val fileSizeAgg = averageFileSizePerFileCopyAgg(StatsAggregationKeys.SIZE);

    val fileFormatAgg = nestedAgg(StatsAggregationKeys.FORMAT, EsFields.FILE_COPIES,
        terms(StatsAggregationKeys.FORMAT).field(toRawFieldName(Fields.FILE_FORMAT)));

    val dataTypeAgg = terms(aggName).field(toRawFieldName(Fields.DATA_TYPE))
        .subAggregation(donorIdAgg(StatsAggregationKeys.DONOR))
        .subAggregation(fileSizeAgg)
        .subAggregation(fileFormatAgg);

    // Primary Site => Project Code => Donor ID
    val primarySiteAgg = primarySiteAgg(Fields.PRIMARY_SITE, 100)
        .subAggregation(primarySiteAgg(Fields.PROJECT_CODE, 100)
            .subAggregation(primarySiteAgg(Fields.DONOR_ID, 30000)));

    val statsAgg = filter(aggName)
        .filter(filter)
        .subAggregation(dataTypeAgg)
        .subAggregation(nestedAgg(StatsAggregationKeys.DONOR_PRIMARY_SITE, EsFields.DONORS, primarySiteAgg));

    val response = searchFileCentric("findFileStats", request -> request
        .setSearchType(COUNT)
        .addAggregation(statsAgg));

    log.debug("findFileStats - ES response is: {}", response);

    return response;
  }

  private SearchResponse findFileInfoPQL(StatementNode pqlAst, SearchType searchType, int size) {
    val response = searchFileCentricPQL("findFileInfoPQL", pqlAst, request -> request
        .setSearchType(searchType)
        .setSize(size));

    log.debug("findFileInfoPQL - ES response is: {}", response);
    return response;
  }

  private SearchResponse findDonorIdsPQL(@NonNull StatementNode pqlAst, int pageNumber, int size) {
    pqlAst.setLimit(limit(pageNumber * size, size));

    val response = searchFileCentricPQL("findDonorIdsPQL", pqlAst, request -> {
    });

    log.debug("findDonorIdsPQL - ES response is: '{}'.", response);
    return response;
  }

  // Special aggregation to get unique donor count for each repository
  private TermFacet searchGroupByRepoNameDonorId(List<Bucket> buckets, Query query) {
    if (isEmpty(buckets)) {
      return repoTermFacet(0L, 0, ImmutableList.of());
    }

    val repoNames = transform(buckets, bucket -> bucket.getKey());

    val donorAggKey = CustomAggregationKeys.REPO_DONOR_COUNT;
    val repoFilterTemplate = "{file: {repoName: {is: [\"%s\"]}}}";
    val userFilter = query.getFilters();

    val multiSearch = client.prepareMultiSearch();

    for (val repoName : repoNames) {
      val repoFilter = new FiltersParam(format(repoFilterTemplate, repoName));
      val mergedFilter = merge(userFilter, repoFilter.get());
      val filters = repoFilters(mergedFilter);

      val oneSearch = client.prepareSearch(repoIndexName)
          .setSearchType(COUNT)
          .setQuery(filteredQuery(filters))
          .addAggregation(donorIdAgg(donorAggKey));

      multiSearch.add(oneSearch);
    }

    val response = multiSearch.execute().actionGet();
    Item[] responseItems = response.getResponses();
    val responseItemCount = responseItems.length;

    val donorResult = range(0, responseItemCount).boxed()
        .map(i -> {
          final Aggregations aggResult = responseItems[i].getResponse().getAggregations();
          final int donorCount = bucketSize(getSubAggResultFromNested(aggResult, donorAggKey), donorAggKey);

          return new Term(repoNames.get(i), Long.valueOf(donorCount));
        })
        .collect(toImmutableList());

    // Total does not have any meaning in this context because a donor can cross multiple repositories.
    val total = -1L;
    return repoTermFacet(total, 0, donorResult);
  }

  private SearchResponse searchFileCentric(String logMessage, Consumer<SearchRequestBuilder> customizer) {
    return searchFiles(FILE_INDEX_TYPE, logMessage, customizer);
  }

  private SearchResponse searchFileCentricPQL(String logMessage, StatementNode pqlAst,
      Consumer<SearchRequestBuilder> customizer) {
    val request = queryEngine.execute(pqlAst, FILE).getRequestBuilder();
    customizer.accept(request);

    log.debug(logMessage + "; ES query is: '{}'", request);
    return request.execute().actionGet();
  }

  private SearchResponse searchFileCentric(Consumer<SearchRequestBuilder> queryCustomizer, String[] fields) {
    val size = 5000;

    return searchFileCentric("Preparing data table export", request -> {
      request.setSearchType(SCAN)
          .setSize(size)
          .setScroll(KEEP_ALIVE)
          .addFields(fields);

      queryCustomizer.accept(request);
    });
  }

  private SearchResponse searchFileDonorText(String logMessage, Consumer<SearchRequestBuilder> customizer) {
    return searchFiles(FILE_DONOR_TEXT_INDEX_TYPE, logMessage, customizer);
  }

  private SearchResponse searchFiles(String indexType, String logMessage,
      Consumer<SearchRequestBuilder> customizer) {
    val request = client.prepareSearch(repoIndexName).setTypes(indexType);
    customizer.accept(request);

    log.debug(logMessage + "; ES query is: '{}'", request);
    return request.execute().actionGet();
  }

  private static boolean isNestedField(String fieldAlias) {
    return TYPE_MODEL.isAliasDefined(fieldAlias) && TYPE_MODEL.isNested(fieldAlias);
  }

  /**
   * Converters.
   */

  // Special aggregation to get file size for each repository
  private static TermFacet convertRepoSizeAggregation(List<Bucket> buckets) {
    val termsBuilder = ImmutableList.<Term> builder();
    long total = 0;

    for (val bucket : buckets) {
      val childCount = sumValue(bucket.getAggregations(), CustomAggregationKeys.FILE_SIZE);

      termsBuilder.add(new Term(bucket.getKey(), (long) childCount));
      total += childCount;
    }

    val result = repoTermFacet(total, 0, termsBuilder.build());

    log.debug("Result of convertRepoSizeAggregation is: {}", result);
    return result;
  }

  private static TermFacet convertNormalAggregation(Aggregations aggregations, String name) {
    return convertNormalAggregation(aggregations, name, (bucket, notUsed) -> bucket.getDocCount());
  }

  private static TermFacet convertFileFormatAggregation(Aggregations aggregations, String name) {
    return convertNormalAggregation(aggregations, name,
        (bucket, aggKey) -> ((SingleBucketAggregation) bucket.getAggregations().get(aggKey)).getDocCount());
  }

  private static TermFacet convertNormalAggregation(Aggregations aggregations, String name,
      BiFunction<Bucket, String, Long> docCountGetter) {
    val termsAgg = isNestedField(name) ? getSubAggResultFromNested(aggregations, name) : aggregations;
    val aggResult = (Terms) termsAgg.get(name);
    val termsBuilder = ImmutableList.<Term> builder();
    long total = 0;

    for (val bucket : aggResult.getBuckets()) {
      val bucketKey = bucket.getKey();
      val count = docCountGetter.apply(bucket, name);
      log.debug("convertNormalAggregation bucketKey: {}, count: {}", bucketKey, count);

      total += count;
      termsBuilder.add(new Term(bucketKey, count));
    }

    val missingAgg = (Missing) termsAgg.get(MISSING);
    val missingCount = missingAgg.getDocCount();
    log.debug("convertNormalAggregation Missing count is: {}", missingCount);

    // No need to return a term with a value of 0.
    if (missingCount > 0) {
      termsBuilder.add(new Term(MISSING, missingCount));
    }

    return repoTermFacet(total, missingCount, termsBuilder.build());
  }

  private static Map<String, Map<String, Map<String, Object>>> convertStats(Aggregations aggs, String aggName) {
    val stats = (Filter) aggs.get(aggName);
    val statsAggregations = stats.getAggregations();

    val result = Maps.<String, Map<String, Map<String, Object>>> newHashMap();

    // donorPrimarySite
    val donorPrimarySite = Maps.<String, Map<String, Object>> newHashMap();
    val primarySiteAggKey = StatsAggregationKeys.DONOR_PRIMARY_SITE;
    val donorFacets = (Terms) getSubAggResultFromNested(statsAggregations, primarySiteAggKey)
        .get(primarySiteAggKey);

    for (val bucket : donorFacets.getBuckets()) {
      val projectFacets = (Terms) bucket.getAggregations().get(primarySiteAggKey);
      val newEntries = projectFacets.getBuckets().stream().collect(toMap(
          project -> project.getKey(),
          project -> bucketSize(project.getAggregations(), primarySiteAggKey)));

      val name = bucket.getKey();
      val map = donorPrimarySite.getOrDefault(name, Maps.<String, Object> newHashMap());
      map.putAll(newEntries);

      donorPrimarySite.putIfAbsent(name, map);
    }

    result.put(primarySiteAggKey, donorPrimarySite);

    // statistics
    val statistics = Maps.<String, Map<String, Object>> newHashMap();
    val datatypes = (Terms) statsAggregations.get(aggName);

    for (val bucket : datatypes.getBuckets()) {
      val bucketAggregations = bucket.getAggregations();
      val donorCount = bucketSize(getSubAggResultFromNested(bucketAggregations, StatsAggregationKeys.DONOR),
          StatsAggregationKeys.DONOR);

      val fileSizeResult = (Terms) bucketAggregations.get(StatsAggregationKeys.SIZE);
      val totalFileSize = sumFileCopySize(fileSizeResult.getBuckets(), StatsAggregationKeys.SIZE);

      val dataFormat = (Terms) getSubAggResultFromNested(bucketAggregations, StatsAggregationKeys.FORMAT)
          .get(StatsAggregationKeys.FORMAT);
      val formats = transform(dataFormat.getBuckets(), b -> b.getKey());

      // TODO: We should use StatsAggregationKeys for these keys too, though it requires changes in the client side.
      val map = ImmutableMap.<String, Object> of(
          "fileCount", bucket.getDocCount(),
          "donorCount", donorCount,
          "fileSize", totalFileSize,
          "dataFormat", formats);

      statistics.put(bucket.getKey(), map);
    }

    result.put("stats", statistics);

    log.debug("Result {}", result);
    return result;
  }

  private static FilteredQueryBuilder filteredQuery(FilterBuilder filters) {
    return new FilteredQueryBuilder(matchAllQuery(), filters);
  }

  private static FilteredQueryBuilder fileSetIdQuery(String setId) {
    val lookupFilter = createTermsLookupFilter(toRawFieldName(Fields.ID), FILE_IDS, UUID.fromString(setId));
    return filteredQuery(lookupFilter);
  }

  /**
   * FIXME: This is a temporary solution. We really should use the PQL infrastructure to build. <br>
   * Negation is not supported <br>
   * _missing is not supported for data_types.datatype and data_type.dataformat <br>
   */
  private static FilterBuilder repoFilters(final ObjectNode filters) {
    val fields = filters.path(PREFIX).fields();

    if (!fields.hasNext()) {
      // If there is no filter defined under "file", return a match-all filter.
      return matchAllFilter();
    }

    val result = boolFilter();

    // Used for creating the terms lookup filter when ENTITY_SET_ID and donorId are in the JQL.
    FilterBuilder entitySetIdFilter = null;
    BoolFilterBuilder donorIdFilter = null;

    while (fields.hasNext()) {
      val facetField = fields.next();
      val fieldAlias = facetField.getKey();

      checkArgument(JQL_FIELD_NAME_MAPPING.containsKey(fieldAlias),
          "'%s' is not a valid field in this query.", fieldAlias);

      val facetValue = facetField.getValue().path(IS);
      if (facetValue.isMissingNode()) {
        throw new BadRequestException(format("Expected '%s' in filter for %s.", "is", facetField.getKey()));
      }
      val filterValues = transform(newArrayList(facetValue),
          item -> item.textValue());

      if (fieldAlias.equals(ENTITY_SET_ID)) {
        // The assumption here is there should be only one "entitySetId" filter in JQL.
        entitySetIdFilter = entitySetIdFilter(filterValues);
      } else if (fieldAlias.equals(Fields.ID)) {
        // Prepare for processing two types of file ids
        val uuids = Lists.<String> newArrayList();
        val ids = Lists.<String> newArrayList();

        // Partition and parse
        for (val id : filterValues) {
          if (id.startsWith(ENTITY_SET_PREFIX)) {
            val uuid = id.substring(ENTITY_SET_PREFIX.length());
            uuids.add(uuid);
          } else {
            ids.add(id);
          }
        }

        // Normal ids
        BoolFilterBuilder idFilter = null;
        if (!ids.isEmpty()) {
          idFilter = missingInclusiveTermsFilter(fieldAlias, ids);
        }

        // Entity set ids
        BoolFilterBuilder uuidFilter = null;
        if (!uuids.isEmpty()) {
          uuidFilter = boolFilter();
          for (val uuid : uuids) {
            uuidFilter.should(
                createTermsLookupFilter(toRawFieldName(fieldAlias), TermLookupType.FILE_IDS, UUID.fromString(uuid)));
          }
        }

        // Combine
        if (idFilter != null && uuidFilter != null) {
          result.should(idFilter).should(uuidFilter);
        } else if (idFilter != null) {
          result.must(idFilter);
        } else if (uuidFilter != null) {
          result.must(uuidFilter);
        }
      } else {
        val filter = missingInclusiveTermsFilter(fieldAlias, filterValues);

        if (fieldAlias.equals(Fields.DONOR_ID)) {
          // The assumption here is there should be only one "donorId" filter in JQL.
          donorIdFilter = filter;
        } else {
          result.must(filter);
        }
      }
    }

    // Creates the terms lookup filter when both ENTITY_SET_ID and donorId are in the JQL.
    if (null != donorIdFilter && null != entitySetIdFilter) {
      result.must(boolFilter()
          .should(donorIdFilter)
          .should(entitySetIdFilter));
    } else if (null != donorIdFilter) {
      result.must(donorIdFilter);
    } else if (null != entitySetIdFilter) {
      result.must(entitySetIdFilter);
    }

    return result;
  }

  private static FilterBuilder entitySetIdFilter(List<String> uuids) {
    if (isEmpty(uuids)) {
      return null;
    }

    if (1 == uuids.size()) {
      return nestedEntitySetIdFilter(uuids.get(0));
    }

    val result = boolFilter();

    for (val uuid : uuids) {
      result.should(nestedEntitySetIdFilter(uuid));
    }

    return result;
  }

  private static FilterBuilder selfRemovingFilter(final ObjectNode filters, String facetAlias) {
    if (!filters.fieldNames().hasNext()) {
      return matchAllFilter();
    }

    val facetFilters = filters.deepCopy();

    if (facetFilters.has(PREFIX)) {
      // Remove the facet itself from the "file" filter.
      facetFilters.with(PREFIX).remove(facetAlias);
    }

    return repoFilters(facetFilters);
  }

  private static NestedFilterBuilder nestedEntitySetIdFilter(String uuid) {
    return nestedFilter(EsFields.DONORS,
        createTermsLookupFilter(DONOR_ID_RAW_FIELD_NAME, DONOR_IDS, UUID.fromString(uuid)));
  }

  private static BoolFilterBuilder missingInclusiveTermsFilter(String fieldAlias, List<String> filterValues) {
    val rawFieldName = toRawFieldName(fieldAlias);
    val result = boolFilter();
    val terms = termsFilter(rawFieldName, filterValues);

    // Special processing for "no data" terms
    if (filterValues.remove(MISSING)) {
      val missing = missingFilter(rawFieldName).existence(true).nullValue(true);
      result.should(missing).should(terms);
    } else {
      result.must(terms);
    }

    return isNestedField(fieldAlias) ? boolFilter().must(
        nestedFilter(TYPE_MODEL.getNestedPath(fieldAlias), result)) : result;
  }

  private static NestedBuilder nestedAgg(String aggName, String path, AbstractAggregationBuilder... subAggs) {
    val result = nested(aggName).path(path);

    for (val subAgg : subAggs) {
      result.subAggregation(subAgg);
    }

    return result;
  }

  private static NestedBuilder donorIdAgg(String aggKey) {
    return nestedAgg(aggKey, EsFields.DONORS,
        terms(aggKey).size(100000).field(DONOR_ID_RAW_FIELD_NAME));
  }

  private static List<AggregationBuilder<?>> aggs(final ObjectNode filters) {
    val regularUiFacets = transform(AVAILABLE_FACETS, facet -> {
      final String rawFieldName = toRawFieldName(facet);
      final FilterAggregationBuilder filterAgg = filter(facet).filter(selfRemovingFilter(filters, facet));

      if (facet.equals(Fields.FILE_FORMAT)) {
        return addReverseNestedTermsAgg(filterAgg, facet, rawFieldName, TYPE_MODEL.getNestedPath(facet));
      } else if (isNestedField(facet)) {
        return filterAgg.subAggregation(nestedTermsAgg(facet, rawFieldName, TYPE_MODEL.getNestedPath(facet)));
      } else {
        return addSubTermsAgg(filterAgg, facet, rawFieldName);
      }
    });
    val result = ImmutableList.<AggregationBuilder<?>> builder().addAll(regularUiFacets);

    /*
     * Facets that aren't visible in the UI, mostly used by the Manifest Download modal dialog. These use special
     * filters, which do not exclude self.
     */
    val repoFilters = repoFilters(filters.deepCopy());
    val repoNameFieldName = toRawFieldName(Fields.REPO_NAME);

    // repositoryNamesFiltered - file count
    val repoNameAggKey = CustomAggregationKeys.REPO_NAME;
    val filterAgg = filter(repoNameAggKey).filter(repoFilters);
    val repoNameSubAgg = filterAgg.subAggregation(
        nestedTermsAgg(repoNameAggKey, repoNameFieldName, EsFields.FILE_COPIES));
    result.add(repoNameSubAgg);

    // repositorySize
    val repoSizeAggKey = CustomAggregationKeys.REPO_SIZE;
    val repoSizeTermsSubAgg = terms(repoSizeAggKey).size(MAX_FACET_TERM_COUNT).field(repoNameFieldName)
        .subAggregation(
            sum(CustomAggregationKeys.FILE_SIZE).field(toRawFieldName(Fields.FILE_SIZE)));
    val repoSizeSubAgg = filter(repoSizeAggKey)
        .filter(repoFilters)
        .subAggregation(nestedAgg(repoSizeAggKey, EsFields.FILE_COPIES, repoSizeTermsSubAgg));

    result.add(repoSizeSubAgg);

    return result.build();
  }

  private static NestedBuilder nestedTermsAgg(String aggregationKey, String fieldName, String path) {
    val agg = TermsMissingAggPair.from(aggregationKey, fieldName);

    return nestedAgg(aggregationKey, path, agg.terms, agg.missing);
  }

  private static FilterAggregationBuilder addReverseNestedTermsAgg(FilterAggregationBuilder builder,
      String aggregationKey, String fieldName, String path) {
    val agg = TermsMissingAggPair.from(aggregationKey, fieldName);
    val reverseNestedAgg = agg.terms.subAggregation(reverseNested(aggregationKey));
    val nestedAgg = nestedAgg(aggregationKey, path, reverseNestedAgg, agg.missing);

    return builder.subAggregation(nestedAgg);
  }

  private static FilterAggregationBuilder addSubTermsAgg(FilterAggregationBuilder builder,
      String aggregationKey, String fieldName) {
    val agg = TermsMissingAggPair.from(aggregationKey, fieldName);

    return builder.subAggregation(agg.terms).subAggregation(agg.missing);
  }

  private static TermsBuilder primarySiteAgg(@NonNull String fieldAlias, int size) {
    return terms(StatsAggregationKeys.DONOR_PRIMARY_SITE)
        .field(toRawFieldName(fieldAlias))
        .size(size);
  }

  private static TermsBuilder averageFileSizePerFileCopyAgg(@NonNull String aggName) {
    return terms(aggName).size(100000).field(toRawFieldName(Fields.FILE_ID))
        .subAggregation(nestedAgg(aggName, EsFields.FILE_COPIES,
            avg(aggName).field(toRawFieldName(Fields.FILE_SIZE))));
  }

  /**
   * Aggregation utilites
   */

  private static Aggregations getSubAggResultFromNested(Aggregations nestedAggs, String aggKey) {
    return ((Nested) nestedAggs.get(aggKey)).getAggregations();
  }

  private static List<Bucket> termsBuckets(Aggregations aggResult, String aggKey) {
    return ((Terms) aggResult.get(aggKey)).getBuckets();
  }

  private static int bucketSize(Aggregations aggResult, String aggKey) {
    return termsBuckets(aggResult, aggKey).size();
  }

  private static double sumValue(Aggregations aggResult, String name) {
    return ((Sum) aggResult.get(name)).getValue();
  }

  private static double averageValue(Aggregations aggResult, String name) {
    return ((Avg) aggResult.get(name)).getValue();
  }

  private static double sumFileCopySize(List<Bucket> buckets, String aggregationKey) {
    return buckets.stream().mapToDouble(
        bucket -> averageValue(
            getSubAggResultFromNested(bucket.getAggregations(), aggregationKey), aggregationKey))
        .sum();
  }

  private static class CustomAggregationKeys {

    public static final String FILE_SIZE = "fileSize";
    public static final String REPO_SIZE = "repositorySizes";
    public static final String REPO_NAME = "repositoryNamesFiltered";
    public static final String REPO_DONOR_COUNT = "repositoryDonors";

  }

  private static class SummaryAggregationKeys {

    public static final String FILE = "file";
    public static final String DONOR = "donor";
    public static final String PROJECT = "project";
    public static final String PRIMARY_SITE = "primarySite";

  }

  private static class StatsAggregationKeys {

    public static final String DONOR = "donor";
    public static final String SIZE = "size";
    public static final String FORMAT = "format";
    public static final String DONOR_PRIMARY_SITE = "donorPrimarySite";

  }

  @Value
  private static class TermsMissingAggPair {

    TermsBuilder terms;
    MissingBuilder missing;

    public static TermsMissingAggPair from(String aggregationKey, String fieldName) {
      val termsAgg = terms(aggregationKey).field(fieldName)
          .size(MAX_FACET_TERM_COUNT);
      val missingAgg = missing(MISSING).field(fieldName);

      return new TermsMissingAggPair(termsAgg, missingAgg);
    }

  }

}