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
import static org.dcc.portal.pql.es.utils.Visitors.createNestedFieldsVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createNonNestedFieldsVisitor;

import java.util.Optional;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.NestedNode.ScoreMode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.NotNode;
import org.dcc.portal.pql.es.ast.query.FunctionScoreNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.meta.TypeModel;
import org.dcc.portal.pql.query.QueryContext;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Adds score queries. When initialized accepts an initialized {@link NestedNode} which is different for MutationCentic
 * and the other types.
 */
@Slf4j
public abstract class ScoreQueryVisitor extends NodeVisitor<ExpressionNode, QueryContext> {

  private static final ScoreMode SCORE_MODE = ScoreMode.TOTAL;

  private final NestedNode nestedNode;
  private final String nestingPath;
  private final TypeModel typeModel;

  protected ScoreQueryVisitor(@NonNull NestedNode nestedNode, @NonNull TypeModel typeModel) {
    this.nestedNode = nestedNode;
    this.nestingPath = nestedNode.getPath();
    this.typeModel = typeModel;
  }

  public String getPath() {
    return nestedNode.getPath();
  }

  @Override
  public ExpressionNode visitRoot(@NonNull RootNode node, @NonNull Optional<QueryContext> context) {
    val queryNode = Nodes.getOptionalChild(node, QueryNode.class);
    if (queryNode.isPresent()) {
      queryNode.get().accept(this, context);
    } else {
      node.addChildren(createQueryNode(nestedNode));
    }

    return node;
  }

  @Override
  public ExpressionNode visitQuery(@NonNull QueryNode queryNode, @NonNull Optional<QueryContext> context) {
    // This method must always get a QueryNode with a FilterNode that represent a filtered query.
    log.debug("Adding scores to QueryNode: \n{}", queryNode);
    val filterNodeOpt = Nodes.getOptionalChild(queryNode, FilterNode.class);
    checkState(filterNodeOpt.isPresent(), "Malformed QueryNode \n%s", queryNode);

    // Do not generate scores for NOT nodes.
    if (filterNodeOpt.get().hasChildren() && filterNodeOpt.get().getFirstChild() instanceof NotNode) {
      return queryNode;
    }

    // Remove all the chilren from the query node (FilterNode only). Add filters with non-nested fields
    // as well a fields nested under the nestingPath
    // NB: By 'non-nested' fields we mean fields that are not nested under the nesting path of the scoring node
    // E.g. nesting path for a MutationCentric type is 'ssm_occurrence'. Fields nested under 'transcript' are non-nested
    // in this context.
    queryNode.removeAllChildren();

    // Preparing filters with non-nested fields.
    val scoreQueryContext = Optional.of(new ScoreQueryContext(nestingPath, typeModel));
    ExpressionNode filtersClone = Nodes.cloneNode(filterNodeOpt.get());

    // Returns a FilterNode with filters non-nested under the path.
    val nonNestedChildren = filtersClone.accept(createNonNestedFieldsVisitor(), scoreQueryContext).get();

    val mustNode = new MustBoolNode();
    mustNode.addChildren(nonNestedChildren);
    log.debug("Added non nested filters \n{}", nonNestedChildren);

    // Preparing filters with nested fields.
    filtersClone = Nodes.cloneNode(filterNodeOpt.get());
    val requestContext = new NestedFieldsVisitor.RequestContext(typeModel, nestedNode);
    val nestedChildren = filtersClone.accept(createNestedFieldsVisitor(), Optional.of(requestContext)).get();

    mustNode.addChildren(nestedChildren);
    log.debug("Added nested filters \n{}", nestedChildren);

    // Result: QueryNode - BoolNode - MustBoolNode - children
    // children are [NestedNode - FunctionScoreNode] and [FilterNode (with non-nested fields)]
    queryNode.addChildren(new BoolNode(mustNode));

    return queryNode;
  }

  private static ExpressionNode createQueryNode(ExpressionNode nestedNode) {
    val nestedNodeClone = Nodes.cloneNode(nestedNode);

    return new QueryNode(nestedNodeClone);
  }

  static NestedNode createdFunctionScoreNestedNode(String script, String path) {
    return createNodesStructure(script, path);
  }

  private static NestedNode createNodesStructure(String script, String path, ExpressionNode... children) {
    val functionScoreQueryNode = new FunctionScoreNode(script, children);
    val nestedNode = new NestedNode(path, SCORE_MODE, functionScoreQueryNode);

    return nestedNode;
  }

}
