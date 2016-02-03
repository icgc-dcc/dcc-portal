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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.dcc.portal.pql.meta.IndexModel.getTypeModel;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.meta.Type.OBSERVATION_CENTRIC;
import static org.dcc.portal.pql.meta.TypeModel.BIOLOGICAL_PROCESS;
import static org.dcc.portal.pql.meta.TypeModel.CELLULAR_COMPONENT;
import static org.dcc.portal.pql.meta.TypeModel.MOLECULAR_FUNCTION;
import static org.dcc.portal.pql.utils.Tests.assertAndGetNestedNode;
import static org.dcc.portal.pql.utils.Tests.assertBoolAndGetMustNode;
import static org.dcc.portal.pql.utils.Tests.assertBoolAndGetShouldNode;
import static org.dcc.portal.pql.utils.Tests.createEsAst;

import java.util.Optional;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.ExistsNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.meta.TypeModel;
import org.dcc.portal.pql.query.QueryContext;
import org.junit.Test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneSetFilterVisitorTest {

  GeneSetFilterVisitor visitor = new GeneSetFilterVisitor();
  QueryContext context = new QueryContext("", DONOR_CENTRIC);

  @Test
  public void pathwayIdTest() {
    val root = createEsAst("in(gene.pathwayId, 'REACT_6326')");
    assertPathwayAndCuratedSet(root, "gene.pathway", "REACT_6326");
  }

  @Test
  public void curatedSetTest() {
    val root = createEsAst("in(gene.curatedSetId, 'ID1')");
    assertPathwayAndCuratedSet(root, "gene.curated_set", "ID1");
  }

  @Test
  public void goTermTest() {
    val root = createEsAst("in(gene.goTermId, 'GO:0003674')");
    val result = root.accept(visitor, Optional.of(context)).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    // QueryNode FilterNode - NestedNode - BoolNode - ShouldNode (3 TermsNode)
    val nestedNode = result.getFirstChild().getFirstChild().getFirstChild();
    assertThat(nestedNode.childrenCount()).isEqualTo(1);

    val shouldNode = assertBoolAndGetShouldNode(nestedNode.getFirstChild());
    assertGoTerm(shouldNode, getTypeModel(DONOR_CENTRIC), "GO:0003674");
  }

  @Test
  public void goTermNestedTest() {
    val root = new RootNode(new QueryNode(new NestedNode("gene",
        new TermsNode("gene.goTermId", new TerminalNode("GO:0005575")))));
    val result = root.accept(visitor, Optional.of(context)).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    val queryNode = (QueryNode) result.getFirstChild();
    assertThat(queryNode.childrenCount()).isEqualTo(1);

    val nestedNode = (NestedNode) queryNode.getFirstChild();
    assertThat(nestedNode.childrenCount()).isEqualTo(1);
    assertThat(nestedNode.getPath()).isEqualTo("gene");
    assertThat(nestedNode.getScoreMode()).isEqualTo(NestedNode.ScoreMode.AVG);

    val shouldNode = assertBoolAndGetShouldNode(nestedNode.getFirstChild());
    assertGoTerm(shouldNode, getTypeModel(DONOR_CENTRIC), "GO:0005575");
  }

  @Test
  public void geneSetIdTest() {
    val root = createEsAst("in(gene.geneSetId, '123')");
    val result = root.accept(visitor, Optional.of(context)).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    // QueryNode - FilterNode - NestedNode - BoolNode - ShouldNode (3 TermsNode)
    val nestedNode = result.getFirstChild().getFirstChild().getFirstChild();
    assertThat(nestedNode.childrenCount()).isEqualTo(1);

    assertGeneSetId(assertBoolAndGetShouldNode(nestedNode.getFirstChild()), getTypeModel(DONOR_CENTRIC), "123");
  }

  @Test
  public void existsCurratedSetTest() {
    val root = createEsAst("exists(gene.curatedSetId)");
    val result = root.accept(visitor, Optional.of(context)).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "gene.curated_set");
  }

  @Test
  public void existsCurratedSetTest_gene() {
    val root = createEsAst("exists(gene.curatedSetId)", GENE_CENTRIC);
    val result = root.accept(visitor, getGeneContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "curated_set");
  }

  @Test
  public void existsCurratedSetTest_mutation() {
    val root = createEsAst("exists(gene.curatedSetId)", MUTATION_CENTRIC);
    val result = root.accept(visitor, getMutationContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "transcript.gene.curated_set");
  }

  @Test
  public void existsPathwayIdTest() {
    val root = createEsAst("exists(gene.pathwayId)");
    val result = root.accept(visitor, Optional.of(context)).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "gene.pathway");
  }

  @Test
  public void existsPathwayIdTest_gene() {
    val root = createEsAst("exists(gene.pathwayId)", GENE_CENTRIC);
    val result = root.accept(visitor, getGeneContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "pathway");
  }

  @Test
  public void existsPathwayIdTest_mutation() {
    val root = createEsAst("exists(gene.pathwayId)", MUTATION_CENTRIC);
    val result = root.accept(visitor, getMutationContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "transcript.gene.pathway");
  }

  @Test
  public void existsGoTermTest() {
    val root = createEsAst("exists(gene.GoTerm)");
    val result = root.accept(visitor, Optional.of(context)).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "gene.go_term");
  }

  @Test
  public void existsGoTermTest_gene() {
    val root = createEsAst("exists(gene.GoTerm)", GENE_CENTRIC);
    val result = root.accept(visitor, getGeneContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "go_term");
  }

  @Test
  public void existsGoTermTest_mutation() {
    val root = createEsAst("exists(gene.GoTerm)", MUTATION_CENTRIC);
    val result = root.accept(visitor, getMutationContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    assertExists(result, "transcript.gene.go_term");
  }

  @Test
  public void goTermTest_gene() {
    val root = createEsAst("in(gene.goTermId, 'GO:0003674')", GENE_CENTRIC);
    val result = root.accept(visitor, getGeneContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    // QueryNode - FilterNode - OrNode (3 TermsNode)
    val shouldNode = assertBoolAndGetShouldNode(result.getFirstChild().getFirstChild().getFirstChild());
    assertGoTerm(shouldNode, getTypeModel(GENE_CENTRIC), "GO:0003674");
  }

  @Test
  public void curatedSetTest_gene() {
    val root = createEsAst("in(curatedSetId, 'ID1')", GENE_CENTRIC);
    assertPathwayAndCuratedSet(root, "curated_set", "ID1");
  }

  @Test
  public void pathwayIdTest_gene() {
    val root = createEsAst("in(pathwayId, 'REACT_6326')", GENE_CENTRIC);
    assertPathwayAndCuratedSet(root, "pathway", "REACT_6326");
  }

  @Test
  public void geneSetIdTest_gene() {
    val root = createEsAst("in(gene.geneSetId, '123')", GENE_CENTRIC);
    val result = root.accept(visitor, getGeneContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    // QueryNode - FilterNode OrNode (3 TermsNode)
    val boolNode = result.getFirstChild().getFirstChild().getFirstChild();
    assertGeneSetId(assertBoolAndGetShouldNode(boolNode), getTypeModel(GENE_CENTRIC), "123");
  }

  @Test
  public void goTermTest_mutation() {
    val root = createEsAst("in(gene.goTermId, 'GO:0003674')", MUTATION_CENTRIC);
    val result = root.accept(visitor, getMutationContextOptional()).get();

    // QueryNode - FilterNode - NestedNode - OrNode (3 TermsNode)
    val nestedNode = result.getFirstChild().getFirstChild().getFirstChild();
    assertThat(nestedNode.childrenCount()).isEqualTo(1);

    val shouldNode = assertBoolAndGetShouldNode(nestedNode.getFirstChild());
    assertGoTerm(shouldNode, getTypeModel(MUTATION_CENTRIC), "GO:0003674");
  }

  @Test
  public void goTermTest_observation() {
    val root = createEsAst("in(gene.goTermId, 'GO:0003674')", OBSERVATION_CENTRIC);
    val result = root.accept(visitor, getObservationContextOptional()).get();

    // QueryNode - FilterNode - NestedNode - OrNode (3 TermsNode)
    val nestedNode = result.getFirstChild().getFirstChild().getFirstChild();
    assertThat(nestedNode.childrenCount()).isEqualTo(1);

    val shouldNode = assertBoolAndGetShouldNode(nestedNode.getFirstChild());
    assertGoTerm(shouldNode, getTypeModel(OBSERVATION_CENTRIC), "GO:0003674");
  }

  @Test
  public void curatedSetTest_mutation() {
    val root = createEsAst("in(gene.curatedSetId, 'ID1')", MUTATION_CENTRIC);
    assertPathwayAndCuratedSet(root, "transcript.gene.curated_set", "ID1");
  }

  @Test
  public void pathwayIdTest_mutation() {
    val root = createEsAst("in(gene.pathwayId, 'REACT_6326')", MUTATION_CENTRIC);
    log.debug("After GeneSetFilterVisitor: {}", root);
    assertPathwayAndCuratedSet(root, "transcript.gene.pathway", "REACT_6326");
  }

  @Test
  public void pathwayIdTest_observation() {
    val root = createEsAst("in(gene.pathwayId, 'REACT_6326')", OBSERVATION_CENTRIC);
    log.debug("After GeneSetFilterVisitor: {}", root);
    assertPathwayAndCuratedSet(root, "ssm.gene.pathway", "REACT_6326");
  }

  @Test
  public void geneSetIdTest_mutation() {
    val root = createEsAst("in(gene.geneSetId, '123')", MUTATION_CENTRIC);
    val result = root.accept(visitor, getMutationContextOptional()).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    // QueryNode - FilterNode - NestedNode - OrNode (3 TermsNode)
    val nestedNode = result.getFirstChild().getFirstChild().getFirstChild();
    assertThat(nestedNode.childrenCount()).isEqualTo(1);

    val boolNode = nestedNode.getFirstChild();
    assertGeneSetId(assertBoolAndGetShouldNode(boolNode), getTypeModel(MUTATION_CENTRIC), "123");
  }

  @Test
  public void nestedPathwayAndGoTermTest_mutation() {
    assertPathwayAndGoTerm(MUTATION_CENTRIC, "transcript.gene.pathway", "transcript");
  }

  @Test
  public void nestedPathwayAndGoTermTest_donor() {
    assertPathwayAndGoTerm(DONOR_CENTRIC, "gene.pathway", "gene");
  }

  private void assertPathwayAndGoTerm(Type indexType, String existPath, String nestedPath) {
    val existsNode = new ExistsNode(existPath);
    val root = createRootForPathwayAndGoTermTest(nestedPath, existsNode);
    Optional<QueryContext> contextOpt = null;

    switch (indexType) {
    case DONOR_CENTRIC:
      contextOpt = Optional.of(context);
      break;
    case MUTATION_CENTRIC:
      contextOpt = getMutationContextOptional();
      break;
    default:
      fail("The 'indexType' argument in this test can only be DONOR_CENTRIC or MUTATION_CENTRIC.");
    }

    val result = root.accept(visitor, contextOpt).get();
    log.debug("After GeneSetFilterVisitor: {}", result);

    // Root - Query - Filter - Nested
    val nestedNode = assertAndGetNestedNode(result.getFirstChild().getFirstChild().getFirstChild(), nestedPath);

    // Nested - Bool - Must
    val mustNode = assertBoolAndGetMustNode(nestedNode.getFirstChild());
    assertThat(mustNode.childrenCount()).isEqualTo(2);
    assertThat(mustNode.getFirstChild()).isEqualTo(existsNode);

    val shouldNode = assertBoolAndGetShouldNode(mustNode.getChild(1));
    assertGoTerm(shouldNode, getTypeModel(indexType), "GO123");
  }

  private static RootNode createRootForPathwayAndGoTermTest(String nestedPath, ExistsNode existsNode) {
    val queryNestedNode = new NestedNode(nestedPath, new BoolNode(new MustBoolNode(
        existsNode,
        new TermsNode("gene.goTermId", new TerminalNode("GO123")))));

    return new RootNode(new QueryNode(new FilterNode(queryNestedNode)));
  }

  private static void assertExists(ExpressionNode node, String value) {
    // QueryNode - FilterNode - ExistsNode
    val existsNode = (ExistsNode) node.getFirstChild().getFirstChild().getFirstChild();
    assertThat(existsNode.getField()).isEqualTo(value);
  }

  private static void assertTermsNode(ExpressionNode node, String fieldName, String value) {
    val child = getTermsNode(node);
    assertThat(child.getField()).isEqualTo(fieldName);
    assertThat(getTerminalNode(child).getValue()).isEqualTo(value);
  }

  private static void assertPathwayAndCuratedSet(ExpressionNode root, String fieldName, String value) {
    // QueryNode - FilterNode - BoolNode - MustBoolNode - TermsNode
    val termsNode = (TermsNode) root
        .getFirstChild() // Query
        .getFirstChild() // Filter
        .getFirstChild(); // Terms
    assertThat(termsNode.getField()).isEqualTo(fieldName);
    assertThat(termsNode.childrenCount()).isEqualTo(1);

    val terminalNode = (TerminalNode) termsNode.getFirstChild();
    assertThat(terminalNode.getValue()).isEqualTo(value);
  }

  private static TermsNode getTermsNode(ExpressionNode termsNode) {
    return (TermsNode) termsNode;
  }

  private static TerminalNode getTerminalNode(ExpressionNode termsNode) {
    return (TerminalNode) termsNode.getFirstChild();
  }

  private static Optional<QueryContext> getGeneContextOptional() {
    return Optional.of(new QueryContext("", GENE_CENTRIC));
  }

  private static Optional<QueryContext> getMutationContextOptional() {
    return Optional.of(new QueryContext("", MUTATION_CENTRIC));
  }

  private static Optional<QueryContext> getObservationContextOptional() {
    return Optional.of(new QueryContext("", OBSERVATION_CENTRIC));
  }

  private static void assertGeneSetId(ShouldBoolNode shouldNode, TypeModel typeModel, String value) {
    assertThat(shouldNode.childrenCount()).isEqualTo(5);

    assertTermsNode(shouldNode.getChild(0), typeModel.getInternalField(CELLULAR_COMPONENT), value);
    assertTermsNode(shouldNode.getChild(1), typeModel.getInternalField(BIOLOGICAL_PROCESS), value);
    assertTermsNode(shouldNode.getChild(2), typeModel.getInternalField(MOLECULAR_FUNCTION), value);
    assertTermsNode(shouldNode.getChild(3), typeModel.getField("gene.pathwayId"), value);
    assertTermsNode(shouldNode.getChild(4), typeModel.getField("gene.curatedSetId"), value);
  }

  private static void assertGoTerm(ShouldBoolNode shouldNode, TypeModel typeModel, String value) {
    assertThat(shouldNode.childrenCount()).isEqualTo(3);

    assertTermsNode(shouldNode.getChild(0), typeModel.getInternalField(CELLULAR_COMPONENT), value);
    assertTermsNode(shouldNode.getChild(1), typeModel.getInternalField(BIOLOGICAL_PROCESS), value);
    assertTermsNode(shouldNode.getChild(2), typeModel.getInternalField(MOLECULAR_FUNCTION), value);
  }

}
