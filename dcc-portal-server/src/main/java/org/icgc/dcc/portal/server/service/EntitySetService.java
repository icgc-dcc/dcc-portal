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
package org.icgc.dcc.portal.server.service;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static lombok.AccessLevel.PRIVATE;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.search.SearchResponse;
import org.icgc.dcc.portal.server.analysis.UnionAnalyzer;
import org.icgc.dcc.portal.server.config.ServerProperties;
import org.icgc.dcc.portal.server.model.BaseEntitySet;
import org.icgc.dcc.portal.server.model.BaseEntitySetDefinition;
import org.icgc.dcc.portal.server.model.DerivedEntitySetDefinition;
import org.icgc.dcc.portal.server.model.EntitySet;
import org.icgc.dcc.portal.server.model.EntitySet.SubType;
import org.icgc.dcc.portal.server.model.EntitySetDefinition;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.server.repository.EntitySetRepository;
import org.icgc.dcc.portal.server.repository.FileRepository;
import org.icgc.dcc.portal.server.repository.TermsLookupRepository;
import org.icgc.dcc.portal.server.util.SearchResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.supercsv.io.CsvListWriter;

import com.google.common.collect.ImmutableList;

import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * A service to facilitate entity set operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EntitySetService {

  /**
   * Dependencies
   */
  @NonNull
  private final EntitySetRepository entitySetRepository;
  @NonNull
  private final TermsLookupRepository termsLookupRepository;
  @NonNull
  private final FileRepository fileRepository;
  @NonNull
  private final EntitySetRepository repository;
  @NonNull
  private final UnionAnalyzer analyzer;
  @NonNull
  private final ServerProperties properties;
  @NonNull
  private final QueryEngine queryEngine;

  private final Jql2PqlConverter converter = Jql2PqlConverter.getInstance();

  /**
   * Configuration.
   */
  private int maxNumberOfHits;
  private int maxMultiplier;
  private int maxUnionCount;
  // TODO: Either use this or loose this!
  @SuppressWarnings("unused")
  private int maxPreviewNumberOfHits;

  @Getter(lazy = true, value = PRIVATE)
  private final int currentDataVersion = resolveDataVersion();

  public EntitySet getEntitySet(@NonNull final UUID entitySetId) {
    val list = repository.find(entitySetId);

    if (null == list) {
      log.error("No list is found for id: '{}'.", entitySetId);
    } else {
      log.debug("Got entity list: '{}'.", list);
    }

    return list;
  }

  public EntitySet updateEntitySet(@NonNull final UUID entitySetId, @NonNull final String newName) {
    val entitySet = repository.find(entitySetId);
    if (null == entitySet) {
      log.error("No list is found for id: '{}'.", entitySetId);
      return null;
    }

    log.debug("Found entity set for update: '{}'.", entitySet);
    val updatedSet =
        new EntitySet(entitySet.getId(), entitySet.getState(), entitySet.getCount(), newName,
            entitySet.getDescription(), entitySet.getType(),
            entitySet.getVersion());

    val updateCount = repository.update(updatedSet, updatedSet.getVersion());
    return updateCount == 1 ? updatedSet : null;
  }

  public EntitySet updateEntitySet(@NonNull final UUID entitySetId,
      @NonNull final EntitySetDefinition entitySetDefinition) {
    materializeList(entitySetId, entitySetDefinition);
    return getEntitySet(entitySetId);
  }

  public EntitySet updateEntitySet(@NonNull final UUID entitySetId,
      @NonNull final DerivedEntitySetDefinition entitySetDefinition) {
    analyzer.combineLists(entitySetId, entitySetDefinition);
    return getEntitySet(entitySetId);
  }

  public EntitySet createEntitySet(@NonNull final EntitySetDefinition entitySetDefinition, boolean async) {
    val newEntitySet = createAndSaveNewListFrom(entitySetDefinition);
    if (async) {
      materializeListAsync(newEntitySet.getId(), entitySetDefinition);
    } else {
      materializeList(newEntitySet.getId(), entitySetDefinition);
      return getEntitySet(newEntitySet.getId());
    }
    return newEntitySet;
  }

  public EntitySet createExternalEntitySet(@NonNull final EntitySetDefinition entitySetDefinition) {
    val newEntitySet = createAndSaveNewListFrom(entitySetDefinition);
    materializeRepositoryList(newEntitySet.getId(), entitySetDefinition);
    return getEntitySet(newEntitySet.getId());
  }

  public EntitySet createFileEntitySet(@NonNull final EntitySetDefinition entitySetDefinition) {
    val newEntitySet = createAndSaveNewListFrom(entitySetDefinition);
    materializeFileSet(newEntitySet.getId(), entitySetDefinition);
    return getEntitySet(newEntitySet.getId());
  }

  public EntitySet computeEntitySet(@NonNull final DerivedEntitySetDefinition entitySetDefinition, boolean async) {
    val newEntitySet = createAndSaveNewListFrom(entitySetDefinition);
    if (async) {
      analyzer.combineListsAsync(newEntitySet.getId(), entitySetDefinition);
    } else {
      analyzer.combineLists(newEntitySet.getId(), entitySetDefinition);
      return getEntitySet(newEntitySet.getId());
    }
    return newEntitySet;
  }

  public List<String> getSetItems(@NonNull EntitySet entitySet) {
    // TODO: This method should be moved to this class
    return analyzer.retriveListItems(entitySet);
  }

  public void exportSetItems(@NonNull EntitySet entitySet, @NonNull OutputStream outputStream) throws IOException {
    // TODO: Explain why this distinction is needed
    val isGeneType = BaseEntitySet.Type.GENE == entitySet.getType();
    val records =
        isGeneType ? convertToListOfListForGene(
            analyzer.retrieveGeneIdsAndSymbolsByListId(entitySet.getId())) : convertToListOfList(
                analyzer.retriveListItems(entitySet));

    @Cleanup
    val writer = new CsvListWriter(new OutputStreamWriter(outputStream, UTF_8), TAB_PREFERENCE);
    for (val record : records) {
      writer.write(record);
    }
    // This should already be done by @Cleanup. Not sure why it is here...
    writer.flush();
  }

  @Async
  private void materializeListAsync(@NonNull final UUID newEntityId,
      @NonNull final EntitySetDefinition entitySetDefinition) {
    materializeList(newEntityId, entitySetDefinition);
  }

  private void materializeList(@NonNull final UUID newEntityId,
      @NonNull final EntitySetDefinition entitySetDefinition) {
    EntitySet newEntity = null;

    try {
      newEntity = entitySetRepository.find(newEntityId);
      val dataVersion = newEntity.getVersion();
      entitySetRepository.update(newEntity.updateStateToInProgress(), dataVersion);

      val max = entitySetDefinition.getLimit(maxNumberOfHits);
      val response = executeFilterQuery(entitySetDefinition, max);

      val entityIds = SearchResponses.getHitIds(response);
      log.debug("The result of running a FilterParam query is: '{}'", entityIds);

      val lookupType = entitySetDefinition.getType().toLookupType();
      termsLookupRepository.createTermsLookup(lookupType, newEntityId, entityIds, entitySetDefinition.isTransient());

      val count = getCountFrom(response, max);
      entitySetRepository.update(newEntity.updateStateToFinished(count), dataVersion);
    } catch (Exception e) {
      log.error("Error while materializing list for {}: {}", newEntityId, e);

      if (null != newEntity) {
        entitySetRepository.update(newEntity.updateStateToError(), newEntity.getVersion());
      }
    }
  }

  @SneakyThrows
  private void materializeRepositoryList(@NonNull final UUID newEntityId,
      @NonNull final EntitySetDefinition entitySet) {
    val newEntity = entitySetRepository.find(newEntityId);
    val dataVersion = newEntity.getVersion();

    val query = Query.builder()
        .filters(entitySet.getFilters())
        .fields(Arrays.asList("donorId"))
        .sort("id")
        .order("desc")
        .size(maxNumberOfHits)
        .defaultLimit(maxNumberOfHits)
        .build();
    val maxSetSize = entitySet.getLimit(maxNumberOfHits);
    val entityIds = fileRepository.findAllDonorIds(query, maxSetSize);

    val lookupType = entitySet.getType().toLookupType();
    termsLookupRepository.createTermsLookup(lookupType, newEntityId, entityIds, entitySet.isTransient());

    val count = entityIds.size();
    // Done - update status to finished
    entitySetRepository.update(newEntity.updateStateToFinished(count), dataVersion);
  }

  private void materializeFileSet(@NonNull final UUID newEntityId,
      @NonNull final EntitySetDefinition entitySet) {
    val newEntity = entitySetRepository.find(newEntityId);
    val dataVersion = newEntity.getVersion();

    val query = Query.builder()
        .filters(entitySet.getFilters())
        .sort("id")
        .order("desc")
        .size(entitySet.getSize())
        .defaultLimit(maxNumberOfHits)
        .build();

    val entityIds = fileRepository.findAllFileIds(query);
    val lookupType = entitySet.getType().toLookupType();

    val repoList = entitySet.getFilters().path("file").path("repoName").path("is");
    if (repoList.isArray()) {
      termsLookupRepository.createTermsLookup(lookupType, newEntityId, entityIds, repoList.get(0).asText());
    } else {
      termsLookupRepository.createTermsLookup(lookupType, newEntityId, entityIds);
    }

    val count = entityIds.size();
    entitySetRepository.update(newEntity.updateStateToFinished(count), dataVersion);
  }

  private int resolveDataVersion() {
    return properties.getRelease().getDataVersion();
  }

  private EntitySet createAndSaveNewListFrom(final BaseEntitySet entitySetDefinition, final SubType subtype) {
    val dataVersion = getCurrentDataVersion();
    val newEntitySet = EntitySet.createFromDefinition(entitySetDefinition, dataVersion);

    if (null != subtype) {
      newEntitySet.setSubtype(subtype);
    }

    val insertCount = repository.save(newEntitySet, dataVersion);
    checkState(insertCount == 1, "Could not save list - Insert count: %s", insertCount);

    return newEntitySet;
  }

  private EntitySet createAndSaveNewListFrom(final BaseEntitySetDefinition entitySetDefinition) {
    val subtype = entitySetDefinition.isTransient() ? SubType.TRANSIENT : null;
    return createAndSaveNewListFrom(entitySetDefinition, subtype);
  }

  // Helpers to facilitate exportListItems() only. They should not be used in anywhere else.
  private List<List<String>> convertToListOfList(@NonNull final List<String> list) {
    val result = new ArrayList<List<String>>(list.size());

    for (val v : list) {
      result.add(Arrays.asList(v));
    }

    return result;
  }

  private List<List<String>> convertToListOfListForGene(@NonNull final Map<String, String> map) {
    val result = new ArrayList<List<String>>(map.size());
    val entrySet = map.entrySet();

    for (val v : entrySet) {
      result.add(Arrays.asList(v.getKey(), v.getValue()));
    }

    return result;
  }

  private Type getRepositoryByEntityType(final BaseEntitySet.Type entityType) {
    if (entityType == BaseEntitySet.Type.DONOR) {
      return Type.DONOR_CENTRIC;
    } else if (entityType == BaseEntitySet.Type.GENE) {
      return Type.GENE_CENTRIC;
    } else if (entityType == BaseEntitySet.Type.MUTATION) {
      return Type.MUTATION_CENTRIC;
    }

    log.error("No mapping for enum value '{}' of BaseEntitySet.Type.", entityType);
    throw new IllegalStateException("No mapping for enum value: " + entityType);
  }

  private long getCountFrom(@NonNull final SearchResponse response, final long max) {
    val result = SearchResponses.getTotalHitCount(response);

    return min(max, result);
  }

  private SearchResponse executeFilterQuery(@NonNull final EntitySetDefinition definition, final int max) {
    log.debug("List def is: " + definition);

    val query = Query.builder()
        .fields(ImmutableList.of("id"))
        .filters(definition.getFilters())
        .sort(definition.getSortBy())
        .order(definition.getSortOrder().getName())
        .build();

    val type = getRepositoryByEntityType(definition.getType());
    val pql = converter.convert(query, type);
    val request = queryEngine.execute(pql, type);
    return request.getRequestBuilder()
        .setSize(max)
        .execute().actionGet();
  }

  @PostConstruct
  private void init() {
    val setOpSettings = properties.getSetOperation();
    maxNumberOfHits = setOpSettings.getMaxNumberOfHits();
    maxMultiplier = setOpSettings.getMaxMultiplier();
    maxUnionCount = maxNumberOfHits * maxMultiplier;
    maxPreviewNumberOfHits = min(setOpSettings.getMaxPreviewNumberOfHits(), maxUnionCount);
  }

}
