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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagate;
import static java.lang.Math.min;
import static lombok.AccessLevel.PRIVATE;
import static org.dcc.portal.pql.meta.Type.FILE;
import static org.dcc.portal.pql.meta.TypeModel.DONOR_LOOKUP;
import static org.dcc.portal.pql.meta.TypeModel.FILE_LOOKUP;
import static org.dcc.portal.pql.meta.TypeModel.GENE_LOOKUP;
import static org.dcc.portal.pql.meta.TypeModel.MUTATION_LOOKUP;
import static org.elasticsearch.index.query.FilterBuilders.termsLookupFilter;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static org.icgc.dcc.portal.server.model.IndexType.DONOR;
import static org.icgc.dcc.portal.server.model.IndexType.DONOR_TEXT;
import static org.icgc.dcc.portal.server.model.IndexType.FILE_DONOR_TEXT;
import static org.icgc.dcc.portal.server.util.ElasticsearchRequestUtils.toBoolFilterFrom;
import static org.icgc.dcc.portal.server.util.ElasticsearchRequestUtils.toDonorBoolFilter;
import static org.icgc.dcc.portal.server.util.JsonUtils.MAPPER;
import static org.icgc.dcc.portal.server.util.SearchResponses.getHitIdsSet;
import static org.icgc.dcc.portal.server.util.SearchResponses.getTotalHitCount;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Min;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsLookupFilterBuilder;
import org.icgc.dcc.portal.server.config.ServerProperties;
import org.icgc.dcc.portal.server.model.BaseEntitySet;
import org.icgc.dcc.portal.server.model.EntitySet.SubType;
import org.icgc.dcc.portal.server.model.UnionUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TermsLookupRepository {

  /**
   * Constants.
   */
  public static final String TERMS_LOOKUP_PATH = "values";
  public static final String TERMS_LOOKUP_INDEX_NAME = "terms-lookup";

  private final static MatchAllQueryBuilder MATCH_ALL = QueryBuilders.matchAllQuery();

  /**
   * Dependencies.
   */
  @NonNull
  private final Client client;
  @Value("#{indexName}")
  private final String indexName;
  @Value("#{repoIndexName}")
  private final String repoIndexName;
  @NonNull
  private final ServerProperties properties;

  /**
   * Configuration.
   */
  @Min(1)
  private int maxNumberOfHits;
  @Min(1)
  private int maxMultiplier;
  @Min(1)
  @Getter
  private int maxUnionCount;
  @Min(1)
  @Getter
  private int maxPreviewNumberOfHits;

  /**
   * Supported index types.
   */
  @Getter
  @RequiredArgsConstructor(access = PRIVATE)
  public enum TermLookupType {

    GENE_IDS(GENE_LOOKUP),
    MUTATION_IDS(MUTATION_LOOKUP),
    DONOR_IDS(DONOR_LOOKUP),
    FILE_IDS(FILE_LOOKUP);

    @NonNull
    private final String name;

  }

  @PostConstruct
  public void init() {
    val setOpSettings = properties.getSetOperation();
    maxNumberOfHits = setOpSettings.getMaxNumberOfHits();
    maxMultiplier = setOpSettings.getMaxMultiplier();
    maxUnionCount = maxNumberOfHits * maxMultiplier;
    maxPreviewNumberOfHits = min(setOpSettings.getMaxPreviewNumberOfHits(), maxUnionCount);

    val indexName = TERMS_LOOKUP_INDEX_NAME;
    val index = client.admin().indices();

    log.info("Checking index '{}' for existence...", indexName);
    boolean exists = index.prepareExists(indexName)
        .execute()
        .actionGet()
        .isExists();

    if (exists) {
      log.info("Index '{}' exists. Nothing to do.", indexName);
      return;
    }

    try {
      log.info("Creating index '{}'...", indexName);
      checkState(index
          .prepareCreate(indexName)
          .setSettings(createSettings())
          .execute()
          .actionGet()
          .isAcknowledged(),
          "Index '%s' creation was not acknowledged!", indexName);

    } catch (Throwable t) {
      propagate(t);
    }
  }

  @SneakyThrows
  private void createTermsLookup(@NonNull final TermLookupType type, @NonNull final UUID id,
      @NonNull final Map<String, Object> keyValuePairs) {
    client.prepareIndex(TERMS_LOOKUP_INDEX_NAME, type.getName())
        .setId(id.toString())
        .setSource(keyValuePairs).execute().get();
  }

  public void createTermsLookup(@NonNull final TermLookupType type, @NonNull final UUID id,
      @NonNull final Iterable<String> values) {
    createTermsLookup(type, id, Collections.singletonMap(TERMS_LOOKUP_PATH, (Object) values));
  }

  public void createTermsLookup(@NonNull final TermLookupType type, @NonNull final UUID id,
      @NonNull final Iterable<String> values, @NonNull final Map<String, Object> additionalAttributes) {
    additionalAttributes.put(TERMS_LOOKUP_PATH, values);
    createTermsLookup(type, id, additionalAttributes);
  }

  public void createTermsLookup(@NonNull final TermLookupType type, @NonNull final UUID id,
      @NonNull final Iterable<String> values, final boolean trans) {
    val attributes = ImmutableMap.<String, Object> of(TERMS_LOOKUP_PATH, values, SubType.TRANSIENT.getName(), trans);
    createTermsLookup(type, id, attributes);
  }

  public void createTermsLookup(@NonNull final TermLookupType type, @NonNull final UUID id,
      @NonNull final Iterable<String> values, @NonNull final String repoName) {
    val attributes = ImmutableMap.<String, Object> of(TERMS_LOOKUP_PATH, values, "repo", repoName);
    createTermsLookup(type, id, attributes);
  }

  public static TermsLookupFilterBuilder createTermsLookupFilter(@NonNull String fieldName,
      @NonNull TermLookupType type, @NonNull UUID id) {
    val key = id.toString();
    return termsLookupFilter(fieldName)
        // .cacheKey(key)
        .lookupId(key)
        .lookupIndex(TERMS_LOOKUP_INDEX_NAME)
        .lookupType(type.getName())
        .lookupPath(TERMS_LOOKUP_PATH);
  }

  public SearchResponse runUnionEsQuery(final String indexTypeName, @NonNull final SearchType searchType,
      @NonNull final BoolFilterBuilder boolFilter, final int max) {
    val query = QueryBuilders.filteredQuery(MATCH_ALL, boolFilter);
    return execute("Union ES Query", false, (request) -> {
      request
          .setTypes(indexTypeName)
          .setSearchType(searchType)
          .setQuery(query)
          .setSize(max)
          .setNoFields();

      if (indexTypeName.equalsIgnoreCase(FILE.getId())) {
        request.setIndices(repoIndexName);
      }
    });
  }

  /**
   * Special case for Survival Analysis, the fields selected for return are the only ones we currently care about.
   */
  public SearchResponse singleUnion(final String indexTypeName,
      @NonNull final SearchType searchType,
      @NonNull final BoolFilterBuilder boolFilter, final int max,
      @NonNull final String[] fields,
      @NonNull final List<String> sort) {
    val query = filteredQuery(MATCH_ALL, boolFilter);

    // Donor type is not analyzed but this works due to terms-lookup on _id field.
    // https://github.com/icgc-dcc/dcc-release/blob/develop/dcc-release-resources/src/main/resources/org/icgc/dcc/release/resources/mappings/donor.mapping.json#L12-L13
    return execute("Union ES Query", false, (request) -> {
      request
          .setTypes(DONOR.getId())
          .setSearchType(searchType)
          .setQuery(query)
          .setSize(max)
          .addFields(fields);

      sort.forEach(s -> request.addSort(s, ASC));
    });
  }

  public SearchResponse donorSearchRequest(final BoolFilterBuilder boolFilter) {
    val query = QueryBuilders.filteredQuery(MATCH_ALL, boolFilter);
    return execute("Terms Lookup - Donor Search", true, (request) -> request
        .setTypes(DONOR_TEXT.getId(), FILE_DONOR_TEXT.getId())
        .setQuery(query)
        .setSize(maxUnionCount)
        .setNoFields()
        .setSearchType(SearchType.DEFAULT));
  }

  public long getUnionCount(
      final UnionUnit unionDefinition,
      final BaseEntitySet.Type entityType) {

    val response = runUnionEsQuery(
        entityType.getIndexTypeName(),
        SearchType.COUNT,
        toBoolFilterFrom(unionDefinition, entityType),
        maxUnionCount);

    val count = getCountFrom(response, maxUnionCount);
    log.debug("Total hits: {}", count);

    return count;
  }

  public SearchResponse getDonorUnion(final Iterable<UnionUnit> definitions) {
    val boolFilter = toBoolFilterFrom(definitions, BaseEntitySet.Type.DONOR);
    val response = donorSearchRequest(boolFilter);

    return response;
  }

  public long getDonorCount(final UnionUnit unionDefinition) {
    val boolFilter = toDonorBoolFilter(unionDefinition);
    val response = donorSearchRequest(boolFilter);

    return getHitIdsSet(response).size();
  }

  public String getRepoName(@NonNull String setId) {
    val response = client.prepareGet(TERMS_LOOKUP_INDEX_NAME, TermLookupType.FILE_IDS.getName(), setId)
        .setFields("repo")
        .execute()
        .actionGet();

    val field = response.getField("repo");
    return field.getValue().toString();
  }

  private long getCountFrom(@NonNull final SearchResponse response, final long max) {
    val result = getTotalHitCount(response);
    return min(max, result);
  }

  private String createSettings() {
    val settings = MAPPER.createObjectNode();
    settings.put("index.auto_expand_replicas", "0-all");
    settings.put("index.number_of_shards", "1");

    return settings.toString();
  }

  private SearchResponse execute(String message, Boolean multiIndex, Consumer<SearchRequestBuilder> customizer) {
    val request = multiIndex ? client.prepareSearch(repoIndexName, indexName) : client.prepareSearch(indexName);
    customizer.accept(request);

    log.debug("{}: {}", message, request);
    val response = request.execute().actionGet();
    log.debug("ElasticSearch result is: '{}'", response);
    return response;
  }

}
