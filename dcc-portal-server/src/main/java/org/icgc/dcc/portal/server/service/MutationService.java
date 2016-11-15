package org.icgc.dcc.portal.server.service;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.IntStream.range;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.query.PqlParser.parse;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.createResponseMap;
import static org.icgc.dcc.portal.server.util.SearchResponses.getCounts;
import static org.icgc.dcc.portal.server.util.SearchResponses.getNestedCounts;
import static org.icgc.dcc.portal.server.util.SearchResponses.getTotalHitCount;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.MultiSearchResponse;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.Mutation;
import org.icgc.dcc.portal.server.model.Mutations;
import org.icgc.dcc.portal.server.model.Pagination;
import org.icgc.dcc.portal.server.model.Query;
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

  private final MutationRepository mutationRepository;

  @NonNull
  public Mutations findAllCentric(Query query) {
    return findAllCentric(query, false);
  }

  @NonNull
  public Mutations findAllCentric(Query query, boolean facetsOnly) {
    val pql =
        facetsOnly ? QUERY_CONVERTER.convertCount(query, MUTATION_CENTRIC) : QUERY_CONVERTER.convert(query,
            MUTATION_CENTRIC);
    log.debug("PQL of findAllCentric is: {}", pql);

    val pqlAst = parse(pql);
    val response = mutationRepository.findAllCentric(pqlAst);

    val hits = response.getHits();

    // Include _score if either: no custom fields or custom fields include affectedDonorCountFiltered
    val includeScore = !query.hasFields() || query.getFields().contains("affectedDonorCountFiltered");

    val list = ImmutableList.<Mutation> builder();

    for (val hit : hits) {
      val map = createResponseMap(hit, query, EntityType.MUTATION);
      if (includeScore) map.put("_score", hit.getScore());
      list.add(new Mutation(map));
    }

    val mutations = new Mutations(list.build());
    mutations.addFacets(AGGS_TO_FACETS_CONVERTER.convert(response.getAggregations()));
    mutations.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return mutations;
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

  public Mutations protein(Query query) {
    log.info("{}", query);

    val response = mutationRepository.protein(query);
    val hits = response.getHits();

    val list = ImmutableList.<Mutation> builder();

    for (val hit : hits) {
      val map = Maps.<String, Object> newHashMap();
      val transcripts = Lists.<Map<String, Object>> newArrayList();

      map.put("_mutation_id", hit.getFields().get("_mutation_id").getValue());
      map.put("mutation", hit.getFields().get("mutation").getValue());
      map.put("_summary._affected_donor_count", hit.getFields().get("_summary._affected_donor_count").getValue());
      /*
       * map.put("functional_impact_prediction_summary", hit.getFields().get("functional_impact_prediction_summary")
       * .getValues());
       */

      List<Object> transcriptIds = hit.getFields().get("transcript.id").getValues();
      val predictionSummary = hit.getFields().get("transcript.functional_impact_prediction_summary").getValues();

      for (int i = 0; i < transcriptIds.size(); ++i) {
        val transcript = Maps.<String, Object> newHashMap();

        transcript.put("id", transcriptIds.get(i));
        transcript.put("functional_impact_prediction_summary", predictionSummary.get(i));

        val consequence = Maps.<String, Object> newHashMap();
        List<Object> aminoAcidChange = hit.getFields().get("transcript.consequence.aa_mutation").getValues();
        val mutationString = aminoAcidChange.get(i).toString();

        // Only add to results if not empty.
        if (!mutationString.isEmpty()) {
          consequence.put("aa_mutation", mutationString);
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
