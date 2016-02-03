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
package org.dcc.portal.pql.es.visitor.score;

import static com.google.common.base.Preconditions.checkState;
import static org.dcc.portal.pql.es.utils.VisitorHelpers.checkOptional;

import java.util.Optional;

import lombok.NonNull;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.ExistsNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MissingNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.NotNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.es.ast.filter.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.es.ast.query.FunctionScoreNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.es.visitor.score.NestedFieldsVisitor.RequestContext;
import org.dcc.portal.pql.meta.TypeModel;

import com.google.common.collect.ImmutableList;

/**
 * Returns an AST with filters that are nested under the nesting path. All methods beneath visitFilter return nodes
 * which must be included in the result set.
 */
@Slf4j
public class NestedFieldsVisitor extends NodeVisitor<Optional<ExpressionNode>, RequestContext> {

  @Value
  public static class RequestContext {

    @NonNull
    TypeModel typeModel;
    @NonNull
    NestedNode nestedNode;

  }

  @Override
  public Optional<ExpressionNode> visitFilter(FilterNode node, Optional<RequestContext> context) {
    checkOptional(context);
    log.debug("[visitFilter] Processing \n{}", node);

    // This method assumes that this FilterNode has no FilterNode children otherwise this method will be called for that
    // FilterNode again and will return incorrect results.
    validateFilterNode(node);

    val result = processChildren(node, context);
    if (result.isPresent()) {
      // processChildren() return result of type of the current method, FilterNode in our case. But scoring works only
      // in a query context. Thus, we have to return children of the result(FilterNode).
      checkState(result.get().childrenCount() == 1, "Malfrormed FilterNode. %s", result.get());
      val resultChild = result.get().getFirstChild();
      log.debug("Prepared nested score node with filters: \n{}", resultChild);

      return Optional.of(resultChild);
    }

    // After visiting all the chilren of this FilterNode none of them was added to the final result. Therefore,
    // a NestedNode with FunctionScoreNode child will be returned.
    log.debug("Prepared nested score node with filters: \n{}", context.get().getNestedNode());

    return Optional.of(context.get().getNestedNode());
  }

  /**
   * Checks that the {@code node} has not FilterNode children.
   */
  private static void validateFilterNode(FilterNode node) {
    for (val child : node.getChildren()) {
      validateFilterNodeChild(child);
    }
  }

  private static void validateFilterNodeChild(ExpressionNode node) {
    checkState(!(node instanceof FilterNode), "FilterNode has a FilterNode child.");
    for (val child : node.getChildren()) {
      validateFilterNodeChild(child);
    }
  }

  @Override
  public Optional<ExpressionNode> visitBool(BoolNode node, Optional<RequestContext> context) {
    return processChildren(node, context);
  }

  @Override
  public Optional<ExpressionNode> visitMustBool(MustBoolNode node, Optional<RequestContext> context) {
    return processChildren(node, context);
  }

  @Override
  public Optional<ExpressionNode> visitShouldBool(ShouldBoolNode node, Optional<RequestContext> context) {
    return processChildren(node, context);
  }

  @Override
  public Optional<ExpressionNode> visitNot(NotNode node, Optional<RequestContext> context) {
    return processChildren(node, context);
  }

  @Override
  public Optional<ExpressionNode> visitNested(NestedNode node, Optional<RequestContext> context) {
    log.debug("[visitNested] Path: {}\n{}", node.getPath(), node);
    val scoredNestedNode = context.get().getNestedNode();
    val scoredNestingPath = scoredNestedNode.getPath();

    // This assumes that scoredNesting is first level only. If it's deeper the method will produce wrong results
    if (scoredNestingPath.equals(node.getPath())) {
      checkState(node.childrenCount() == 1, "NestedNode has no children. \n%s", node);
      val childToVisit = node.getFirstChild();

      // Now we need to create a new correct structure so the visited children could detect that they don't need to
      // create another nested scored node
      // NestedNode - ScoreNode - nestedNodeChild
      val nestedClone = Nodes.cloneNode(scoredNestedNode);
      val scoreNode = nestedClone.getFirstChild();
      scoreNode.addChildren(childToVisit);

      // Now visit the children
      val visitChildResult = childToVisit.accept(this, context).get();

      // and replace children of the filterNode with the correct children
      // Wrap visitChildResult in a filterNode for filter caching.
      scoreNode.removeAllChildren();
      scoreNode.addChildren(new FilterNode(visitChildResult));
      log.debug("[visitNested] Path: {}. Result -\n{}", node.getPath(), nestedClone);

      return Optional.of(nestedClone);
    }

    // We could come here because of couple reasons:
    // - node.path - donor, scoredNestingPath - gene
    // - node.path - gene.ssm, scoredNestingPath - gene

    // Case: node.path - donor, scoredNestingPath - gene
    // This node is included by the NonNestedFieldsVisitor
    if (!node.getPath().startsWith(scoredNestingPath)) {
      return Optional.empty();
    }

    // Case: node.path - gene.ssm, globalNestingPath - gene
    if (hasScoring(node)) {
      return Optional.of(node);
    }

    val result = Nodes.cloneNode(scoredNestedNode);
    result.getFirstChild().addChildren(new FilterNode(node));
    log.debug("[visitNested] Path: {}. Result -\n{}", node.getPath(), result);

    return Optional.of(result);
  }

