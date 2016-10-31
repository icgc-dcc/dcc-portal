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
package org.icgc.dcc.portal.server.analysis;

import static java.lang.Math.min;
import static org.icgc.dcc.portal.server.repository.TermsLookupRepository.TERMS_LOOKUP_PATH;
import static org.icgc.dcc.portal.server.util.ElasticsearchRequestUtils.toBoolFilterFrom;
import static org.icgc.dcc.portal.server.util.ElasticsearchRequestUtils.toDonorBoolFilter;
import static org.icgc.dcc.portal.server.util.JsonUtils.LIST_TYPE_REFERENCE;
import static org.icgc.dcc.portal.server.util.SearchResponses.getHitIdsSet;
import static org.icgc.dcc.portal.server.util.SearchResponses.getTotalHitCount;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.icgc.dcc.portal.server.config.ServerProperties;
import org.icgc.dcc.portal.server.model.BaseEntitySet;
import org.icgc.dcc.portal.server.model.DerivedEntitySetDefinition;
import org.icgc.dcc.portal.server.model.EntitySet;
import org.icgc.dcc.portal.server.model.UnionAnalysisRequest;
import org.icgc.dcc.portal.server.model.UnionAnalysisResult;
import org.icgc.dcc.portal.server.model.UnionUnit;
import org.icgc.dcc.portal.server.model.UnionUnitWithCount;
import org.icgc.dcc.portal.server.repository.DonorRepository;
import org.icgc.dcc.portal.server.repository.EntitySetRepository;
import org.icgc.dcc.portal.server.repository.FileRepository;
import org.icgc.dcc.portal.server.repository.GeneRepository;
import org.icgc.dcc.portal.server.repository.TermsLookupRepository;
import org.icgc.dcc.portal.server.repository.UnionAnalysisRepository;
import org.icgc.dcc.portal.server.util.SearchResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides various set operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UnionAnalyzer {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();

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

  @NonNull
  private final UnionAnalysisRepository unionAnalysisRepository;
  @NonNull
  private final EntitySetRepository entitySetRepository;
  @NonNull
  private final TermsLookupRepository termsLookupRepository;
  @NonNull
  private final GeneRepository geneRepository;
  @NonNull
  private final FileRepository fileRepository;
  @NonNull
  private final DonorRepository donorRepository;

  @Async
  public void calculateUnionUnitCounts(@NonNull final UUID id, @NonNull final UnionAnalysisRequest request) {
    UnionAnalysisResult analysis = null;

    try {
      analysis = unionAnalysisRepository.find(id);
      val dataVersion = analysis.getVersion();
      unionAnalysisRepository.update(analysis.updateStateToInProgress(), dataVersion);

      val entityType = request.getType();
      val definitions = request.toUnionSets();

      val result = new ArrayList<UnionUnitWithCount>(definitions.size());

      if (entityType == BaseEntitySet.Type.DONOR) {
        for (val def : definitions) {
          val count = getDonorCount(def);
          result.add(UnionUnitWithCount.copyOf(def, count));
        }
      } else {
        for (val def : definitions) {
          val count = getUnionCount(def, entityType);
          result.add(UnionUnitWithCount.copyOf(def, count));
        }
      }

      log.debug("Result of Union Analysis is: '{}'", result);
      unionAnalysisRepository.update(analysis.updateStateToFinished(result), dataVersion);
    } catch (Exception e) {
      log.error("Error while calculating UnionUnitCounts for {}: {}", id, e);

      if (null != analysis) {
        unionAnalysisRepository.update(analysis.updateStateToError(), analysis.getVersion());
      }
    }
  }

  public List<String> previewSetUnion(@NonNull final DerivedEntitySetDefinition definition) {
    val definitions = definition.getUnion();
    val entityType = definition.getType();

    val response = unionAll(definitions, entityType, termsLookupRepository.getMaxPreviewNumberOfHits());
    return SearchResponses.getHitIds(response);
  }

  public SearchResponse computeExclusion(@NonNull final UnionUnit unionUnit, BaseEntitySet.Type type,
      List<String> fields, List<String> sort) {
    val response =
        subtractOne(unionUnit, type, 20000, fields, sort);
    return response;
  }

  @Async
  public void combineListsAsync(@NonNull final UUID newEntityId,
      @NonNull final DerivedEntitySetDefinition entitySetDefinition) {
    combineLists(newEntityId, entitySetDefinition);
  }

  public void combineLists(@NonNull final UUID newEntityId,
      @NonNull final DerivedEntitySetDefinition entitySetDefinition) {
    EntitySet newEntity = null;

    try {
      newEntity = entitySetRepository.find(newEntityId);
      val dataVersion = newEntity.getVersion();
      entitySetRepository.update(newEntity.updateStateToInProgress(), dataVersion);

      val definitions = entitySetDefinition.getUnion();
      val entityType = entitySetDefinition.getType();

      SearchResponse response;
      long totalHits;
      Iterable<String> entityIds;
      val maxUnionCount = termsLookupRepository.getMaxUnionCount();
      if (entityType == BaseEntitySet.Type.DONOR) {
        response = getDonorUnion(definitions);
        entityIds = SearchResponses.getHitIdsSet(response);
        totalHits = Iterables.size(entityIds);
      } else {
        response = unionAll(definitions, entityType, maxUnionCount);
        totalHits = SearchResponses.getTotalHitCount(response);
        entityIds = SearchResponses.getHitIds(response);
      }
      log.debug("Union result is: '{}'", entityIds);

      if (totalHits > maxUnionCount) {
        log.info(
            "Because the total hit count ({}) exceeds the allowed maximum ({}), this set operation is aborted.",
            totalHits, maxUnionCount);

        entitySetRepository.update(newEntity.updateStateToError(), dataVersion);
        return;
      }

      val lookupType = entityType.toLookupType();
      termsLookupRepository.createTermsLookup(lookupType, newEntityId, entityIds, entitySetDefinition.isTransient());
      entitySetRepository.update(newEntity.updateStateToFinished(totalHits), dataVersion);
    } catch (Exception e) {
      log.error("Error while combining lists for {}. See exception below.", newEntityId);
      log.error("Error while combining lists: '{}'", e);

      if (null != newEntity) {
        entitySetRepository.update(newEntity.updateStateToError(), newEntity.getVersion());
      }
    }
  }

  public List<String> retriveListItems(@NonNull final EntitySet entitySet) {
    val lookupTypeName = entitySet.getType().toLookupType().getName();
    val query = client.prepareGet(TermsLookupRepository.TERMS_LOOKUP_INDEX_NAME,
        lookupTypeName, entitySet.getId().toString());

    val response = query.execute().actionGet();
    val rawValues = response.getSource().get(TERMS_LOOKUP_PATH);
    log.debug("Raw values of {} are: '{}'", lookupTypeName, rawValues);

    return MAPPER.convertValue(rawValues, LIST_TYPE_REFERENCE);
  }

  public Map<String, String> retrieveGeneIdsAndSymbolsByListId(final UUID setId) {
    return geneRepository.findGeneSymbolsByGeneListId(setId);
  }

  private SearchResponse unionAll(final Iterable<UnionUnit> definitions, final BaseEntitySet.Type entityType,
      final int max) {

    val response = termsLookupRepository.runUnionEsQuery(
        entityType.getIndexTypeName(),
        SearchType.QUERY_THEN_FETCH,
        toBoolFilterFrom(definitions, entityType),
        max);

    return response;
  }

  private SearchResponse subtractOne(final UnionUnit definition, final BaseEntitySet.Type entityType,
      final int max, List<String> fields, List<String> sort) {
    val response = donorRepository.singleDonorUnion(
        entityType.getIndexTypeName(),
        SearchType.QUERY_THEN_FETCH,
        toBoolFilterFrom(definition, entityType),
        max,
        fields.toArray(new String[fields.size()]),
        sort);

    return response;
  }

  private SearchResponse getDonorUnion(final Iterable<UnionUnit> definitions) {
    val boolFilter = toBoolFilterFrom(definitions, BaseEntitySet.Type.DONOR);
    val response = donorRepository.donorSearchRequest(boolFilter, termsLookupRepository.getMaxUnionCount());

    return response;
  }

  private long getDonorCount(final UnionUnit unionDefinition) {
    val boolFilter = toDonorBoolFilter(unionDefinition);
    val response = donorRepository.donorSearchRequest(boolFilter, termsLookupRepository.getMaxUnionCount());

    return getHitIdsSet(response).size();
  }

  private long getUnionCount(
      final UnionUnit unionDefinition,
      final BaseEntitySet.Type entityType) {
    val maxUnionCount = termsLookupRepository.getMaxUnionCount();

    val response = termsLookupRepository.runUnionEsQuery(
        entityType.getIndexTypeName(),
        SearchType.COUNT,
        toBoolFilterFrom(unionDefinition, entityType),
        maxUnionCount);

    val count = getCountFrom(response, maxUnionCount);
    log.debug("Total hits: {}", count);

    return count;
  }

  private static long getCountFrom(@NonNull final SearchResponse response, final long max) {
    val result = getTotalHitCount(response);
    return min(max, result);
  }

}