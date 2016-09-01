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
import static lombok.AccessLevel.PRIVATE;
import static org.dcc.portal.pql.meta.Type.DIAGRAM;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.dcc.portal.pql.meta.Type.DRUG_CENTRIC;
import static org.dcc.portal.pql.meta.Type.FILE;
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.meta.Type.OBSERVATION_CENTRIC;
import static org.dcc.portal.pql.meta.Type.PROJECT;

import java.util.Map;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.visitor.CreateQueryBuilderVisitor;
import org.dcc.portal.pql.es.visitor.FilterBuilderVisitor;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.es.visitor.ResolveNestedFilterFieldVisitor;
import org.dcc.portal.pql.es.visitor.aggs.AggregationFiltersVisitor;
import org.dcc.portal.pql.es.visitor.aggs.AggregationsResolverVisitor;
import org.dcc.portal.pql.es.visitor.aggs.CreateAggregationBuilderVisitor;
import org.dcc.portal.pql.es.visitor.aggs.MissingAggregationVisitor;
import org.dcc.portal.pql.es.visitor.aggs.NestedAggregationVisitor;
import org.dcc.portal.pql.es.visitor.aggs.RemoveAggregationFilterVisitor;
import org.dcc.portal.pql.es.visitor.aggs.ResolveNestedFilterVisitor;
import org.dcc.portal.pql.es.visitor.aggs.SearchNonNestedFieldsVisitor;
import org.dcc.portal.pql.es.visitor.aggs.VerifyNestedFilterVisitor;
import org.dcc.portal.pql.es.visitor.score.DefaultScoreQueryVisitor;
import org.dcc.portal.pql.es.visitor.score.DonorScoreQueryVisitor;
import org.dcc.portal.pql.es.visitor.score.GeneScoreQueryVisitor;
import org.dcc.portal.pql.es.visitor.score.MutationScoreQueryVisitor;
import org.dcc.portal.pql.es.visitor.score.NestedFieldsVisitor;
import org.dcc.portal.pql.es.visitor.score.NonNestedFieldsVisitor;
import org.dcc.portal.pql.es.visitor.score.ScoreQueryVisitor;
import org.dcc.portal.pql.es.visitor.special.EntitySetVisitor;
import org.dcc.portal.pql.es.visitor.special.FieldsToSourceVisitor;
import org.dcc.portal.pql.es.visitor.special.FixNotQueryVisitor;
import org.dcc.portal.pql.es.visitor.special.GeneSetFilterVisitor;
import org.dcc.portal.pql.es.visitor.special.LocationFilterVisitor;
import org.dcc.portal.pql.es.visitor.special.ScoreSortVisitor;
import org.dcc.portal.pql.es.visitor.util.CloneNodeVisitor;
import org.dcc.portal.pql.es.visitor.util.EmptyNodesCleanerVisitor;
import org.dcc.portal.pql.es.visitor.util.QuerySimplifierVisitor;
import org.dcc.portal.pql.es.visitor.util.ToStringVisitor;
import org.dcc.portal.pql.meta.Type;

import com.google.common.collect.ImmutableMap;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
public final class Visitors {

  private static final NodeVisitor<String, Void> TO_STRING_VISITOR = new ToStringVisitor();
  private static final NodeVisitor<ExpressionNode, Void> CLONE_VISITOR = new CloneNodeVisitor();

  private static final CreateAggregationBuilderVisitor AGGREGATION_BUILDER_VISITOR =
      new CreateAggregationBuilderVisitor();

  private static final FilterBuilderVisitor FILTER_BUILDER_VISITOR = new FilterBuilderVisitor();

  private static final RemoveAggregationFilterVisitor REMOVE_AGGS_FILTER_VISITOR = new RemoveAggregationFilterVisitor();

