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

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.meta.TypeModel.LOOKUP_INDEX;
import static org.dcc.portal.pql.meta.TypeModel.LOOKUP_PATH;
import static org.dcc.portal.pql.meta.TypeModel.LOOKUP_TYPE;
import static org.dcc.portal.pql.utils.Tests.createEsAst;

import java.util.Optional;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.ast.filter.TermsNode;
import org.dcc.portal.pql.es.utils.Nodes;
import org.dcc.portal.pql.meta.IndexModel;
import org.dcc.portal.pql.meta.TypeModel;
import org.dcc.portal.pql.query.QueryContext;
import org.junit.Test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EntitySetVisitorTest {

  EntitySetVisitor visitor = new EntitySetVisitor();
  QueryContext queryContext;

  @Test
  public void noFilterTest() {
    val root = createEsAst("select(id)", DONOR_CENTRIC);
    val rootClone = Nodes.cloneNode(root);
    val result = rootClone.accept(visitor, Optional.empty());
    assertThat(result.get()).isEqualTo(root);
  }

  @Test
  public void donorEntitySetTest_donor() {
    queryContext = new QueryContext("", DONOR_CENTRIC);
    val root = createEsAst("in(donor.entitySetId, 'ID1')", DONOR_CENTRIC);
    assertStructure(root, "_donor_id");
  }

  @Test
  public void donorEntitySetTest_gene() {
    queryContext = new QueryContext("", GENE_CENTRIC);
    val root = createEsAst("in(donor.entitySetId, 'ID1')", GENE_CENTRIC);
    assertStructure(root, "donor._donor_id");
  }

  @Test
  public void donorEntitySetTest_mutation() {
    queryContext = new QueryContext("", MUTATION_CENTRIC);
    val root = createEsAst("in(donor.entitySetId, 'ID1')", MUTATION_CENTRIC);
    assertStructure(root, "ssm_occurrence.donor._donor_id");
  }

  @Test
  public void geneEntitySetTest_donor() {
    queryContext = new QueryContext("", DONOR_CENTRIC);
    val root = createEsAst("in(gene.entitySetId, 'ID1')", DONOR_CENTRIC);
    assertStructure(root, "gene._gene_id");
  }

  @Test
  public void geneEntitySetTest_gene() {
    queryContext = new QueryContext("", GENE_CENTRIC);
    val root = createEsAst("in(gene.entitySetId, 'ID1')", GENE_CENTRIC);
    assertStructure(root, "_gene_id");
  }

  @Test
  public void geneEntitySetTest_mutation() {
    queryContext = new QueryContext("", MUTATION_CENTRIC);
    val root = createEsAst("in(gene.entitySetId, 'ID1')", MUTATION_CENTRIC);
    assertStructure(root, "transcript.gene._gene_id");
  }

  @Test
  public void mutationEntitySetTest_donor() {
    queryContext = new QueryContext("", DONOR_CENTRIC);
    val root = createEsAst("in(mutation.entitySetId, 'ID1')", DONOR_CENTRIC);
    assertStructure(root, "gene.ssm._mutation_id");
  }

  @Test
  public void mutationEntitySetTest_gene() {
    queryContext = new QueryContext("", GENE_CENTRIC);
    val root = createEsAst("in(mutation.entitySetId, 'ID1')", GENE_CENTRIC);
    assertStructure(root, "donor.ssm._mutation_id");
  }

  @Test
  public void mutationEntitySetTest_mutation() {
    queryContext = new QueryContext("", MUTATION_CENTRIC);
    val root = createEsAst("in(mutation.entitySetId, 'ID1')", MUTATION_CENTRIC);
    assertStructure(root, "_mutation_id");
  }

  private void assertStructure(ExpressionNode root, String expectedField) {
    val result = root.accept(visitor, Optional.of(queryContext));
    log.debug("After Visitor: {}", result);

    // QueryNode - FilterNode - BoolNode - MustBoolNode - TermsNode
    val termsNode = (TermsNode) result.get()
        .getFirstChild() // Query
        .getFirstChild() // Filter
        .getFirstChild(); // Terms
    assertThat(termsNode.childrenCount()).isEqualTo(1);
    assertThat(termsNode.getField()).isEqualTo(expectedField);

    val lookup = termsNode.getLookup();
    val typeModel = getTypeModelBySearchField(expectedField);
    assertThat(lookup.getIndex()).isEqualTo(typeModel.getInternalField(LOOKUP_INDEX));
    assertThat(lookup.getType()).isEqualTo(typeModel.getInternalField(LOOKUP_TYPE));
    assertThat(lookup.getPath()).isEqualTo(typeModel.getInternalField(LOOKUP_PATH));
    assertThat(lookup.getId()).isEqualTo("ID1");
  }

  private static TypeModel getTypeModelBySearchField(String searchField) {
    if (searchField.contains("_donor_id")) {
      return IndexModel.getDonorCentricTypeModel();
    }

    if (searchField.contains("_gene_id")) {
      return IndexModel.getGeneCentricTypeModel();
    }

    if (searchField.contains("_mutation_id")) {
      return IndexModel.getMutationCentricTypeModel();
    }

    throw new IllegalArgumentException(format("Failed to resolve type model for '%s'", searchField));
  }

}
