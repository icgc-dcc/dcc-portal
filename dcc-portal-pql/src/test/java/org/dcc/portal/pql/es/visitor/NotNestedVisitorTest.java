/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.dcc.portal.pql.es.visitor;

import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;

import java.util.Optional;

import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.meta.Type;
import org.dcc.portal.pql.query.EsRequestBuilder;
import org.dcc.portal.pql.query.QueryContext;
import org.dcc.portal.pql.utils.BaseElasticsearchTest;
import org.junit.Before;
import org.junit.Test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotNestedVisitorTest extends BaseElasticsearchTest {

  CreateQueryBuilderVisitor visitor = new CreateQueryBuilderVisitor();
  QueryContext context = new QueryContext("", Type.DONOR_CENTRIC);
  Optional<QueryContext> contextOptional = Optional.of(context);

  private EsRequestBuilder esVisitor;

  @Before
  public void setUp() {
    es.execute(createIndexMappings(DONOR_CENTRIC).withData(bulkFile(getClass())));
    esVisitor = new EsRequestBuilder(es.client());
    queryContext = new QueryContext(INDEX_NAME, DONOR_CENTRIC);
  }

  @Test
  public void testNot_basic() {
    val query = "not(nested(gene, eq(gene.id, 'G1')))";

    ExpressionNode esAst = createTree(query);
    esAst = esAstTransformator.process(esAst, queryContext);
    val request = esVisitor.buildSearchRequest(esAst, context);

    log.info("ES Visitor Visitor: {}", request);
  }

  @Test
  public void testNot_complex() {
    val query = "select(*), eq(id, 'D1'), nested(gene, eq(gene.type, 'protein_coding'), eq(gene.chromosome, '12'))," +
        "not(nested(gene, eq(gene.id, 'G1'))), not(nested(gene, eq(gene.symbol, 'TTN')))";

    ExpressionNode esAst = createTree(query);
    esAst = esAstTransformator.process(esAst, queryContext);
    val request = esVisitor.buildSearchRequest(esAst, context);

    log.info("ES Visitor Visitor: {}", request);
  }

}