  /*
   * ScoreQueryVisitors
   */
  private static final DonorScoreQueryVisitor DONOR_SCORE_QUERY_VISITOR = new DonorScoreQueryVisitor();
  private static final GeneScoreQueryVisitor GENE_SCORE_QUERY_VISITOR = new GeneScoreQueryVisitor();
  private static final MutationScoreQueryVisitor MUTATION_SCORE_QUERY_VISITOR = new MutationScoreQueryVisitor();
  private static final DefaultScoreQueryVisitor DEFAULT_SCORE_QUERY_VISITOR = new DefaultScoreQueryVisitor();

  /*
   * QueryBuilderVisitor
   */
  private static final CreateQueryBuilderVisitor QUERY_BUILDER_VISITOR = new CreateQueryBuilderVisitor();

  private static final AggregationFiltersVisitor AGGREGATION_FILTER_VISITOR = new AggregationFiltersVisitor();
  private static final AggregationsResolverVisitor AGGREGATIONS_RESOLVER_VISITOR = new AggregationsResolverVisitor();
  private static final MissingAggregationVisitor MISSING_AGGREGATION_VISITOR = new MissingAggregationVisitor();
  private static final NestedAggregationVisitor NESTED_AGGREGATION_VISITOR = new NestedAggregationVisitor();

  private static final EmptyNodesCleanerVisitor EMPTY_NODES_CLEANER_VISITOR = new EmptyNodesCleanerVisitor();

  private static final GeneSetFilterVisitor GENE_SET_FILTER_VISITOR = new GeneSetFilterVisitor();
  private static final LocationFilterVisitor LOCATION_FILTER_VISITOR = new LocationFilterVisitor();
  private static final ScoreSortVisitor SCORE_SORT_VISITOR = new ScoreSortVisitor();

  private static final FixNotQueryVisitor FIX_NOT_QUERY_VISITOR = new FixNotQueryVisitor();

  private static final EntitySetVisitor ENTITY_SET_VISITOR = new EntitySetVisitor();
  private static final FieldsToSourceVisitor FIELDS_TO_SOURCE_VISITOR = new FieldsToSourceVisitor();

  private static final NonNestedFieldsVisitor NON_NESTED_FIELDS_VISITOR = new NonNestedFieldsVisitor();
  private static final NestedFieldsVisitor NESTED_FIELDS_VISITOR = new NestedFieldsVisitor();
  private static final QuerySimplifierVisitor QUERY_SIMPLIFIER_VISITOR = new QuerySimplifierVisitor();

  private static final SearchNonNestedFieldsVisitor SEARCH_NON_NESTED_FILTERS_VISITOR =
      new SearchNonNestedFieldsVisitor();
  private static final ResolveNestedFilterVisitor RESOLVE_NESTED_FILTER_VISITOR = new ResolveNestedFilterVisitor();
  private static final ResolveNestedFilterFieldVisitor RESOLVE_NESTED_FIELD_VISITOR =
      new ResolveNestedFilterFieldVisitor();
  private static final VerifyNestedFilterVisitor VERIFY_NESTED_FILTER_VISITOR = new VerifyNestedFilterVisitor();

  private static final Map<Type, ScoreQueryVisitor> META_TYPE_SCORE_VISITOR_MAPPING =
      ImmutableMap.<Type, ScoreQueryVisitor> builder()
          .put(DONOR_CENTRIC, DONOR_SCORE_QUERY_VISITOR)
          .put(GENE_CENTRIC, GENE_SCORE_QUERY_VISITOR)
          .put(MUTATION_CENTRIC, MUTATION_SCORE_QUERY_VISITOR)
          .put(OBSERVATION_CENTRIC, DEFAULT_SCORE_QUERY_VISITOR)
          .put(PROJECT, DEFAULT_SCORE_QUERY_VISITOR)
          .put(FILE, DEFAULT_SCORE_QUERY_VISITOR)
          .put(DRUG_CENTRIC, DEFAULT_SCORE_QUERY_VISITOR)
          .put(DIAGRAM, DEFAULT_SCORE_QUERY_VISITOR)
          .build();

