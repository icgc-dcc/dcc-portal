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

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.ast.filter.FilterNode;
import org.dcc.portal.pql.ast.function.CountNode;
import org.dcc.portal.pql.ast.function.FacetsNode;
import org.dcc.portal.pql.ast.function.LimitNode;
import org.dcc.portal.pql.ast.function.SelectNode;
import org.dcc.portal.pql.ast.function.SortNode;
import org.dcc.portal.pql.ast.visitor.PqlNodeVisitor;

import com.google.common.collect.Lists;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class StatementNode extends PqlNode {

  private List<SelectNode> select = Lists.<SelectNode> newArrayList();
  private List<FacetsNode> facets = Lists.<FacetsNode> newArrayList();
  private CountNode count;
  private SortNode sort;
  private LimitNode limit;
  private FilterNode filters;

  public StatementNode(@NonNull CountNode count) {
    super(count);
    this.count = count;
  }

  public boolean hasSelect() {
    return !select.isEmpty();
  }

  public void addSelect(@NonNull SelectNode node) {
    canUpdateField();
    addChildren(node);
  }

  public void setSelect(@NonNull SelectNode node) {
    canUpdateField();

    for (val selectNode : select) {
      selectNode.removeParent();
      removeChild(selectNode);
    }

    select.clear();
    addChildren(node);
  }

  public boolean hasFacets() {
    return !facets.isEmpty();
  }

  public void setFacets(@NonNull FacetsNode node) {
    addChildren(node);
  }

  public void setFacets(@NonNull Collection<FacetsNode> facetsNodes) {
    addChildren(facetsNodes);
  }

  public boolean isCount() {
    return count != null;
  }

  public boolean hasSort() {
    return sort != null;
  }

  public void setSort(@NonNull SortNode node) {
    canUpdateField();

    if (sort != null) {
      sort.removeParent();
      removeChild(sort);
    }
    addChildren(node);
  }

  public boolean hasLimit() {
    return limit != null;
  }

  public void setLimit(@NonNull LimitNode node) {
    canUpdateField();

    if (limit != null) {
      limit.removeParent();
      removeChild(limit);
    }
    addChildren(node);
  }

  public boolean hasFilters() {
    return filters != null;
  }

  public void setFilters(@NonNull FilterNode node) {
    if (filters != null) {
      filters.removeParent();
      removeChild(filters);
    }

    filters = node;
    node.setParent(this);
    addChildren(node);
  }

  @Override
  public Type type() {
    return Type.ROOT;
  }

  @Override
  public <T, A> T accept(@NonNull PqlNodeVisitor<T, A> visitor, @NonNull Optional<A> context) {
    return visitor.visitStatement(this, context);
  }

  @Override
  public StatementNode toStatementNode() {
    return this;
  }

  public void addChildren(@NonNull Collection<? extends PqlNode> children) {
    addChildren(children.stream().toArray(PqlNode[]::new));
  }

  @Override
  public void addChildren(@NonNull PqlNode... children) {
    super.addChildren(children);

    for (val child : children) {
      switch (child.type()) {
      case SELECT:
        this.select.add(child.toSelectNode());
        break;
      case FACETS:
        this.facets.add(child.toFacetsNode());
        break;
      case LIMIT:
        this.limit = child.toLimitNode();
        break;
      case COUNT:
        this.count = child.toCountNode();
        break;
      case SORT:
        this.sort = child.toSortNode();
        break;
      default:
        checkState(child instanceof FilterNode);
        this.filters = (FilterNode) child;
        break;
      }
    }
  }

  private void canUpdateField() {
    if (isCount()) {
      throw new IllegalArgumentException("This operation is not valid for a count request");
    }
  }

}
