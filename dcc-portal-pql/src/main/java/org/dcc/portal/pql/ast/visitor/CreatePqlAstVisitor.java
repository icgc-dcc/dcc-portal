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

import static java.lang.Integer.parseInt;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.count;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.facets;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.facetsAll;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.limit;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.select;
import static org.dcc.portal.pql.ast.function.FunctionBuilders.selectAll;
import static org.dcc.portal.pql.query.ParseTreeVisitors.cleanString;
import static org.dcc.portal.pql.query.ParseTreeVisitors.getOrderAt;

import java.util.List;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.dcc.portal.pql.ast.PqlNode;
import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.ast.filter.AndNode;
import org.dcc.portal.pql.ast.filter.EqNode;
import org.dcc.portal.pql.ast.filter.ExistsNode;
import org.dcc.portal.pql.ast.filter.FilterNode;
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
import org.dcc.portal.pql.ast.function.SortNode;
import org.icgc.dcc.portal.pql.antlr4.PqlBaseVisitor;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.AndContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.CountContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.EqContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.EqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.ExistsContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.FacetsContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GreaterEqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GreaterThanContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.GtContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.InArrayContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.InContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LessEqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LessThanContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.LtContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.MissingContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NestedContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NotContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.NotEqualContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.OrContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.OrderContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.RangeContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.SelectContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.StatementContext;
import org.icgc.dcc.portal.pql.antlr4.PqlParser.ValueContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@Slf4j
public class CreatePqlAstVisitor extends PqlBaseVisitor<PqlNode> {

  @Override
  public PqlNode visitStatement(@NonNull StatementContext context) {
    log.debug("Visiting statement context: {}", context.toStringTree());

    val result = new StatementNode();
    val filters = Lists.<PqlNode> newArrayList();
    for (val child : context.children) {
      val visitResult = child.accept(this);
      if (visitResult != null) {
        // Explicit is better than Implicit. Explicitly enclose all top level filters in an AndNode
        if (visitResult instanceof FilterNode) {
          filters.add(visitResult);
        } else {
          result.addChildren(visitResult);
        }
      }
    }

    if (!filters.isEmpty()) {
      result.addChildren(resolveFilters(filters));
    }

    return result;
  }

  @Override
  public PqlNode visitSelect(@NonNull SelectContext context) {
    if (context.ASTERISK() != null) {
      return selectAll();
    }

    return select(resolveValues(context.ID()));
  }

  @Override
  public PqlNode visitFacets(@NonNull FacetsContext context) {
    if (context.ASTERISK() != null) {
      return facetsAll();
    }

    return facets(resolveValues(context.ID()));
  }

  @Override
  public PqlNode visitRange(@NonNull RangeContext context) {
    val values = context.INT();
    if (values.size() == 2) {
      return limit(parseInt(values.get(0).getText()), parseInt(values.get(1).getText()));
    } else {
      return limit(parseInt(values.get(0).getText()));
    }
  }

  @Override
  public PqlNode visitOrder(@NonNull OrderContext context) {
    val result = SortNode.builder();
    for (val id : context.ID()) {
      val order = getOrderAt(context, id.getSymbol().getCharPositionInLine() - 1);
      if (order != null) {
        result.sort(id.getText(), order);
      } else {
        result.sortAsc(id.getText());
      }
    }

    return result.build();
  }

  @Override
  public PqlNode visitCount(@NonNull CountContext context) {
    return count();
  }

  @Override
  public PqlNode visitEqual(@NonNull EqualContext context) {
    return context.eq().accept(this);
  }

  @Override
  public PqlNode visitEq(@NonNull EqContext context) {
    return new EqNode(context.ID().getText(), getValue(context.value()));
  }

  @Override
  public PqlNode visitNotEqual(@NonNull NotEqualContext context) {
    return context.ne().accept(this);
  }

  @Override
  public PqlNode visitNe(@NonNull NeContext context) {
    return new NeNode(context.ID().getText(), getValue(context.value()));
  }

  @Override
  public PqlNode visitGreaterEqual(@NonNull GreaterEqualContext context) {
    return context.ge().accept(this);
  }

  @Override
  public PqlNode visitGe(@NonNull GeContext context) {
    return new GeNode(context.ID().getText(), getValue(context.value()));
  }

  @Override
  public PqlNode visitGreaterThan(@NonNull GreaterThanContext context) {
    return context.gt().accept(this);
  }

  @Override
  public PqlNode visitGt(@NonNull GtContext context) {
    return new GtNode(context.ID().getText(), getValue(context.value()));
  }

  @Override
  public PqlNode visitLessEqual(@NonNull LessEqualContext context) {
    return context.le().accept(this);
  }

  @Override
  public PqlNode visitLe(@NonNull LeContext context) {
    return new LeNode(context.ID().getText(), getValue(context.value()));
  }

  @Override
  public PqlNode visitLessThan(@NonNull LessThanContext context) {
    return context.lt().accept(this);
  }

  @Override
  public PqlNode visitLt(@NonNull LtContext context) {
    return new LtNode(context.ID().getText(), getValue(context.value()));
  }

  @Override
  public PqlNode visitNot(@NonNull NotContext context) {
    val childNode = (FilterNode) context.filter().accept(this);

    return new NotNode(childNode);
  }

  @Override
  public PqlNode visitExists(@NonNull ExistsContext context) {
    return new ExistsNode(context.ID().getText());
  }

  @Override
  public PqlNode visitMissing(@NonNull MissingContext context) {
    return new MissingNode(context.ID().getText());
  }

  @Override
  public PqlNode visitInArray(@NonNull InArrayContext nodeContext) {
    return nodeContext.in().accept(this);
  }

  @Override
  public PqlNode visitIn(@NonNull InContext context) {
    val values = ImmutableList.builder();
    for (val value : context.value()) {
      values.add(getValue(value));
    }

    return new InNode(context.ID().getText(), values.build());
  }

  @Override
  public PqlNode visitOr(@NonNull OrContext context) {
    val result = new OrNode();
    for (val filter : context.filter()) {
      result.addChildren(filter.accept(this));
    }

    return result;
  }

  @Override
  public PqlNode visitAnd(@NonNull AndContext context) {
    val result = new AndNode();
    for (val filter : context.filter()) {
      result.addChildren(filter.accept(this));
    }

    return result;
  }

  @Override
  public PqlNode visitNested(@NonNull NestedContext nodeContext) {
    val nestedNode = new NestedNode(nodeContext.ID().getText());
    for (val child : nodeContext.filter()) {
      nestedNode.addChildren(child.accept(this));
    }

    return nestedNode;
  }

  private static Object getValue(ValueContext context) {
    if (context.STRING() != null) {
      return cleanString(context.STRING().getText());
    } else if (context.FLOAT() != null) {
      return Double.parseDouble(context.FLOAT().getText());
    } else {
      return Integer.parseInt(context.INT().getText());
    }
  }

  private static ImmutableList<String> resolveValues(List<TerminalNode> values) {
    val result = ImmutableList.<String> builder();
    for (val value : values) {
      result.add(value.getText());
    }

    return result.build();
  }

  private static PqlNode resolveFilters(List<PqlNode> filters) {
    if (filters.size() == 1) {
      return filters.get(0);
    }

    return new AndNode(filters.toArray(new FilterNode[filters.size()]));
  }

}
