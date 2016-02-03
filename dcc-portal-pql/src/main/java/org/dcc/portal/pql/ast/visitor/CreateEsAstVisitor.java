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

import static org.dcc.portal.pql.query.ParseTreeVisitors.getField;

import java.util.List;
import java.util.Optional;

import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.ast.PqlNode;
import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.ast.filter.AndNode;
import org.dcc.portal.pql.ast.filter.EqNode;
import org.dcc.portal.pql.ast.filter.EqualityFilterNode;
import org.dcc.portal.pql.ast.filter.ExistsNode;
import org.dcc.portal.pql.ast.filter.FilterNode;
import org.dcc.portal.pql.ast.filter.GeNode;
import org.dcc.portal.pql.ast.filter.GtNode;
import org.dcc.portal.pql.ast.filter.InNode;
import org.dcc.portal.pql.ast.filter.LeNode;
import org.dcc.portal.pql.ast.filter.LtNode;
import org.dcc.portal.pql.ast.filter.MissingNode;
import org.dcc.portal.pql.ast.filter.NeNode;
import org.dcc.portal.pql.ast.filter.NestedNode;
import org.dcc.portal.pql.ast.filter.NotNode;
import org.dcc.portal.pql.ast.filter.OrNode;
import org.dcc.portal.pql.ast.function.CountNode;
import org.dcc.portal.pql.ast.function.FacetsNode;
import org.dcc.portal.pql.ast.function.LimitNode;
import org.dcc.portal.pql.ast.function.SelectNode;
import org.dcc.portal.pql.ast.function.SortNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.FieldsNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.GreaterEqualNode;
import org.dcc.portal.pql.es.ast.filter.GreaterThanNode;
import org.dcc.portal.pql.es.ast.filter.LessEqualNode;
import org.dcc.portal.pql.es.ast.filter.LessThanNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.es.ast.filter.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.meta.TypeModel;

import com.google.common.collect.Lists;

public class CreateEsAstVisitor extends PqlNodeVisitor<ExpressionNode, TypeModel> {

  @Override
  public ExpressionNode visitStatement(@NonNull StatementNode node, @NonNull Optional<TypeModel> context) {
    val result = new RootNode();
    val fields = Lists.<ExpressionNode> newArrayList();

    for (val child : node.getChildren()) {
      val childVisitResult = child.accept(this, context);
      if (child instanceof SelectNode) {
        fields.add(childVisitResult);
      } else {
        result.addChildren(encloseFilters(child, childVisitResult));
      }
    }

    if (!fields.isEmpty()) {
      result.addChildren(resolveFields(fields));
    }

    return result;
  }

  @Override
  public ExpressionNode visitEq(@NonNull EqNode node, @NonNull Optional<TypeModel> context) {
    return new TermNode(_getField(node, context), node.getValue());
  }

  @Override
  public ExpressionNode visitSelect(@NonNull SelectNode node, @NonNull Optional<TypeModel> context) {
    val typeModel = context.get();
    if (isSelectAll(node.getFields())) {
      return new FieldsNode(typeModel.getFields());
    }

    val result = new FieldsNode();
    for (val field : node.getFields()) {
      result.addField(getField(field, typeModel));
    }

    return result;
  }

  @Override
  public ExpressionNode visitFacets(@NonNull FacetsNode node, @NonNull Optional<TypeModel> context) {
    val typeModel = context.get();
    val result = new AggregationsNode();
    List<String> facetNames = null;

    if (isSelectAll(node.getFacets())) {
      facetNames = typeModel.getFacets();
    } else {
      facetNames = node.getFacets();
    }

    for (val facetName : facetNames) {
      result.addChildren(new TermsAggregationNode(facetName, getField(facetName, typeModel)));
    }

    return result;
  }

  @Override
  public ExpressionNode visitLimit(@NonNull LimitNode node, @NonNull Optional<TypeModel> context) {
    return new org.dcc.portal.pql.es.ast.LimitNode(node.getFrom(), node.getSize());
  }

  @Override
  public ExpressionNode visitSort(@NonNull SortNode node, @NonNull Optional<TypeModel> context) {
    val typeModel = context.get();
    val result = new org.dcc.portal.pql.es.ast.SortNode();
    for (val entry : node.getFields().entrySet()) {
      result.addField(getField(entry.getKey(), typeModel), entry.getValue());
    }

    return result;
  }

