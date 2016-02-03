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
package org.dcc.portal.pql.es.visitor.aggs;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import java.util.Optional;

import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.ExistsNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.MissingNode;
import org.dcc.portal.pql.es.ast.filter.MustBoolNode;
import org.dcc.portal.pql.es.ast.filter.NotNode;
import org.dcc.portal.pql.es.ast.filter.RangeNode;
import org.dcc.portal.pql.es.ast.filter.ShouldBoolNode;
import org.dcc.portal.pql.es.ast.filter.TermNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.es.utils.VisitorHelpers;
import org.dcc.portal.pql.es.visitor.NodeVisitor;

/**
 * Checks if the visited filter AST has filter at some particular level. The AST must be processed by the
 * {@link ResolveNestedFilterVisitor} first.
 */
public class VerifyNestedFilterVisitor extends NodeVisitor<Boolean, String> {

  @Override
  public Boolean visitFilter(@NonNull FilterNode node, @NonNull Optional<String> context) {
    return visitChildren(node, context);
  }

  @Override
  public Boolean visitBool(@NonNull BoolNode node, @NonNull Optional<String> context) {
    return visitChildren(node, context);
  }

  @Override
  public Boolean visitMustBool(@NonNull MustBoolNode node, @NonNull Optional<String> context) {
    return visitChildren(node, context);
  }

  @Override
  public Boolean visitShouldBool(ShouldBoolNode node, Optional<String> context) {
    return visitChildren(node, context);
  }

  @Override
  public Boolean visitTerm(TermNode node, Optional<String> context) {
    return false;
  }

  @Override
  public Boolean visitNot(NotNode node, Optional<String> context) {
    return visitChildren(node, context);
  }

  @Override
  public Boolean visitRange(RangeNode node, Optional<String> context) {
    return false;
  }

  @Override
  public Boolean visitTerms(TermsNode node, Optional<String> context) {
    return false;
  }

  @Override
  public Boolean visitExists(ExistsNode node, Optional<String> context) {
    return false;
  }

  @Override
  public Boolean visitMissing(MissingNode node, Optional<String> context) {
    return false;
  }

  @Override
  public Boolean visitNested(NestedNode node, Optional<String> context) {
    VisitorHelpers.checkOptional(context);
    val searchPath = context.get();
    val nodePath = node.getPath();
    if (isParentNesting(searchPath, nodePath)) {
      return visitChildren(node, context);
    }

    if (searchPath.equals(nodePath)) {
      return true;
    }

    return false;
  }

  private boolean isParentNesting(String searchPath, String nodePath) {
    return !searchPath.equals(nodePath) && searchPath.startsWith(nodePath);
  }

  private Boolean visitChildren(ExpressionNode node, Optional<String> context) {
    for (val child : node.getChildren()) {
      if (child.accept(this, context)) {
        return TRUE;
      }
    }

    return FALSE;
  }

}
