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
import static org.icgc.dcc.portal.server.util.JsonUtils.MAPPER;

import java.util.Collections;
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
import org.icgc.dcc.portal.server.model.EntitySet.SubType;
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
    val key = id.toString();
    val request = client.prepareIndex(TERMS_LOOKUP_INDEX_NAME, type.getName())
        .setId(key)
        .setSource(keyValuePairs)
        .setRefresh(true);

    log.trace("{}", request.request());
    request.execute().get();
  }

  public void createTermsLookup(@NonNull final TermLookupType type, @NonNull final UUID id,
      @NonNull final Iterable<String> values) {
    createTermsLookup(type, id, Collections.singletonMap(TERMS_LOOKUP_PATH, (Object) values));
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
        .lookupCache(false)
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
