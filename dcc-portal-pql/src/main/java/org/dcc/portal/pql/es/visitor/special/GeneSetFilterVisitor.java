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
import static org.dcc.portal.pql.es.utils.VisitorHelpers.checkOptional;
import static org.dcc.portal.pql.es.utils.VisitorHelpers.visitChildren;
import static org.dcc.portal.pql.meta.TypeModel.BIOLOGICAL_PROCESS;
import static org.dcc.portal.pql.meta.TypeModel.CELLULAR_COMPONENT;
import static org.dcc.portal.pql.meta.TypeModel.GENE_CURATED_SET_ID;
import static org.dcc.portal.pql.meta.TypeModel.GENE_GO_TERM_ID;
import static org.dcc.portal.pql.meta.TypeModel.GENE_PATHWAY_ID;
import static org.dcc.portal.pql.meta.TypeModel.GENE_SET_ID;
import static org.dcc.portal.pql.meta.TypeModel.MOLECULAR_FUNCTION;

import java.util.Optional;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.RootNode;
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
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.meta.TypeModel;
import org.dcc.portal.pql.query.QueryContext;

/**
 * Resolves GeneSet filters.
 */
@Slf4j
public class GeneSetFilterVisitor extends NodeVisitor<Optional<ExpressionNode>, QueryContext> {

  @Override
  public Optional<ExpressionNode> visitRoot(@NonNull RootNode node, @NonNull Optional<QueryContext> context) {
    val queryNode = Nodes.getOptionalChild(node, QueryNode.class);
    if (queryNode.isPresent()) {
      queryNode.get().accept(this, context);
    }

    return Optional.of(node);
  }

  @Override
  public Optional<ExpressionNode> visitQuery(@NonNull QueryNode node, @NonNull Optional<QueryContext> context) {
    checkState(node.childrenCount() == 1, "Malformed QueryNode %s", node);

    return visitChildren(this, node, context);
  }

  @Override
  public Optional<ExpressionNode> visitTerms(@NonNull TermsNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);
    log.debug("[visitTerms]. Field name: {}", node.getField());
    if (node.getField().equals(GENE_GO_TERM_ID)) {
      return resolveGoTermArray(node, context.get().getTypeModel());
    }

    if (node.getField().equals(GENE_SET_ID)) {
      return resolveGeneSetIdArray(node, context.get().getTypeModel());
    }

    return Optional.of(node);
  }

  @Override
  public Optional<ExpressionNode> visitFilter(@NonNull FilterNode node, @NonNull Optional<QueryContext> context) {
    return visitChildren(this, node, context);
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
  public Optional<ExpressionNode> visitShouldBool(@NonNull ShouldBoolNode node, @NonNull Optional<QueryContext> context) {
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

  @Override
  public Optional<ExpressionNode> visitTerm(@NonNull TermNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);

    val fieldName = node.getNameNode().getValueAsString();

    if (fieldName.equals(GENE_GO_TERM_ID) || fieldName.equals(GENE_SET_ID)) {
      val termsNode = new TermsNode(fieldName, node.getValueNode());
      return termsNode.accept(this, context);
    }

    return Optional.empty();
  }

  private Optional<ExpressionNode> resolveGeneSetIdArray(TermsNode termsNode, TypeModel typeModel) {
    val shouldNode = new ShouldBoolNode(createGoTermChildren(termsNode, typeModel));
    shouldNode.addChildren(createPathwayAndCuratedSetIdNodes(termsNode, typeModel));
    val fullyQualifiedName = typeModel.getInternalField(CELLULAR_COMPONENT);
    val result = new BoolNode(shouldNode);

    if (typeModel.isNested(fullyQualifiedName) && !hasNestedParent(termsNode, typeModel, fullyQualifiedName)) {
      return createNestedNodeOptional(typeModel.getNestedPath(fullyQualifiedName), result);
    }

    return Optional.of(result);
  }

  private ExpressionNode[] createPathwayAndCuratedSetIdNodes(TermsNode termsNode, TypeModel typeModel) {
    val pathwayTermsNode = new TermsNode(typeModel.getField(GENE_PATHWAY_ID), termsNode.getChildrenArray());
    val curatedSetIdTermsNode = new TermsNode(typeModel.getField(GENE_CURATED_SET_ID), termsNode.getChildrenArray());

    return new ExpressionNode[] { pathwayTermsNode, curatedSetIdTermsNode };
  }

  private static Optional<ExpressionNode> createNestedNodeOptional(String path, ExpressionNode... children) {
    return Optional.of(new NestedNode(path, children));
  }

  private Optional<ExpressionNode> resolveGoTermArray(TermsNode termsNode, TypeModel typeModel) {
    val shouldNode = new ShouldBoolNode(createGoTermChildren(termsNode, typeModel));
    val fullyQualifiedName = typeModel.getInternalField(CELLULAR_COMPONENT);
    val result = new BoolNode(shouldNode);

    if (typeModel.isNested(fullyQualifiedName) && !hasNestedParent(termsNode, typeModel, fullyQualifiedName)) {
      return createNestedNodeOptional(typeModel.getNestedPath(fullyQualifiedName), result);
    }

    return Optional.of(result);
  }

  private boolean hasNestedParent(ExpressionNode node, TypeModel typeModel, String nestedField) {
    val nestedPath = getNestedPath(typeModel, nestedField);

    val parent = Nodes.findParent(node, NestedNode.class);
    if (parent.isPresent()) {
      val nestedNode = parent.get();
      if (nestedNode.getPath().startsWith(nestedPath)) {
        return true;
      }
    }

    return false;
  }

  private String getNestedPath(TypeModel typeModel, String fieldName) {
    return typeModel.getNestedPath(fieldName);
  }

  private static ExpressionNode[] createGoTermChildren(TermsNode termsNode, TypeModel typeModel) {
    ExpressionNode[] children = termsNode.getChildrenArray();

    return new ExpressionNode[] {
        new TermsNode(typeModel.getInternalField(CELLULAR_COMPONENT), children),
        new TermsNode(typeModel.getInternalField(BIOLOGICAL_PROCESS), children),
        new TermsNode(typeModel.getInternalField(MOLECULAR_FUNCTION), children) };
  }

}
