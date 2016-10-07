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

import static com.google.common.collect.Maps.toMap;
import static java.lang.String.format;
import static org.dcc.portal.pql.meta.Type.OBSERVATION_CENTRIC;
import static org.elasticsearch.action.search.SearchType.COUNT;
import static org.elasticsearch.action.search.SearchType.SCAN;
import static org.icgc.dcc.common.core.model.ConsequenceType.CODING_SEQUENCE_VARIANT;
import static org.icgc.dcc.common.core.model.ConsequenceType.DISRUPTIVE_INFRAME_DELETION;
import static org.icgc.dcc.common.core.model.ConsequenceType.DISRUPTIVE_INFRAME_INSERTION;
import static org.icgc.dcc.common.core.model.ConsequenceType.EXON_LOSS_VARIANT;
import static org.icgc.dcc.common.core.model.ConsequenceType.EXON_VARIANT;
import static org.icgc.dcc.common.core.model.ConsequenceType.FRAMESHIFT_VARIANT;
import static org.icgc.dcc.common.core.model.ConsequenceType.INFRAME_DELETION;
import static org.icgc.dcc.common.core.model.ConsequenceType.INFRAME_INSERTION;
import static org.icgc.dcc.common.core.model.ConsequenceType.INITIATOR_CODON_VARIANT;
import static org.icgc.dcc.common.core.model.ConsequenceType.MISSENSE_VARIANT;
import static org.icgc.dcc.common.core.model.ConsequenceType.NON_CANONICAL_START_CODON;
import static org.icgc.dcc.common.core.model.ConsequenceType.RARE_AMINO_ACID_VARIANT;
import static org.icgc.dcc.common.core.model.ConsequenceType.SPLICE_ACCEPTOR_VARIANT;
import static org.icgc.dcc.common.core.model.ConsequenceType.SPLICE_DONOR_VARIANT;
import static org.icgc.dcc.common.core.model.ConsequenceType.SPLICE_REGION_VARIANT;
import static org.icgc.dcc.common.core.model.ConsequenceType.START_LOST;
import static org.icgc.dcc.common.core.model.ConsequenceType.STOP_GAINED;
import static org.icgc.dcc.common.core.model.ConsequenceType.STOP_LOST;
import static org.icgc.dcc.common.core.model.ConsequenceType.STOP_RETAINED_VARIANT;
import static org.icgc.dcc.common.core.model.ConsequenceType.SYNONYMOUS_VARIANT;
import static org.icgc.dcc.common.core.model.ConsequenceType._3_PRIME_UTR_TRUNCATION;
import static org.icgc.dcc.common.core.model.ConsequenceType._3_PRIME_UTR_VARIANT;
import static org.icgc.dcc.common.core.model.ConsequenceType._5_PRIME_UTR_PREMATURE_START_CODON_GAIN_VARIANT;
import static org.icgc.dcc.common.core.model.ConsequenceType._5_PRIME_UTR_TRUNCATION;
import static org.icgc.dcc.common.core.model.ConsequenceType._5_PRIME_UTR_VARIANT;
import static org.icgc.dcc.common.core.util.Separators.COMMA;
import static org.icgc.dcc.common.core.util.Strings2.DOUBLE_QUOTE;
import static org.icgc.dcc.portal.server.model.IndexModel.getFields;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.checkResponseState;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.createResponseMap;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getString;
import static org.icgc.dcc.portal.server.util.SearchResponses.getTotalHitCount;
import static org.icgc.dcc.portal.server.util.SearchResponses.hasHits;

import java.util.Map;
import java.util.Map.Entry;

