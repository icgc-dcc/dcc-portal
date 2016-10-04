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
package org.dcc.portal.pql.ast.visitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.ast.builder.FilterBuilders.and;
import static org.dcc.portal.pql.ast.builder.FilterBuilders.eq;
import static org.dcc.portal.pql.ast.builder.FilterBuilders.exists;
import static org.dcc.portal.pql.ast.builder.FilterBuilders.ge;
import static org.dcc.portal.pql.ast.builder.FilterBuilders.gt;
import static org.dcc.portal.pql.ast.builder.FilterBuilders.in;
import static org.dcc.portal.pql.ast.builder.FilterBuilders.le;
import static org.dcc.portal.pql.ast.builder.FilterBuilders.lt;
import static org.dcc.portal.pql.ast.builder.FilterBuilders.missing;
import static org.dcc.portal.pql.ast.builder.FilterBuilders.nested;
import static org.dcc.portal.pql.ast.builder.FilterBuilders.not;
import static org.dcc.portal.pql.ast.builder.FilterBuilders.or;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.count;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.facets;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.facetsAll;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.limit;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.select;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.selectAll;
import static org.dcc.portal.pql.ast.function.SortNode.builder;
import static org.dcc.portal.pql.query.PqlParser.parse;
import static org.dcc.portal.pql.utils.Tests.assertBoolAndGetMustNode;
import static org.dcc.portal.pql.utils.Tests.assertBoolAndGetShouldNode;

import java.util.Optional;

import org.dcc.portal.pql.ast.PqlNode;
import org.dcc.portal.pql.ast.builder.FilterBuilders;
import org.dcc.portal.pql.ast.builder.PqlBuilders;
import org.dcc.portal.pql.ast.builder.PqlSearchBuilder;
import org.dcc.portal.pql.ast.filter.EqNode;
import org.dcc.portal.pql.ast.filter.NeNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.FieldsNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.SortNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.ast.filter.ExistsNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.GreaterEqualNode;
import org.dcc.portal.pql.es.ast.filter.GreaterThanNode;
import org.dcc.portal.pql.es.ast.filter.LessEqualNode;
import org.dcc.portal.pql.es.ast.filter.LessThanNode;
import org.dcc.portal.pql.es.ast.filter.MissingNode;
import org.dcc.portal.pql.es.ast.filter.NotNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.es.model.Order;
import org.dcc.portal.pql.meta.IndexModel;
import org.junit.Test;

import lombok.val;

public class CreateEsAstVisitorTest {

  CreateEsAstVisitor visitor = new CreateEsAstVisitor();

  @Test
  public void visitEqTest() {
    val node = new EqNode("id", 1);
    val result = (TermNode) visit(node);
    assertThat(result.getNameNode().getValue()).isEqualTo("_mutation_id");
    assertThat(result.getValueNode().getValue()).isEqualTo(1);
  }

  @Test
  public void visitSelectTest() {

    FieldsNode result = (FieldsNode) visit(select("id", "end"));
    assertThat(result.childrenCount()).isEqualTo(2);
    assertThat(result.getFields()).containsOnly("_mutation_id", "chromosome_end");

    result = (FieldsNode) visit(selectAll());
    assertThat(result.getFields()).containsOnlyElementsOf(IndexModel.getMutationCentricTypeModel().getFields());
  }

  @Test
  public void visitFacetsTest() {
    AggregationsNode result = (AggregationsNode) visit(facets("id", "end"));
    assertThat(result.childrenCount()).isEqualTo(2);
    assertThat(((TermsAggregationNode) result.getChild(0)).getFieldName()).isEqualTo("_mutation_id");
    assertThat(((TermsAggregationNode) result.getChild(1)).getFieldName()).isEqualTo("chromosome_end");

    result = (AggregationsNode) visit(facetsAll());
    assertThat(result.childrenCount()).isEqualTo(6);
  }

  @Test
  public void visitLimitTest() {
    val result = (org.dcc.portal.pql.es.ast.LimitNode) visit(limit(20, 5));
    assertThat(result.getFrom()).isEqualTo(20);
    assertThat(result.getSize()).isEqualTo(5);
  }

  @Test
  public void visitSortTest() {
    val node = PqlSearchBuilder.statement()
        .sort(builder()
            .sortAsc("id")
            .sortDesc("end"))
        .build()
        .getSort();
    val fields = ((SortNode) visit(node)).getFields();

    assertThat(fields.size()).isEqualTo(2);
    assertThat(fields.get("_mutation_id")).isEqualTo(Order.ASC);
    assertThat(fields.get("chromosome_end")).isEqualTo(Order.DESC);
  }

  @Test
  public void visitCountTest() {
    assertThat(visit(count())).isInstanceOf(org.dcc.portal.pql.es.ast.CountNode.class);
  }

  @Test
  public void visitNeTest() {
    val result = (NotNode) visit(new NeNode("id", 1));
    assertThat(result.childrenCount()).isEqualTo(1);

    val termNode = (TermNode) result.getFirstChild();
    assertThat(termNode.getNameNode().getValue()).isEqualTo("_mutation_id");
    assertThat(termNode.getValueNode().getValue()).isEqualTo(1);
  }