  @Override
  public Optional<ExpressionNode> visitTerm(TermNode node, Optional<RequestContext> context) {
    return processCommonCases(node.getNameNode().getValueAsString(), node, context);
  }

  @Override
  public Optional<ExpressionNode> visitTerms(TermsNode node, Optional<RequestContext> context) {
    return processCommonCases(node.getField(), node, context);
  }

  @Override
  public Optional<ExpressionNode> visitExists(ExistsNode node, Optional<RequestContext> context) {
    return processCommonCases(node.getField(), node, context);
  }

  @Override
  public Optional<ExpressionNode> visitMissing(MissingNode node, Optional<RequestContext> context) {
    return processCommonCases(node.getField(), node, context);
  }

  @Override
  public Optional<ExpressionNode> visitRange(RangeNode node, Optional<RequestContext> context) {
    return processCommonCases(node.getFieldName(), node, context);
  }

  private Optional<ExpressionNode> processCommonCases(String field, ExpressionNode node,
      Optional<RequestContext> context) {
    if (!isInclude(field, context)) {
      return Optional.empty();
    }

    if (hasScoring(node)) {
      // Already in a scored nested node
      return Optional.of(node);
    }

    val nestedNodeClone = Nodes.cloneNode(context.get().getNestedNode());
    val enclosedNode = encloseInNestedNode(field, node, context.get());

    // Enclose in a FilterNode to create a filtered query
    nestedNodeClone.getFirstChild().addChildren(new FilterNode(enclosedNode));

    // Any nested node must be enclosed in a QueryNode. Otherwise, the nested scoring node will be created in a filter
    // context what's incorrect.
    return Optional.of(new QueryNode(nestedNodeClone));
  }

  /**
   * Checks if the {@code node} is already enclosed in a NestedNode with scoring.
   */
  private boolean hasScoring(ExpressionNode node) {
    val scoreNode = Nodes.findParent(node, FunctionScoreNode.class);

    return scoreNode.isPresent();
  }

  /**
   * Encloses this {@code node} in a NestedNode if this {@code node} in nested deeper than the 'global nested node'
   */
  private ExpressionNode encloseInNestedNode(String field, ExpressionNode node, RequestContext requestContext) {
    val nodeNestedPath = requestContext.getTypeModel().getNestedPath(field);
    val globalNestedPath = requestContext.getNestedNode().getPath();

    return nodeNestedPath.equals(globalNestedPath) ? node : new NestedNode(nodeNestedPath, node);
  }

  private static boolean isInclude(String fieldName, Optional<RequestContext> context) {
    val nestingPath = context.get().getNestedNode().getPath();
    val typeModel = context.get().getTypeModel();

    return typeModel.isNested(fieldName, nestingPath);
  }

  /**
   * Visits children of the {@code parent}. If they should be included in the result adds them instead of the original
   * children.
   */
  private Optional<ExpressionNode> processChildren(ExpressionNode parent, Optional<RequestContext> context) {
    val childrenToInclude = new ImmutableList.Builder<ExpressionNode>();

    for (val child : parent.getChildren()) {
      val visitResult = child.accept(this, context);
      if (visitResult.isPresent()) {
        childrenToInclude.add(visitResult.get());
      }
    }

    parent.removeAllChildren();
    parent.addChildren(childrenToInclude.build());

    return parent.hasChildren() ? Optional.of(parent) : Optional.empty();
  }

}
