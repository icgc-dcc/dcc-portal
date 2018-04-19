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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.IndexModel;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.fields.SearchField;
import org.icgc.dcc.portal.server.model.fields.SearchKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Sets.newHashSet;
import static lombok.AccessLevel.PRIVATE;
import static org.elasticsearch.action.search.SearchType.DFS_QUERY_THEN_FETCH;
import static org.elasticsearch.index.query.Operator.OR;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.elasticsearch.index.query.QueryBuilders.typeQuery;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.server.model.IndexModel.getFields;
import static org.icgc.dcc.portal.server.model.fields.SearchField.EXACT_MATCH_FIELDNAME;
import static org.icgc.dcc.portal.server.model.fields.SearchField.LOWERCASE_MATCH_FIELDNAME;
import static org.icgc.dcc.portal.server.model.fields.SearchField.PARTIAL_MATCH_FIELDNAME;
import static org.icgc.dcc.portal.server.model.fields.SearchField.newBoostedSearchField;
import static org.icgc.dcc.portal.server.model.fields.SearchField.newNoneBoostedSearchField;
import static org.icgc.dcc.portal.server.model.fields.SearchFieldMapper.EXACT_MATCH_SUFFIX;
import static org.icgc.dcc.portal.server.model.fields.SearchKey.newSearchKey;
import static org.icgc.dcc.portal.server.util.Strings.toStringArray;

@Slf4j
@Component
public class SearchRepository {

  /**
   * Constants
   */
  private static final String[] NO_EXCLUDE = null;

  private static final Set<SearchField> NORMAL_SEARCH_FIELDS = ImmutableSet.of(
      newBoostedSearchField(4, EXACT_MATCH_FIELDNAME),
      newBoostedSearchField(2, LOWERCASE_MATCH_FIELDNAME),
      newNoneBoostedSearchField(PARTIAL_MATCH_FIELDNAME)
  );

  private static final Set<SearchField> BOOSTED_SEARCH_FIELDS = ImmutableSet.of(
      newBoostedSearchField(4, EXACT_MATCH_FIELDNAME),
      newBoostedSearchField(2, LOWERCASE_MATCH_FIELDNAME),
      newBoostedSearchField(2, PARTIAL_MATCH_FIELDNAME)
  );

  /**
   * DCCPRTL-245 so that lowercase works
   */
  private static final Set<String> SPECIAL_BOOSTED_SEARCH_FIELD_NAMES = ImmutableSet.of(
      "text.symbol",
      "text.id",
      "text.data_bundle_id",
      "text.donor_id",
      "text.object_id",
      "text.file_name",
      "text.project_code"
  );

  private static final Set<SearchField> SPECIAL_BOOSTED_SEARCH_FIELDS = ImmutableSet.of(
          newBoostedSearchField(4, EXACT_MATCH_FIELDNAME),
          newBoostedSearchField(4, LOWERCASE_MATCH_FIELDNAME),
          newBoostedSearchField(2, PARTIAL_MATCH_FIELDNAME)
  );

  /**
   * DCCPRTL-484 so that mutation partials outmatch gene exact but not too
   * much, (special boost would result in kras matching mutations instead of genes)
   */
  private static final Set<String> GENE_MUTATIONS_BOOSTED_SEARCH_FIELD_NAMES = ImmutableSet.of(
          "text.geneMutations"
  );

  private static final Set<SearchField> GENE_MUTATIONS_BOOSTED_SEARCH_FIELDS = ImmutableSet.of(
          newBoostedSearchField(4, EXACT_MATCH_FIELDNAME),
          newBoostedSearchField(2.8f, LOWERCASE_MATCH_FIELDNAME), // 2.8 / 4 = 0.7 (which is the ES tiebreak score)
          newBoostedSearchField(2, PARTIAL_MATCH_FIELDNAME)
  );


  @NoArgsConstructor(access = PRIVATE)
  private static final class TypeNames {

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
      FieldNames.CHEMBL, FieldNames.DRUG_BANK, FieldNames.ATC_CODES, FieldNames.ATC_LEVEL5_CODES)
      .stream().map(s -> IndexModel.TEXT_PREFIX + s).collect(toImmutableList()); // Append "text."

  private static final Set<String> FIELD_KEYS = ImmutableSet.copyOf(FIELDS_MAPPING.get(EntityType.KEYWORD).values());

  /**
   * TODO: [DCCPRTL-242] Need to investigate if this is a correct value
   */
  private static final float TIE_BREAKER = 0.7F;
  private static final List<String> SIMPLE_TERM_FILTER_TYPES = ImmutableList.of(
      TypeNames.PATHWAY, TypeNames.CURATED_SET, TypeNames.GO_TERM);

