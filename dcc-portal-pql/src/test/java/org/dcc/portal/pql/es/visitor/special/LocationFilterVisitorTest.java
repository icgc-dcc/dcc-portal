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
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.meta.TypeModel.GENE_LOCATION;
import static org.dcc.portal.pql.meta.TypeModel.MUTATION_LOCATION;
import static org.dcc.portal.pql.utils.Tests.assertAndGetNestedNode;
import static org.dcc.portal.pql.utils.Tests.assertBoolAndGetMustNode;
import static org.dcc.portal.pql.utils.Tests.assertBoolAndGetShouldNode;
import static org.dcc.portal.pql.utils.Tests.createEsAst;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.filter.GreaterEqualNode;
import org.dcc.portal.pql.es.ast.filter.LessEqualNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.meta.IndexModel;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.meta.TypeModel;
import org.dcc.portal.pql.query.QueryContext;
import org.icgc.dcc.common.core.model.ChromosomeLocation;
import org.junit.Test;

@Slf4j
public class LocationFilterVisitorTest {

  private static final String SHORT_FORM_LOCATION_VALUE = "chr22";
  private static final String SHORT_FORM_CHROMOSOME_VALUE =
      ChromosomeLocation.parse(SHORT_FORM_LOCATION_VALUE).getChromosome().getName();
  private static final String LONG_FORM_LOCATION_VALUE = "chr12:123-456";
  private static final String GENE_FILTER = format("in(gene.location, ['%s'])", LONG_FORM_LOCATION_VALUE);
  private static final ChromosomeLocation LOCATION_OBJECT = ChromosomeLocation.parse(LONG_FORM_LOCATION_VALUE);
  private static final String CHROMOSOME_VALUE = LOCATION_OBJECT.getChromosome().getName();
  private static final long CHROMOSOME_START = LOCATION_OBJECT.getStart();
  private static final long CHROMOSOME_END = LOCATION_OBJECT.getEnd();

  LocationFilterVisitor visitor = new LocationFilterVisitor();
  QueryContext queryContext;

  @Test
  public void geneLocationTest_donor_notNested() {
    val esAst = new TermNode(GENE_LOCATION, LONG_FORM_LOCATION_VALUE);
    val result = executeQuery(esAst, DONOR_CENTRIC);
    assertLocation(result, DONOR_CENTRIC, false, GENE_LOCATION);
  }

  @Test
  public void geneLocationTest_donor_notNested_with_short_form_location() {
    val esAst = new TermNode(GENE_LOCATION, SHORT_FORM_LOCATION_VALUE);
    val result = executeQuery(esAst, DONOR_CENTRIC);

    assertShortFormLocation(result, DONOR_CENTRIC, false, GENE_LOCATION);
  }

  @Test
  public void geneLocationTest_donor_nested() {
    val esAst = new NestedNode("gene", new TermNode(GENE_LOCATION, LONG_FORM_LOCATION_VALUE));
    assertLocation(executeQuery(esAst, DONOR_CENTRIC), DONOR_CENTRIC, false, GENE_LOCATION);
  }

  @Test
  public void geneLocationArrayTest_donor_notNested() {
    val esAst = new TermsNode(GENE_LOCATION, new TerminalNode(LONG_FORM_LOCATION_VALUE));
    assertLocation(executeQuery(esAst, DONOR_CENTRIC), DONOR_CENTRIC, true, GENE_LOCATION);
  }

  @Test
  public void geneLocationArrayTest_donor_nested() {
    val esAst = new NestedNode("gene", new TermsNode(GENE_LOCATION, new TerminalNode(LONG_FORM_LOCATION_VALUE)));
    assertLocation(executeQuery(esAst, DONOR_CENTRIC), DONOR_CENTRIC, true, GENE_LOCATION);
  }

  @Test
  public void geneLocationTest_gene_notNested() {
    val esAst = new TermNode(GENE_LOCATION, LONG_FORM_LOCATION_VALUE);
    assertLocation(executeQuery(esAst, GENE_CENTRIC), GENE_CENTRIC, false, GENE_LOCATION);
  }

  @Test
  public void geneLocationArrayTest_gene_notNested() {
    val esAst = new TermsNode(GENE_LOCATION, new TerminalNode(LONG_FORM_LOCATION_VALUE));
    assertLocation(executeQuery(esAst, GENE_CENTRIC), GENE_CENTRIC, true, GENE_LOCATION);
  }

  @Test
  public void geneLocationTest_mutation_notNested() {
    val esAst = new TermNode(GENE_LOCATION, LONG_FORM_LOCATION_VALUE);
    assertLocation(executeQuery(esAst, MUTATION_CENTRIC), MUTATION_CENTRIC, false, GENE_LOCATION);
  }

