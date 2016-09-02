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
package org.dcc.portal.pql.es.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static org.dcc.portal.pql.es.utils.Visitors.createAggregationsResolverVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createEmptyNodesCleanerVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createEntitySetVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createFieldsToSourceVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createFixNotQueryVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createGeneSetFilterVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createLocationFilterVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createMissingAggregationVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createNestedAggregationVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createQuerySimplifierVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createRemoveAggregationFilterVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createResolveNestedFieldVisitor;
import static org.dcc.portal.pql.es.utils.Visitors.createScoreSortVisitor;
import static org.dcc.portal.pql.meta.IndexModel.getDiagramTypeModel;
import static org.dcc.portal.pql.meta.IndexModel.getDonorCentricTypeModel;
import static org.dcc.portal.pql.meta.IndexModel.getDrugTypeModel;
import static org.dcc.portal.pql.meta.IndexModel.getFileTypeModel;
import static org.dcc.portal.pql.meta.IndexModel.getGeneCentricTypeModel;
import static org.dcc.portal.pql.meta.IndexModel.getMutationCentricTypeModel;
import static org.dcc.portal.pql.meta.IndexModel.getObservationCentricTypeModel;
import static org.dcc.portal.pql.meta.IndexModel.getProjectTypeModel;
import static org.dcc.portal.pql.meta.Type.DIAGRAM;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.dcc.portal.pql.meta.Type.DRUG_CENTRIC;
import static org.dcc.portal.pql.meta.Type.FILE;
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.meta.Type.OBSERVATION_CENTRIC;
import static org.dcc.portal.pql.meta.Type.PROJECT;

import java.util.Map;
import java.util.Optional;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.visitor.aggs.Context;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.meta.TypeModel;
import org.dcc.portal.pql.query.QueryContext;

import com.google.common.collect.ImmutableMap;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Performs series of transformations to resolve different processing rules and to optimize the AST
 */
@Slf4j
@NoArgsConstructor
public class EsAstTransformer {

  private static final Map<Type, Optional<Context>> RESOLVE_FACETS_CONTEXT_MAPPING =
      ImmutableMap.<Type, Optional<Context>> builder()
          .put(DONOR_CENTRIC, createFacetContext(getDonorCentricTypeModel()))
          .put(GENE_CENTRIC, createFacetContext(getGeneCentricTypeModel()))
          .put(MUTATION_CENTRIC, createFacetContext(getMutationCentricTypeModel()))
          .put(OBSERVATION_CENTRIC, createFacetContext(getObservationCentricTypeModel()))
          .put(PROJECT, createFacetContext(getProjectTypeModel()))
          .put(FILE, createFacetContext(getFileTypeModel()))
          .put(DRUG_CENTRIC, createFacetContext(getDrugTypeModel()))
          .put(DIAGRAM, createFacetContext(getDiagramTypeModel()))
          .build();

  public ExpressionNode process(@NonNull ExpressionNode esAst, @NonNull QueryContext context) {
    log.debug("Running all ES AST Transformators. Original ES AST: {}", esAst);
    esAst = resolveSpecialCases(esAst, context);

    log.debug("Resolving filters on nested fields...");
    esAst = esAst.accept(createResolveNestedFieldVisitor(), Optional.of(context.getTypeModel())).get();
    log.debug("Resolved nested filters. Resulting AST: {}", esAst);

    esAst = resolveFacets(esAst, context.getTypeModel());
    esAst = score(esAst, context);
    esAst = fixNotQuery(esAst, context);
    esAst = optimize(esAst);
    log.debug("ES AST after the transformations: {}", esAst);

    return esAst;
  }

  public ExpressionNode score(@NonNull ExpressionNode esAst, @NonNull QueryContext context) {
    val tag = "[score]";
    log.debug("{} Adding scores to the query...", tag);
    val result = esAst.accept(Visitors.createScoreQueryVisitor(context.getType()), Optional.of(context));
    log.debug("{} Added scores to the query. Resulting AST: {}", tag, result);

    return result;
  }

