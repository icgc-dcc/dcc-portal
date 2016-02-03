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
package org.dcc.portal.pql.es.visitor;

import java.util.Optional;

import org.dcc.portal.pql.es.ast.CountNode;
import org.dcc.portal.pql.es.ast.FieldsNode;
import org.dcc.portal.pql.es.ast.LimitNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.SortNode;
import org.dcc.portal.pql.es.ast.SourceNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.FilterAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.MissingAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.NestedAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.ReverseNestedAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.ExistsNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.GreaterEqualNode;
import org.dcc.portal.pql.es.ast.filter.GreaterThanNode;
import org.dcc.portal.pql.es.ast.filter.LessEqualNode;
import org.dcc.portal.pql.es.ast.filter.LessThanNode;
import org.dcc.portal.pql.es.ast.filter.MissingNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.NotNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.es.ast.filter.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.es.ast.query.FunctionScoreNode;
import org.dcc.portal.pql.es.ast.query.QueryNode;

public abstract class NodeVisitor<T, A> {

  private static final String DEFAULT_ERROR_MESSAGE = "The method is not implemented by the subclass";

  public T visitShouldBool(ShouldBoolNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitCount(CountNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitFilter(FilterNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitNested(NestedNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitBool(BoolNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitMustBool(MustBoolNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitTerm(TermNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitTerms(TermsNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitNot(NotNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitRoot(RootNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitSort(SortNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitTerminal(TerminalNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitRange(RangeNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitGreaterEqual(GreaterEqualNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitGreaterThan(GreaterThanNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitLessEqual(LessEqualNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitLessThan(LessThanNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitLimit(LimitNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitFields(FieldsNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitSource(SourceNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitQuery(QueryNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitAggregations(AggregationsNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitNestedAggregation(NestedAggregationNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitTermsAggregation(TermsAggregationNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitMissingAggregation(MissingAggregationNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitFilterAggregation(FilterAggregationNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitReverseNestedAggregation(ReverseNestedAggregationNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitFunctionScore(FunctionScoreNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitExists(ExistsNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  public T visitMissing(MissingNode node, Optional<A> context) {
    return defaultUnimplementedMethod();
  }

  private T defaultUnimplementedMethod() {
    throw new UnsupportedOperationException(DEFAULT_ERROR_MESSAGE);
  }

}