  @Test
  public void geneLocationTest_mutation_nested() {
    val esAst = new NestedNode("transcript", new TermNode(GENE_LOCATION, LONG_FORM_LOCATION_VALUE));
    assertLocation(executeQuery(esAst, MUTATION_CENTRIC), MUTATION_CENTRIC, false, GENE_LOCATION);
  }

  @Test
  public void geneLocationArrayTest_mutation_notNested() {
    val esAst = new TermsNode(GENE_LOCATION, new TerminalNode(LONG_FORM_LOCATION_VALUE));
    assertLocation(executeQuery(esAst, MUTATION_CENTRIC), MUTATION_CENTRIC, true, GENE_LOCATION);
  }

  @Test
  public void geneLocationArrayTest_mutation_nested() {
    val esAst = new NestedNode("transcript", new TermsNode(GENE_LOCATION, new TerminalNode(LONG_FORM_LOCATION_VALUE)));
    assertLocation(executeQuery(esAst, MUTATION_CENTRIC), MUTATION_CENTRIC, true, GENE_LOCATION);
  }

  @Test
  public void mutationLocationTest_donor_notNested() {
    val esAst = new TermNode(MUTATION_LOCATION, LONG_FORM_LOCATION_VALUE);
    val result = executeQuery(esAst, DONOR_CENTRIC);
    assertLocation(result, DONOR_CENTRIC, false, MUTATION_LOCATION);
  }

  @Test
  public void mutationLocationTest_donor_nested() {
    val esAst = new NestedNode("gene.ssm", new TermNode(MUTATION_LOCATION, LONG_FORM_LOCATION_VALUE));
    assertLocation(executeQuery(esAst, DONOR_CENTRIC), DONOR_CENTRIC, false, MUTATION_LOCATION);
  }

  @Test
  public void mutationLocationArrayTest_donor_notNested() {
    val esAst = new TermsNode(MUTATION_LOCATION, new TerminalNode(LONG_FORM_LOCATION_VALUE));
    assertLocation(executeQuery(esAst, DONOR_CENTRIC), DONOR_CENTRIC, true, MUTATION_LOCATION);
  }

  @Test
  public void mutationLocationArrayTest_donor_nested() {
    val esAst =
        new NestedNode("gene.ssm", new TermsNode(MUTATION_LOCATION, new TerminalNode(LONG_FORM_LOCATION_VALUE)));
    assertLocation(executeQuery(esAst, DONOR_CENTRIC), DONOR_CENTRIC, true, MUTATION_LOCATION);
  }

  @Test
  public void mutationLocationTest_gene_notNested() {
    val esAst = new TermNode(MUTATION_LOCATION, LONG_FORM_LOCATION_VALUE);
    assertLocation(executeQuery(esAst, GENE_CENTRIC), GENE_CENTRIC, false, MUTATION_LOCATION);
  }

  @Test
  public void mutationLocationArrayTest_gene_notNested() {
    val esAst = new TermsNode(MUTATION_LOCATION, new TerminalNode(LONG_FORM_LOCATION_VALUE));
    assertLocation(executeQuery(esAst, GENE_CENTRIC), GENE_CENTRIC, true, MUTATION_LOCATION);
  }

  @Test
  public void mutationLocationTest_gene_Nested() {
    val esAst = new NestedNode("donor.ssm", new TermNode(MUTATION_LOCATION, LONG_FORM_LOCATION_VALUE));
    assertLocation(executeQuery(esAst, GENE_CENTRIC), GENE_CENTRIC, false, MUTATION_LOCATION);
  }

  @Test
  public void mutationLocationArrayTest_gene_Nested() {
    val esAst =
        new NestedNode("donor.ssm", new TermsNode(MUTATION_LOCATION, new TerminalNode(LONG_FORM_LOCATION_VALUE)));
    assertLocation(executeQuery(esAst, GENE_CENTRIC), GENE_CENTRIC, true, MUTATION_LOCATION);
  }

  @Test
  public void mutationLocationTest_mutation_notNested() {
    val esAst = new TermNode(MUTATION_LOCATION, LONG_FORM_LOCATION_VALUE);
    assertLocation(executeQuery(esAst, MUTATION_CENTRIC), MUTATION_CENTRIC, false, MUTATION_LOCATION);
  }

  @Test
  public void mutationLocationArrayTest_mutation_notNested() {
    val esAst = new TermsNode(MUTATION_LOCATION, new TerminalNode(LONG_FORM_LOCATION_VALUE));
    assertLocation(executeQuery(esAst, MUTATION_CENTRIC), MUTATION_CENTRIC, true, MUTATION_LOCATION);
  }

