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
package org.dcc.portal.pql.es.visitor.special;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.dcc.portal.pql.meta.Type.FILE;
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.utils.Tests.createEsAst;

import java.util.Optional;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.filter.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.es.model.LookupInfo;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.query.QueryContext;
import org.junit.Test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EntitySetVisitorTest {

  private static final String ID_VALUE = "6d66b2bd-daed-431e-9a8d-b1d99be0bc18";

  EntitySetVisitor visitor = new EntitySetVisitor();
  QueryContext queryContext;

  @Test
  public void noFilterTest() {
    val root = createEsAst("select(id)", DONOR_CENTRIC);
    val rootClone = Nodes.cloneNode(root);
    val result = rootClone.accept(visitor, Optional.empty());
    assertThat(result.get()).isEqualTo(root);
  }

  @Test
  public void donorEntitySetTest_donor() {
    val result = prepareResult("in(donor.id, 'ES: 6d66b2bd-daed-431e-9a8d-b1d99be0bc18', 'DO1', "
        + "'ES: 6d66b2bd-daed-431e-9a8d-b1d99be0bc19')", DONOR_CENTRIC);
    val shouldNode = getShouldNode(result);
    val children = shouldNode.getChildren();

    assertThat(children).hasSize(3);
    val nonIdentifiableChild = (TermsNode) children.get(0);
    assertThat(nonIdentifiableChild.getField()).isEqualTo("_donor_id");
    assertThat(nonIdentifiableChild.getChildren()).hasSize(1);
    assertThat(((TerminalNode) nonIdentifiableChild.getFirstChild()).getValue()).isEqualTo("DO1");

    assertIdentifiableChild(children.get(1), "6d66b2bd-daed-431e-9a8d-b1d99be0bc18");
    assertIdentifiableChild(children.get(2), "6d66b2bd-daed-431e-9a8d-b1d99be0bc19");
  }

  @Test
  public void donorEntitySetTest_file() {
    val result = prepareResult("in(donor.id, 'ES: 6d66b2bd-daed-431e-9a8d-b1d99be0bc18', 'DO1', "
        + "'ES: 6d66b2bd-daed-431e-9a8d-b1d99be0bc19')", FILE);
    val shouldNode = getShouldNode(result);
    val children = shouldNode.getChildren();

    assertThat(children).hasSize(3);
    val nonIdentifiableChild = (TermsNode) children.get(0);
    assertThat(nonIdentifiableChild.getField()).isEqualTo("donors.donor_id");
    assertThat(nonIdentifiableChild.getChildren()).hasSize(1);
    assertThat(((TerminalNode) nonIdentifiableChild.getFirstChild()).getValue()).isEqualTo("DO1");

    assertIdentifiableChildFile(children.get(1), "6d66b2bd-daed-431e-9a8d-b1d99be0bc18");
    assertIdentifiableChildFile(children.get(2), "6d66b2bd-daed-431e-9a8d-b1d99be0bc19");
  }

  @Test
  public void mutationIdentifiableTermTest_mutation() {
    val result = prepareResult(getQuery("mutation"), MUTATION_CENTRIC);
    val termNode = getTermNode(result);

    assertThat(termNode.getField()).isEqualTo("_mutation_id");
    assertLookupInfo(termNode.getLookup(), "mutation-ids");
  }

  @Test
  public void donorIdentifiableTermTest_mutation() {
    val result = prepareResult(getQuery("donor"), MUTATION_CENTRIC);
    val termNode = getTermNode(result);

    assertThat(termNode.getField()).isEqualTo("ssm_occurrence.donor._donor_id");
    assertLookupInfo(termNode.getLookup(), "donor-ids");
  }

  @Test
  public void geneIdentifiableTermTest_mutation() {
    val result = prepareResult(getQuery("gene"), MUTATION_CENTRIC);
    val termNode = getTermNode(result);

    assertThat(termNode.getField()).isEqualTo("transcript.gene._gene_id");
    assertLookupInfo(termNode.getLookup(), "gene-ids");
  }

  @Test
  public void mutationIdentifiableTermTest_donor() {
    val result = prepareResult(getQuery("mutation"), DONOR_CENTRIC);
    val termNode = getTermNode(result);

    assertThat(termNode.getField()).isEqualTo("gene.ssm._mutation_id");
    assertLookupInfo(termNode.getLookup(), "mutation-ids");
  }

  @Test
  public void donorIdentifiableTermTest_donor() {
    val result = prepareResult(getQuery("donor"), DONOR_CENTRIC);
    val termNode = getTermNode(result);

    assertThat(termNode.getField()).isEqualTo("_donor_id");
    assertLookupInfo(termNode.getLookup(), "donor-ids");
  }

  @Test
  public void geneIdentifiableTermTest_donor() {
    val result = prepareResult(getQuery("gene"), DONOR_CENTRIC);
    val termNode = getTermNode(result);

    assertThat(termNode.getField()).isEqualTo("gene._gene_id");
    assertLookupInfo(termNode.getLookup(), "gene-ids");
  }

  @Test
  public void mutationIdentifiableTermTest_gene() {
    val result = prepareResult(getQuery("mutation"), GENE_CENTRIC);
    val termNode = getTermNode(result);

    assertThat(termNode.getField()).isEqualTo("donor.ssm._mutation_id");
    assertLookupInfo(termNode.getLookup(), "mutation-ids");
  }

  @Test
  public void donorIdentifiableTermTest_gene() {
    val result = prepareResult(getQuery("donor"), GENE_CENTRIC);
    val termNode = getTermNode(result);

    assertThat(termNode.getField()).isEqualTo("donor._donor_id");
    assertLookupInfo(termNode.getLookup(), "donor-ids");
  }

  @Test
  public void geneIdentifiableTermTest_gene() {
    val result = prepareResult(getQuery("gene"), GENE_CENTRIC);
    val termNode = getTermNode(result);

    assertThat(termNode.getField()).isEqualTo("_gene_id");
    assertLookupInfo(termNode.getLookup(), "gene-ids");
  }

  @Test
  public void fileRepoIdentifiableTermTest() {
    val result = prepareResult(getQuery("file"), FILE);
    val termNode = getTermNode(result);

    assertThat(termNode.getField()).isEqualTo("id");
    assertLookupInfo(termNode.getLookup(), "file-ids");
  }

  @Test
  public void fileRepoDonorIdentifiableTermTest() {
    val result = prepareResult(getQuery("donor"), FILE);
    val termNode = getTermNode(result);

    assertThat(termNode.getField()).isEqualTo("donors.donor_id");
    assertLookupInfo(termNode.getLookup(), "donor-ids");
  }

  @Test
  public void identifiableFieldWithoutId() {
    val result = prepareResult("eq(mutation.id, '" + ID_VALUE + "')", MUTATION_CENTRIC);
    val termNode = getTermNode(result);
    assertThat(termNode.getField()).isEqualTo("_mutation_id");
    assertThat(termNode.getValue()).isEqualTo(ID_VALUE);
  }

  @Test(expected = IllegalStateException.class)
  public void nonIdentifiableFieldWithId() {
    prepareResult("eq(mutation.type, 'ES:ID1')", MUTATION_CENTRIC);
  }

  private static String getQuery(String nameSpace) {
    val query = format("eq(%s.id, 'ES: %s')", nameSpace, ID_VALUE);
    log.info("Query: {}", query);

    return query;
  }

  private static void assertIdentifiableChild(ExpressionNode child, String expectedId) {
    val node = (TermNode) child;
    assertThat(node.getField()).isEqualTo("_donor_id");
    assertLookupInfo(node.getLookup(), "donor-ids", expectedId);
  }

  private static void assertIdentifiableChildFile(ExpressionNode child, String expectedId) {
    val node = (TermNode) child;
    assertThat(node.getField()).isEqualTo("donors.donor_id");
    assertLookupInfo(node.getLookup(), "donor-ids", expectedId);
  }

  private static ShouldBoolNode getShouldNode(Optional<ExpressionNode> rootNode) {
    return (ShouldBoolNode) rootNode.get()
        .getFirstChild() // Query
        .getFirstChild() // Filter
        .getFirstChild() // Bool
        .getFirstChild(); // ShouldBoolNode
  }

  private Optional<ExpressionNode> prepareResult(String query, Type type) {
    queryContext = new QueryContext("", type);
    val root = createEsAst(query, type);
    val result = root.accept(visitor, Optional.of(queryContext));
    log.debug("After Visitor: {}", result);

    return result;
  }

  private static void assertLookupInfo(LookupInfo lookup, String expectedType) {
    assertLookupInfo(lookup, expectedType, ID_VALUE);
  }

  private static void assertLookupInfo(LookupInfo lookup, String expectedType, String expectedId) {
    assertThat(lookup.getIndex()).isEqualTo("terms-lookup");
    assertThat(lookup.getId()).isEqualTo(expectedId);
    assertThat(lookup.getPath()).isEqualTo("values");
    assertThat(lookup.getType()).isEqualTo(expectedType);
  }

  private static TermNode getTermNode(Optional<ExpressionNode> result) {
    return (TermNode) result.get()
        .getFirstChild() // Query
        .getFirstChild() // Filter
        .getFirstChild(); // Term
  }

}
