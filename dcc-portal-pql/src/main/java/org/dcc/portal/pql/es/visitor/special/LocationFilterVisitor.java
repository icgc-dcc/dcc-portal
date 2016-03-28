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
import static org.dcc.portal.pql.meta.TypeModel.GENE_LOCATION;
import static org.dcc.portal.pql.meta.TypeModel.MUTATION_LOCATION;

import java.util.Optional;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.NestedNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.TerminalNode;
import org.dcc.portal.pql.es.ast.filter.BoolNode;
import org.dcc.portal.pql.es.ast.filter.ExistsNode;
import org.dcc.portal.pql.es.ast.filter.FilterNode;
import org.dcc.portal.pql.es.ast.filter.GreaterEqualNode;
import org.dcc.portal.pql.es.ast.filter.LessEqualNode;
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
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.meta.TypeModel;
import org.dcc.portal.pql.query.QueryContext;
import org.icgc.dcc.common.core.model.ChromosomeLocation;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves filtering by {@code gene.location} and {@code mutation.location}
 */
@Slf4j
public class LocationFilterVisitor extends NodeVisitor<Optional<ExpressionNode>, QueryContext> {

  private static final String MUTATION_CHROMOSOME = "mutation.chromosome";
  private static final String GENE_CHROMOSOME = "gene.chromosome";

  private static final String GENE_START = "gene.start";
  private static final String GENE_END = "gene.end";
  private static final String MUTATION_START = "mutation.start";
  private static final String MUTATION_END = "mutation.end";

