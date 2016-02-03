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

public interface ConvertiblePqlNode {

  default StatementNode toStatementNode() {
    return defaultImplementation();
  }

  default AndNode toAndNode() {
    return defaultImplementation();
  }

  default EqNode toEqNode() {
    return defaultImplementation();
  }

  default ExistsNode toExistsNode() {
    return defaultImplementation();
  }

  default GeNode toGeNode() {
    return defaultImplementation();
  }

  default GtNode toGtNode() {
    return defaultImplementation();
  }

  default InNode toInNode() {
    return defaultImplementation();
  }

  default LeNode toLeNode() {
    return defaultImplementation();
  }

  default LtNode toLtNode() {
    return defaultImplementation();
  }

  default MissingNode toMissingNode() {
    return defaultImplementation();
  }

  default NeNode toNeNode() {
    return defaultImplementation();
  }

  default NestedNode toNestedNode() {
    return defaultImplementation();
  }

  default NotNode toNotNode() {
    return defaultImplementation();
  }

  default CountNode toCountNode() {
    return defaultImplementation();
  }

  default FacetsNode toFacetsNode() {
    return defaultImplementation();
  }

  default LimitNode toLimitNode() {
    return defaultImplementation();
  }

  default SelectNode toSelectNode() {
    return defaultImplementation();
  }

  default SortNode toSortNode() {
    return defaultImplementation();
  }

  default OrNode toOrNode() {
    return defaultImplementation();
  }

  static <T> T defaultImplementation() {
    throw new IllegalStateException("This PQL node can't be casted to the target node.");
  }

}