  public ExpressionNode fixNotQuery(@NonNull ExpressionNode esAst, @NonNull QueryContext context) {
    val result = esAst.accept(createFixNotQueryVisitor(), Optional.of(context));

    return result;
  }

  public ExpressionNode resolveSpecialCases(@NonNull ExpressionNode esAst, @NonNull QueryContext context) {
    val tag = "[resoveSpecialCases]";
    log.debug("Resolving the special cases...");

    log.debug("{} Moving object fields to _source...", tag);
    esAst = esAst.accept(createFieldsToSourceVisitor(), Optional.of(context)).get();
    log.debug("{} Moved object fields to _source. Resulting AST: {}", tag, esAst);

    log.debug("{} Resolving EntitySets...", tag);
    esAst = esAst.accept(createEntitySetVisitor(), Optional.of(context)).get();
    log.debug("{} Resolved EntitySets. Resulting AST: {}", tag, esAst);

    log.debug("{} Resolving sorting by score...", tag);
    esAst = esAst.accept(createScoreSortVisitor(), Optional.empty());
    log.debug("{} Resolved sorting by score. Resulting AST: {}", tag, esAst);

    log.debug("{} Resolving GeneSets...", tag);
    esAst = esAst.accept(createGeneSetFilterVisitor(), Optional.of(context)).get();
    log.debug("{} Resolved GeneSets. Resulting AST: {}", tag, esAst);

    log.debug("{} Resolving location filters...", tag);
    esAst = esAst.accept(createLocationFilterVisitor(), Optional.of(context)).get();
    log.debug("{} Resolved location filters. Resulting AST: {}", tag, esAst);

    return esAst;
  }

  public ExpressionNode optimize(@NonNull ExpressionNode esAst) {
    val tag = "[optimize]";
    log.debug("{} Cleaning empty nodes...", tag);
    esAst = esAst.accept(createEmptyNodesCleanerVisitor(), Optional.empty());
    log.debug("{} Cleaned empty nodes. Resulting AST: {}", tag, esAst);

    // Remove FilterAggregationNodes without filters
    // TODO: check if this is needed anymore
    val aggsNode = Nodes.getOptionalChild(esAst, AggregationsNode.class);
    if (aggsNode.isPresent()) {
      esAst = esAst.accept(createRemoveAggregationFilterVisitor(), Optional.empty()).get();
    }

    log.debug("{} Simplifying the resulting AST...", tag);
    esAst = esAst.accept(createQuerySimplifierVisitor(), Optional.empty()).get();
    log.debug("{} Simplified the resulting AST. Resulting AST: {}", tag, esAst);

    return esAst;
  }

  public ExpressionNode resolveFacets(@NonNull ExpressionNode esAst, @NonNull TypeModel typeModel) {
    val tag = "[resolveFacets]";
    log.debug("{} Resolving aggregations...", tag);
    esAst = esAst.accept(createAggregationsResolverVisitor(), getResolveFacetsContext(typeModel.getType())).get();
    log.debug("{} Resolved aggregations. Resulting AST: {}", tag, esAst);

    log.debug("{} Adding missing aggregations...", tag);
    esAst = esAst.accept(createMissingAggregationVisitor(), Optional.empty());
    log.debug("{} Added missing aggregations. Resulting AST: {}", tag, esAst);

    log.debug("{} Resolving aggregations on nested fields...", tag);
    esAst = esAst.accept(createNestedAggregationVisitor(), Optional.of(typeModel));
    log.debug("{} Resolved aggregations on nested fields. Resulting AST: {}", tag, esAst);

    return esAst;
  }

  @NonNull
  private static Optional<Context> createFacetContext(TypeModel typeModel) {
    return Optional.of(new Context(null, typeModel));
  }

  @NonNull
  private static Optional<Context> getResolveFacetsContext(Type indexType) {
    val context = RESOLVE_FACETS_CONTEXT_MAPPING.get(indexType);
    checkArgument(null != context, "Unknown index type '%s'", indexType.getId());

    return context;
  }

}
