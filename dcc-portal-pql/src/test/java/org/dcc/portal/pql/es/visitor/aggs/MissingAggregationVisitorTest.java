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
import static org.dcc.portal.pql.utils.Tests.createEsAst;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.FilterAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.MissingAggregationNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.utils.Visitors;
import org.dcc.portal.pql.meta.IndexModel;
import org.junit.Test;

@Slf4j
public class MissingAggregationVisitorTest {

  MissingAggregationVisitor visitor = new MissingAggregationVisitor();

  @Test
  public void noFiltersTest() {
    val aggregationsNode = prepareAggregationsNode("facets(gender)");
    val result = visitor.visitAggregations(aggregationsNode, Optional.empty());
    log.debug("\n{}", result);

    assertThat(result.childrenCount()).isEqualTo(2);
    val missingAgg = Nodes.getOptionalChild(result, MissingAggregationNode.class).get();
    assertThat(missingAgg.getFieldName()).isEqualTo("donor_sex");
    assertThat(missingAgg.getAggregationName()).isEqualTo("gender" + MissingAggregationVisitor.MISSING_SUFFIX);
  }

  @Test
  public void withFiltersTest() {
    val aggregationsNode = prepareAggregationsNode("facets(gender), eq(id, 'ID1')");
    val result = visitor.visitAggregations(aggregationsNode, Optional.empty());
    log.warn("\n{}", result);

    assertThat(result.childrenCount()).isEqualTo(2);
    val originalFilterAgg = (FilterAggregationNode) result.getFirstChild();
    val filterAgg = (FilterAggregationNode) result.getChild(1);
    assertThat(filterAgg.getFilters()).isEqualTo(originalFilterAgg.getFilters());
    assertThat(filterAgg.getAggregationName()).isEqualTo("gender" + MissingAggregationVisitor.MISSING_SUFFIX);

    val missingAgg = Nodes.getOptionalChild(filterAgg, MissingAggregationNode.class).get();
    assertThat(missingAgg.getFieldName()).isEqualTo("donor_sex");
    assertThat(missingAgg.getAggregationName()).isEqualTo("gender" + MissingAggregationVisitor.MISSING_SUFFIX);
  }

  private static AggregationsNode prepareAggregationsNode(String query) {
    val esAst = createEsAst(query);

    val resolvedAst = esAst.accept(Visitors.createAggregationsResolverVisitor(), createContext()).get();
    val result = Nodes.getOptionalChild(resolvedAst, AggregationsNode.class).get();
    log.warn("Prepared - \n{}", result);

    return result;
  }

  private static Optional<Context> createContext() {
    return Optional.of(new Context(null, IndexModel.getDonorCentricTypeModel()));
  }

}
