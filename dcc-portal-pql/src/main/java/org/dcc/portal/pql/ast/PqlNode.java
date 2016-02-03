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
package org.dcc.portal.pql.ast;

import static org.dcc.portal.pql.ast.visitor.Visitors.createPqlStringVisitor;

import java.util.List;
import java.util.Optional;

import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.ast.visitor.PqlNodeVisitor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public abstract class PqlNode implements ConvertiblePqlNode {

  private PqlNode parent;
  private final List<PqlNode> children = Lists.newArrayList();

  public PqlNode(@NonNull List<PqlNode> children) {
    setParent(children);
    this.children.addAll(children);
  }

  public PqlNode(PqlNode... children) {
    val resultBuilder = ImmutableList.<PqlNode> builder();
    resultBuilder.add(children);
    val result = resultBuilder.build();

    setParent(result);
    this.children.addAll(result);
  }

  /**
   * By implementing this method each subclass indicates what type of node it is.
   */
  public abstract Type type();

  public abstract <T, A> T accept(PqlNodeVisitor<T, A> visitor, Optional<A> context);

  public void addChildren(@NonNull PqlNode... children) {
    val resultBuilder = ImmutableList.<PqlNode> builder();
    resultBuilder.add(children);
    val result = resultBuilder.build();

    setParent(result);
    this.children.addAll(result);
  }

  public void addChildren(@NonNull List<PqlNode> children) {
    setParent(children);
    this.children.addAll(children);
  }

  public void removeChild(@NonNull PqlNode child) {
    this.children.remove(child);
  }

  public List<PqlNode> getChildren() {
    return children;
  }

  public int childrenCount() {
    return children.size();
  }

  public PqlNode getChild(int index) {
    return children.get(index);
  }

  public PqlNode getFirstChild() {
    return children.get(0);
  }

  public PqlNode getParent() {
    return parent;
  }

  public void setParent(@NonNull PqlNode parent) {
    this.parent = parent;
  }

  public void removeParent() {
    this.parent = null;
  }

  private void setParent(List<PqlNode> children) {
    for (val child : children) {
      child.parent = this;
    }
  }

  @Override
  public String toString() {
    return accept(createPqlStringVisitor(), Optional.empty());
  }

}
