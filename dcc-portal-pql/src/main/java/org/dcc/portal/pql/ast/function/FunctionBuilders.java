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
package org.dcc.portal.pql.ast.function;

import static com.google.common.collect.ImmutableList.copyOf;
import static lombok.AccessLevel.PRIVATE;
import static org.dcc.portal.pql.ast.function.FacetsNode.ALL_FACETS;
import static org.dcc.portal.pql.ast.function.SelectNode.ALL_FIELDS;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.dcc.portal.pql.ast.function.SortNode.SortNodeBuilder;

import com.google.common.collect.ImmutableList;

@NoArgsConstructor(access = PRIVATE)
public final class FunctionBuilders {

  public static SelectNode select(@NonNull ImmutableList<String> fields) {
    return new SelectNode(fields);
  }

  public static SelectNode select(@NonNull String... fields) {
    return new SelectNode(copyOf(fields));
  }

  public static SelectNode selectAll() {
    return select(ALL_FIELDS);
  }

  public static FacetsNode facets(@NonNull ImmutableList<String> facets) {
    return new FacetsNode(facets);
  }

  public static FacetsNode facets(@NonNull String... facets) {
    return new FacetsNode(copyOf(facets));
  }

  public static FacetsNode facetsAll() {
    return facets(ALL_FACETS);
  }

  public static LimitNode limit(int size) {
    return limit(0, size);
  }

  public static LimitNode limit(int from, int size) {
    return new LimitNode(from, size);
  }

  public static SortNodeBuilder sortBuilder() {
    return SortNode.builder();
  }

  public static CountNode count() {
    return new CountNode();
  }

}
