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
package org.dcc.portal.pql.es.visitor.aggs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.es.utils.Nodes.cloneNode;
import static org.dcc.portal.pql.es.utils.Visitors.createQuerySimplifierVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createResolveNestedFieldVisitor;
import static org.dcc.portal.pql.es.visitor.aggs.AggregationsResolverVisitor.createNestedAggregaionNode;
import static org.dcc.portal.pql.meta.IndexModel.getMutationCentricTypeModel;
import static org.dcc.portal.pql.utils.Tests.createEsAst;

import java.util.Optional;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.FilterAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.NestedAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.meta.IndexModel;
import org.dcc.portal.pql.meta.Type;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AggregationsResolverVisitorTest {

  private static Optional<Context> CONTEXT = createContext();
  AggregationsResolverVisitor resolver;

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    resolver = new AggregationsResolverVisitor();
  }

  @Test
  public void visitRoot_noAggregationsNode() {
    val originalRoot = (RootNode) createEsAst("eq(gender, 'male')");
    val clone = cloneNode(originalRoot);
    val rootNode = resolver.visitRoot(originalRoot, Optional.empty()).get();
    assertThat(clone).isEqualTo(rootNode);
  }

  @Test
  public void visitRoot_noFilters() {
    val originalRoot = (RootNode) createEsAst("facets(id)", Type.MUTATION_CENTRIC);
    val clone = cloneNode(originalRoot);
    val result = originalRoot.accept(resolver, CONTEXT).get();
    assertThat(clone).isEqualTo(result);
  }

  @Test
  public void visitTermsFacet_match() {
    val result = visit("facets(id), eq(id, 'MU1'), eq(start, 60)");
    log.debug("Result: \n{}", result);

    val filterAgg = (FilterAggregationNode) result.getFirstChild();
    assertThat(filterAgg.childrenCount()).isEqualTo(1);
    val filterNode = filterAgg.getFilters();
    assertThat(filterNode.childrenCount()).isEqualTo(1);

    val startTermNode = (TermNode) filterNode.getFirstChild();
    assertThat(startTermNode.getNameNode().getValue()).isEqualTo("chromosome_start");
    assertThat(startTermNode.getValueNode().getValue()).isEqualTo(60);
  }

  /**
   * Facets field does not match filter. TermsFacetNode should not contain filters
   */
  @Test
  public void visitTermsFacet_noMatch() {
    val root = (RootNode) createEsAst("facets(chromosome), eq(id, 60)", Type.MUTATION_CENTRIC);
    val result = root.accept(resolver, CONTEXT).get();

    val filterAgg = (FilterAggregationNode) result.getFirstChild().getFirstChild();
    assertThat(filterAgg.childrenCount()).isEqualTo(1);
    val filterNode = filterAgg.getFilters();
    assertThat(filterNode.childrenCount()).isEqualTo(1);

    // FilterNode - TermNode
    val termNode = filterNode.getFirstChild();
    assertThat(termNode.childrenCount()).isEqualTo(2);

    val terminalIdNode = (TerminalNode) termNode.getChild(0);
    val terminalValueNode = (TerminalNode) termNode.getChild(1);
    assertThat(terminalIdNode.getValue()).isEqualTo("_mutation_id");
    assertThat(terminalValueNode.getValue()).isEqualTo(60);
  }

  @Test
  public void visitRoot_removedFilterTest() {
    val result = visit("facets(id), eq(id, 60)");
    log.debug("Result - \n{}", result);

    assertThat(result.childrenCount()).isEqualTo(1);
    val termsAggNode = (TermsAggregationNode) result.getFirstChild();
    assertThat(termsAggNode.getAggregationName()).isEqualTo("id");
    assertThat(termsAggNode.getFieldName()).isEqualTo("_mutation_id");
  }

  @Test
  public void nestedFieldTest() {
    val result = visit("facets(transcriptId)");
    val nestedAggr = (NestedAggregationNode) result.getFirstChild();
    assertThat(nestedAggr.getAggregationName()).isEqualTo("transcriptId");
    assertThat(nestedAggr.getPath()).isEqualTo("transcript");

    val termsAggr = (TermsAggregationNode) nestedAggr.getFirstChild();
    assertThat(termsAggr.getFieldName()).isEqualTo("transcript.id");
  }

  @Test
  public void nestedFieldWithNonNestedFilterTest() {
    val result = visit("facets(transcriptId),eq(id, 'M')");
    val filterAggr = (FilterAggregationNode) result.getFirstChild();

    val termNode = (TermNode) filterAggr.getFilters().getFirstChild();
    assertThat(termNode.getNameNode().getValue()).isEqualTo("_mutation_id");

    val nestedAggr = (NestedAggregationNode) filterAggr.getFirstChild();
    assertThat(nestedAggr.getAggregationName()).isEqualTo("transcriptId");
    assertThat(nestedAggr.getPath()).isEqualTo("transcript");

    val termsAggr = (TermsAggregationNode) nestedAggr.getFirstChild();
    assertThat(termsAggr.getFieldName()).isEqualTo("transcript.id");
  }

  @Test
  public void nestedAggregationWithNestedFilterTest() {
    val result = visit("facets(consequenceType),eq(transcriptId, 'T1')");

    val nestedNode = (NestedAggregationNode) result.getFirstChild();
    assertThat(nestedNode.getPath()).isEqualTo("transcript");

    val filterNode = (FilterAggregationNode) nestedNode.getFirstChild();
    val filter = filterNode.getFilters();
    val termNode = (TermNode) filter.getFirstChild();
    assertThat(termNode.getNameNode().getValue()).isEqualTo("transcript.id");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("T1");

    val termAggregationNode = (TermsAggregationNode) filterNode.getFirstChild();
    assertThat(termAggregationNode.getFieldName()).isEqualTo("transcript.consequence.consequence_type");
  }

  @Test
  public void doubleNestedFieldTest() {
    val result = visit("facets(platform)");
    val nestedAggr = (NestedAggregationNode) result.getFirstChild();
    assertThat(nestedAggr.getAggregationName()).isEqualTo("platform");
    assertThat(nestedAggr.getPath()).isEqualTo("ssm_occurrence.observation");

    val termsAggr = (TermsAggregationNode) nestedAggr.getFirstChild();
    assertThat(termsAggr.getFieldName()).isEqualTo("ssm_occurrence.observation.platform");
  }

  @Test
  public void doubleNestedFieldTest_nonNestedFilter() {
    val result = visit("facets(platform), eq(chromosome, '12')");

    // FilterAgg - NestedAgg - TermsAgg
    val filterAggr = (FilterAggregationNode) result.getFirstChild();
    val termNode = (TermNode) filterAggr.getFilters().getFirstChild();
    assertThat(termNode.getNameNode().getValue()).isEqualTo("chromosome");
    assertThat(termNode.getValueNode().getValue()).isEqualTo("12");

    val nestedAggr = (NestedAggregationNode) filterAggr.getFirstChild();
    assertThat(nestedAggr.getAggregationName()).isEqualTo("platform");
    assertThat(nestedAggr.getPath()).isEqualTo("ssm_occurrence.observation");

    val termsAggr = (TermsAggregationNode) nestedAggr.getFirstChild();
    assertThat(termsAggr.getFieldName()).isEqualTo("ssm_occurrence.observation.platform");
  }

  @Test
  public void doubleNestedFieldTest_nestedFilter() {
    val result = visit("facets(platform), eq(donor.projectId, 'ALL-US')");
    val nestedAggr = (NestedAggregationNode) result.getFirstChild();
    assertThat(nestedAggr.getAggregationName()).isEqualTo("platform");
    assertThat(nestedAggr.getPath()).isEqualTo("ssm_occurrence");

    val ssmFilterAggr = (FilterAggregationNode) nestedAggr.getFirstChild();
    val ssmFilter = (TermNode) ssmFilterAggr.getFilters().getFirstChild();
    assertThat(ssmFilter.getNameNode().getValue()).isEqualTo("ssm_occurrence.project._project_id");
    assertThat(ssmFilter.getValueNode().getValue()).isEqualTo("ALL-US");

    val observationNestedAggr = (NestedAggregationNode) ssmFilterAggr.getFirstChild();
    assertThat(observationNestedAggr.getPath()).isEqualTo("ssm_occurrence.observation");

    val termsAggr = (TermsAggregationNode) observationNestedAggr.getFirstChild();
    assertThat(termsAggr.getFieldName()).isEqualTo("ssm_occurrence.observation.platform");
  }

  @Test
  public void doubleNestedFieldTest_doubleNestedFilter() {
    val result = visit("facets(platform), eq(verificationStatus, 'tested')");
    val nestedAggr = (NestedAggregationNode) result.getFirstChild();
    assertThat(nestedAggr.getPath()).isEqualTo("ssm_occurrence.observation");

    val filtersAggr = (FilterAggregationNode) nestedAggr.getFirstChild();
    assertThat(filtersAggr.getFilters()).isNotNull();

    val termsAggr = (TermsAggregationNode) filtersAggr.getFirstChild();
    assertThat(termsAggr.getFieldName()).isEqualTo("ssm_occurrence.observation.platform");
  }

  @Test
  public void doubleNestedFieldTest_allFilters() {
    val result = visit("facets(platform), "
        + "eq(chromosome, '12'), "
        + "nested(ssm_occurrence, eq(donor.projectId, 'ALL-US'), "
        + "nested(ssm_occurrence.observation, eq(verificationStatus, 'tested')))");

    val globalFistersAggr = (FilterAggregationNode) result.getFirstChild();
    assertThat(globalFistersAggr.getFilters()).isNotNull();

    val nestedAggr = (NestedAggregationNode) globalFistersAggr.getFirstChild();
    assertThat(nestedAggr.getPath()).isEqualTo("ssm_occurrence");

    val secondLevelFiltersAggr = (FilterAggregationNode) nestedAggr.getFirstChild();
    assertThat(secondLevelFiltersAggr.getFilters()).isNotNull();

    val secondLevelNestedAggr = (NestedAggregationNode) secondLevelFiltersAggr.getFirstChild();
    assertThat(secondLevelNestedAggr.getPath()).isEqualTo("ssm_occurrence.observation");

    val termsAggr = (TermsAggregationNode) secondLevelNestedAggr.getFirstChild();
    assertThat(termsAggr.getFieldName()).isEqualTo("ssm_occurrence.observation.platform");
  }

  @Test
  public void createNestedAggrTest_sameLevel_noFilters() {
    val result = createNestedAggregaionNode("transcript", "transcript", "name", Optional.empty(),
        getMutationCentricTypeModel()).get();
    assertThat(result.getPath()).isEqualTo("transcript");
    assertThat(result.getAggregationName()).isEqualTo("name");
    assertThat(result.hasChildren()).isFalse();
  }

  @Test
  public void createNestedAggrTest_differentLevels_noFilters() {
    val result = createNestedAggregaionNode("ssm_occurrence", "ssm_occurrence.observation", "name", Optional.empty(),
        getMutationCentricTypeModel());
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  public void createNestedAggrTest_sameLevel_withFilters() {
    val filters = Optional.of(
        new FilterNode(
            new NestedNode("transcript",
                new TermNode("transcript.id", 1))));
    val result = createNestedAggregaionNode("transcript", "transcript", "name", filters,
        getMutationCentricTypeModel()).get();

    assertThat(result.getPath()).isEqualTo("transcript");
    assertThat(result.getAggregationName()).isEqualTo("name");
    assertThat(result.hasChildren()).isTrue();
    assertThat(result.getFirstChild()).isInstanceOf(FilterAggregationNode.class);
  }

  @Test
  public void createNestedAggrTest_sameLevel_noMatchingFilters() {
    val filters = Optional.of(new FilterNode(new TermNode("chromosome", 1)));
    val result = createNestedAggregaionNode("transcript", "transcript", "name", filters,
        getMutationCentricTypeModel()).get();

    assertThat(result.getPath()).isEqualTo("transcript");
    assertThat(result.getAggregationName()).isEqualTo("name");
    assertThat(result.hasChildren()).isFalse();
  }

  @Test
  public void createNestedAggrTest_multiLevel_secondLevelFilters() {
    val filters = Optional.of(new FilterNode(
        new NestedNode("ssm_occurrence.observation",
            new TermNode("ssm_occurrence.observation.verification_status", 1))));

    val result = createNestedAggregaionNode("ssm_occurrence", "ssm_occurrence.observation", "name", filters,
        getMutationCentricTypeModel());
    assertThat(result.isPresent()).isFalse();
  }

  private ExpressionNode visit(String pql) {
    ExpressionNode root = createEsAst(pql, Type.MUTATION_CENTRIC);
    root = root.accept(createQuerySimplifierVisitor(), Optional.empty()).get();

    log.warn("Before: {}", root);
    root = root.accept(createResolveNestedFieldVisitor(), Optional.of(getMutationCentricTypeModel())).get();
    log.warn("After: {}", root);

    ExpressionNode result = root.accept(resolver, CONTEXT).get();

    result = result.accept(createQuerySimplifierVisitor(), Optional.empty()).get();
    log.debug("Result - \n{}", result);

    return Nodes.getOptionalChild(result, AggregationsNode.class).get();
  }

  private static Optional<Context> createContext() {
    return Optional.of(new Context(null, IndexModel.getMutationCentricTypeModel()));
  }

}
