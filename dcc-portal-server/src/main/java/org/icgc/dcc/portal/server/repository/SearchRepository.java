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
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;
import static org.elasticsearch.action.search.SearchType.DFS_QUERY_THEN_FETCH;
import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.index.query.FilterBuilders.termsFilter;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.server.model.IndexModel.getFields;
import static org.icgc.dcc.portal.server.model.SearchFieldMapper.EXACT_MATCH_SUFFIX;
import static org.icgc.dcc.portal.server.model.SearchFieldMapper.LOWERCASE_MATCH_SUFFIX;
import static org.icgc.dcc.portal.server.model.SearchFieldMapper.PARTIAL_MATCH_SUFFIX;
import static org.icgc.dcc.portal.server.model.SearchFieldMapper.boost;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.IndicesFilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TypeFilterBuilder;
import org.icgc.dcc.portal.server.model.IndexModel;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.IndexType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SearchRepository {

  // Constants
  @NoArgsConstructor(access = PRIVATE)
  private static final class Types {

    public static final String PATHWAY = "pathway";
    public static final String CURATED_SET = "curated_set";
    public static final String GO_TERM = "go_term";
    public static final String DONOR = "donor";
    public static final String PROJECT = "project";
    public static final String GENE = "gene";
    public static final String MUTATION = "mutation";
    public static final String GENE_SET = "geneSet";
    public static final String FILE = "file";
    public static final String FILE_DONOR = "file-donor";
    public static final String DRUG = "compound";

  }

  @NoArgsConstructor(access = PRIVATE)
  private static final class FieldNames {

    // Fields to be included as prefix queries
    public static final String FILE_NAME = "file_name";
    public static final String ID = "id";
    public static final String INCHIKEY = "inchikey";
    public static final String DRUG_BANK = "external_references_drugbank";
    public static final String CHEMBL = "external_references_chembl";
    public static final String ATC_CODES = "atc_codes_code";
    public static final String ATC_LEVEL5_CODES = "atc_level5_codes";

    public static final String GENE_MUTATIONS = "geneMutations";

  }

  private static final List<String> PREFIX_QUERY_FIELDS = ImmutableList.of(
      FieldNames.FILE_NAME, FieldNames.INCHIKEY, FieldNames.ID,
      FieldNames.CHEMBL, FieldNames.DRUG_BANK, FieldNames.ATC_CODES, FieldNames.ATC_LEVEL5_CODES);

  private static final Set<String> FIELD_KEYS = FIELDS_MAPPING.get(EntityType.KEYWORD).keySet();
  private static final float TIE_BREAKER = 0.7F;
  private static final List<String> SIMPLE_TERM_FILTER_TYPES = ImmutableList.of(
      Types.PATHWAY, Types.CURATED_SET, Types.GO_TERM);

  private static final Map<String, IndexType> TYPE_MAPPINGS = ImmutableMap.<String, IndexType> builder()
      .put(Types.GENE, IndexType.GENE_TEXT)
      .put(Types.MUTATION, IndexType.MUTATION_TEXT)
      .put(Types.DONOR, IndexType.DONOR_TEXT)
      .put(Types.PROJECT, IndexType.PROJECT_TEXT)
      .put(Types.PATHWAY, IndexType.GENESET_TEXT)
      .put(Types.GENE_SET, IndexType.GENESET_TEXT)
      .put(Types.GO_TERM, IndexType.GENESET_TEXT)
      .put(Types.CURATED_SET, IndexType.GENESET_TEXT)
      .put(Types.FILE, IndexType.FILE_TEXT)
      .put(Types.FILE_DONOR, IndexType.FILE_DONOR_TEXT)
      .put(Types.DRUG, IndexType.DRUG_TEXT)
      .build();
  private static final Map<String, String> TYPE_ID_MAPPINGS = transformValues(TYPE_MAPPINGS, type -> type.getId());
  private static final String MUTATION_PREFIX = TYPE_ID_MAPPINGS.get(Types.MUTATION);

  // private static final String[] MULTIPLE_SEARCH_TYPES = Stream.of(
  private static final Set<String> MULTIPLE_SEARCH_TYPES = Stream.of(
      Types.GENE,
      /*
       * Types.FILE must appear before Types.DONOR for searching file UUID in "file-text" to work. See DCC-3967 and
       * https://github.com/elastic/elasticsearch/issues/2218 for details.
       */
      Types.FILE,
      Types.DONOR,
      Types.PROJECT,
      Types.MUTATION,
      Types.GENE_SET,
      Types.DRUG)
      .map(t -> TYPE_ID_MAPPINGS.get(t))
      // TODO
      // .distinct()
      // .toArray(String[]::new);
      .collect(toImmutableSet());

  private static final List<String> SEARCH_SUFFIXES = ImmutableList.of(
      boost(LOWERCASE_MATCH_SUFFIX, 2), PARTIAL_MATCH_SUFFIX);
  private static final List<String> BOOSTED_SEARCH_SUFFIXES = ImmutableList.of(
      boost(LOWERCASE_MATCH_SUFFIX, 2), boost(PARTIAL_MATCH_SUFFIX, 2));

  // Instance variables
  private final Client client;

  @Value("#{indexName}")
  private String indexName;

  @Value("#{repoIndexName}")
  private String repoIndexName;

  @Autowired
  SearchRepository(Client client, IndexModel indexModel) {
    this.client = client;
  }

  @NonNull
  public SearchResponse findAll(Query query, String type) {
    log.debug("Requested search type is: '{}'.", type);

    val typeFilterBuilder = new TypeFilterBuilder("donor-text");
    val typeBoolFilter = new BoolFilterBuilder().mustNot(typeFilterBuilder);
    val indicesFilterBuilder = new IndicesFilterBuilder(typeBoolFilter, repoIndexName);

    val filteredQuery = new FilteredQueryBuilder(getQuery(query, type), indicesFilterBuilder);

    val search = createSearch(type)
        .setSearchType(DFS_QUERY_THEN_FETCH)
        .setFrom(query.getFrom())
        .setSize(query.getSize())
        .setTypes(getSearchTypes(type))
        .addFields(getFields(query, EntityType.KEYWORD))
        .setQuery(filteredQuery)
        .setPostFilter(getPostFilter(type));

    log.debug("ES search query is: {}", search);
    val response = search.execute().actionGet();
    log.debug("ES search result is: {}", response);

    return response;
  }

  // Helpers
  private static boolean isRepositoryFileRelated(String type) {
    return type.equals(Types.FILE) || type.equals(Types.FILE_DONOR);
  }

  private SearchRequestBuilder createSearch(String type) {
    // Determines which index to use as external file repository are in a daily generated index separated
    // from the main icgc-index.
    if (isRepositoryFileRelated(type)) {
      return client.prepareSearch(repoIndexName);
    }

    if (type.equals(Types.DONOR)) {
      return client.prepareSearch(indexName);
    }

    return client.prepareSearch(indexName, repoIndexName);
  }

  // Helpers
  private static String[] toStringArray(Collection<String> source) {
    return source.stream().toArray(String[]::new);
  }

  private static String[] getSearchTypes(String type) {
    val result = TYPE_ID_MAPPINGS.containsKey(type) ? newHashSet(TYPE_ID_MAPPINGS.get(type)) : MULTIPLE_SEARCH_TYPES;

    return toStringArray(result);
    // return TYPE_ID_MAPPINGS.containsKey(type) ? new String[] { TYPE_ID_MAPPINGS.get(type) } : MULTIPLE_SEARCH_TYPES;
  }

  private static FilterBuilder getPostFilter(String type) {
    val field = "type";
    val result = boolFilter();

    if (SIMPLE_TERM_FILTER_TYPES.contains(type)) {
      return result.must(termFilter(field, type));
    }

    val donor = boolFilter()
        .must(termFilter(field, Types.DONOR));
    val project = boolFilter()
        .must(termFilter(field, Types.PROJECT));
    val others = boolFilter()
        .mustNot(termsFilter(field, Types.DONOR, Types.PROJECT));

    // FIXME
    return result
        .should(donor)
        .should(project)
        .should(others);
  }

  private static QueryBuilder getQuery(Query query, String type) {
    val queryString = query.getQuery();
    val keys = buildMultiMatchFieldList(FIELD_KEYS, queryString, type);
    val multiMatchQuery = multiMatchQuery(queryString, toStringArray(keys)).tieBreaker(TIE_BREAKER);

    val result = boolQuery();

    for (val field : PREFIX_QUERY_FIELDS) {
      result.should(prefixQuery(field + EXACT_MATCH_SUFFIX, queryString));
    }

    return result.should(multiMatchQuery);
  }

  private static boolean shouldProcess(String sourceField) {
    val fieldsToSkip = ImmutableList.of(FieldNames.FILE_NAME, FieldNames.GENE_MUTATIONS);
    return fieldsToSkip.stream().noneMatch(fieldToAvoid -> sourceField.equals(fieldToAvoid));
  }

  private static List<String> appendSuffixes(String field, List<String> suffixes) {
    return transform(suffixes, suffix -> field + suffix);
  }

  private static List<String> appendSearchSuffixes(String field) {
    return appendSuffixes(field, SEARCH_SUFFIXES);
  }

  @NonNull
  private static Set<String> buildMultiMatchFieldList(Iterable<String> fields, String queryString, String type) {
    val keys = Sets.<String> newHashSet();

    for (val field : fields) {
      // Exact match fields (DCC-2324)
      if (field.equals("start")) {
        // TODO: Investigate and see if we still need this in the ES version wer're using.
        /*
         * NOTE: This is a work around quirky ES issue. We need to prefix the document type here to prevent
         * NumberFormatException, it appears that ES cannot determine what type 'start' is. This is for ES 0.9, later
         * versions may not have this problem.
         */
        keys.add(format("%s.%s", MUTATION_PREFIX, field));

      } else if (shouldProcess(field)) {
        keys.addAll(appendSearchSuffixes(field));
      }
    }

    // Don't boost without space or genes won't show when partially matched
    val geneMutationSearchSuffixes = queryString.contains(" ") ? BOOSTED_SEARCH_SUFFIXES : SEARCH_SUFFIXES;
    keys.addAll(appendSuffixes(FieldNames.GENE_MUTATIONS, geneMutationSearchSuffixes));

    // Exact-match search on "id".
    keys.add(FieldNames.ID);

    if (isRepositoryFileRelated(type)) {
      // For repository file related searches, we want fuzzy search.
      keys.addAll(appendSearchSuffixes(FieldNames.ID));
    }

    return keys;
  }

}
