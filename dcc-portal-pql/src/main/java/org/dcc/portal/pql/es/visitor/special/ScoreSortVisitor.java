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

import static org.dcc.portal.pql.meta.TypeModel.SCORE;

import java.util.Optional;

import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.SortNode;
import org.dcc.portal.pql.es.model.Order;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.visitor.NodeVisitor;

/**
 * If sorting does not contain field {@code _score} or is not sorted. Add sorting by {@code _score} descending.
 */
public class ScoreSortVisitor extends NodeVisitor<ExpressionNode, Void> {

  @Override
  public ExpressionNode visitRoot(@NonNull RootNode node, Optional<Void> context) {
    val sortNode = Nodes.getOptionalChild(node, SortNode.class);
    if (sortNode.isPresent()) {
      sortNode.get().accept(this, context);
    } else {
      val newSortNode = new SortNode();
      newSortNode.addField(SCORE, Order.DESC);
      node.addChildren(newSortNode);
    }

    return node;
  }

  @Override
  public ExpressionNode visitSort(@NonNull SortNode node, Optional<Void> context) {
    if (!node.getFields().containsKey(SCORE)) {
      node.addField(SCORE, Order.DESC);
    }

    return node;
  }

}