  public static final VerifyNestedFilterVisitor createVerifyNestedFilterVisitor() {
    return VERIFY_NESTED_FILTER_VISITOR;
  }

  public static final ResolveNestedFilterFieldVisitor createResolveNestedFieldVisitor() {
    return RESOLVE_NESTED_FIELD_VISITOR;
  }

  public static final ResolveNestedFilterVisitor createResolveNestedFilterVisitor() {
    return RESOLVE_NESTED_FILTER_VISITOR;
  }

  public static SearchNonNestedFieldsVisitor createSearchNonNestedFieldsVisitor() {
    return SEARCH_NON_NESTED_FILTERS_VISITOR;
  }

  public static QuerySimplifierVisitor createQuerySimplifierVisitor() {
    return QUERY_SIMPLIFIER_VISITOR;
  }

  public static NestedFieldsVisitor createNestedFieldsVisitor() {
    return NESTED_FIELDS_VISITOR;
  }

  public static NonNestedFieldsVisitor createNonNestedFieldsVisitor() {
    return NON_NESTED_FIELDS_VISITOR;
  }

  public static FieldsToSourceVisitor createFieldsToSourceVisitor() {
    return FIELDS_TO_SOURCE_VISITOR;
  }

  public static FixNotQueryVisitor createFixNotQueryVisitor() {
    return FIX_NOT_QUERY_VISITOR;
  }

  public static EntitySetVisitor createEntitySetVisitor() {
    return ENTITY_SET_VISITOR;
  }

  public static MutationScoreQueryVisitor createScoreMutatationQueryVisitor() {
    return MUTATION_SCORE_QUERY_VISITOR;
  }

  public static ScoreSortVisitor createScoreSortVisitor() {
    return SCORE_SORT_VISITOR;
  }

  public static GeneSetFilterVisitor createGeneSetFilterVisitor() {
    return GENE_SET_FILTER_VISITOR;
  }

  public static LocationFilterVisitor createLocationFilterVisitor() {
    return LOCATION_FILTER_VISITOR;
  }

  public static EmptyNodesCleanerVisitor createEmptyNodesCleanerVisitor() {
    return EMPTY_NODES_CLEANER_VISITOR;
  }

  public static AggregationsResolverVisitor createAggregationsResolverVisitor() {
    return AGGREGATIONS_RESOLVER_VISITOR;
  }

  public static MissingAggregationVisitor createMissingAggregationVisitor() {
    return MISSING_AGGREGATION_VISITOR;
  }

  public static AggregationFiltersVisitor createAggregationFiltersVisitor() {
    return AGGREGATION_FILTER_VISITOR;
  }

  public static NestedAggregationVisitor createNestedAggregationVisitor() {
    return NESTED_AGGREGATION_VISITOR;
  }

  public static CreateQueryBuilderVisitor createQueryBuilderVisitor() {
    return QUERY_BUILDER_VISITOR;
  }

  public static NodeVisitor<String, Void> createToStringVisitor() {
    return TO_STRING_VISITOR;
  }

  public static NodeVisitor<ExpressionNode, Void> createCloneNodeVisitor() {
    return CLONE_VISITOR;
  }

  public static CreateAggregationBuilderVisitor createAggregationBuilderVisitor() {
    return AGGREGATION_BUILDER_VISITOR;
  }

  public static FilterBuilderVisitor filterBuilderVisitor() {
    return FILTER_BUILDER_VISITOR;
  }

  public static RemoveAggregationFilterVisitor createRemoveAggregationFilterVisitor() {
    return REMOVE_AGGS_FILTER_VISITOR;
  }

  @NonNull
  public static ScoreQueryVisitor createScoreQueryVisitor(Type type) {
    val scoreVisitor = META_TYPE_SCORE_VISITOR_MAPPING.get(type);
    checkArgument(null != scoreVisitor, "Unknown index type '%s'", type.getId());

    return scoreVisitor;
  }

}
