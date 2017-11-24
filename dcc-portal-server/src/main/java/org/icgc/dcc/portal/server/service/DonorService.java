package org.icgc.dcc.portal.server.service;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.replaceAll;
import static java.util.Collections.sort;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.range;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.dcc.portal.pql.query.PqlParser.parse;
import static org.icgc.dcc.portal.server.model.IndexModel.TEXT_PREFIX;
import static org.icgc.dcc.portal.server.repository.DonorRepository.DONOR_ID_SEARCH_FIELDS;
import static org.icgc.dcc.portal.server.repository.DonorRepository.FILE_DONOR_ID_SEARCH_FIELDS;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.createResponseMap;
import static org.icgc.dcc.portal.server.util.SearchResponses.getCounts;
import static org.icgc.dcc.portal.server.util.SearchResponses.getNestedCounts;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.*;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.meta.DonorCentricTypeModel.Fields;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHits;
import org.icgc.dcc.portal.server.model.*;
import org.icgc.dcc.portal.server.pql.convert.AggregationToFacetConverter;
import org.icgc.dcc.portal.server.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.server.repository.DonorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.supercsv.io.CsvMapWriter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class DonorService {

  private final DonorRepository donorRepository;

  private static final AggregationToFacetConverter AGGS_TO_FACETS_CONVERTER = AggregationToFacetConverter.getInstance();
  private static final Jql2PqlConverter QUERY_CONVERTER = Jql2PqlConverter.getInstance();
  private static final String INCLUDE_SCORE_STRING = "ssmAffectedGenes";
  @NonNull
  public Donors findAllCentric(Query query) {
    return findAllCentric(query, false);
  }

  @NonNull
  public Donors findAllCentric(Query query, boolean facetsOnly) {
    val pqlString = getPQL(query, facetsOnly);
    val donors =  findAllCentric(pqlString, query.getIncludes());

    val p = donors.getPagination();
    donors.setPagination(Pagination.of(p.getCount(),p.getTotal(),query));

    return donors;
  }

  @NonNull
  public Donors findAllCentric(Query query, String pqlString) {

    return findAllCentric(pqlString, query.getIncludes());
  }

  public Donors findAllCentric(String pqlString, Collection<String> includes) {
    StatementNode pql = parse(pqlString);
    log.error("PQL of findAllCentric is: {}", pqlString);
    val response = donorRepository.findAllCentric(pql);

    val includeScore = hasField(pql, INCLUDE_SCORE_STRING);
    val donors = buildDonors(response, includeScore, includes, PaginationRequest.of(pql));
    return donors;
  }


  public String getPQL(Query query, boolean facetsOnly) {
    return facetsOnly ?
        QUERY_CONVERTER.convertCount(query, DONOR_CENTRIC) :
        QUERY_CONVERTER.convert(query, DONOR_CENTRIC);
  }

  private Donors buildDonors(SearchResponse response, Query query) {
    val includeScore = hasField(query, INCLUDE_SCORE_STRING);
    return buildDonors(response, includeScore, query.getIncludes(), PaginationRequest.of(query) );
  }

  boolean hasField(Query query, String field) {
    return !query.hasFields() || query.getFields().contains(field);
  }

  boolean hasField(StatementNode pql, String field) {
    return !pql.hasSelect() || pql.getSelect().contains(field);
  }

  private Donors buildDonors(SearchResponse response, boolean includeScore,
      Collection<String> fieldsToNotFlatten, PaginationRequest request) {
    val hits = response.getHits();
    val list = ImmutableList.<Donor> builder();

    for (val hit : hits) {
      val fieldMap = createResponseMap(hit, fieldsToNotFlatten, EntityType.DONOR);

      if (includeScore) {
        fieldMap.put("_score", hit.getScore());
      }

      list.add(new Donor(fieldMap));
    }

    val donors = new Donors(list.build());
    donors.addFacets(AGGS_TO_FACETS_CONVERTER.convert(response.getAggregations()));

    donors.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), request));

    return donors;
  }

  /**
   * Matches donors based on the ids provided.
   * 
   * @param ids List of ids as strings
   * @param isForExternalfile False - donor-text, True - file-donor-text
   * @return A Map keyed on search fields from file-donor-text or donor-text with values being a multimap containing the
   * matched field as the key and the matched donor as the value.
   */
  public Map<String, Multimap<String, String>> validateIdentifiers(@NonNull List<String> ids,
      boolean isForExternalfile) {
    val result = Maps.<String, Multimap<String, String>> newHashMap();
    val fields = isForExternalfile ? FILE_DONOR_ID_SEARCH_FIELDS : DONOR_ID_SEARCH_FIELDS;

    for (val search : fields.values()) {
      val typeResult = ArrayListMultimap.<String, String> create();
      result.put(search, typeResult);
    }

    val response = donorRepository.validateIdentifiers(ids, isForExternalfile);

    for (val hit : response.getHits()) {
      val highlightedFields = hit.getHighlightFields();
      // The 'id' field in both 'donor-text' and 'file-donor-text' is the donor ID.
      val matchedDonor = hit.getId();

      for (val searchEntry : fields.entrySet()) {
        val keys = highlightedFields.get(TEXT_PREFIX + searchEntry.getKey());
        if (keys != null) {
          val field = searchEntry.getValue();

          for (val key : keys.getFragments()) {
            result.get(field).put(key.toString(), matchedDonor);
          }
        }
      }
    }

    return result;
  }



  @NonNull
  public Map<String, TermFacet> projectDonorCount(List<String> geneIds, List<Query> queries) {
    // The queries should be derived from geneIds, which means they are of the same size and order.
    val geneCount = geneIds.size();
    checkState(geneCount == queries.size(),
        "The number of gene IDs ({}) does not match the number of queries.",
        geneIds);

    val facetName = Fields.PROJECT_ID;
    val response = donorRepository.projectDonorCount(queries, facetName);
    val responseItems = response.getResponses();
    val responseItemCount = responseItems.length;

    checkState(geneCount == responseItemCount,
        "The number of gene IDs ({}) does not match the number of responses in a multi-search.",
        geneIds);

    return range(0, geneCount).boxed().collect(toMap(
        i -> geneIds.get(i),
        i -> {
          final SearchResponse item = responseItems[i].getResponse();
          return AGGS_TO_FACETS_CONVERTER.convert(item.getAggregations()).get(facetName);
        }));
  }

  public long count(Query query) {
    return donorRepository.count(query);
  }

  public LinkedHashMap<String, Long> counts(LinkedHashMap<String, Query> queries) {
    MultiSearchResponse sr = donorRepository.counts(queries);

    return getCounts(queries, sr);
  }

  public LinkedHashMap<String, LinkedHashMap<String, Long>> nestedCounts(
      LinkedHashMap<String, LinkedHashMap<String, Query>> queries) {
    MultiSearchResponse sr = donorRepository.nestedCounts(queries);

    return getNestedCounts(queries, sr);
  }

  public Donor findOne(String donorId, Query query) {
    return new Donor(donorRepository.findOne(donorId, query));
  }

  public Set<String> findIds(Query query) {
    return donorRepository.findIds(query);
  }

  public Set<String> findIds(String pql) {
    return donorRepository.findIds(pql);
  }

  public Donors getDonorAndSampleByProject(String projectId) {
    val query = new Query()
        .setSort("_id")
        .setOrder("desc");
    return buildDonors(donorRepository.getDonorSamplesByProject(projectId), query);
  }

  public List<Map<String, Object>> getSamples(List<Donor> donors) {
    val records = Lists.<Map<String, Object>> newArrayList();
    for (val donor : donors) {
      val dRecord = Maps.<String, Object> newHashMap();
      dRecord.put("icgc_donor_id", donor.getId());
      dRecord.put("submitted_donor_id", donor.getSubmittedDonorId());
      dRecord.put("project_code", donor.getProjectId());
      if (donor.getSpecimen() != null) {
        for (val specimen : donor.getSpecimen()) {
          val spRecord = Maps.<String, Object> newHashMap();
          spRecord.putAll(dRecord);
          spRecord.put("icgc_specimen_id", specimen.getId());
          spRecord.put("submitted_specimen_id", specimen.getSubmittedId());
          spRecord.put("specimen_type", specimen.getType());
          spRecord.put("specimen_type_other", specimen.getTypeOther());
          if (specimen.getSamples() != null) {
            for (val sample : specimen.getSamples()) {
              val saRecord = Maps.<String, Object> newHashMap();
              saRecord.putAll(spRecord);
              saRecord.put("icgc_sample_id", sample.getId());
              saRecord.put("submitted_sample_id", sample.getAnalyzedId());
              saRecord.put("analyzed_sample_interval", sample.getAnalyzedInterval());
              saRecord.put("study", sample.getStudy());

              val seqData = sample.getAvailableRawSequenceData();
              if (seqData != null && !seqData.isEmpty()) {
                for (val external : seqData) {
                  val eRecord = Maps.<String, Object> newHashMap();
                  eRecord.putAll(saRecord);
                  // eRecord.put("raw_sequence_repository", external.getRepository());
                  eRecord.put("repository", external.getRepository());
                  eRecord.put("sequencing_strategy", external.getLibraryStrategy());
                  eRecord.put("analysis_data_uri", external.getDataUri());
                  eRecord.put("raw_data_accession", external.getRawDataAccession());
                  records.add(eRecord);
                }
              } else
                records.add(saRecord);
            }
          } else
            records.add(spRecord);
        }
      } else
        records.add(dRecord);
    }
    return sortSamples(records);
  }

  public String sampleFilename(String projectId) {
    return format("sample.%s.%s.tsv", projectId, currentTimeMillis());
  }

  public StreamingOutput asSampleStream(final List<Map<String, Object>> samples) {
    return new StreamingOutput() {

      @Override
      public void write(OutputStream os) throws IOException, WebApplicationException {

        @Cleanup
        val writer =
            new CsvMapWriter(new BufferedWriter(new OutputStreamWriter(os, Charset.forName("UTF-8"))), TAB_PREFERENCE);

        final String[] headers =
            { "icgc_sample_id", "submitted_sample_id", "icgc_specimen_id", "submitted_specimen_id", "icgc_donor_id", "submitted_donor_id", "project_code", "specimen_type", "specimen_type_other", "analyzed_sample_interval", "repository", "sequencing_strategy", "raw_data_accession", "study" };

        // Write TSV
        writer.writeHeader(headers);
        for (val sample : samples) {
          writer.write(sample, headers);
        }

        writer.flush();
      }

    };
  }

  private List<Map<String, Object>> sortSamples(List<Map<String, Object>> records) {
    // Multi-key sort
    sort(records, new Comparator<Map<String, Object>>() {

      @Override
      public int compare(Map<String, Object> r1, Map<String, Object> r2) {
        Ordering<Comparable<?>> ordering = Ordering.natural();

        return ComparisonChain
            .start()
            // Column 1
            .compare((String) r1.get("icgc_donor_id"), (String) r2.get("icgc_donor_id"), ordering)
            // Column 2
            .compare((String) r1.get("icgc_specimen_id"), (String) r2.get("icgc_specimen_id"), ordering)
            // Column 3
            .compare((String) r1.get("icgc_sample_id"), (String) r2.get("icgc_sample_id"), ordering)
            .result();
      }
    });

    return records;
  }
}