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

import java.util.Optional;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.RootNode;
import org.dcc.portal.pql.es.ast.aggs.AggregationsNode;
import org.dcc.portal.pql.es.ast.aggs.FilterAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.MissingAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.NestedAggregationNode;
import org.dcc.portal.pql.es.ast.aggs.TermsAggregationNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.es.visitor.NodeVisitor;

import com.google.common.collect.Lists;

/**
 * TermsAggregations do not report number of documents without value (i.e. the missing field in facets). There is
 * MissingAggregation for this purpose.<br>
 * <br>
 * This class creates a missing aggregation for each TermsAggregation it encounters.
 */
@Slf4j
public class MissingAggregationVisitor extends NodeVisitor<ExpressionNode, Void> {

  public static final String MISSING_SUFFIX = "_missing";

  @Override
  public ExpressionNode visitRoot(@NonNull RootNode node, Optional<Void> context) {
    val aggsNode = Nodes.getOptionalChild(node, AggregationsNode.class);
    if (aggsNode.isPresent()) {
      aggsNode.get().accept(this, context);
    }

    return node;
  }

  @Override
  public ExpressionNode visitAggregations(@NonNull AggregationsNode node, Optional<Void> context) {
    val missingAggregations = Lists.<ExpressionNode> newArrayList();

    for (val child : node.getChildren()) {
      missingAggregations.add(child.accept(this, context));
    }

    if (!missingAggregations.isEmpty()) {
      node.addChildren(missingAggregations.toArray(new ExpressionNode[missingAggregations.size()]));
    }

    return node;
  }

  @Override
  public ExpressionNode visitTermsAggregation(@NonNull TermsAggregationNode node, Optional<Void> context) {
    return new MissingAggregationNode(node.getAggregationName() + MISSING_SUFFIX, node.getFieldName());
  }

  @Override
  public ExpressionNode visitFilterAggregation(@NonNull FilterAggregationNode node, Optional<Void> context) {
    val child = node.getFirstChild().accept(this, context);

    val result = new FilterAggregationNode(node.getAggregationName() + MISSING_SUFFIX, node.getFilters());
    result.addChildren(child);
    log.debug("\n{}", result);

    return result;
  }

  @Override
  public ExpressionNode visitNestedAggregation(@NonNull NestedAggregationNode node, Optional<Void> context) {
    val child = node.getFirstChild().accept(this, context);

    val result = new NestedAggregationNode(node.getAggregationName() + MISSING_SUFFIX, node.getPath(), child);
    log.debug("\n{}", result);

    return result;
  }

}