  @Override
  public ExpressionNode visitCount(@NonNull CountNode node, @NonNull Optional<TypeModel> context) {
    return new org.dcc.portal.pql.es.ast.CountNode();
  }

  @Override
  public ExpressionNode visitNe(@NonNull NeNode node, @NonNull Optional<TypeModel> context) {
    return new org.dcc.portal.pql.es.ast.filter.NotNode(new TermNode(_getField(node, context), node.getValue()));
  }

  @Override
  public ExpressionNode visitGe(@NonNull GeNode node, @NonNull Optional<TypeModel> context) {
    return new RangeNode(_getField(node, context), new GreaterEqualNode(node.getValue()));
  }

  @Override
  public ExpressionNode visitGt(@NonNull GtNode node, @NonNull Optional<TypeModel> context) {
    return new RangeNode(_getField(node, context), new GreaterThanNode(node.getValue()));
  }

  @Override
  public ExpressionNode visitLe(@NonNull LeNode node, @NonNull Optional<TypeModel> context) {
    return new RangeNode(_getField(node, context), new LessEqualNode(node.getValue()));
  }

  @Override
  public ExpressionNode visitLt(@NonNull LtNode node, @NonNull Optional<TypeModel> context) {
    return new RangeNode(_getField(node, context), new LessThanNode(node.getValue()));
  }

  @Override
  public ExpressionNode visitNot(@NonNull NotNode node, @NonNull Optional<TypeModel> context) {
    return new org.dcc.portal.pql.es.ast.filter.NotNode(node.getFirstChild().accept(this, context));
  }

  @Override
  public ExpressionNode visitExists(@NonNull ExistsNode node, @NonNull Optional<TypeModel> context) {
    return new org.dcc.portal.pql.es.ast.filter.ExistsNode(_getField(node, context));
  }

  @Override
  public ExpressionNode visitMissing(@NonNull MissingNode node, @NonNull Optional<TypeModel> context) {
    return new org.dcc.portal.pql.es.ast.filter.MissingNode(_getField(node, context));
  }

  @Override
  public ExpressionNode visitIn(@NonNull InNode node, @NonNull Optional<TypeModel> context) {
    val children = Lists.<TerminalNode> newArrayList();
    for (val value : node.getValues()) {
      children.add(new TerminalNode(value));
    }

    return new TermsNode(getField(node.getField(), context.get()), children.toArray(new TerminalNode[children.size()]));
  }

  @Override
  public ExpressionNode visitAnd(@NonNull AndNode node, @NonNull Optional<TypeModel> context) {
    val result = new MustBoolNode();
    for (val child : node.getChildren()) {
      result.addChildren(child.accept(this, context));
    }

    return new BoolNode(result);
  }

  @Override
  public ExpressionNode visitOr(@NonNull OrNode node, @NonNull Optional<TypeModel> context) {
    val result = new ShouldBoolNode();
    for (val child : node.getChildren()) {
      result.addChildren(child.accept(this, context));
    }

    return new BoolNode(result);
  }

  @Override
  public ExpressionNode visitNested(@NonNull NestedNode node, @NonNull Optional<TypeModel> context) {
    val mustNode = new MustBoolNode();
    val result = new org.dcc.portal.pql.es.ast.NestedNode(node.getPath(), new BoolNode(mustNode));
    for (val child : node.getChildren()) {
      mustNode.addChildren(child.accept(this, context));
    }

    return result;
  }

  private static ExpressionNode encloseFilters(PqlNode child, ExpressionNode childVisitResult) {
    if (child instanceof FilterNode) {
      return new QueryNode(new org.dcc.portal.pql.es.ast.filter.FilterNode(childVisitResult));
    }

    return childVisitResult;
  }

  private static String _getField(EqualityFilterNode node, Optional<TypeModel> context) {
    return getField(node.getField(), context.get());
  }

  private boolean isSelectAll(List<String> fields) {
    return fields.size() == 1 && fields.contains(SelectNode.ALL_FIELDS);
  }

  private static ExpressionNode resolveFields(List<ExpressionNode> fields) {
    val result = fields.get(0);
    if (fields.size() == 1) {
      return result;
    }

    for (int i = 1; i < fields.size(); i++) {
      result.addChildren(fields.get(i).getChildrenArray());
    }

    return result;
  }

}
