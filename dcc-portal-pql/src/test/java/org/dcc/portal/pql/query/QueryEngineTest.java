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
package org.dcc.portal.pql.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.limit;
import static org.dcc.portal.pql.utils.Tests.initQueryContext;

import org.dcc.portal.pql.ast.StatementNode;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;

import lombok.SneakyThrows;
import lombok.val;

public class QueryEngineTest {

  /**
   * Dependencies
   */
  private static ObjectMapper MAPPER = new ObjectMapper();
  private Client client = new TransportClient();
  private QueryContext context = initQueryContext();
  private QueryEngine queryEngine = new QueryEngine(client, context.getIndex());

  @Test
  public void typesTest() {
    val query = "select(id, gender)";
    val request = executeQuery(query).request();
    assertThat(request.types()).isEqualTo(new Object[] { "donor-centric" });
  }

  @Test
  public void testSimpleSource() {
    val query = "in(gender, 'male', 'female')";
    val request = executeQuery(query);
    val sourceTree = getSource(request);
    val mustNode = sourceTree.path("query").path("bool").path("must");
    assertThat(mustNode).isNotExactlyInstanceOf(MissingNode.class);

    val termsNode = mustNode.get(0).path("filtered").path("filter").path("terms");
    assertThat(termsNode).isNotExactlyInstanceOf(MissingNode.class);
    assertThat(termsNode.path("donor_sex").get(0).asText()).isEqualToIgnoringCase("male");
    assertThat(termsNode.path("donor_sex").get(1).asText()).isEqualToIgnoringCase("female");
  }

  @Test
  public void testSourceFromPqlAst() {
    val query = "in(gender, 'male', 'female')";
    StatementNode pqlAst = PqlParser.parse(query);

    val requestNoLimit = queryEngine.execute(pqlAst, context.getType()).getRequestBuilder();
    val sourceTree = getSource(requestNoLimit);
    assertThat(sourceTree.path("from")).isExactlyInstanceOf(MissingNode.class);
    assertThat(sourceTree.path("size")).isExactlyInstanceOf(MissingNode.class);

    pqlAst.setLimit(limit(100));
    val requestWithLimit = queryEngine.execute(pqlAst, context.getType()).getRequestBuilder();
    val limitSourceTree = getSource(requestWithLimit);
    assertThat(limitSourceTree.path("from").asText()).isEqualTo("0");
    assertThat(limitSourceTree.path("size").asText()).isEqualTo("100");

    val mustNode = limitSourceTree.path("query").path("bool").path("must");
    assertThat(mustNode).isNotExactlyInstanceOf(MissingNode.class);

    val termsNode = mustNode.get(0).path("filtered").path("filter").path("terms");
    assertThat(termsNode).isNotExactlyInstanceOf(MissingNode.class);
    assertThat(termsNode.path("donor_sex").get(0).asText()).isEqualToIgnoringCase("male");
    assertThat(termsNode.path("donor_sex").get(1).asText()).isEqualToIgnoringCase("female");
  }

  @Test
  public void testSource() {
    val query = "or(gt(ageAtDiagnosis, 10), ge(ageAtEnrollment, 20.2)), "
        + "eq(diseaseStatusLastFollowup, 100), ne(relapseInterval, 200), "
        + "and(lt(ageAtLastFollowup, 30), le(intervalOfLastFollowup, 40)), in(gender, 'male', 'female')";
    val request = executeQuery(query);
    val sourceTree = getSource(request);
    val mustNode = sourceTree.path("query").path("bool").path("must").get(0);
    assertThat(mustNode).isNotExactlyInstanceOf(MissingNode.class);

    val filteredMust = mustNode.path("filtered").path("filter").path("bool").path("must");

    val shouldNode = filteredMust.get(0).path("bool").path("should");
    assertThat(shouldNode).isNotExactlyInstanceOf(MissingNode.class);

    val diagNode = ((ArrayNode) shouldNode).get(0).path("range").path("donor_age_at_diagnosis");
    assertThat(diagNode).isNotExactlyInstanceOf(MissingNode.class);
    assertThat(diagNode.path("from").asText()).isEqualTo("10");

    val enrollNode = ((ArrayNode) shouldNode).get(1).path("range").path("donor_age_at_enrollment");
    assertThat(enrollNode).isNotExactlyInstanceOf(MissingNode.class);
    assertThat(enrollNode.path("from").asText()).isEqualTo("20.2");

    val termNode = filteredMust.get(1).path("term");
    assertThat(termNode).isNotExactlyInstanceOf(MissingNode.class);
    assertThat(termNode.path("disease_status_last_followup").asText()).isEqualTo("100");

    val notNode = filteredMust.get(2).path("not");
    assertThat(notNode).isNotExactlyInstanceOf(MissingNode.class);
    assertThat(notNode.path("filter").path("term").path("donor_relapse_interval").asText()).isEqualTo("200");

    val boolMust = filteredMust.get(3).path("bool").path("must");
    assertThat(boolMust).isNotExactlyInstanceOf(MissingNode.class);

    val ageFollowUpNode = boolMust.get(0).path("range").path("donor_age_at_last_followup");
    assertThat(ageFollowUpNode).isNotExactlyInstanceOf(MissingNode.class);
    assertThat(ageFollowUpNode.path("to").asText()).isEqualTo("30");

    val intervalFollowUpNode = boolMust.get(1).path("range").path("donor_interval_of_last_followup");
    assertThat(intervalFollowUpNode).isNotExactlyInstanceOf(MissingNode.class);
    assertThat(intervalFollowUpNode.path("to").asText()).isEqualTo("40");

    val termsNode = filteredMust.get(4).path("terms");
    assertThat(termsNode).isNotExactlyInstanceOf(MissingNode.class);
    assertThat(termsNode.path("donor_sex").get(0).asText()).isEqualTo("male");
    assertThat(termsNode.path("donor_sex").get(1).asText()).isEqualTo("female");
  }

  private SearchRequestBuilder executeQuery(String query) {
    return queryEngine.execute(query, context.getType()).getRequestBuilder();
  }

  @SneakyThrows
  private static JsonNode getSource(SearchRequestBuilder request) {
    return MAPPER.readTree(request.toString());
  }

}
