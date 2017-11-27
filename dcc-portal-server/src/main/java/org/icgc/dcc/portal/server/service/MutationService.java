package org.icgc.dcc.portal.server.service;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.IntStream.range;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.query.PqlParser.parse;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.createResponseMap;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.flattenMap;
import static org.icgc.dcc.portal.server.util.SearchResponses.getCounts;
import static org.icgc.dcc.portal.server.util.SearchResponses.getNestedCounts;
import static org.icgc.dcc.portal.server.util.SearchResponses.getTotalHitCount;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.dcc.portal.pql.ast.StatementNode;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.icgc.dcc.common.core.util.stream.Collectors;
import org.icgc.dcc.portal.server.model.*;
import org.icgc.dcc.portal.server.pql.convert.AggregationToFacetConverter;
import org.icgc.dcc.portal.server.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.server.repository.MutationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class MutationService {
  private static final AggregationToFacetConverter AGGS_TO_FACETS_CONVERTER = AggregationToFacetConverter.getInstance();
  private static final Jql2PqlConverter QUERY_CONVERTER = Jql2PqlConverter.getInstance();
  private static final String INCLUDE_SCORE_FIELD = "affectedDonorCountFiltered";

  private final MutationRepository mutationRepository;

  @NonNull
  public Mutations findAllCentric(Query query) {
    return findAllCentric(query, false);
  }

  @NonNull
  public Mutations findAllCentric(Query query, boolean facetsOnly) {
    val pqlString = getPQLString(query, facetsOnly);
    val mutations = findAllCentric(pqlString, query.getIncludes());

    val p = mutations.getPagination();
    mutations.setPagination(Pagination.of(p.getCount(),p.getTotal(),query));

    return mutations;
  }

  @NonNull
  public String getPQLString(Query query, boolean facetsOnly) {
    return facetsOnly ?
        QUERY_CONVERTER.convertCount(query, MUTATION_CENTRIC) :
        QUERY_CONVERTER.convert(query, MUTATION_CENTRIC);
  }

  public Mutations findAllCentric(String pqlString, List<String> fieldsToNotFlattten) {
    log.debug("PQL of findAllCentric is: {}", pqlString);
    val pql = parse(pqlString);
    val response = mutationRepository.findAllCentric(pql);
    val includeScore = hasField(pql, INCLUDE_SCORE_FIELD);
    return buildMutations(response, fieldsToNotFlattten, includeScore, PaginationRequest.of(pql));
  }

  private Mutations buildMutations(SearchResponse response, List<String> fieldsToNotFlatten,
      boolean includeScore, PaginationRequest request) {
    val hits = response.getHits();
    val list = ImmutableList.<Mutation> builder();

    for (val hit : hits) {
      val map = createResponseMap(hit, fieldsToNotFlatten, EntityType.MUTATION);
      if (includeScore) map.put("_score", hit.getScore());
      list.add(new Mutation(map));
    }

    val mutations = new Mutations(list.build());
    mutations.addFacets(AGGS_TO_FACETS_CONVERTER.convert(response.getAggregations()));
    mutations.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), request));

    return mutations;
  }

  boolean hasField(StatementNode pql, String field) {
    return !pql.hasSelect() || pql.getSelect().contains(field);
  }

  public Mutations findMutationsByDonor(Query query, String donorId) {
    val response = mutationRepository.findMutationsByDonor(query, donorId);

    val hits = response.getHits();
    val list = ImmutableList.<Mutation> builder();

    for (val hit : hits) {
      val map = createResponseMap(hit, query, EntityType.MUTATION);
      map.put("_score", hit.getScore());
      list.add(new Mutation(map));
    }
    val mutations = new Mutations(list.build());
    mutations.addFacets(AGGS_TO_FACETS_CONVERTER.convert(response.getAggregations()));
    mutations.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));
    return mutations;
  }

  public long count(Query query) {
    return mutationRepository.count(query);
  }

  public Map<String, Long> counts(LinkedHashMap<String, Query> queries) {
    val sr = mutationRepository.counts(queries);

    return getCounts(queries, sr);
  }

  public List<Map<String, Object>> counts(@NonNull List<String> geneIds,
      LinkedHashMap<String, Query> queries,
      int maxSize,
      boolean sortDescendingly) {
    val genes = geneIds.stream()
        .filter(id -> !isNullOrEmpty(id))
        .distinct()
        .collect(toImmutableList());

    final Comparator<Map<String, Object>> comparator =
        (a, b) -> ((Long) a.get("mutationCount")).compareTo((Long) b.get("mutationCount"));
    final MultiSearchResponse.Item[] responseItems = mutationRepository.counts(queries).getResponses();

    return range(0, responseItems.length).boxed()
        .map(i -> {
          final long count = getTotalHitCount(responseItems[i].getResponse());
          return ImmutableMap.<String, Object> of("geneId", genes.get(i), "mutationCount", count);
        })
        .sorted(sortDescendingly ? comparator.reversed() : comparator)
        .limit(maxSize)
        .collect(toImmutableList());
  }

  public Map<String, LinkedHashMap<String, Long>> nestedCounts(
      LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    val sr = mutationRepository.nestedCounts(queries);

    return getNestedCounts(queries, sr);
  }

  public Mutation findOne(String mutationId, Query query) {
    return new Mutation(mutationRepository.findOne(mutationId, query));
  }

  @SuppressWarnings("unchecked")
  public Mutations protein(Query query) {
    log.info("{}", query);

    val response = mutationRepository.protein(query);
    val hits = response.getHits();

    val list = ImmutableList.<Mutation> builder();

    for (val hit : hits) {
      val map = Maps.<String, Object> newHashMap();
      val transcripts = Lists.<Map<String, Object>> newArrayList();

      val hitSource = flattenMap(hit.getSource());
      map.put("_mutation_id", hitSource.get("_mutation_id"));
      map.put("mutation", hitSource.get("mutation"));
      map.put("_summary._affected_donor_count", hitSource.get("_summary._affected_donor_count"));

      val nestedTranscripts = ((List<Map<String, Object>>) hitSource.get("transcript")).stream()
          .filter(t -> t.get("id") != null) // This is to filter out fake transcripts.
          .collect(toImmutableList());

      List<String> transcriptIds = nestedTranscripts.stream()
          .map(t -> (String) t.get("id"))
          .collect(toImmutableList());

      val predictionSummary = nestedTranscripts.stream()
          .map(t -> t.get("functional_impact_prediction_summary").toString())
          .collect(toImmutableList());

      Map<String, String> aaMutations = nestedTranscripts.stream()
          .map(t -> new SimpleImmutableEntry<String, String>(t.get("id").toString(),
              ((Map<String, Object>) t.get("consequence")).get("aa_mutation").toString()))
          .collect(Collectors.toImmutableMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue));

      for (int i = 0; i < transcriptIds.size(); ++i) {
        val id = transcriptIds.get(i);
        val aminoAcidChange = aaMutations.get(id);

        if (!aminoAcidChange.isEmpty()) {
          val transcript = Maps.<String, Object> newHashMap();
          transcript.put("id", id);
          transcript.put("functional_impact_prediction_summary", predictionSummary.get(i));

          val consequence = Maps.<String, Object> newHashMap();
          consequence.put("aa_mutation", aaMutations.get(id));
          transcript.put("consequence", consequence);

          transcripts.add(transcript);
        }
      }

      map.put("transcript", transcripts);

      list.add(new Mutation(map));
    }

    Mutations mutations = new Mutations(list.build());
    mutations.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return mutations;
  }

}
