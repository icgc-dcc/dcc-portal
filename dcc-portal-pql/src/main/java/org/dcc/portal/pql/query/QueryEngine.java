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
package org.dcc.portal.pql.query;

import static java.lang.String.format;
import static org.dcc.portal.pql.ast.visitor.Visitors.createEsAstVisitor;
import static org.dcc.portal.pql.meta.IndexModel.getTypeModel;
import static org.dcc.portal.pql.meta.Type.DIAGRAM;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.dcc.portal.pql.meta.Type.DRUG;
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.dcc.portal.pql.meta.Type.OBSERVATION_CENTRIC;
import static org.dcc.portal.pql.meta.Type.PROJECT;
import static org.dcc.portal.pql.meta.Type.REPOSITORY_FILE;

import java.util.Optional;

import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.es.ast.ExpressionNode;
import org.dcc.portal.pql.es.utils.EsAstTransformer;
import org.dcc.portal.pql.meta.Type;
import org.elasticsearch.client.Client;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryEngine {

  private static final EsAstTransformer esAstTransformator = new EsAstTransformer();
  private final EsRequestBuilder requestBuilder;

  private final QueryContext donorContext;
  private final QueryContext geneContext;
  private final QueryContext mutationContext;
  private final QueryContext observationContext;
  private final QueryContext projectContext;
  private final QueryContext repositoryFileContext;
  private final QueryContext drugContext;
  private final QueryContext diagramContext;

  public QueryEngine(@NonNull Client client, @NonNull String index) {
    this.requestBuilder = new EsRequestBuilder(client);

    this.donorContext = new QueryContext(index, DONOR_CENTRIC);
    this.geneContext = new QueryContext(index, GENE_CENTRIC);
    this.mutationContext = new QueryContext(index, MUTATION_CENTRIC);
    this.observationContext = new QueryContext(index, OBSERVATION_CENTRIC);
    this.projectContext = new QueryContext(index, PROJECT);
    this.repositoryFileContext = new QueryContext(index, REPOSITORY_FILE);
    this.drugContext = new QueryContext(index, DRUG);
    this.diagramContext = new QueryContext(index, DIAGRAM);
  }

  public QueryRequest execute(@NonNull String pql, @NonNull Type type) {
    val pqlAst = PqlParser.parse(pql);
    return execute(pqlAst, type);
  }

  public QueryRequest execute(@NonNull StatementNode pqlAst, @NonNull Type type) {
    ExpressionNode esAst = resolvePqlAst(pqlAst, type);
    log.debug("Resolved PQL AST into ES AST: {}", esAst);

    val context = createQueryContext(type);
    esAst = esAstTransformator.process(esAst, context);

    val result = new QueryRequest(requestBuilder.buildSearchRequest(esAst, context));

    return result;
  }

  private static ExpressionNode resolvePqlAst(StatementNode pqlAst, Type type) {
    return pqlAst.accept(createEsAstVisitor(), Optional.of(getTypeModel(type)));
  }

  private QueryContext createQueryContext(Type type) {
    switch (type) {
    case DONOR_CENTRIC:
      return donorContext;
    case GENE_CENTRIC:
      return geneContext;
    case MUTATION_CENTRIC:
      return mutationContext;
    case OBSERVATION_CENTRIC:
      return observationContext;
    case PROJECT:
      return projectContext;
    case REPOSITORY_FILE:
      return repositoryFileContext;
    case DRUG:
      return drugContext;
    case DIAGRAM:
      return diagramContext;
    default:
      throw new IllegalArgumentException(format("Type %s is not supported", type.getId()));
    }
  }
}
