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
package org.icgc.dcc.portal.analysis;

import static org.icgc.dcc.portal.repository.TermsLookupRepository.TERMS_LOOKUP_PATH;
import static org.icgc.dcc.portal.util.ElasticsearchRequestUtils.toBoolFilterFrom;
import static org.icgc.dcc.portal.util.JsonUtils.LIST_TYPE_REFERENCE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.icgc.dcc.portal.config.PortalProperties;
import org.icgc.dcc.portal.model.BaseEntitySet;
import org.icgc.dcc.portal.model.DerivedEntitySetDefinition;
import org.icgc.dcc.portal.model.EntitySet;
import org.icgc.dcc.portal.model.UnionAnalysisRequest;
import org.icgc.dcc.portal.model.UnionAnalysisResult;
import org.icgc.dcc.portal.model.UnionUnit;
import org.icgc.dcc.portal.model.UnionUnitWithCount;
import org.icgc.dcc.portal.repository.EntityListRepository;
import org.icgc.dcc.portal.repository.GeneRepository;
import org.icgc.dcc.portal.repository.RepositoryFileRepository;
import org.icgc.dcc.portal.repository.TermsLookupRepository;
import org.icgc.dcc.portal.repository.UnionAnalysisRepository;
import org.icgc.dcc.portal.util.SearchResponses;
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
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
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
  private final PortalProperties properties;

  @NonNull
  private final UnionAnalysisRepository unionAnalysisRepository;
  @NonNull
  private final EntityListRepository entityListRepository;
  @NonNull
  private final TermsLookupRepository termsLookupRepository;
  @NonNull
  private final GeneRepository geneRepository;
  @NonNull
  private final RepositoryFileRepository repositoryFileRepository;

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
          val count = termsLookupRepository.getDonorCount(def);
          result.add(UnionUnitWithCount.copyOf(def, count));
        }
      } else {
        for (val def : definitions) {
          val count = termsLookupRepository.getUnionCount(def, entityType);
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

  @Async
  public void combineListsAsync(@NonNull final UUID newEntityId,
      @NonNull final DerivedEntitySetDefinition entitySetDefinition) {
    combineLists(newEntityId, entitySetDefinition);
  }

  public void combineLists(@NonNull final UUID newEntityId,
      @NonNull final DerivedEntitySetDefinition entitySetDefinition) {
    EntitySet newEntity = null;

    try {
      newEntity = entityListRepository.find(newEntityId);
      val dataVersion = newEntity.getVersion();
      entityListRepository.update(newEntity.updateStateToInProgress(), dataVersion);

      val definitions = entitySetDefinition.getUnion();
      val entityType = entitySetDefinition.getType();

      SearchResponse response;
      long totalHits;
      Iterable<String> entityIds;
      val maxUnionCount = termsLookupRepository.getMaxUnionCount();
      if (entityType == BaseEntitySet.Type.DONOR) {
        response = termsLookupRepository.getDonorUnion(definitions);
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

        entityListRepository.update(newEntity.updateStateToError(), dataVersion);
        return;
      }

      val lookupType = entityType.toLookupType();
      termsLookupRepository.createTermsLookup(lookupType, newEntityId, entityIds, entitySetDefinition.isTransient());
      entityListRepository.update(newEntity.updateStateToFinished(totalHits), dataVersion);
    } catch (Exception e) {
      log.error("Error while combining lists for {}. See exception below.", newEntityId);
      log.error("Error while combining lists: '{}'", e);

      if (null != newEntity) {
        entityListRepository.update(newEntity.updateStateToError(), newEntity.getVersion());
      }
    }
  }

  public List<String> retriveListItems(@NonNull final EntitySet entityList) {
    val lookupTypeName = entityList.getType().toLookupType().getName();
    val query = client.prepareGet(TermsLookupRepository.TERMS_LOOKUP_INDEX_NAME,
        lookupTypeName, entityList.getId().toString());

    val response = query.execute().actionGet();
    val rawValues = response.getSource().get(TERMS_LOOKUP_PATH);
    log.debug("Raw values of {} are: '{}'", lookupTypeName, rawValues);

    return MAPPER.convertValue(rawValues, LIST_TYPE_REFERENCE);
  }

  public Map<String, String> retrieveGeneIdsAndSymbolsByListId(final UUID listId) {
    return geneRepository.findGeneSymbolsByGeneListId(listId);
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

}