  private static final Map<String, IndexType> TYPE_MAPPINGS = ImmutableMap.<String, IndexType> builder()
      .put(TypeNames.GENE, IndexType.GENE_TEXT)
      .put(TypeNames.MUTATION, IndexType.MUTATION_TEXT)
      .put(TypeNames.DONOR, IndexType.DONOR_TEXT)
      .put(TypeNames.PROJECT, IndexType.PROJECT_TEXT)
      .put(TypeNames.PATHWAY, IndexType.GENESET_TEXT)
      .put(TypeNames.GENE_SET, IndexType.GENESET_TEXT)
      .put(TypeNames.GO_TERM, IndexType.GENESET_TEXT)
      .put(TypeNames.CURATED_SET, IndexType.GENESET_TEXT)
      .put(TypeNames.FILE, IndexType.FILE_TEXT)
      .put(TypeNames.FILE_DONOR, IndexType.FILE_DONOR_TEXT)
      .put(TypeNames.DRUG, IndexType.DRUG_TEXT)
      .build();
  private static final Map<String, String> TYPE_ID_MAPPINGS = transformValues(TYPE_MAPPINGS, IndexType::getId);
  private static final String MUTATION_PREFIX = TYPE_ID_MAPPINGS.get(TypeNames.MUTATION);

  // private static final String[] MULTIPLE_SEARCH_TYPES = Stream.of(
  private static final Set<String> MULTIPLE_SEARCH_TYPES = Stream.of(
      TypeNames.GENE,
      /*
       * TypeNames.FILE must appear before TypeNames.DONOR for searching file UUID in "file-text" to work. See DCC-3967 and
       * https://github.com/elastic/elasticsearch/issues/2218 for details.
       */
      TypeNames.FILE,
      TypeNames.DONOR,
      TypeNames.PROJECT,
      TypeNames.MUTATION,
      TypeNames.GENE_SET,
      TypeNames.DRUG)
      .map(t -> TYPE_ID_MAPPINGS.get(t))
      // TODO
      // .distinct()
      // .toArray(String[]::new);
      .collect(toImmutableSet());


  // Instance variables
  private final Client client;

  @Value("#{indexName}")
  private String indexName;

  @Value("#{repoIndexName}")
  private String repoIndexName;

  @Autowired
  SearchRepository(Client client, String indexName, String repoIndexName) {
    this.client = client;
    this.indexName = indexName;
    this.repoIndexName = repoIndexName;
  }

  @NonNull
  @SuppressWarnings("deprecation")
  public SearchResponse findAll(Query query, String type) {
    log.debug("Requested search type is: '{}'.", type);

    val typeBoolFilter = boolQuery().mustNot(typeQuery("donor-text"));
    val indicesFilterBuilder = QueryBuilders.indicesQuery(typeBoolFilter, repoIndexName);
    val filteredQuery = boolQuery().must(getQuery(query, type)).filter(indicesFilterBuilder);

    val sourceFields = getFields(query, EntityType.KEYWORD);
    val search = createSearch(type)
        .setSearchType(DFS_QUERY_THEN_FETCH)
        .setFrom(query.getFrom())
        .setSize(query.getSize())
        .setTypes(getSearchTypes(type))
        .setFetchSource(sourceFields, NO_EXCLUDE)
        .setQuery(filteredQuery)
        .setPostFilter(getPostFilter(type));

    log.debug("ES search query is: {}", search);
    val response = search.execute().actionGet();
    log.debug("ES search result is: {}", response);

    return response;
  }

  // Helpers
  private static boolean isRepositoryFileRelated(String type) {
    return type.equals(TypeNames.FILE) || type.equals(TypeNames.FILE_DONOR);
  }

  private SearchRequestBuilder createSearch(String type) {
    // Determines which index to use as external file repository are in a daily generated index separated
    // from the main icgc-index.
    if (isRepositoryFileRelated(type)) {
      return client.prepareSearch(repoIndexName);
    }

    if (type.equals(TypeNames.DONOR)) {
      return client.prepareSearch(indexName);
    }

    return client.prepareSearch(indexName, repoIndexName);
  }


  private static String[] getSearchTypes(String type) {
    // lombok "val" was making result of type Object, so needed to explicitly define
    final Set<String> result = TYPE_ID_MAPPINGS.containsKey(type) ? newHashSet(TYPE_ID_MAPPINGS.get(type)) : MULTIPLE_SEARCH_TYPES;
    return toStringArray(result);
  }

