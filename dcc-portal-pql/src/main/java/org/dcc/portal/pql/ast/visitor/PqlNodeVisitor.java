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
package org.dcc.portal.pql.ast.visitor;

import java.util.Optional;

import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.ast.filter.AndNode;
import org.dcc.portal.pql.ast.filter.EqNode;
import org.dcc.portal.pql.ast.filter.ExistsNode;
import org.dcc.portal.pql.ast.filter.GeNode;
import org.dcc.portal.pql.ast.filter.GtNode;
import org.dcc.portal.pql.ast.filter.InNode;
import org.dcc.portal.pql.ast.filter.LeNode;
import org.dcc.portal.pql.ast.filter.LtNode;
import org.dcc.portal.pql.ast.filter.MissingNode;
import org.dcc.portal.pql.ast.filter.NeNode;
import org.dcc.portal.pql.ast.filter.NestedNode;
import org.dcc.portal.pql.ast.filter.NotNode;
import org.dcc.portal.pql.ast.filter.OrNode;
import org.dcc.portal.pql.ast.function.CountNode;
import org.dcc.portal.pql.ast.function.FacetsNode;
import org.dcc.portal.pql.ast.function.LimitNode;
import org.dcc.portal.pql.ast.function.SelectNode;
import org.dcc.portal.pql.ast.function.SortNode;

public abstract class PqlNodeVisitor<T, A> {

  public T visitStatement(StatementNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitEq(EqNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitAnd(AndNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitExists(ExistsNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitGe(GeNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitGt(GtNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitLe(LeNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitLt(LtNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitIn(InNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitMissing(MissingNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitNe(NeNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitNested(NestedNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitNot(NotNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitOr(OrNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitCount(CountNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitFacets(FacetsNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitLimit(LimitNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitSelect(SelectNode node, Optional<A> context) {
    return defaultImplementation();
  }

  public T visitSort(SortNode node, Optional<A> context) {
    return defaultImplementation();
  }

  private static <T> T defaultImplementation() {
    throw new UnsupportedOperationException();
  }

}
