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
package org.dcc.portal.pql.es.visitor.special;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.dcc.portal.pql.es.utils.VisitorHelpers.checkOptional;
import static org.dcc.portal.pql.es.utils.VisitorHelpers.visitChildren;
import static org.dcc.portal.pql.meta.IndexModel.getTypeModel;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.meta.Type.REPOSITORY_FILE;
import static org.dcc.portal.pql.meta.TypeModel.ENTITY_SET_ID;
import static org.dcc.portal.pql.meta.TypeModel.LOOKUP_INDEX;
import static org.dcc.portal.pql.meta.TypeModel.LOOKUP_PATH;
import static org.dcc.portal.pql.meta.TypeModel.LOOKUP_TYPE;

import java.util.List;
import java.util.Optional;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
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
import org.dcc.portal.pql.es.ast.query.QueryNode;
import org.dcc.portal.pql.es.model.LookupInfo;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.meta.TypeModel;
import org.dcc.portal.pql.query.QueryContext;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;
import lombok.val;

/**
 * Resolves querying by {@code entitySetId}.<br>
 * <b>NB:</b> Must be run one of the first because it does not modify structure of the AST, just 'enriches' search
 * parameters.
 */
public class EntitySetVisitor extends NodeVisitor<Optional<ExpressionNode>, QueryContext> {

  private static final List<Type> LOOKUP_TYPES =
      ImmutableList.of(DONOR_CENTRIC, GENE_CENTRIC, MUTATION_CENTRIC, REPOSITORY_FILE);

  @Override
  public Optional<ExpressionNode> visitRoot(@NonNull RootNode node, Optional<QueryContext> context) {
    val queryNodeOpt = Nodes.getOptionalChild(node, QueryNode.class);
    if (queryNodeOpt.isPresent()) {
      queryNodeOpt.get().accept(this, context);
    }

    return Optional.of(node);
  }

  @Override
  public Optional<ExpressionNode> visitQuery(@NonNull QueryNode node, Optional<QueryContext> context) {
    checkState(node.childrenCount() == 1, "Malformed node %s", node);

    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitFilter(@NonNull FilterNode node, Optional<QueryContext> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitTerms(@NonNull TermsNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);
    if (!isProcess(node.getField())) {
      return Optional.empty();
    }

    val field = resolveField(node, context.get().getTypeModel());
    val lookupInfo = resolveLookup(getValue(node), resolveTypeModelByField(node.getField()));

    return Optional.of(new TermsNode(field, lookupInfo, node.getChildrenArray()));
  }

  @Override
  public Optional<ExpressionNode> visitTerm(@NonNull TermNode node, @NonNull Optional<QueryContext> context) {
    // TODO: Think about moving all the processing from visitTerms, because visitTerms uses only the first value
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitBool(@NonNull BoolNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitMustBool(@NonNull MustBoolNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitShouldBool(@NonNull ShouldBoolNode node,
      @NonNull Optional<QueryContext> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitNot(@NonNull NotNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitNested(@NonNull NestedNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitRange(@NonNull RangeNode node, @NonNull Optional<QueryContext> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitExists(@NonNull ExistsNode node, @NonNull Optional<QueryContext> context) {
    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitMissing(@NonNull MissingNode node, @NonNull Optional<QueryContext> context) {
    return Optional.empty();
  }

  private static String resolveField(TermsNode node, TypeModel typeModel) {
    return typeModel.getInternalField(node.getField());
  }

  private static String getValue(ExpressionNode node) {
    val terminalNode = (TerminalNode) node.getFirstChild();

    return terminalNode.getValueAsString();
  }

  private static LookupInfo resolveLookup(String field, TypeModel typeModel) {
    return new LookupInfo(
        typeModel.getInternalField(LOOKUP_INDEX),
        typeModel.getInternalField(LOOKUP_TYPE),
        field,
        typeModel.getInternalField(LOOKUP_PATH));
  }

  private static boolean isProcess(String field) {
    return field.endsWith(ENTITY_SET_ID);
  }

  private static TypeModel resolveTypeModelByField(String field) {
    for (val type : LOOKUP_TYPES) {
      if (field.startsWith(type.getPrefix())) {
        return getTypeModel(type);
      }
    }

    throw new IllegalArgumentException(format("Failed to resolve TypeModel by field '%s'", field));
  }

}
