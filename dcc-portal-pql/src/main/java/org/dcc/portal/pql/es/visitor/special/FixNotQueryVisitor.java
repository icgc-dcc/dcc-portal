/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import java.util.Optional;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.NotNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.utils.Visitors;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.query.QueryContext;

import lombok.NonNull;
import lombok.val;

/**
 * DCC-5113 This visitor is for when we have a query made up of only a NOT node. Since we don't score on NOTs, we need
 * to generate a match all query with scoring in order to generate scores for all returned hits.
 */
public class FixNotQueryVisitor extends NodeVisitor<ExpressionNode, QueryContext> {

  @Override
  public ExpressionNode visitRoot(@NonNull RootNode node, Optional<QueryContext> context) {
    val optionalChild = Nodes.getOptionalChild(node, QueryNode.class);
    optionalChild.ifPresent(queryNode -> queryNode.accept(this, context));

    return node;
  }

  @Override
  public ExpressionNode visitQuery(QueryNode node, Optional<QueryContext> context) {
    val filterNode = Nodes.getOptionalChild(node, FilterNode.class);

    if (filterNode.isPresent()) {
      val bool = filterNode.get().accept(this, context);
      // We want to replace the filter node with a bool node. Filter node is guaranteed to be fist.
      node.setChild(0, bool);
    }

    return node;
  }

  @Override
  public ExpressionNode visitFilter(FilterNode node, Optional<QueryContext> context) {
    val child = node.getFirstChild();

    if (child instanceof NotNode) {
      val notNode = (NotNode) child;
      val bool = notNode.accept(this, context);

      return bool;
    }

    return node;
  }

  @Override
  public ExpressionNode visitNot(NotNode node, Optional<QueryContext> context) {
    // This is guaranteed to be a Nested as it is the default empty query.
    val scoreRoot = createScore(context);

    // Either we are scoring with a script on this, or we are not.
    if (scoreRoot.hasChildren() && scoreRoot.getFirstChild().hasChildren()) {
      val res = (NestedNode) scoreRoot.getFirstChild().getFirstChild();

      // We don't care what the inner not query looks like, we just want to score along side it.
      val boolNode = new BoolNode(new MustBoolNode(res, node));
      return boolNode;
    }

    return node;
  }

  private ExpressionNode createScore(Optional<QueryContext> context) {
    val scoreQueryVisitor = Visitors.createScoreQueryVisitor(context.get().getType());
    return scoreQueryVisitor.visitRoot(new RootNode(new ExpressionNode[0]), context);
  }

}
