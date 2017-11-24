package org.icgc.dcc.portal.server.service;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.query.PqlParser.parse;
import static org.hibernate.validator.internal.util.CollectionHelper.newArrayList;
import static org.icgc.dcc.common.core.model.FieldNames.GENE_UNIPROT_IDS;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableMap;
import static org.icgc.dcc.portal.server.model.IndexModel.TEXT_PREFIX;
import static org.icgc.dcc.portal.server.repository.GeneRepository.GENE_ID_RESPONSE_SOURCE;
import static org.icgc.dcc.portal.server.repository.GeneRepository.GENE_ID_SEARCH_FIELDS;
import static org.icgc.dcc.portal.server.repository.GeneRepository.TEXT_PATH;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.createResponseMap;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getString;
import static org.icgc.dcc.portal.server.util.SearchResponses.getCounts;
import static org.icgc.dcc.portal.server.util.SearchResponses.getNestedCounts;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.tuple.Pair;
import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.ast.Type;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.icgc.dcc.portal.server.model.*;
import org.icgc.dcc.portal.server.pql.convert.AggregationToFacetConverter;
import org.icgc.dcc.portal.server.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.server.repository.GeneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class GeneService {

  /**
   * Constants.
   */
  private static final AggregationToFacetConverter AGGS_TO_FACETS_CONVERTER = AggregationToFacetConverter.getInstance();
  private static final Jql2PqlConverter QUERY_CONVERTER = Jql2PqlConverter.getInstance();
  private static final String INCLUDE_SCORE_FIELD = "affectedDonorCountFiltered";
  /**
   * Dependencies.
   */
  private final GeneRepository geneRepository;

  /**
   * State.
   */
  private final AtomicReference<Map<String, String>> ensemblIdGeneSymbolMap =
      new AtomicReference<Map<String, String>>();

  @Async
  public void init() {
    try {
      val watch = Stopwatch.createStarted();
      log.info("[init] Initializing EnsemblId-to-GeneSymbol lookup table...");

      // The key is a gene symbol and the value is an ensembl ID.
      val groupedByGeneSymbol = geneRepository.getGeneSymbolEnsemblIdMap();
      val lookupTable = groupedByGeneSymbol.keySet().parallelStream()
          .map(geneSymbol -> ensemblAliasPairs(geneSymbol, groupedByGeneSymbol))
          .flatMap(Collection::stream)
          .collect(toImmutableMap(Pair::getKey, Pair::getValue));

      log.debug("[init] EnsemblId-to-GeneSymbol lookup table ({} entries) is: {}", lookupTable.size(), lookupTable);

      ensemblIdGeneSymbolMap.set(lookupTable);

      log.info("[init] Finished initializing EnsemblId-to-GeneSymbol lookup table in {}", watch);
    } catch (Exception e) {
      log.error("[init] Error intializing EnsemblId-to-GeneSymbol lookup table: {}", e.getMessage());
    }
  }

  /**
   * Check whether <strong>ids</strong> match any gene identifiers.
   * 
   * The output is doubly grouped/pivoted. The first level is by the search type, i.e., symbol, id. The second level is
   * by the input id. In most cases the input id is one-to-one with a specific gene, but there are exceptions.
   * 
   * Note here we assume the <strong>ids</strong> are in lower cases <br>
   * <br>
   * 
   * Example repository response: <br>
   * <ul>
   * <li>hit1
   * <ul>
   * <li>fields: [...]</li>
   * <li>highlight: [...]</li>
   * </ul>
   * </li>
   * <li>hit2
   * <ul>
   * <li>fields: [...]</li>
   * <li>highlight: [...]</li>
   * </ul>
   * </li>
   * 
   * </ul>
   * 
   * Example output:<br>
   * <ul>
   * <li>symbol:
   * <ul>
   * <li>id1: [G1]</li>
   * <li>id2: [G2]</li>
   * </ul>
   * </li>
   * <li>uniprot:
   * <ul>
   * <li>id3: [G3, G4]</li>
   * <li>id:4: [G5]</li>
   * </ul>
   * </li>
   * </ul>
   */
  public Map<String, Multimap<String, Gene>> validateIdentifiers(List<String> ids) {
    val response = geneRepository.validateIdentifiers(ids);

    val result = Maps.<String, Multimap<String, Gene>> newHashMap();
    for (val search : GENE_ID_SEARCH_FIELDS.values()) {
      val typeResult = ArrayListMultimap.<String, Gene> create();
      result.put(search, typeResult);
    }

    // Organize the results into the categories
    // Note: it may be possible that a uniprot id can be matched to multiple genes
    for (val hit : response.getHits()) {
      val source = hit.getSource();
      val highlightedFields = hit.getHighlightFields();
      val matchedGene = geneText2Gene(hit);

      // Check which search field got the "hit"
      for (val searchField : GENE_ID_SEARCH_FIELDS.entrySet()) {

        if (highlightedFields.containsKey(TEXT_PREFIX + searchField.getKey())) {

          val field = searchField.getValue();

          // Note: it is possible that a gene hit has multiple uniprot ids (TAF9, FAU, to name a few)
          // Because we need to group by the inpu, we need to figure out which one of the uniprot ids
          // was in the input identifiers - this requires us to normalize to lower case to make the comparisons
          if (field.equals(GENE_UNIPROT_IDS)) {
            val cleanedKey = searchField.getKey().substring(0, searchField.getKey().lastIndexOf(".search"));
            val textSource = source.get(TEXT_PATH);

            if (textSource instanceof Map<?, ?>) {
              val keys = ((Map<?, ?>) textSource).get(cleanedKey);
              if (keys instanceof List<?>) {
                for (val key : (List<?>) keys) {
                  if (ids.contains(key.toString().toLowerCase())) {
                    result.get(field).put(getString(key), matchedGene);
                  }
                }
              }
            }

          } else {
            val keys = highlightedFields.get(TEXT_PREFIX + searchField.getKey());
            if (keys != null) {
              for (val key : keys.getFragments()) {
                result.get(field).put(key.toString(), matchedGene);
              }
            }
          }

        }
      }
    }

    return result;
  }

  @NonNull
  public Genes findAllCentric(Query query) {
    return findAllCentric(query, false);
  }

  @NonNull
  public Genes findAllCentric(Query query, boolean facetsOnly) {
    val pqlString = getPQLString(query, facetsOnly);
    val genes= findAllCentric(pqlString, query.getIncludes());

    val p = genes.getPagination();
    genes.setPagination(Pagination.of(p.getCount(),p.getTotal(),query));

    return genes;
  }

  public String getPQLString(Query query, boolean facetsOnly) {
    return facetsOnly ?
        QUERY_CONVERTER.convertCount(query, GENE_CENTRIC) :
        QUERY_CONVERTER.convert(query, GENE_CENTRIC);
  }

  public Genes findAllCentric(String pqlString, List<String> fieldsNotToFlatten) {
    log.debug("PQL of findAllCentric is: {}", pqlString);
    val pql = parse(pqlString);
    val response = geneRepository.findAllCentric(pql);
    log.debug("Response: {}", response);
    val includeScore = hasField(pql, INCLUDE_SCORE_FIELD);
    val projectIds = getProjectIds(pql);
    log.debug("ProjectIds: {}",projectIds);

    return buildGenes(response, projectIds, fieldsNotToFlatten, includeScore,
        PaginationRequest.of(pql));
  }


  boolean hasField(StatementNode pql, String field) {
    return !pql.hasSelect() || pql.getSelect().contains(field);
  }


  private List getProjectIds(StatementNode pql) {
    if (!pql.hasFilters()) {
      return Collections.emptyList();
    }

    val projectIds = newArrayList();
    for(val filter: pql.getFilters().getChildren() ) {
        if (filter.type() == Type.IN &&
            filter.toInNode().getField().equalsIgnoreCase("donor.projectId")) {
            projectIds.addAll(filter.toInNode().getValues());
          }

        if (filter.type() == Type.EQ &&
            filter.toEqNode().getField().equalsIgnoreCase("donor.projectId")) {
            projectIds.add(filter.toEqNode().getValue());
        }
    }
    return projectIds;
  }

  @NonNull
  public Genes buildGenes(SearchResponse response, List projectIds, List<String> fieldsNotToFlatten,
      boolean includeScore, PaginationRequest request) {
    log.debug("Response: {}", response);
    val hits = response.getHits();
    val list = ImmutableList.<Gene> builder();

    for (val hit : hits) {
      val fieldMap = createResponseMap(hit, fieldsNotToFlatten, EntityType.GENE);

      if (includeScore) {
        fieldMap.put("_score", hit.getScore());
      }

      fieldMap.put("projectIds", projectIds);
      list.add(new Gene(fieldMap));
    }

    val genes = new Genes(list.build());
    genes.addFacets(AGGS_TO_FACETS_CONVERTER.convert(response.getAggregations()));
    genes.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), request));

    return genes;
  }

  public long count(Query query) {
    return geneRepository.count(query);
  }

  public LinkedHashMap<String, Long> counts(LinkedHashMap<String, Query> queries) {
    MultiSearchResponse sr = geneRepository.counts(queries);

    return getCounts(queries, sr);
  }

  public LinkedHashMap<String, LinkedHashMap<String, Long>> nestedCounts(
      LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    MultiSearchResponse sr = geneRepository.nestedCounts(queries);

    return getNestedCounts(queries, sr);
  }

  public Gene findOne(String geneId, Query query) {
    return new Gene(geneRepository.findOne(geneId, query));
  }

  public List<String> getAffectedTranscripts(String geneId) {
    return geneRepository.getAffectedTranscripts(geneId);
  }

  public Map<String, String> getEnsemblIdGeneSymbolMap() {
    val result = ensemblIdGeneSymbolMap.get();

    if (null == result) {
      throw new NotAvailableException(
          "The EnsemblId-to-GeneSymbol lookup table is currently not available yet. Please retry later.");
    }

    return result;
  }

  @NonNull
  public Map<String, String> getEnsemblIdGeneSymbolMap(List<String> ensemblIds) {
    val map = getEnsemblIdGeneSymbolMap();

    // Returns a value of gene symbol if there is a match; otherwise returns the ensemblId itself.
    return ImmutableSet.copyOf(ensemblIds).stream().collect(
        toMap(identity(), ensemblId -> map.getOrDefault(ensemblId, ensemblId)));
  }

  @NonNull
  private static List<Pair<String, String>> ensemblAliasPairs(String geneSymbol,
      Multimap<String, String> groupedByGeneSymbols) {
    val oneToManyIndicator = "*";
    val ensemblIds = groupedByGeneSymbols.get(geneSymbol);
    val suffix = (ensemblIds.size() > 1) ? oneToManyIndicator : "";
    val symbol = geneSymbol + suffix;

    // The key is an ensembl ID and the value is a gene symbol.
    return ensemblIds.stream()
        .map(id -> Pair.of(id, symbol))
        .collect(toList());
  }

  /**
   * Convert result from gene-text to a gene model
   */
  private Gene geneText2Gene(SearchHit hit) {
    val fieldMap = createResponseMap(hit, Query.builder().build(), EntityType.GENE);
    Map<String, Object> geneMap = Maps.newHashMap();
    fieldMap.forEach((k, v) -> {
      // Strip text prefix as we construct map for gene POJO.
      geneMap.put(GENE_ID_RESPONSE_SOURCE.get(k.substring(TEXT_PREFIX.length())), v);
    });

    return new Gene(geneMap);
  }

}
