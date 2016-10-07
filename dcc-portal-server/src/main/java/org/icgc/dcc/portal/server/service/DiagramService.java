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
package org.icgc.dcc.portal.server.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.icgc.dcc.common.core.model.FieldNames.DIAGRAM_PROTEIN_MAP_DB_ID;
import static org.icgc.dcc.common.core.model.FieldNames.DIAGRAM_PROTEIN_MAP_UNIPROT_IDS;
import static org.icgc.dcc.common.core.model.FieldNames.GENE_UNIPROT_IDS;
import static org.icgc.dcc.common.core.model.FieldNames.MUTATION_CONSEQUENCE_TYPES;
import static org.icgc.dcc.common.core.model.FieldNames.MUTATION_FUNCTIONAL_IMPACT_PREDICTION_SUMMARY;
import static org.icgc.dcc.common.core.model.FieldNames.MUTATION_TRANSCRIPTS;
import static org.icgc.dcc.common.core.model.FieldNames.MUTATION_TRANSCRIPTS_CONSEQUENCE;
import static org.icgc.dcc.common.core.model.FieldNames.MUTATION_TRANSCRIPTS_GENE;
import static org.icgc.dcc.portal.server.util.SearchResponses.getTotalHitCount;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.icgc.dcc.common.core.util.Joiners;
import org.icgc.dcc.portal.server.model.DiagramProtein;
import org.icgc.dcc.portal.server.model.IndexModel;
import org.icgc.dcc.portal.server.model.EntityType;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.repository.DiagramRepository;
import org.icgc.dcc.portal.server.repository.MutationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }) )
public class DiagramService {

  @NonNull
  private final DiagramRepository diagramRepository;
  @NonNull
  private final MutationRepository mutationRepository;

  private ImmutableMap<String, String> INDEX_MODEL = IndexModel.FIELDS_MAPPING.get(EntityType.DIAGRAM);

  private static final List<String> CONSEQUENCE_TYPES = ImmutableList.<String> builder().add(
      "frameshift_variant",
      "missense_variant",
      "start_lost",
      "initiator_codon_variant",
      "stop_gained",
      "stop_lost",
      "exon_loss_variant",
      "exon_variant",
      "splice_acceptor_variant",
      "splice_donor_variant", "splice_region_variant",
      "5_prime_UTR_premature_start_codon_gain_variant",
      "disruptive_inframe_deletion",
      "inframe_deletion",
      "disruptive_inframe_insertion",
      "inframe_insertion").build();

  public Map<String, DiagramProtein> mapProteinIds(@NonNull String pathwayId, @NonNull String[] impactFilter) {
    val dbToUniprotMap = getProteinIdMap(pathwayId);
    if (dbToUniprotMap.isEmpty()) {
      return Maps.newHashMap();
    }
    val uniprotToDbMap = getReverseMap(dbToUniprotMap);

    val queries = new ArrayList<QueryBuilder>();
    uniprotToDbMap.keySet().forEach(id -> {
      queries.add(getQuery(id.toString(), impactFilter));
    });

    val response = mutationRepository.countSearches(queries);

    val map = Maps.<String, DiagramProtein> newHashMap();

    int count = 0;
    for (val uniprotId : uniprotToDbMap.keySet()) {
      val mutations = getTotalHitCount(response.getResponses()[count].getResponse());

      val protein = new DiagramProtein();
      protein.setValue(mutations);
      protein.setDbIds(Joiners.COMMA.join(uniprotToDbMap.get(uniprotId)));

      map.put(uniprotId.toString(), protein);
      count++;
    }

    return map;
  }

  public String getPathwayDiagramString(@NonNull String pathwayId) {
    val pathwayXml = getPathway(pathwayId).get(INDEX_MODEL.get("xml"));

    // return unescape(pathwayXml.toString());
    return pathwayXml.toString();
  }

  public List<String> getShownPathwaySection(@NonNull String pathwayId) {
    @SuppressWarnings("unchecked")
    val highlights = (List<String>) getPathway(pathwayId).get(INDEX_MODEL.get("highlights"));

    return highlights;
  }

  private Map<String, Object> getPathway(String id) {
    val query = Query.builder().build();
    return diagramRepository.findOne(id, query);
  }

  private Multimap<Object, Object> getReverseMap(Map<String, List<String>> map) {
    val reverse = ArrayListMultimap.create();
    map.forEach((dbId, uniprotIds) -> {
      for (String uniprotId : uniprotIds) {
        reverse.put(parseUniprot(uniprotId), dbId);
      }
    });
    return reverse;
  }

  private String parseUniprot(String uniprot) {
    return uniprot.substring(uniprot.indexOf(":") + 1);
  }

  @SuppressWarnings("unchecked")
  private Map<String, List<String>> getProteinIdMap(@NonNull String pathwayId) {
    val fieldName = INDEX_MODEL.get("proteinMap");

    val pathway = getPathway(pathwayId);
    val list = (List<Map<String, Object>>) pathway.get(fieldName);

    val map = Maps.<String, List<String>> newHashMap();
    if (list != null) {
      for (val element : list) {
        val dbId = (String) element.get(DIAGRAM_PROTEIN_MAP_DB_ID);
        val uniprotIds = (List<String>) element.get(DIAGRAM_PROTEIN_MAP_UNIPROT_IDS);

        map.put(dbId, uniprotIds);
      }
    }

    return map;
  }

  private BoolQueryBuilder getQuery(String id, String[] impacts) {
    val uniprotIdsFieldName = MUTATION_TRANSCRIPTS + "." + MUTATION_TRANSCRIPTS_GENE + "." + GENE_UNIPROT_IDS;
    val functionalImpactFieldName = MUTATION_TRANSCRIPTS + "." + MUTATION_FUNCTIONAL_IMPACT_PREDICTION_SUMMARY;
    val consequenceTypeFullPath =
        MUTATION_TRANSCRIPTS + "." + MUTATION_TRANSCRIPTS_CONSEQUENCE + "." + MUTATION_CONSEQUENCE_TYPES;

    val query = boolQuery().must(termQuery(uniprotIdsFieldName, id));
    if (impacts.length > 0) {
      query.must(termsQuery(functionalImpactFieldName, impacts));
    }
    query.must(termsQuery(consequenceTypeFullPath, CONSEQUENCE_TYPES));

    val nestedQuery = QueryBuilders.nestedQuery(MUTATION_TRANSCRIPTS, query);
    return boolQuery().must(nestedQuery);
  }
}