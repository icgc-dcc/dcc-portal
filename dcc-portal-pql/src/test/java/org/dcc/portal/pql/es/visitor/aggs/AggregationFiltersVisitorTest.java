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
import static org.dcc.portal.pql.utils.Tests.createEsAst;

import java.util.Optional;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.junit.Before;
import org.junit.Test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AggregationFiltersVisitorTest {

  private static final ExpressionNode REMOVE_CHILD = null;
  private static final Optional<String> AGGREGATION_FIELD = Optional.of("donor_sex");
  AggregationFiltersVisitor visitor;

  @Before
  public void setUp() {
    visitor = new AggregationFiltersVisitor();
  }

  @Test
  public void visitTerm_noMatch() {
    val original = (TermNode) getMustNode("eq(ageAtDiagnosis, 60)").getFirstChild();
    val clone = cloneNode(original);
    val result = visitor.visitTerm(original, AGGREGATION_FIELD);
    assertThat(result).isEqualTo(clone);
  }

  @Test
  public void visitTerm_match() {
    val original = (TermNode) getMustNode("eq(gender, 'male')").getFirstChild();
    val result = visitor.visitTerm(original, AGGREGATION_FIELD);
    assertThat(result).isEqualTo(REMOVE_CHILD);
  }

  @Test
  public void visitRange_noMatch() {
    val original = (RangeNode) getMustNode("gt(ageAtDiagnosis, 60)").getFirstChild();
    val clone = cloneNode(original);
    val result = visitor.visitRange(original, AGGREGATION_FIELD);
    assertThat(result).isEqualTo(clone);
  }

  @Test
  public void visitRange_match() {
    val original = (RangeNode) getMustNode("gt(gender, 70)").getFirstChild();
    val result = visitor.visitRange(original, AGGREGATION_FIELD);
    assertThat(result).isEqualTo(REMOVE_CHILD);
  }

  @Test
  public void visitTerms_noMatch() {
    val original = (TermsNode) getMustNode("in(ageAtDiagnosis, 60, 70)").getFirstChild();
    val clone = cloneNode(original);
    val result = visitor.visitTerms(original, AGGREGATION_FIELD);
    assertThat(result).isEqualTo(clone);
  }

  @Test
  public void visitTerms_match() {
    val original = (TermsNode) getMustNode("in(gender, 'male', 'female')").getFirstChild();
    val result = visitor.visitTerms(original, AGGREGATION_FIELD);
    assertThat(result).isEqualTo(REMOVE_CHILD);
  }

  @Test
  public void missingTest() {
    val boolNode = (BoolNode) getMustNode("or(missing(donor.gender),in(donor.gender,'male'))").getFirstChild();
    val boolNodeClone = cloneNode(boolNode);
    val result = visitor.visitBool(boolNode, AGGREGATION_FIELD);
    log.debug("Result {}", result);

    assertThat(result).isNotEqualTo(boolNodeClone);
    assertThat(result.getFirstChild().hasChildren()).isFalse();
  }

  private ExpressionNode getMustNode(String query) {
    ExpressionNode filterNode = createEsAst(query).getFirstChild();

    // QueryNode - FilterNode - BoolNode - MustBoolNode
    // Make a clean copy of the must node
    val mustNode = Nodes.cloneNode(filterNode.getFirstChild());

    return mustNode;
  }

}
