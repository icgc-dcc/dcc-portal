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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.get;
import static org.dcc.portal.pql.es.utils.Nodes.getStringValues;
import static org.dcc.portal.pql.es.utils.VisitorHelpers.checkOptional;
import static org.dcc.portal.pql.es.utils.VisitorHelpers.visitChildren;
import static org.dcc.portal.pql.meta.IndexModel.getTypeModel;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.meta.Type.FILE;
import static org.dcc.portal.pql.meta.TypeModel.LOOKUP_INDEX;
import static org.dcc.portal.pql.meta.TypeModel.LOOKUP_PATH;
import static org.dcc.portal.pql.meta.TypeModel.LOOKUP_TYPE;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

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
import org.icgc.dcc.common.core.util.Separators;
import org.icgc.dcc.common.core.util.Splitters;
import org.icgc.dcc.common.core.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

/**
 * Resolves querying by {@code entitySetId}.<br>
 * <b>NB:</b> Must be run one of the first because it does not modify structure of the AST, just 'enriches' search
 * parameters.
 */
@Slf4j
public class EntitySetVisitor extends NodeVisitor<Optional<ExpressionNode>, QueryContext> {

  /**
   * The prefix indicates that value of the field should be resolved using the terms lookup database.
   */
  public static final String IDENTIFIABLE_VALUE_PREFIX = "ES:";

  private static final List<Type> LOOKUP_TYPES =
      ImmutableList.of(DONOR_CENTRIC, GENE_CENTRIC, MUTATION_CENTRIC, FILE);

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
    val values = getStringValues(node);
    val containsIdentifiableValue = values.stream()
        .anyMatch(value -> hasIdentifiableValue(value));

    if (containsIdentifiableValue) {
      // Bool - Should
      val identifiableValues = values.stream()
          .filter(value -> hasIdentifiableValue(value))
          .collect(toImmutableList());

      val nonIdentifiableValues = values.stream()
          .filter(value -> !hasIdentifiableValue(value))
          .collect(toImmutableList());

      val shouldNode = new ShouldBoolNode();
      val field = node.getField();
      // Clone this node without the identifiable values
      shouldNode.addChildren(createTermsNode(field, nonIdentifiableValues));
      // Add identifiable values as a list of TermNodes which will be resolved by this visitor
      shouldNode.addChildren(createTermNodes(field, identifiableValues, context));

      return Optional.of(new BoolNode(shouldNode));
    }

    return Optional.empty();
  }

  @Override
  public Optional<ExpressionNode> visitTerm(@NonNull TermNode node, @NonNull Optional<QueryContext> context) {
    checkOptional(context);
    val field = node.getNameNode().getValueAsString();
    val value = node.getValueNode().getValue();
    val typeModel = context.get().getTypeModel();
    val identifiable = typeModel.isIdentifiable(field);

    if (identifiable && hasIdentifiableValue((String) value)) {
      val stringValue = (String) value;
      log.debug("'{}' has identifiable value '{}'", field, stringValue);
      val id = getIdentifiableValue(stringValue);
      log.debug("Resolved id value ({}) for '{}'", id, field);
      val idValueTypeModel = resolveIdentifiableTypeModel(field, typeModel);
      val lookupInfo = resolveLookup(id, idValueTypeModel);

      return Optional.of(new TermNode(field, id, lookupInfo));
    }

    if (value instanceof String) {
      val stringValue = (String) value;
      checkState(!hasIdentifiableValue(stringValue), "Only identifiable fields can start with '%s' prefix. "
          + "Field: '%s'. Value: '%s'", IDENTIFIABLE_VALUE_PREFIX, field, value);
    }

    return Optional.empty();
  }

  private boolean hasIdentifiableValue(String value) {
    return value.startsWith(IDENTIFIABLE_VALUE_PREFIX);
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

  private static LookupInfo resolveLookup(String lookupId, TypeModel typeModel) {
    return new LookupInfo(
        typeModel.getInternalField(LOOKUP_INDEX),
        typeModel.getInternalField(LOOKUP_TYPE),
        lookupId,
        typeModel.getInternalField(LOOKUP_PATH));
  }

  private static TypeModel resolveTypeModelByField(String field) {
    val typeModel = LOOKUP_TYPES.stream()
        .filter(type -> field.startsWith(type.getPrefix()))
        .map(type -> getTypeModel(type))
        .collect(Collectors.toImmutableList());
    checkArgument(typeModel.size() == 1, "Failed to resolve TypeModel by field '%s'", field);

    return typeModel.get(0);
  }

  private List<ExpressionNode> createTermNodes(String field, List<String> values, Optional<QueryContext> context) {
    return values.stream()
        .map(value -> new TermNode(field, value))
        .map(node -> node.accept(this, context).get())
        .collect(toImmutableList());
  }

  private static ExpressionNode createTermsNode(String field, List<String> values) {
    val children = values.stream()
        .map(value -> new TerminalNode(value))
        .collect(toImmutableList());

    return new TermsNode(field, children);
  }

  private static TypeModel resolveIdentifiableTypeModel(String field, TypeModel typeModel) {
    val aliases = typeModel.getAliasByField(field);
    val prefix = resolveIdentifiableTypeModelName(aliases);

    return prefix.equals("id") ? typeModel : resolveTypeModelByField(prefix);
  }

  private static String resolveIdentifiableTypeModelName(Set<String> aliases) {
    if (aliases.size() == 1) {
      return get(aliases, 0);
    }

    // However, if a couple aliases available use the one with the prefix
    val prefix = aliases.stream()
        .map(alias -> Splitters.DOT.splitToList(alias))
        .filter(parts -> parts.size() == 2)
        .map(parts -> parts.get(0))
        .collect(toImmutableList());
    checkArgument(prefix.size() == 1, "Failed to resolve type model prefix from aliases %s", aliases);

    return prefix.get(0);
  }

  private String getIdentifiableValue(String value) {
    val id = value.replaceFirst(IDENTIFIABLE_VALUE_PREFIX, Separators.EMPTY_STRING).trim();
    // Verify it's a UUID
    UUID.fromString(id);

    return id;
  }
}
