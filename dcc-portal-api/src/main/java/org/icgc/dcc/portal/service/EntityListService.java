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
package org.icgc.dcc.portal.service;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.min;
import static lombok.AccessLevel.PRIVATE;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Min;

import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.search.SearchResponse;
import org.icgc.dcc.portal.analysis.UnionAnalyzer;
import org.icgc.dcc.portal.config.PortalProperties;
import org.icgc.dcc.portal.model.BaseEntitySet;
import org.icgc.dcc.portal.model.BaseEntitySetDefinition;
import org.icgc.dcc.portal.model.DerivedEntitySetDefinition;
import org.icgc.dcc.portal.model.EntitySet;
import org.icgc.dcc.portal.model.EntitySet.SubType;
import org.icgc.dcc.portal.model.EntitySetDefinition;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.repository.EntityListRepository;
import org.icgc.dcc.portal.repository.RepositoryFileRepository;
import org.icgc.dcc.portal.repository.TermsLookupRepository;
import org.icgc.dcc.portal.util.SearchResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.supercsv.io.CsvListWriter;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
@RequiredArgsConstructor(onConstructor = @__(@Autowired) )
public class EntityListService {

  /**
   * Dependencies
   */
  @NonNull
  private final EntityListRepository entityListRepository;
  @NonNull
  private final TermsLookupRepository termsLookupRepository;
  @NonNull
  private final RepositoryFileRepository repositoryFileRepository;
  @NonNull
  private final EntityListRepository repository;
  @NonNull
  private final UnionAnalyzer analyzer;
  @NonNull
  private final PortalProperties properties;

  private final QueryEngine queryEngine;
  private final Jql2PqlConverter converter = Jql2PqlConverter.getInstance();

  /**
   * Configuration.
   */
  @Min(1)
  private int maxNumberOfHits;
  @Min(1)
  private int maxMultiplier;
  @Min(1)
  private int maxUnionCount;
  @Min(1)
  private int maxPreviewNumberOfHits;

  @Getter(lazy = true, value = PRIVATE)
  private final int currentDataVersion = resolveDataVersion();

  public EntitySet getEntityList(@NonNull final UUID entitySetId) {
    val list = repository.find(entitySetId);

    if (null == list) {
      log.error("No list is found for id: '{}'.", entitySetId);
    } else {
      log.debug("Got entity list: '{}'.", list);
    }

    return list;
  }

  public EntitySet createEntityList(@NonNull final EntitySetDefinition entitySetDefinition, boolean async) {
    val newEntitySet = createAndSaveNewListFrom(entitySetDefinition);
    if (async) {
      materializeListAsync(newEntitySet.getId(), entitySetDefinition);
    } else {
      materializeList(newEntitySet.getId(), entitySetDefinition);
    }
    return newEntitySet;
  }

  public EntitySet createExternalEntityList(@NonNull final EntitySetDefinition entitySetDefinition) {
    val newEntitySet = createAndSaveNewListFrom(entitySetDefinition);
    materializeRepositoryList(newEntitySet.getId(), entitySetDefinition);
    return newEntitySet;
  }

  public EntitySet createFileEntitySet(@NonNull final EntitySetDefinition entitySetDefinition) {
    val newEntitySet = createAndSaveNewListFrom(entitySetDefinition);
    materializeFileSet(newEntitySet.getId(), entitySetDefinition);
    return newEntitySet;
  }

  public EntitySet computeEntityList(@NonNull final DerivedEntitySetDefinition entitySetDefinition, boolean async) {
    val newEntitySet = createAndSaveNewListFrom(entitySetDefinition);
    if (async) {
      analyzer.combineListsAsync(newEntitySet.getId(), entitySetDefinition);
    } else {
      analyzer.combineLists(newEntitySet.getId(), entitySetDefinition);
    }
    return newEntitySet;
  }