  @Override
  public Optional<ExpressionNode> visitRoot(@NonNull RootNode node, @NonNull Optional<QueryContext> context) {
    val filterNode = Nodes.getOptionalChild(node, QueryNode.class);
    if (filterNode.isPresent()) {
      filterNode.get().accept(this, context);
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
    if (!node.getField().equals(GENE_LOCATION) && !node.getField().equals(MUTATION_LOCATION)) {
      return Optional.empty();
    }

    checkOptional(context);
    val result = new ShouldBoolNode();
    for (val child : node.getChildren()) {
      // visitTerm has already implemented logic. Let's reuse it by creating a TermNode and visiting it.
      val termNode = createTermNode(node.getField(), child);

      // Wrap with a NestedNode so when the child is visited it will not create own NestedNode
      val nestedNodeOpt = createNestedNode(node, context.get().getTypeModel());
      if (nestedNodeOpt.isPresent()) {
        nestedNodeOpt.get().addChildren(termNode);
      }

      val visitTermNodeResult = termNode.accept(this, context);
      result.addChildren(visitTermNodeResult.get());
    }

    return Optional.of(nest(node.getField(), new BoolNode(result), context.get().getTypeModel(), node));
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

  @Override
  public Optional<ExpressionNode> visitTerm(@NonNull TermNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);

    val field = (String) node.getNameNode().getValue();
    log.debug("[visitTerm] Field: {}", field);

    if (!field.equals(GENE_LOCATION) && !field.equals(MUTATION_LOCATION)) {
      return Optional.empty();
    }

    val locationString = (String) node.getValueNode().getValue();
    log.debug("[visitTerm] Value: {}", locationString);

    val typeModel = context.get().getTypeModel();
    val location = ChromosomeLocation.parse(locationString);

    val mustBoolNodes = ImmutableList.<ExpressionNode> builder()
        .add(createChromosomeNameTermNode(field, typeModel, location.getChromosome().getName()));

    if (location.hasStart() && location.hasEnd()) {
      mustBoolNodes.add(createGreaterEqualNode(resolveStartField(field, typeModel), location.getStart()));
      mustBoolNodes.add(createLessEqualNode(resolveEndField(field, typeModel), location.getEnd()));
    } else if (location.hasStart()) {
      mustBoolNodes.add(createGreaterEqualNode(resolveEndField(field, typeModel), location.getStart()));
      mustBoolNodes.add(createLessEqualNode(resolveEndField(field, typeModel), location.getStart()));
    }

    val result = new BoolNode(new MustBoolNode(mustBoolNodes.build()));

    return Optional.of(nest(field, result, typeModel, node));
  }

  private static ExpressionNode createTermNode(String field, ExpressionNode child) {
    val terminalNode = (TerminalNode) child;

    return new TermNode(field, terminalNode.getValue());
  }

  private static Optional<NestedNode> createNestedNode(TermsNode node, TypeModel typeModel) {
    val field = node.getField();
    if (isNonNestedField(field, typeModel)) {
      return Optional.empty();
    }

    val nestedNodeOpt = Nodes.findParent(node, NestedNode.class);
    if (nestedNodeOpt.isPresent()) {
      val nestedNodeClone = Nodes.cloneNode(nestedNodeOpt.get());
      nestedNodeClone.removeAllChildren();

      return Optional.of((NestedNode) nestedNodeClone);
    }

    val nestedPath = typeModel.getNestedPath(resolveAlias(field));

    return Optional.of(new NestedNode(nestedPath));
  }

  private static ExpressionNode nest(String field, ExpressionNode visitResultNode, TypeModel typeModel,
      ExpressionNode originalNode) {
    if (isNonNestedField(field, typeModel) || hasNestedParent(field, originalNode, typeModel)) {
      return visitResultNode;
    }

    val nestedPath = typeModel.getNestedPath(resolveAlias(field));
    log.debug("Nested path: '{}' for field '{}'", nestedPath, field);
    val nestedNodeOpt = Nodes.findParent(originalNode, NestedNode.class);
    if (nestedNodeOpt.isPresent()) {
      val nestedNode = nestedNodeOpt.get();
      if (nestedNode.getPath().equals(nestedPath)) {
        // The node is correctly nested. Nothing to do
        return visitResultNode;
      }

      checkNestingLevels(nestedPath, nestedNodeOpt.get().getPath());
    }

    return new NestedNode(nestedPath, visitResultNode);
  }

  private static boolean hasNestedParent(String field, ExpressionNode node, TypeModel typeModel) {
    val nestedPath = getNestedPath(typeModel, field);

    val parent = Nodes.findParent(node, NestedNode.class);
    if (parent.isPresent()) {
      val nestedNode = parent.get();
      if (nestedNode.getPath().equals(nestedPath)) {
        return true;
      }
    }

    return false;
  }

  private static String getNestedPath(TypeModel typeModel, String field) {
    if (field.equals(MUTATION_LOCATION)) {
      return typeModel.getNestedPath(MUTATION_START);
    }

    return typeModel.getNestedPath(GENE_START);
  }

  private static boolean isNonNestedField(String field, TypeModel typeModel) {
    val type = typeModel.getType();

    return type == Type.GENE_CENTRIC && field.equals(GENE_LOCATION) ||
        type == Type.MUTATION_CENTRIC && field.equals(MUTATION_LOCATION);
  }

  private static String resolveAlias(String field) {
    return field.equals(MUTATION_LOCATION) ? MUTATION_CHROMOSOME : GENE_CHROMOSOME;
  }

  /**
   * @param fieldNestedPath - path at which location nodes are nested
   * @param nestedNodePath - path of the closest {@link NestedNode} parent
   */
  private static void checkNestingLevels(String fieldNestedPath, String nestedNodePath) {
    // Assuming the longer String the deeper nesting
    checkState(fieldNestedPath.startsWith(nestedNodePath), "Location nodes must be nested deeper than the parent "
        + "NestedNode. Location nodes nesting level: '%s'. Parent's nesting level: '%s", fieldNestedPath,
        nestedNodePath);
  }

  private static String resolveEndField(String field, TypeModel typeModel) {
    if (field.startsWith("gene")) {
      return typeModel.getField(GENE_END);
    }

    return typeModel.getField(MUTATION_END);
  }

  private static ExpressionNode createLessEqualNode(String field, long value) {
    val leNode = new LessEqualNode(value);

    return new RangeNode(field, leNode);
  }

  private static ExpressionNode createChromosomeNameTermNode(String field, TypeModel typeModel, String chromosomeName) {
    return new TermNode(resolveChromosomeField(field, typeModel), chromosomeName);
  }

  private static ExpressionNode createGreaterEqualNode(String field, long value) {
    val geNode = new GreaterEqualNode(value);

    return new RangeNode(field, geNode);
  }

  private static String resolveStartField(String field, TypeModel typeModel) {
    if (field.startsWith("gene")) {
      return typeModel.getField(GENE_START);
    }

    return typeModel.getField(MUTATION_START);
  }

  private static String resolveChromosomeField(String field, TypeModel typeModel) {
    if (field.startsWith("gene")) {
      return typeModel.getField(GENE_CHROMOSOME);
    }

    return typeModel.getField(MUTATION_CHROMOSOME);
  }

}
