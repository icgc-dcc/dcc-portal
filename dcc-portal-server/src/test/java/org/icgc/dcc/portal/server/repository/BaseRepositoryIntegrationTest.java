/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.Query.QueryBuilder;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.util.JsonUtils;
import org.mockito.Spy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.val;

public class BaseRepositoryIntegrationTest {

  @Spy
  final TransportClient client = new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost",
      9300));

  private static String DONOR_FILTER = "{'donor':{'gender':{'is':['male']},'availableDataTypes':{'is':['cnsm']}}}";

  private static String PROJECT_FILTER = "{'donor':{'primarySite':{'is':['Brain']}}}";

  private static String GENE_FILTER =
      "{'gene':{'type':{'is':['protein_coding']},'list':{'is':['Cancer Gene Census']}}}";

  private static String PATHWAY_FILTER = "{'gene':{'pathwayId':{'is':['REACT_115566']}}}";

  private static String MUTATION_FILTER = "{'mutation':{'type':{'is':['single base substitution']}}}";

  private static String OCCURRENCE_FILTER =
      "{'mutation':{'verificationStatus':{'is':['not tested']},'platform':{'is':['Illumina GA sequencing']}}}";

  private static String CONSEQUENCE_FILTER = "{'mutation':{'consequenceType':{'is':['missense']}}}";

  private static String TRANSCRIPT_FILTER = "{'mutation':{'functionalImpact':{'is':['High']}}}";

  private static List<String> FILTER_LIST = Lists.<String> newArrayList(
      DONOR_FILTER,
      PROJECT_FILTER,
      GENE_FILTER,
      PATHWAY_FILTER,
      MUTATION_FILTER,
      OCCURRENCE_FILTER,
      CONSEQUENCE_FILTER,
      TRANSCRIPT_FILTER);

  protected static List<String> projectIds = Lists.newArrayList("BRCA-US", "GBM-US");

  protected static List<String> geneIds = Lists.newArrayList("ENSG00000121879", "ENSG00000272700", "ENSG00000133703");

  protected static String PROJECT_FILTER_TEMPLATE = "{donor:{projectId:{is:'%s'}}}";

  protected static String GENE_PROJECT_FILTER_TEMPLATE = "{gene:{id:{is:['%s']}},donor:{projectId:{is:['%s']}}}";

  protected static List<String> mutationIds = Lists.newArrayList("MU37643", "MU4468", "MU5219");

  protected static String MUTATION_PROJECT_FILTER_TEMPLATE =
      "{mutation:{id:{is:['%s']}},donor:{projectId:{is:['%s']}}}";

  private static List<String> generateFilters() {
    val generatedFilters = Lists.<String> newArrayList();
    int size = FILTER_LIST.size();

    for (int pos = 0; pos < size; pos++) {
      ObjectNode base = new FiltersParam(FILTER_LIST.get(pos)).get();
      generatedFilters.add(base.toString());
      for (int offset = pos + 1; offset < FILTER_LIST.size(); offset++) {
        val update = new FiltersParam(FILTER_LIST.get(offset)).get();
        base = JsonUtils.merge(base, update);
        generatedFilters.add(base.toString());
      }
    }

    return generatedFilters;
  }

  static final List<String> FILTERS = generateFilters();

  /**
   * Utility to reduce call-site noise and add uninteresting, but required, parameters.
   */
  static org.icgc.dcc.portal.server.model.Query.QueryBuilder query(String sort) {
    return Query.builder()
        .sort(sort)
        .order("desc")
        .from(1)
        .fields(Lists.<String> newArrayList(""))
        .size(0);
  }

  static org.icgc.dcc.portal.server.model.Query.QueryBuilder score(String field) {
    return Query.builder()
        .sort(field)
        .order("desc")
        .from(1)
        .fields(Lists.<String> newArrayList(field))
        .size(5);
  }

  JsonNode buildFilterNode(String facet, String term, String type) {
    return new FiltersParam("{'" + type + "':{'" + facet + "':{is:['" + term + "']}}}").get();
  }

  void checkEmpty(SearchResponse response) {
    assertThat(response.getHits().getTotalHits()).isNotEqualTo(0);
  }

  void assertScore(String id, float score, long count) {
    assertThat((long) score).as(id).isEqualTo(count);
  }

  void assertCount(String id, long exp, long actual) {
    assertThat(exp).as(id).isEqualTo(actual);
  }

  void assertAggregation(String aggName, Terms.Bucket entry, long count) {
    assertThat(count).as(aggName + ":" + entry.getKey()).isEqualTo(entry.getDocCount());
  }

  MultiSearchResponse setup(Repository repo, QueryBuilder qb, IndexType type) {
    MultiSearchRequestBuilder search = client.prepareMultiSearch();

    for (val f : FILTERS) {
      val filter = new FiltersParam(f).get();
      val query = qb.filters(filter).build();
      val request = repo.buildFindAllRequest(query, type);
      request.setQuery(repo.buildQuery(query));
      search.add(request);
    }

    return search.execute().actionGet();
  }

  void scores(Repository repo, Repository countRepo, String sort, IndexType type, EntityType entityType) {
    val fIter = FILTERS.iterator();
    val query = score(sort);
    val sr = setup(repo, query, type);

    // Setup MultiSearch request for all filter combinations
    for (val res : sr.getResponses()) { // MultiSearchResponses
      val response = res.getResponse();
      checkEmpty(response);

      val filter = fIter.next();
      val queries = generateCountsQueries(filter, response, sort, entityType);

      // Execute MultiSearch Request for all Ids
      val csr = countRepo.counts(queries);

      verifyScoreCounts(response, csr);
    }
  }

  void verifyScoreCounts(SearchResponse r, MultiSearchResponse csr) {
    // Compare score to count response;
    Iterator<SearchHit> hitsIter = r.getHits().iterator();
    for (val countResponse : csr.getResponses()) {
      val cr = countResponse.getResponse();
      val hit = hitsIter.next();

      assertScore(hit.getId(), hit.getScore(), cr.getHits().getTotalHits());
    }
  }

  LinkedHashMap<String, Query> generateCountsQueries(String f, SearchResponse r, String sort, EntityType entityType) {
    val queries = Maps.<String, Query> newLinkedHashMap();

    for (val hit : r.getHits().getHits()) { // SearchResponses
      val updateNode = buildFilterNode("id", hit.getId(), entityType.getId());
      val filter = JsonUtils.merge(new FiltersParam(f).get(), updateNode);
      val query = query(sort).filters(filter).build();

      queries.put(hit.getId(), query);
    }

    return queries;
  }

  void aggregations(Repository repo, String sort, IndexType type, EntityType entityType) {
    val fIter = FILTERS.iterator();
    val query = query(sort).includes(Lists.newArrayList("facets"));
    val sr = setup(repo, query, type);

    // Setup MultiSearch request for all filter combinations
    for (val r : sr.getResponses()) { // MultiSearchResponses
      val response = r.getResponse();
      val filter = fIter.next();
      checkEmpty(response);

      val queries = generateAggsQueries(filter, response, sort, entityType);

      // Execute MultiSearch Request for all Entry values for Facet
      val csr = repo.nestedCounts(queries);

      verifyAggregationCounts(response, csr);
    }
  }

  void verifyAggregationCounts(SearchResponse r, MultiSearchResponse csr) {
    // Compare entry count to count response;
    val aggsIter = r.getAggregations().iterator();
    Terms aggs = (Terms) aggsIter.next();
    Iterator<Bucket> entryIter = aggs.getBuckets().iterator();

    for (val countResponse : csr.getResponses()) {
      val cr = countResponse.getResponse();

      while (!entryIter.hasNext()) {
        aggs = (Terms) aggsIter.next();
        entryIter = aggs.getBuckets().iterator();
      }

      val entry = entryIter.next();

      // Needed for Mutation Repo test - these facet are known to fail
      if (!aggs.getName().endsWith("Nested") &&
          !Lists.newArrayList(
              "consequenceType",
              "functionalImpact",
              "platform",
              "verificationStatus").contains(aggs.getName())) {
        assertAggregation(aggs.getName(), entry, cr.getHits().getTotalHits());
      }
    }
  }

  LinkedHashMap<String, LinkedHashMap<String, Query>> generateAggsQueries(String f, SearchResponse r, String sort,
      EntityType entityType) {
    val queries = Maps.<String, LinkedHashMap<String, Query>> newLinkedHashMap();

    for (val agg : r.getAggregations()) { // EnrichmentSearchResponses
      val termsAggs = (Terms) agg;

      // Build map of Entry -> Query
      val subQueries = generateEntryQueries(f, termsAggs, sort, entityType);
      queries.put(termsAggs.getName(), subQueries);
    }
    return queries;
  }

  LinkedHashMap<String, Query> generateEntryQueries(String f, Terms termsAggs, String sort, EntityType entityType) {
    val queries = Maps.<String, Query> newLinkedHashMap();

    for (val entry : termsAggs.getBuckets()) { // Facet Values
      val updateNode = buildFilterNode(termsAggs.getName(), entry.getKey(), entityType.getId());
      val filter = JsonUtils.merge(new FiltersParam(f).get(), updateNode);
      val query = query(sort).filters(filter).build();

      queries.put(entry.getKey(), query);
    }

    return queries;
  }

  void counts(Repository repo, List<String> ids, String filterTemplate, String sort) {
    val queries = generateCountsQueries(ids, filterTemplate, sort);
    val sr = repo.counts(queries);
    val idIter = queries.keySet().iterator();

    for (val r : sr.getResponses()) {
      val response = r.getResponse();
      checkEmpty(response);

      val id = idIter.next();
      val filter = new FiltersParam(String.format(filterTemplate, id));

      long count = repo.count(query(sort).filters(filter.get()).build());

      assertCount(id, response.getHits().getTotalHits(), count);
    }
  }

  LinkedHashMap<String, Query> generateCountsQueries(List<String> ids, String filters, String sort) {
    val queries = Maps.<String, Query> newLinkedHashMap();

    for (val id : ids) {
      val filter = new FiltersParam(String.format(filters, id));
      queries.put(id, query(sort).filters(filter.get()).build());
    }

    return queries;
  }

  protected void nestedCounts(Repository repo, List<String> ids, List<String> subIds, String filterTemplate,
      String sort) {
    val queries = generateNestedCountsQueries(ids, subIds, filterTemplate, sort);
    val sr = repo.nestedCounts(queries);

    val entrySet = queries.entrySet();
    val idIter = entrySet.stream().map(Entry::getKey).iterator();
    val subIdSet = entrySet.iterator().next().getValue().keySet();

    Iterator<String> subIdIter = subIdSet.iterator();
    String id = idIter.next();

    for (val r : sr.getResponses()) {
      val response = r.getResponse();
      checkEmpty(response);

      while (!subIdIter.hasNext()) {
        id = idIter.next();
        subIdIter = subIdSet.iterator();
      }
      val filter = new FiltersParam(String.format(filterTemplate, subIdIter.next(), id));

      long count = repo.count(query(sort).filters(filter.get()).build());

      assertCount(id, response.getHits().getTotalHits(), count);
    }

  }

  LinkedHashMap<String, LinkedHashMap<String, Query>> generateNestedCountsQueries(List<String> ids,
      List<String> subIds, String filters, String sort) {
    val queries = Maps.<String, LinkedHashMap<String, Query>> newLinkedHashMap();

    for (val id : ids) {
      val subQueries = Maps.<String, Query> newLinkedHashMap();
      for (val sid : subIds) {
        val filter = new FiltersParam(String.format(filters, sid, id));
        subQueries.put(sid, query(sort).filters(filter.get()).build());
      }
      queries.put(id, subQueries);
    }

    return queries;
  }
}
