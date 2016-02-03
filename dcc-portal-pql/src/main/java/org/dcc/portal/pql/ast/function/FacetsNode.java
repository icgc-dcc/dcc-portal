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

import java.util.List;
import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import org.dcc.portal.pql.ast.PqlNode;
import org.dcc.portal.pql.ast.Type;
import org.dcc.portal.pql.ast.visitor.PqlNodeVisitor;

@Value
@EqualsAndHashCode(callSuper = false)
public class FacetsNode extends PqlNode {

  public static final String ALL_FACETS = "*";

  FacetsNode(@NonNull List<String> facets) {
    this.facets = facets;
  }

  @NonNull
  List<String> facets;

  @Override
  public Type type() {
    return Type.FACETS;
  }

  @Override
  public <T, A> T accept(@NonNull PqlNodeVisitor<T, A> visitor, @NonNull Optional<A> context) {
    return visitor.visitFacets(this, context);
  }

  @Override
  public FacetsNode toFacetsNode() {
    return this;
  }

}
