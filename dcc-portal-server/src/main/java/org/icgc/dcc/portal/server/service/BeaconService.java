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

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.elasticsearch.action.search.SearchType.QUERY_THEN_FETCH;
import static org.icgc.dcc.common.core.model.FieldNames.MUTATION_CHROMOSOME;
import static org.icgc.dcc.common.core.model.FieldNames.MUTATION_CHROMOSOME_END;
import static org.icgc.dcc.common.core.model.FieldNames.MUTATION_CHROMOSOME_START;
import static org.icgc.dcc.common.core.model.FieldNames.MUTATION_OBSERVATION_PROJECT;
import static org.icgc.dcc.common.core.model.FieldNames.MUTATION_OCCURRENCES;
import static org.icgc.dcc.common.core.model.FieldNames.PROJECT_ID;
import lombok.val;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.icgc.dcc.portal.server.model.AlleleMutation;
import org.icgc.dcc.portal.server.model.Beacon;
import org.icgc.dcc.portal.server.model.BeaconInfo;
import org.icgc.dcc.portal.server.model.BeaconQuery;
import org.icgc.dcc.portal.server.model.BeaconResponse;
import org.icgc.dcc.portal.server.model.IndexType;
import org.icgc.dcc.portal.server.resource.tool.BeaconResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;

/**
 * The Beacon searches the dataset for mutation in a given position and chromosome. The result is null if no mutations
 * are found at the given position, false if the mutation doesn't match the expected allele and true otherwise. Also see
 * {@link BeaconResource}.
 * 
 * <p>
 * <a href="https://docs.google.com/document/d/154GBOixuZxpoPykGKcPOyrYUcgEXVe2NvKx61P4Ybn4/edit?usp=sharing">Draft of
 * v0.2 API </a>
 * </p>
 * 
 * <p>
 * <a href="https://github.com/ga4gh/beacon-team">GA4GH's Beacon github home </a>
 * </p>
 * 
 */
@Service
public class BeaconService {

  private static final int POSITION_BUFFER = 1000; // Must be larger than any single mutation.

  private final Client client;
  private final String index;

  @Autowired
  public BeaconService(Client client, @Value("#{indexName}") String index) {
    this.index = index;
    this.client = client;
  }

  public Beacon query(String chromosome, int position, String reference, AlleleMutation alleleMutation, String dataset) {
    String allele = alleleMutation.getMutation();

    val search = client.prepareSearch(index)
        .setTypes(IndexType.MUTATION_CENTRIC.getId())
        .setSearchType(QUERY_THEN_FETCH);

    val boolQuery = QueryBuilders.boolQuery();
    boolQuery.must(QueryBuilders.rangeQuery(MUTATION_CHROMOSOME_START).gte(position - POSITION_BUFFER)
        .lte(position));
    boolQuery.must(QueryBuilders.rangeQuery(MUTATION_CHROMOSOME_END).lte(position + POSITION_BUFFER));
    boolQuery.must(QueryBuilders.termQuery(MUTATION_CHROMOSOME, chromosome));
    if (!isNullOrEmpty(dataset)) boolQuery
        .must(QueryBuilders.nestedQuery(MUTATION_OCCURRENCES,
            QueryBuilders.termQuery(MUTATION_OCCURRENCES + '.' + MUTATION_OBSERVATION_PROJECT + '.' + PROJECT_ID,
                dataset)));
    search.setQuery(boolQuery);

    val params = new ImmutableMap.Builder<String, Object>()
        .put("allelelength", allele.length())
        .put("allele", allele)
        .put("position", position).build();

    search.addScriptField("result",
        allele.contains(">") ? generateInsertionOrDeletionScriptField() : generateDefaultScriptField(), params);

    val filter = FilterBuilders.scriptFilter(
        "m = doc['mutation'].value;"
            + "length = m.substring(m.indexOf('>')+1,m.length()).length();"
            + "position <= doc['chromosome_start'].value+length"
        ).addParam("position", position);
    search.setPostFilter(filter);

    val hits = search.execute().actionGet().getHits();
    String finalResult = "null";

    for (val hit : hits) {
      Boolean result = hit.field("result").getValue();
      if (result) {
        finalResult = "true";
        break;
      } else if (!result) {
        finalResult = "false";
      }
    }

    return createBeaconResponse(finalResult, chromosome, position, reference, allele, dataset);
  }

  private String generateDefaultScriptField() {
    return "m = doc['mutation'].value;"
        + "offset = position - doc['chromosome_start'].value;"
        + "int begin = m.indexOf('>') + 1 + offset;"
        + "int end = Math.min(begin + allelelength, m.length());"
        + "m = m.substring(begin,end);"
        + "m==allele";
  }

  private String generateInsertionOrDeletionScriptField() {
    return "doc['mutation'].value == allele";
  }

  private Beacon createBeaconResponse(String exists, String chromosome, int position, String reference, String allele,
      String dataset) {
    val queryResp = new BeaconQuery(allele, chromosome, position, reference, dataset);
    val respResp = new BeaconResponse(exists);
    return new Beacon((new BeaconInfo()).getId(), queryResp, respResp);
  }

}
