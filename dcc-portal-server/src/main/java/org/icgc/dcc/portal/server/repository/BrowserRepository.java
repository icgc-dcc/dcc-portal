/*
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
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

import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.apache.lucene.search.join.ScoreMode.None;
import static org.elasticsearch.action.search.SearchType.QUERY_AND_FETCH;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.util.List;
import java.util.function.Consumer;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.icgc.dcc.portal.server.model.IndexType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BrowserRepository {

  /**
   * Constants
   */
  private static final String MUTATION = IndexType.MUTATION_CENTRIC.getId();
  private static final String GENE = IndexType.GENE_CENTRIC.getId();
  private static final Integer MUTATION_SIZE = 100000;
  private static final Integer GENE_SIZE = 10000;
  private static final String[] FETCH_SOURCE =
      { "_gene_id", "name", "biotype", "chromosome", "start", "end", "strand", "description" };
  private static final String[] NO_EXCLUDE = null;

  private final Client client;
  private final String indexName;

  @Autowired
  public BrowserRepository(@NonNull Client client, @NonNull @Value("#{indexName}") String indexName) {
    this.indexName = indexName;
    this.client = client;
  }

  public SearchResponse getMutation(String segmentId, Long start, Long stop, List<String> consequenceTypes,
      List<String> projectFilters, List<String> impactFilters) {
    val filter = getMutationFilter(segmentId, start, stop, consequenceTypes, projectFilters, impactFilters);

    return execute("Browser Mutation Request", (request) -> request
        .setTypes(MUTATION)
        .setSearchType(QUERY_AND_FETCH)
        .setPostFilter(filter)
        .setFetchSource(
            includes(
                "_mutation_id",
                "chromosome",
                "chromosome_start",
                "chromosome_end",
                "mutation_type",
                "mutation",
                "reference_genome_allele",
                "functional_impact_prediction_summary",
                "ssm_occurrence.project._project_id",
                "ssm_occurrence.project.project_name",
                "ssm_occurrence.project._summary._ssm_tested_donor_count",
                "transcript.gene.symbol",
                "transcript.consequence._transcript_id",
                "transcript.consequence.consequence_type",
                "transcript.consequence.aa_mutation"),
            excludes())
        .setFrom(0)
        .setSize(MUTATION_SIZE));
  }

  public SearchResponse getGene(String segmentId, Long start, Long stop, List<String> biotypes,
      boolean withTranscripts, List<String> impactFilters) {
    val filter = getGeneFilter(segmentId, start, stop, biotypes, impactFilters);

    val request = client.prepareSearch(indexName)
        .setTypes(GENE)
        .setSearchType(QUERY_AND_FETCH)
        .setPostFilter(filter)
        .setFrom(0)
        .setSize(GENE_SIZE);

    if (withTranscripts) {
      request.setFetchSource(addAll(FETCH_SOURCE, "transcripts"), NO_EXCLUDE);
    } else {
      request.setFetchSource(FETCH_SOURCE, NO_EXCLUDE);
    }

    log.info("Browser Gene Request", request);
    return request.execute().actionGet();
  }

  public SearchResponse getGeneHistogram(Long interval, String segmentId, Long start, Long stop,
      List<String> biotypes, List<String> impactFilters) {
    val query = getGeneFilter(segmentId, start, stop, biotypes, impactFilters);

    val histogramAggs = AggregationBuilders.histogram("hf")
        .field("start")
        .interval(interval);

    return execute("Browser Gene Histogram Request", (request) -> request
        .setTypes(GENE)
        .setSearchType(QUERY_AND_FETCH)
        .setQuery(query)
        .addAggregation(histogramAggs)
        .setSize(0));
  }

  public SearchResponse getMutationHistogram(Long interval, String segmentId, Long start, Long stop,
      List<String> consequenceTypes, List<String> projectFilters, List<String> impactFilters) {
    val query = getMutationFilter(segmentId, start, stop, consequenceTypes, projectFilters, impactFilters);

    val histogramAggs = AggregationBuilders.histogram("hf")
        .field("chromosome_start")
        .interval(interval);

    return execute("Browser Mutation Histogram Request", (request) -> request
        .setTypes(MUTATION)
        .setSearchType(QUERY_AND_FETCH)
        .setQuery(query)
        .addAggregation(histogramAggs)
        .setFrom(0)
        .setSize(0));
  }

  private SearchResponse execute(String message, Consumer<SearchRequestBuilder> customizer) {
    val request = client.prepareSearch(indexName);
    customizer.accept(request);

    log.info("{}: {}", message, request);
    return request.execute().actionGet();
  }

  /**
   * Builds a FilterBuilder with only the applicable filter values.
   */
  private static QueryBuilder getMutationFilter(String segmentId, Long start, Long stop, List<String> consequenceTypes,
      List<String> projectFilters, List<String> impacts) {
    val andQueryFilter = boolQuery()
        .must(termQuery("chromosome", segmentId))
        .must(rangeQuery("chromosome_start").lte(stop))
        .must(rangeQuery("chromosome_end").gte(start));

    if (impacts != null && !impacts.isEmpty()) {
      val impactFilter = getImpactFilterMutation(impacts);
      andQueryFilter.must(impactFilter);
    }

    if (consequenceTypes != null) {
      val consequenceFilter = getConsequenceFilter(consequenceTypes);
      andQueryFilter.must(consequenceFilter);
    }

    if (projectFilters != null) {
      val projectFilter = getProjectFilter(projectFilters);
      andQueryFilter.must(projectFilter);
    }

    return andQueryFilter;
  }

  /**
   * Builds a FilterBuilder with only the applicable filter values.
   */
  private static QueryBuilder getGeneFilter(String segmentId, Long start, Long stop,
      List<String> biotypes, List<String> impacts) {
    val andQueryFilter = boolQuery()
        .must(termQuery("chromosome", segmentId))
        .must(rangeQuery("start").lte(stop))
        .must(rangeQuery("end").gte(start));

    if (biotypes != null) {
      val biotypeFilter = getBiotypeFilterBuilder(biotypes);
      andQueryFilter.must(biotypeFilter);
    }

    if (impacts != null && !impacts.isEmpty()) {
      val impactFilter = getImpactFilterGene(impacts);
      andQueryFilter.must(impactFilter);
    }

    return andQueryFilter;
  }

  /**
   * Readability method to build list of biotype filters.
   */
  private static QueryBuilder getBiotypeFilterBuilder(List<String> biotypes) {
    val biotypeFilter = boolQuery();
    for (val biotype : biotypes) {
      biotypeFilter.should(termQuery("biotype", biotype));
    }

    return biotypeFilter;
  }

  /**
   * Readability method for naming literal array of inclusion paths.
   */
  private static String[] includes(String... paths) {
    return paths.length == 0 ? null : paths;
  }

  /**
   * Readability method for naming literal array of exclusion paths.
   */
  private static String[] excludes(String... paths) {
    return paths.length == 0 ? null : paths;
  }

  /**
   * Builds a FilterBuilder for consequence types
   */
  private static QueryBuilder getConsequenceFilter(List<String> consequenceTypes) {
    val consequenceFilter = boolQuery();
    for (val consequenceType : consequenceTypes) {
      consequenceFilter.should(termQuery("transcript.consequence.consequence_type", consequenceType));
    }

    return consequenceFilter;
  }

  /**
   * Builds a FilterBuilder for project names.
   */
  private static QueryBuilder getProjectFilter(List<String> projects) {
    val projectFilter = boolQuery();
    for (val project : projects) {
      projectFilter.should(termQuery("ssm_occurrence.project.project_name", project));
    }

    return projectFilter;
  }

  /**
   * Builds a QueryBuilder for Functional Impact. This filter is special as Transcripts are nested documents.
   */
  private static QueryBuilder getImpactFilterMutation(List<String> impacts) {
    val impactOrFilter = boolQuery();
    for (val impact : impacts) {
      impactOrFilter.should(termQuery("transcript.functional_impact_prediction_summary", impact));
    }

    val nested = nestedQuery("transcript", impactOrFilter, None);
    return nested;
  }

  /**
   * Builds a FilterBuilder for Functional Impact. This filter is special as Transcripts are nested documents.
   */
  private static QueryBuilder getImpactFilterGene(List<String> impacts) {
    val impactOrFilter = boolQuery();
    for (val impact : impacts) {
      impactOrFilter.should(termQuery("donor.ssm.consequence.functional_impact_prediction_summary", impact));
    }

    val nested = nestedQuery("transcript", impactOrFilter, None);
    return nested;
  }

}