  public void exportListItems(@NonNull EntitySet entitySet, @NonNull OutputStream outputStream) throws IOException {
    val isGeneType = BaseEntitySet.Type.GENE == entitySet.getType();
    val content =
        isGeneType ? convertToListOfListForGene(
            analyzer.retrieveGeneIdsAndSymbolsByListId(entitySet.getId())) : convertToListOfList(
                analyzer.retriveListItems(entitySet));

    @Cleanup
    val writer = new CsvListWriter(new OutputStreamWriter(outputStream), TAB_PREFERENCE);

    for (val v : content) {
      writer.write(v);
    }

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
      newEntity = entityListRepository.find(newEntityId);
      val dataVersion = newEntity.getVersion();
      entityListRepository.update(newEntity.updateStateToInProgress(), dataVersion);

      val max = entitySetDefinition.getLimit(maxNumberOfHits);
      val response = executeFilterQuery(entitySetDefinition, max);

      val entityIds = SearchResponses.getHitIds(response);
      log.debug("The result of running a FilterParam query is: '{}'", entityIds);

      val lookupType = entitySetDefinition.getType().toLookupType();
      termsLookupRepository.createTermsLookup(lookupType, newEntityId, entityIds, entitySetDefinition.isTransient());

      val count = getCountFrom(response, max);
      entityListRepository.update(newEntity.updateStateToFinished(count), dataVersion);
    } catch (Exception e) {
      log.error("Error while materializing list for {}: {}", newEntityId, e);

      if (null != newEntity) {
        entityListRepository.update(newEntity.updateStateToError(), newEntity.getVersion());
      }
    }
  }

  @SneakyThrows
  private void materializeRepositoryList(@NonNull final UUID newEntityId,
      @NonNull final EntitySetDefinition entitySet) {
    val newEntity = entityListRepository.find(newEntityId);
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
    val entityIds = repositoryFileRepository.findAllDonorIds(query, maxSetSize);

    val lookupType = entitySet.getType().toLookupType();
    termsLookupRepository.createTermsLookup(lookupType, newEntityId, entityIds, entitySet.isTransient());

    val count = entityIds.size();
    // Done - update status to finished
    entityListRepository.update(newEntity.updateStateToFinished(count), dataVersion);
  }

  private void materializeFileSet(@NonNull final UUID newEntityId,
      @NonNull final EntitySetDefinition entitySet) {
    val newEntity = entityListRepository.find(newEntityId);
    val dataVersion = newEntity.getVersion();

    val query = Query.builder()
        .filters(entitySet.getFilters())
        .sort("id")
        .order("desc")
        .size(maxNumberOfHits)
        .defaultLimit(maxNumberOfHits)
        .build();

    val entityIds = repositoryFileRepository.findAllFileIds(query);
    val lookupType = entitySet.getType().toLookupType();

    val repoList = (ArrayNode) entitySet.getFilters().path("file").path("repoName").path("is");
    termsLookupRepository.createTermsLookup(lookupType, newEntityId, entityIds, repoList.get(0).asText());

    val count = entityIds.size();
    entityListRepository.update(newEntity.updateStateToFinished(count), dataVersion);
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

    log.error("No mapping for enum value '{}' of BaseEntityList.Type.", entityType);
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

  @SneakyThrows
  private static String toFilterParamForGeneSymbols(@NonNull final String symbolList) {
    // Build the ObjectNode to represent this filterParam: { "gene": "symbol": {"is": ["s1", "s2", ...]}
    val nodeFactory = new JsonNodeFactory(false);
    val root = nodeFactory.objectNode();
    val gene = nodeFactory.objectNode();
    root.put("gene", gene);
    val symbol = nodeFactory.objectNode();
    gene.put("symbol", symbol);
    val isNode = nodeFactory.arrayNode();
    symbol.put("is", isNode);

    final String[] symbols = symbolList.split(",");
    for (val s : symbols) {
      isNode.add(s);
    }

    val result = root.toString();
    return URLEncoder.encode(result, UTF_8.name());
  }

}