  private static QueryBuilder getPostFilter(String type) {
    val field = "text.type";
    val result = boolQuery();

    if (SIMPLE_TERM_FILTER_TYPES.contains(type)) {
      return result.must(termQuery(field, type));
    }

    val donor = boolQuery()
        .must(termQuery(field, TypeNames.DONOR));
    val project = boolQuery()
        .must(termQuery(field, TypeNames.PROJECT));
    val others = boolQuery()
        .mustNot(termsQuery(field, TypeNames.DONOR, TypeNames.PROJECT));

    // FIXME
    return result
        .should(donor)
        .should(project)
        .should(others);
  }

  private static MultiMatchQueryBuilder createMultiMatchQuery(final String queryString, Set<SearchField> expandedSearchFields){
    val mmqBuilder = multiMatchQuery(queryString)
        .operator(OR)
        .tieBreaker(TIE_BREAKER);
    for (val expandedSearchField : expandedSearchFields){
        val fieldName = expandedSearchField.getName();
        val boostValue = expandedSearchField.getBoostedValue();
        mmqBuilder.field(fieldName, boostValue);
    }
    return mmqBuilder;
  }

  private static QueryBuilder getQuery(Query query, String type) {
    val queryString = query.getQuery();
    val bestMatchSearchFields = buildMultiMatchFieldList(FIELD_KEYS, queryString, type);
    val multiMatchBestQuery = createMultiMatchQuery(queryString, bestMatchSearchFields);


    val result = boolQuery();

    for (val field : PREFIX_QUERY_FIELDS) {
      result.should(prefixQuery(field + EXACT_MATCH_SUFFIX, queryString));
    }

    return result.should(multiMatchBestQuery);
  }

  private static boolean shouldProcess(String sourceField) {
    val fieldsToSkip = ImmutableList.of(FieldNames.FILE_NAME, FieldNames.GENE_MUTATIONS);
    return fieldsToSkip.stream().noneMatch(sourceField::equals);
  }

  private static boolean isSpecialSearchFieldName(String searchFieldName){
    return SPECIAL_BOOSTED_SEARCH_FIELD_NAMES.contains(searchFieldName);
  }

  private static boolean isGeneMutationsSearchFieldName(String searchFieldName){
    return GENE_MUTATIONS_BOOSTED_SEARCH_FIELD_NAMES.contains(searchFieldName);
  }

  private static SearchKey createSearchKey(String searchFieldName) {
    if(isSpecialSearchFieldName(searchFieldName)){
      return newSearchKey(searchFieldName, SPECIAL_BOOSTED_SEARCH_FIELDS);
    } else if (isGeneMutationsSearchFieldName(searchFieldName)) {
      return newSearchKey(searchFieldName, GENE_MUTATIONS_BOOSTED_SEARCH_FIELDS);
    } else {
      return newSearchKey(searchFieldName, NORMAL_SEARCH_FIELDS);
    }
  }

  @NonNull
  private static Set<SearchField> buildMultiMatchFieldList(Iterable<String> searchKeyNames, String queryString, String type) {
    val expandedSearchFields = ImmutableSet.<SearchField> builder();

    for (val searchKeyName : searchKeyNames) {
      // Exact match searchKeyNames (DCC-2324)
      if (searchKeyName.equals("start")) {
        // TODO: Investigate and see if we still need this in the ES version wer're using.
        /*
         * NOTE: This is a work around quirky ES issue. We need to prefix the document type here to prevent
         * NumberFormatException, it appears that ES cannot determine what type 'start' is. This is for ES 0.9, later
         * versions may not have this problem.
         */
        expandedSearchFields.add(newNoneBoostedSearchField(MUTATION_PREFIX, searchKeyName));
      } else if (shouldProcess(searchKeyName)) {
        val searchKey = createSearchKey(searchKeyName);
        expandedSearchFields.addAll(searchKey.getExpandedSearchFields());
      }
    }

    // Don't boost without space or genes won't show when partially matched
    val geneMutationSearchSubfields = queryString.contains(" ") ? BOOSTED_SEARCH_FIELDS : NORMAL_SEARCH_FIELDS;
    val geneSearchKey = newSearchKey(FieldNames.GENE_MUTATIONS, geneMutationSearchSubfields) ;
    expandedSearchFields.addAll(geneSearchKey.getExpandedSearchFields());

    // Exact-match search on "id". //TODO: Assuming FieldNames.ID is none boosted
    val idSearchKey = newSearchKey(FieldNames.ID, newNoneBoostedSearchField(EXACT_MATCH_FIELDNAME));
    expandedSearchFields.addAll(idSearchKey.getExpandedSearchFields());

    if (isRepositoryFileRelated(type)) {
      // For repository file related searches, we want fuzzy search.
      val repoSearchKey = createSearchKey(FieldNames.ID);
      expandedSearchFields.addAll(repoSearchKey.getExpandedSearchFields());
    }

    return expandedSearchFields.build();
  }

}
