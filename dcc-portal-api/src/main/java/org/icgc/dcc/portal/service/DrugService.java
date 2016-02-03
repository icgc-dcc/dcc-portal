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
package org.icgc.dcc.portal.service;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static org.dcc.portal.pql.meta.Type.DRUG;
import static org.icgc.dcc.portal.model.Drug.parse;

import java.util.Collection;
import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.ast.StatementNode;
import org.dcc.portal.pql.query.PqlParser;
import org.elasticsearch.search.SearchHit;
import org.icgc.dcc.portal.model.Drug;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.repository.DrugRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.FluentIterable;

/**
 * Service to facilitate drug query operations
 */
@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class DrugService {

  private static final Jql2PqlConverter QUERY_CONVERTER = Jql2PqlConverter.getInstance();
  // TODO: use DrugTypeModel.Fields
  private static final String PQL_FIND_DRUGS_BY_GENES = "in (drug.ensemblGeneId, %s), sort (+name)";
  private static final String SINGLE_QUOTE = "'";
  private static final String COMMA = ",";

  private final DrugRepository repository;

  @NonNull
  public List<Drug> findAll(Query query) {
    return findAll(toAst(query));
  }

  @NonNull
  public List<Drug> findDrugsByGeneIds(Collection<String> geneIds) {
    if (geneIds.isEmpty()) {
      return emptyList();
    }

    val genes = geneIds.stream()
        .map(id -> SINGLE_QUOTE + id + SINGLE_QUOTE)
        .collect(joining(COMMA));
    val pql = format(PQL_FIND_DRUGS_BY_GENES, genes);

    return findAll(PqlParser.parse(pql));
  }

  @NonNull
  public Drug findOne(String id) {
    val response = repository.findOne(id);

    return parse(response.getSourceAsString());
  }

  private List<Drug> findAll(StatementNode pqlAst) {
    val response = repository.findAll(pqlAst);

    log.debug("Response of findAll is: {}", response);

    return FluentIterable.from(response.getHits())
        .transform(DrugService::toDrug)
        .toList();
  }

  private static StatementNode toAst(Query query) {
    val pql = QUERY_CONVERTER.convert(query, DRUG);

    return PqlParser.parse(pql);
  }

  private static Drug toDrug(SearchHit hit) {
    return parse(hit.getSourceAsString());
  }

}