  @Test
  public void visitGeTest() {
    val result = (RangeNode) visit(ge("id", 1).build());
    assertThat(result.childrenCount()).isEqualTo(1);
    val geNode = (GreaterEqualNode) result.getFirstChild();
    assertThat(geNode.getValue()).isEqualTo(1);
  }

  @Test
  public void visitGtTest() {
    val result = (RangeNode) visit(gt("id", 1).build());
    assertThat(result.childrenCount()).isEqualTo(1);
    val geNode = (GreaterThanNode) result.getFirstChild();
    assertThat(geNode.getValue()).isEqualTo(1);
  }

  @Test
  public void visitLeTest() {
    val result = (RangeNode) visit(le("id", 1).build());
    assertThat(result.childrenCount()).isEqualTo(1);
    val geNode = (LessEqualNode) result.getFirstChild();
    assertThat(geNode.getValue()).isEqualTo(1);
  }

  @Test
  public void visitLtTest() {
    val result = (RangeNode) visit(lt("id", 1).build());
    assertThat(result.childrenCount()).isEqualTo(1);
    val geNode = (LessThanNode) result.getFirstChild();
    assertThat(geNode.getValue()).isEqualTo(1);
  }

  @Test
  public void visitNotTest() {
    val result = (NotNode) visit(not(eq("id", 1)).build());
    assertThat(result.childrenCount()).isEqualTo(1);
    assertThat(result.getFirstChild()).isInstanceOf(TermNode.class);
  }

  @Test
  public void visitExistsTest() {
    val result = (ExistsNode) visit(exists("id").build());
    assertThat(result.getField()).isEqualTo("_mutation_id");
  }

  @Test
  public void visitMissingTest() {
    val result = (MissingNode) visit(missing("id").build());
    assertThat(result.getField()).isEqualTo("_mutation_id");
  }

  @Test
  public void visitInTest() {
    val result = (TermsNode) visit(in("id", 5, 6).build());
    assertThat(result.getField()).isEqualTo("_mutation_id");
    assertThat(result.childrenCount()).isEqualTo(2);
    assertThat(((TerminalNode) result.getFirstChild()).getValue()).isEqualTo(5);
    assertThat(((TerminalNode) result.getChild(1)).getValue()).isEqualTo(6);
  }

  @Test
  public void visitAndTest() {
    val result = visit(and(eq("id", 1), ge("end", 5)).build());
    val mustNode = assertBoolAndGetMustNode(result);
    assertThat(mustNode.childrenCount()).isEqualTo(2);
    assertThat(mustNode.getFirstChild()).isInstanceOf(TermNode.class);
    assertThat(mustNode.getChild(1)).isInstanceOf(RangeNode.class);
  }

  @Test
  public void visitOrTest() {
    val result = visit(or(eq("id", 1), ge("end", 5)).build());
    val shouldNode = assertBoolAndGetShouldNode(result);
    assertThat(shouldNode.childrenCount()).isEqualTo(2);
    assertThat(shouldNode.getFirstChild()).isInstanceOf(TermNode.class);
    assertThat(shouldNode.getChild(1)).isInstanceOf(RangeNode.class);
  }

  @Test
  public void visitNestedTest() {
    val result = (NestedNode) visit(nested("gene", eq("id", 1)).build());
    assertThat(result.getPath()).isEqualTo("gene");
    val mustNode = assertBoolAndGetMustNode(result.getFirstChild());
    assertThat(mustNode.childrenCount()).isEqualTo(1);
    assertThat(mustNode.getFirstChild()).isInstanceOf(TermNode.class);
  }

  @Test
  public void visitStatementTest_withFilters() {
    val root = PqlBuilders.search().filter(FilterBuilders.eq("id", 1)).build();
    val result = visit(root);

    assertThat(result.childrenCount()).isEqualTo(1);
    val queryNode = (QueryNode) result.getFirstChild();
    assertThat(queryNode.childrenCount()).isEqualTo(1);

    val filterNode = (FilterNode) queryNode.getFirstChild();
    assertThat(queryNode.childrenCount()).isEqualTo(1);

    val termNode = (TermNode) filterNode.getFirstChild();
    assertThat(termNode.getNameNode().getValue()).isEqualTo("_mutation_id");
    assertThat(termNode.getValueNode().getValue()).isEqualTo(1);
  }

  @Test
  public void visitStatementTest_multiSelect() {
    val result = visit(parse("select(id),select(transcripts)"));
    assertThat(result.childrenCount()).isEqualTo(1);
    val fieldsNode = (FieldsNode) result.getFirstChild();
    assertThat(fieldsNode.getFields()).containsOnly("_mutation_id", "transcript");
  }

  private ExpressionNode visit(PqlNode pqlAst) {
    return pqlAst.accept(visitor, Optional.of(IndexModel.getMutationCentricTypeModel()));
  }

}
