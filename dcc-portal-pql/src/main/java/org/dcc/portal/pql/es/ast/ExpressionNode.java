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
package org.dcc.portal.pql.es.ast;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;

import org.dcc.portal.pql.es.utils.Visitors;
import org.dcc.portal.pql.es.visitor.NodeVisitor;

import com.google.common.collect.Lists;

@Getter
@EqualsAndHashCode(exclude = { "parent", "nodeName" })
public abstract class ExpressionNode {

  @Setter
  protected ExpressionNode parent;
  protected List<ExpressionNode> children;

  @Getter(lazy = true)
  private final String nodeName = getClass().getSimpleName();

  public ExpressionNode(ExpressionNode... children) {
    this(Lists.newArrayList(children));
  }

  public ExpressionNode(Iterable<ExpressionNode> children) {
    this(Lists.newArrayList(children));
  }

  private ExpressionNode(List<ExpressionNode> children) {
    this.children = children;
    assignParent(this.children, this);
  }

  private static void assignParent(Collection<ExpressionNode> children, ExpressionNode parent) {
    for (val child : children) {
      child.setParent(parent);
    }
  }

  public abstract <T, A> T accept(NodeVisitor<T, A> visitor, Optional<A> context);

  public int childrenCount() {
    return children.size();
  }

  public boolean hasChildren() {
    return childrenCount() != 0;
  }

  public ExpressionNode[] getChildrenArray() {
    return children.toArray(new ExpressionNode[childrenCount()]);
  }

  public void addChildren(@NonNull ExpressionNode... children) {
    val childrenList = Lists.newArrayList(children);
    this.children.addAll(childrenList);
    assignParent(childrenList, this);
  }

  public void addChildren(@NonNull List<ExpressionNode> children) {
    addChildren(children.toArray(new ExpressionNode[children.size()]));
  }

  public ExpressionNode getChild(int index) {
    return children.get(index);
  }

  public ExpressionNode getFirstChild() {
    return children.get(0);
  }

  public void removeChild(int index) {
    checkChildrenBoundaries(index);
    children.remove(index);
  }

  public void removeAllChildren() {
    for (int i = children.size() - 1; i >= 0; i--) {
      children.remove(i);
    }
  }

  public void setChild(int index, @NonNull ExpressionNode newChild) {
    checkChildrenBoundaries(index);
    val oldChild = children.get(index);
    oldChild.parent = null;
    children.set(index, newChild);
    newChild.parent = this;
  }

  @Override
  public String toString() {
    return accept(Visitors.createToStringVisitor(), Optional.<Void> empty());
  }

  public boolean hasNestedParent() {
    return parent.hasNestedParent();
  }

  private void checkChildrenBoundaries(int index) {
    checkArgument(index >= 0 && index < childrenCount(),
        format("Index %d is out of children bounds. Children count: %d", index, childrenCount()));
  }

}
