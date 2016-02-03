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
package org.icgc.dcc.portal.util;

import static java.util.Collections.emptyList;
import static lombok.AccessLevel.PRIVATE;
import static org.dcc.portal.pql.meta.Type.REPOSITORY_FILE;
import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.index.query.FilterBuilders.boolFilter;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.icgc.dcc.portal.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.repository.TermsLookupRepository.createTermsLookupFilter;
import static org.icgc.dcc.portal.util.SearchResponses.getTotalHitCount;

import java.util.List;

import org.dcc.portal.pql.meta.IndexModel;
import org.dcc.portal.pql.meta.RepositoryFileTypeModel.Fields;
import org.dcc.portal.pql.meta.TypeModel;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.icgc.dcc.portal.model.BaseEntitySet;
import org.icgc.dcc.portal.model.IndexModel.Kind;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.UnionUnit;
import org.icgc.dcc.portal.repository.TermsLookupRepository;

import com.google.common.collect.Lists;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class ElasticsearchRequestUtils {

  private static final String FILE_INDEX_TYPE = REPOSITORY_FILE.getId();
  private static final TypeModel TYPE_MODEL = IndexModel.getRepositoryFileTypeModel();
  private final static String FIELD_NAME = "_id";

  public static final String[] EMPTY_SOURCE_FIELDS = null;

  public static String[] resolveSourceFields(Query query, Kind kind) {
    val sourceFields = getSource(query, kind);
    if (sourceFields.isEmpty()) {
      return EMPTY_SOURCE_FIELDS;
    }

    return sourceFields.toArray(new String[sourceFields.size()]);
  }

  @NonNull
  public static GetRequestBuilder setFetchSourceOfGetRequest(GetRequestBuilder builder, Query query, Kind kind) {
    String[] sourceFields = resolveSourceFields(query, kind);

    if (sourceFields != EMPTY_SOURCE_FIELDS) {
      builder.setFetchSource(sourceFields, EMPTY_SOURCE_FIELDS);
    }

    return builder;
  }

  @NonNull
  public static SearchRequestBuilder setFetchSourceOfSearchRequest(SearchRequestBuilder builder, Query query,
      Kind kind) {
    String[] sourceFields = resolveSourceFields(query, kind);

    if (sourceFields != EMPTY_SOURCE_FIELDS) {
      builder.setFetchSource(sourceFields, EMPTY_SOURCE_FIELDS);
    }

    return builder;
  }

  @NonNull
  public static boolean isRepositoryDonor(Client client, String donorId, String repoIndexName) {
    return isRepositoryDonorExecute(client, Fields.DONOR_ID, donorId, repoIndexName);
  }

  @NonNull
  public static boolean isRepositoryDonorInProject(Client client, String projectId, String repoIndexName) {
    return isRepositoryDonorExecute(client, Fields.PROJECT_CODE, projectId, repoIndexName);
  }

  @NonNull
  public static BoolFilterBuilder toBoolFilterFrom(final UnionUnit unionDefinition,
      final BaseEntitySet.Type entityType) {
    val lookupType = entityType.toLookupType();
    val boolFilter = boolFilter();

    // Adding Musts
    val intersectionUnits = unionDefinition.getIntersection();
    for (val mustId : intersectionUnits) {
      boolFilter.must(createTermsLookupFilter(FIELD_NAME, lookupType, mustId));
    }

    // Adding MustNots
    val exclusionUnits = unionDefinition.getExclusions();
    for (val notId : exclusionUnits) {
      boolFilter.mustNot(createTermsLookupFilter(FIELD_NAME, lookupType, notId));
    }
    return boolFilter;
  }

  @NonNull
  public static BoolFilterBuilder toBoolFilterFrom(final Iterable<UnionUnit> definitions,
      final BaseEntitySet.Type entityType) {
    val boolFilter = boolFilter();

    for (val def : definitions) {
      boolFilter.should(toBoolFilterFrom(def, entityType));
    }
    return boolFilter;
  }

  public static BoolFilterBuilder toDonorBoolFilter(final UnionUnit unionDefinition) {

    val boolFilter = boolFilter();

    // Adding Musts
    val intersectionUnits = unionDefinition.getIntersection();
    for (val mustId : intersectionUnits) {
      val mustTerms =
          TermsLookupRepository.createTermsLookupFilter(FIELD_NAME, TermsLookupRepository.TermLookupType.DONOR_IDS,
              mustId);
      boolFilter.must(mustTerms);
    }

    // Adding MustNots
    val exclusionUnits = unionDefinition.getExclusions();
    for (val notId : exclusionUnits) {
      val mustNotTerms =
          TermsLookupRepository.createTermsLookupFilter(FIELD_NAME, TermsLookupRepository.TermLookupType.DONOR_IDS,
              notId);
      boolFilter.mustNot(mustNotTerms);
    }
    return boolFilter;
  }

  private static boolean isRepositoryDonorExecute(Client client, String fieldAlias, String value,
      String repoIndexName) {
    val query = nestedQuery(TYPE_MODEL.getNestedPath(fieldAlias),
        termQuery(TYPE_MODEL.getField(fieldAlias), value));
    val search = client.prepareSearch(repoIndexName)
        .setTypes(FILE_INDEX_TYPE)
        .setSearchType(COUNT)
        .setQuery(query);

    log.debug("ES query: '{}'.", search);
    val response = search.execute().actionGet();

    return getTotalHitCount(response) > 0;
  }

  private static List<String> getSource(Query query, Kind kind) {
    switch (kind) {
    case MUTATION:
      return prepareMutationIncludes(query);
    case DONOR:
      return prepareDonorIncludes(query);
    case GENE:
      return prepareGeneIncludes(query, kind);
    case PATHWAY:
      return preparePathwayIncludes(query);
    case PROJECT:
      return prepareProjectIncludes(query, kind);
    case OCCURRENCE:
      return prepareOccurrenceIncludes(query, kind);
    case GENE_SET:
      return prepareGeneSetIncludes(query, kind);
    default:
      return emptyList();
    }
  }

  private static List<String> prepareGeneSetIncludes(Query query, Kind kind) {
    return resolveFields(query, kind, "hierarchy", "inferredTree", "synonyms", "altIds");
  }

  private static List<String> prepareOccurrenceIncludes(Query query, Kind kind) {
    return resolveFields(query, kind, "observation");
  }

  private static List<String> prepareMutationIncludes(Query query) {
    val sourceFields = Lists.<String> newArrayList();

    if (query.hasInclude("transcripts") || query.hasInclude("consequences")) {
      sourceFields.add("transcript");
    }

    if (query.hasInclude("occurrences")) {
      sourceFields.add("ssm_occurrence");
    }

    return sourceFields;
  }

  private static List<String> prepareDonorIncludes(Query query) {
    val sourceFields = Lists.<String> newArrayList();

    if (query.hasInclude("specimen")) {
      sourceFields.add("specimen");
    }

    sourceFields.add("family");
    sourceFields.add("exposure");
    sourceFields.add("therapy");

    return sourceFields;
  }

  private static List<String> prepareGeneIncludes(Query query, Kind kind) {
    val sourceFields = Lists.<String> newArrayList();

    // external_db_ids and pathways are objects. Fields support only leaf nodes, that's why they must be included in
    // source
    sourceFields.addAll(resolveFields(query, kind, "externalDbIds", "pathways", "sets"));

    if (query.hasInclude("transcripts")) {
      sourceFields.add("transcripts");
    }

    if (query.hasInclude("projects")) {
      sourceFields.add("project");
    }

    if (query.hasInclude("pathways")) {
      sourceFields.add("pathways");
    }

    return sourceFields;
  }

  private static List<String> resolveFields(Query query, Kind kind, String... fields) {
    val result = Lists.<String> newArrayList();
    val typeFieldsMap = FIELDS_MAPPING.get(kind);
    val queryFields = query.getFields();
    for (val field : fields) {
      if (!query.hasFields() || queryFields.contains(field)) {
        result.add(typeFieldsMap.get(field));
      }
    }

    return result;
  }

  private static List<String> preparePathwayIncludes(Query query) {
    val sourceFields = Lists.<String> newArrayList();

    if (query.hasInclude("projects")) {
      sourceFields.add("projects");
    }

    return sourceFields;
  }

  private static List<String> prepareProjectIncludes(Query query, Kind kind) {
    return resolveFields(query, kind, "experimentalAnalysisPerformedDonorCounts",
        "experimentalAnalysisPerformedSampleCounts");
  }

}