import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHitField;
import org.icgc.dcc.common.core.model.ConsequenceType;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.pql.convert.Jql2PqlConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OccurrenceRepository {

  private static final IndexType CENTRIC_TYPE = IndexType.OCCURRENCE_CENTRIC;
  private final static TimeValue KEEP_ALIVE = new TimeValue(10000);

  private static final Jql2PqlConverter PQL_CONVERTER = Jql2PqlConverter.getInstance();

  static final ImmutableMap<EntityType, String> PREFIX_MAPPING = Maps.immutableEnumMap(ImmutableMap
      .<EntityType, String> builder()
      .put(EntityType.PROJECT, "project")
      .put(EntityType.DONOR, "donor")
      .put(EntityType.MUTATION, "ssm")
      .put(EntityType.CONSEQUENCE, "ssm.consequence")
      .put(EntityType.GENE, "ssm.consequence.gene")
      .put(EntityType.GENE_SET, "ssm.consequence.gene")
      .put(EntityType.OBSERVATION, "ssm.observation")
      .build());

  private static final Joiner COMMA_JOINER = Joiner.on(COMMA).skipNulls();
  private static final String PQL_ALIAS_CONSEQUENCE_DONOR_ID = "donor.id";
  private static final String PQL_ALIAS_CONSEQUENCE_PROJECT_ID = "donor.projectId";
  /*
   * This is a mapping from a PQL field alias to a raw fully-qualified ElasticSearch field name.
   */
  private static final Map<String, String> FIELD_MAP = ImmutableMap.of(
      PQL_ALIAS_CONSEQUENCE_DONOR_ID, "donor._donor_id",
      PQL_ALIAS_CONSEQUENCE_PROJECT_ID, "project._project_id");

  private final Client client;
  private final String indexName;
  private final QueryEngine queryEngine;

  @Autowired
  public OccurrenceRepository(Client client, @Value("#{indexName}") String indexName) {
    this.indexName = indexName;
    this.client = client;
    this.queryEngine = new QueryEngine(client, indexName);
  }

  SearchRequestBuilder buildFindAllRequest(Query query) {
    val pql = PQL_CONVERTER.convert(query, OBSERVATION_CENTRIC);
    log.debug("JQL filter is: '{}'; PQL is: '{}'.", query.getFilters(), pql);

    val request = queryEngine.execute(pql, OBSERVATION_CENTRIC)
        .getRequestBuilder();
    log.debug("Request: {}", request);

    return request;
  }

  public SearchResponse findAll(Query query) {
    val request = buildFindAllRequest(query);
    val response = request.execute().actionGet();
    log.debug("Response: {}", response);

    return response;
  }

  public long count(Query query) {
    val pql = PQL_CONVERTER.convertCount(query, OBSERVATION_CENTRIC);
    log.debug("JQL filter is: '{}'; PQL is: '{}'.", query.getFilters(), pql);

    val request = queryEngine.execute(pql, OBSERVATION_CENTRIC)
        .getRequestBuilder()
        .setSearchType(COUNT);
    log.debug("Count query is: '{}'.", request);

    val response = request.execute().actionGet();
    log.debug("Count response is: '{}'.", response);

    return getTotalHitCount(response);
  }

  public Map<String, Object> findOne(String id, Query query) {
    val search = client.prepareGet(indexName, CENTRIC_TYPE.getId(), id);
    search.setFields(getFields(query, EntityType.OCCURRENCE));

    val response = search.execute().actionGet();
    checkResponseState(id, response, EntityType.OCCURRENCE);

    val map = createResponseMap(response, query, EntityType.OCCURRENCE);
    log.debug("{}", map);

    return map;
  }

  public Map<String, Map<String, Integer>> getProjectDonorMutationDistribution() {
    // See DCC-2612
    val consequenceList = Lists.transform(Lists.<ConsequenceType> newArrayList(
        _3_PRIME_UTR_TRUNCATION,
        _3_PRIME_UTR_VARIANT,
        _5_PRIME_UTR_PREMATURE_START_CODON_GAIN_VARIANT,
        _5_PRIME_UTR_TRUNCATION,
        _5_PRIME_UTR_VARIANT,
        CODING_SEQUENCE_VARIANT,
        DISRUPTIVE_INFRAME_DELETION,
        DISRUPTIVE_INFRAME_INSERTION,
        EXON_LOSS_VARIANT,
        EXON_VARIANT,
        FRAMESHIFT_VARIANT,
        INFRAME_DELETION,
        INFRAME_INSERTION,
        INITIATOR_CODON_VARIANT,
        MISSENSE_VARIANT,
        RARE_AMINO_ACID_VARIANT,
        SPLICE_ACCEPTOR_VARIANT,
        SPLICE_DONOR_VARIANT,
        SPLICE_REGION_VARIANT,
        START_LOST,
        STOP_GAINED,
        STOP_LOST,
        STOP_RETAINED_VARIANT,
        SYNONYMOUS_VARIANT,
        NON_CANONICAL_START_CODON), type -> DOUBLE_QUOTE + type.toString() + DOUBLE_QUOTE);

    val searchSize = 5000;
    val selects = FIELD_MAP.keySet();
    val pql = format("select (%s), in (mutation.consequenceType, %s)",
        COMMA_JOINER.join(selects),
        COMMA_JOINER.join(consequenceList));

    val search = queryEngine.execute(pql, OBSERVATION_CENTRIC).getRequestBuilder()
        .setSearchType(SCAN)
        .setSize(searchSize)
        .setScroll(KEEP_ALIVE);
    log.debug("ES search is: '{}'.", search);

    SearchResponse response = search.execute().actionGet();
    val result = Maps.<String, Map<String, Integer>> newHashMap();

    while (true) {
      response = client.prepareSearchScroll(response.getScrollId())
          .setScroll(KEEP_ALIVE)
          .execute().actionGet();

      for (val hit : response.getHits()) {
        val fields = hit.getFields();
        val projectId = getStringByAlias(fields, PQL_ALIAS_CONSEQUENCE_PROJECT_ID);
        val donorId = getStringByAlias(fields, PQL_ALIAS_CONSEQUENCE_DONOR_ID);
        val project = result.getOrDefault(projectId, Maps.<String, Integer> newHashMap());
        val donorCount = project.getOrDefault(donorId, 0);

        project.put(donorId, donorCount + 1);
        result.put(projectId, project);
      }

      val finished = !hasHits(response);

      if (finished) {
        break;
      }
    }

    // Print out some useful info now
    if (log.isDebugEnabled()) {
      val counts = toMap(result.keySet(), projectId -> result.get(projectId).size())
          .entrySet();

      counts.stream().forEach(entry -> log.debug("Project: {} => Donor Count: {}", entry.getKey(), entry.getValue()));

      val totalDonors = counts.stream()
          .mapToLong(Entry::getValue)
          .sum();
      log.debug("Total donor count: {} ", totalDonors);
    }

    return result;
  }

  @NonNull
  private static String getStringByAlias(Map<String, SearchHitField> fields, String fieldAlias) {
    return getString(fields.get(FIELD_MAP.get(fieldAlias)));
  }

}
