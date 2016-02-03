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
package org.dcc.portal.pql.ast.builder;

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.ast.function.FacetsNode;
import org.dcc.portal.pql.ast.function.FunctionBuilders;
import org.dcc.portal.pql.ast.function.LimitNode;
import org.dcc.portal.pql.ast.function.SelectNode;
import org.dcc.portal.pql.ast.function.SortNode.SortNodeBuilder;

@NoArgsConstructor(access = PRIVATE)
public class PqlSearchBuilder {

  private SortNodeBuilder sortNodeBuilder;
  private SelectNode selectNode;
  private FacetsNode facetsNode;
  private LimitNode limit;
  private FilterBuilder filterBuilder;

  public static PqlSearchBuilder statement() {
    return new PqlSearchBuilder();
  }

  public PqlSearchBuilder select(@NonNull String... field) {
    selectNode = FunctionBuilders.select(field);

    return this;
  }

  public PqlSearchBuilder selectAll() {
    selectNode = FunctionBuilders.selectAll();

    return this;
  }

  public PqlSearchBuilder filter(@NonNull FilterBuilder builder) {
    this.filterBuilder = builder;

    return this;
  }

  public PqlSearchBuilder sort(@NonNull SortNodeBuilder builder) {
    this.sortNodeBuilder = builder;

    return this;
  }

  public PqlSearchBuilder limit(int size) {
    this.limit = FunctionBuilders.limit(size);

    return this;
  }

  public PqlSearchBuilder limit(int from, int size) {
    this.limit = FunctionBuilders.limit(from, size);

    return this;
  }

  public PqlSearchBuilder facets(@NonNull String... facets) {
    facetsNode = FunctionBuilders.facets(facets);

    return this;
  }

  public PqlSearchBuilder facetsAll() {
    facetsNode = FunctionBuilders.facetsAll();

    return this;
  }

  public StatementNode build() {
    val result = new StatementNode();

    if (selectNode != null) {
      result.addSelect(selectNode);
    }

    if (limit != null) {
      result.setLimit(limit);
    }

    if (facetsNode != null) {
      result.setFacets(facetsNode);
    }

    if (filterBuilder != null) {
      result.setFilters(filterBuilder.build());
    }

    if (sortNodeBuilder != null) {
      result.setSort(sortNodeBuilder.build());
    }

    return result;
  }

}