  @Test(expected = IllegalStateException.class)
  public void wrongNestingLevel() {
    val root = createEsAst(format("nested(transcript.ssm, %s)", GENE_FILTER), MUTATION_CENTRIC);
    root.accept(visitor, Optional.of(new QueryContext("", MUTATION_CENTRIC))).get();
  }

  private static String resolveStart(String filterType, TypeModel typeModel) {
    val fieldName = filterType.equals(GENE_LOCATION) ? "gene.start" : "mutation.start";

    return typeModel.getField(fieldName);
  }

  private static String resolveEnd(String filterType, TypeModel typeModel) {
    val fieldName = filterType.equals(GENE_LOCATION) ? "gene.end" : "mutation.end";

    return typeModel.getField(fieldName);
  }

  private static String resolveChromosome(String filterType, TypeModel typeModel) {
    val fieldName = filterType.equals(GENE_LOCATION) ? "gene.chromosome" : "mutation.chromosome";

    return typeModel.getField(fieldName);
  }

  private static void assertLocation(ExpressionNode node, Type type, boolean isArray, String filterType) {
    val typeModel = IndexModel.getTypeModel(type);
    ExpressionNode root = null;
    MustBoolNode mustNode = null;

    // no NestedNode
    if (hasNested(filterType, type)) {
      val nestedPath = typeModel.getNestedPath(resolveStart(filterType, typeModel));
      val nestedNode = assertAndGetNestedNode(node, nestedPath);
      root = nestedNode.getFirstChild();
    } else {
      root = node;
    }

    if (isArray) {
      val shouldNode = assertBoolAndGetShouldNode(root);
      mustNode = assertBoolAndGetMustNode(shouldNode.getFirstChild());
    } else {
      mustNode = assertBoolAndGetMustNode(root);
    }

    assertThat(mustNode.childrenCount()).isEqualTo(3);

    val termNode = (TermNode) mustNode.getChild(0);
    assertThat(termNode.getNameNode().getValue()).isEqualTo(resolveChromosome(filterType, typeModel));
    assertThat(termNode.getValueNode().getValue()).isEqualTo(CHROMOSOME_VALUE);

    // GreaterEqual chromosome start
    RangeNode rangeNode = (RangeNode) mustNode.getChild(1);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo(resolveStart(filterType, typeModel));
    val geNode = (GreaterEqualNode) rangeNode.getFirstChild();
    assertThat(geNode.getValue()).isEqualTo(CHROMOSOME_START);

    // LessEqual chromosome end
    rangeNode = (RangeNode) mustNode.getChild(2);
    assertThat(rangeNode.childrenCount()).isEqualTo(1);
    assertThat(rangeNode.getFieldName()).isEqualTo(resolveEnd(filterType, typeModel));
    val leNode = (LessEqualNode) rangeNode.getFirstChild();
    assertThat(leNode.getValue()).isEqualTo(CHROMOSOME_END);
  }

  private static void assertShortFormLocation(ExpressionNode node, Type type, boolean isArray, String filterType) {
    val typeModel = IndexModel.getTypeModel(type);
    ExpressionNode root = null;
    MustBoolNode mustNode = null;

    // no NestedNode
    if (hasNested(filterType, type)) {
      val nestedPath = typeModel.getNestedPath(resolveStart(filterType, typeModel));
      val nestedNode = assertAndGetNestedNode(node, nestedPath);
      root = nestedNode.getFirstChild();
    } else {
      root = node;
    }

    if (isArray) {
      val shouldNode = assertBoolAndGetShouldNode(root);
      mustNode = assertBoolAndGetMustNode(shouldNode.getFirstChild());
    } else {
      mustNode = assertBoolAndGetMustNode(root);
    }

    assertThat(mustNode.childrenCount()).isEqualTo(1);

    val termNode = (TermNode) mustNode.getChild(0);
    assertThat(termNode.getNameNode().getValue()).isEqualTo(resolveChromosome(filterType, typeModel));
    assertThat(termNode.getValueNode().getValue()).isEqualTo(SHORT_FORM_CHROMOSOME_VALUE);
  }

  /**
   * @param filterType
   * @param type
   * @return
   */
  private static boolean hasNested(String filterType, Type type) {
    return !(type == GENE_CENTRIC && filterType.equals(GENE_LOCATION) || type == MUTATION_CENTRIC
        && filterType.equals(MUTATION_LOCATION));
  }

  private ExpressionNode executeQuery(ExpressionNode esAst, Type type) {
    log.debug("ES AST: \n{}", esAst);
    val result = esAst.accept(visitor, Optional.of(new QueryContext("", type)));
    log.debug("Result: \n{}", result);
    if (result.isPresent()) {
      return result.get();
    }

    return esAst;
  }

}
