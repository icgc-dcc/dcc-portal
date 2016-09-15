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

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.val;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.aggs.*;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.visitor.NodeVisitor;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.meta.TypeModel;

import java.util.Collection;
import java.util.Optional;

import static org.dcc.portal.pql.es.utils.VisitorHelpers.checkOptional;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;

public class NestedAggregationVisitor extends NodeVisitor<ExpressionNode, TypeModel> {

  private static final Collection<String> AGGREGATION_NAMES = ImmutableList.of(
      "transcript.consequence.consequence_type",
      "ssm_occurrence.observation.platform",
      "ssm_occurrence.observation.verification_status",
      "transcript.functional_impact_prediction_summary",
      "ssm_occurrence.observation.sequencing_strategy",
      "file_copies.file_format",
      "donors.project_code",
      "donors.primary_site",
      "donors.specimen_type",
      "donors.study");

  @Override
  public ExpressionNode visitRoot(@NonNull RootNode node, @NonNull Optional<TypeModel> context) {
    checkOptional(context);
    val indexType = context.get().getType();
    if (indexType != MUTATION_CENTRIC && indexType != Type.FILE) {
      return node;
    }

    val aggregationsNode = Nodes.getOptionalChild(node, AggregationsNode.class);
    if (!aggregationsNode.isPresent()) {
      return node;
    }

    aggregationsNode.get().accept(this, context);

    return node;
  }

  @Override
  public ExpressionNode visitAggregations(@NonNull AggregationsNode node, @NonNull Optional<TypeModel> context) {
    return visitChildren(node, context);
  }

  @Override
  public ExpressionNode visitFilterAggregation(@NonNull FilterAggregationNode node, @NonNull Optional<TypeModel> context) {
    return visitChildren(node, context);
  }

  @Override
  public ExpressionNode visitMissingAggregation(@NonNull MissingAggregationNode node,
      @NonNull Optional<TypeModel> context) {
    return processCommonCases(node, node.getAggregationName(), node.getFieldName());
  }

  @Override
  public ExpressionNode visitNestedAggregation(@NonNull NestedAggregationNode node, @NonNull Optional<TypeModel> context) {
    return visitChildren(node, context);
  }

  @Override
  public ExpressionNode visitTermsAggregation(@NonNull TermsAggregationNode node, @NonNull Optional<TypeModel> context) {
    return processCommonCases(node, node.getAggregationName(), node.getFieldName());
  }

  private static ExpressionNode processCommonCases(ExpressionNode node, String aggregationName, String fieldName) {
    if (!AGGREGATION_NAMES.contains(fieldName)) {
      return node;
    }

    node.addChildren(createReverseNestedNode(aggregationName));

    return node;
  }

  private static ReverseNestedAggregationNode createReverseNestedNode(String aggregationName) {
    return new ReverseNestedAggregationNode(aggregationName);

  }

  private ExpressionNode visitChildren(ExpressionNode node, Optional<TypeModel> context) {
    for (val child : node.getChildren()) {
      child.accept(this, context);
    }

    return node;
  }

}
