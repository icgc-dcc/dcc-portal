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
package org.dcc.portal.pql.es.utils;

import static com.google.common.collect.Iterables.filter;
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.Optional;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.visitor.NodeVisitor;

import com.google.common.collect.Lists;

@NoArgsConstructor(access = PRIVATE)
public final class Nodes {

  private static final NodeVisitor<ExpressionNode, Void> CLONE_VISITOR = Visitors.createCloneNodeVisitor();

  public static <T> List<T> filterChildren(@NonNull ExpressionNode node, @NonNull Class<T> childType) {
    val children = filter(node.getChildren(), childType);

    return Lists.<T> newArrayList(children);
  }

  /**
   * Get an {@link Optional} of type {@code childType}. Ensures that the {@code parent} has only a single child of that
   * type if any.
   */
  public static <T extends ExpressionNode> Optional<T> getOptionalChild(@NonNull ExpressionNode parent,
      @NonNull Class<T> childType) {

    val childrenList = filterChildren(parent, childType);
    if (childrenList.isEmpty()) {
      return Optional.empty();
    } else if (childrenList.size() > 1) {
      throw new IllegalStateException(format("Node contains more that one child of type %s. %s", childType, parent));
    }

    return Optional.ofNullable(childrenList.get(0));
  }

  public static ExpressionNode cloneNode(@NonNull ExpressionNode original) {
    return original.accept(CLONE_VISITOR, Optional.empty());
  }

  @SuppressWarnings("unchecked")
  public static <T extends ExpressionNode> Optional<T> findParent(@NonNull ExpressionNode node, @NonNull Class<T> type) {
    if (type.isInstance(node)) {
      node = node.getParent();
    }

    while (node != null && !type.isInstance(node)) {
      node = node.getParent();
    }

    return node == null ? Optional.empty() : Optional.of((T) node);
  }

}
