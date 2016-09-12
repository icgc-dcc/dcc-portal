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
package org.dcc.portal.pql.meta;

import static org.dcc.portal.pql.meta.field.ArrayFieldModel.arrayOfObjects;
import static org.dcc.portal.pql.meta.field.ArrayFieldModel.arrayOfStrings;
import static org.dcc.portal.pql.meta.field.ArrayFieldModel.nestedArrayOfObjects;
import static org.dcc.portal.pql.meta.field.LongFieldModel.long_;
import static org.dcc.portal.pql.meta.field.ObjectFieldModel.object;
import static org.dcc.portal.pql.meta.field.StringFieldModel.identifiableString;
import static org.dcc.portal.pql.meta.field.StringFieldModel.string;

import java.util.List;
import java.util.Map;

import lombok.val;

import org.dcc.portal.pql.meta.field.ArrayFieldModel;
import org.dcc.portal.pql.meta.field.FieldModel;
import org.dcc.portal.pql.meta.field.ObjectFieldModel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class GeneCentricTypeModel extends TypeModel {

  private final static String TYPE_PREFIX = "gene";
  private static final List<String> AVAILABLE_FACETS = ImmutableList.of("type");

  // Including real fields, not aliases. Because after the AST is built by PqlParseTreeVisitor includes are resolved to
  // the real fields
  private static final List<String> INCLUDE_FIELDS = ImmutableList.of("transcripts", "external_db_ids", "project");

  private static final List<String> PUBLIC_FIELDS = ImmutableList.of(
      "id",
      "symbol",
      "name",
      "type",
      "chromosome",
      "start",
      "end",
      "strand",
      "description",
      "synonyms",
      "externalDbIds",
      "affectedDonorCountTotal",
      "affectedDonorCountFiltered",
      "affectedTranscriptIds",
      "gene.location",
      "pathwayId",
      "pathways",

      // NOTE: Centric and non-centric are modelled different. Set only exists in gene and not gene-centric
      "sets");

  public GeneCentricTypeModel() {
    super(defineFields(), defineInternalAliases(), PUBLIC_FIELDS, INCLUDE_FIELDS);
  }

  @Override
  public List<String> getFacets() {
    return AVAILABLE_FACETS;
  }

  @Override
  public String prefix() {
    return TYPE_PREFIX;
  }

  private static List<FieldModel> defineFields() {
    return new ImmutableList.Builder<FieldModel>()
        .add(identifiableString("_gene_id", "id"))
        .add(string("symbol", "symbol"))
        .add(string("name", "name"))
        .add(string("biotype", "type"))
        .add(string("chromosome", ImmutableSet.of("chromosome", "gene.chromosome")))
        .add(long_("start", ImmutableSet.of("start", "gene.start")))
        .add(long_("end", ImmutableSet.of("end", "gene.end")))
        .add(long_("strand", "strand"))
        .add(defineDonor())
        .add(string("description", "description"))
        .add(arrayOfStrings("synonyms", "synonyms"))
        .add(defineExternalDbIds())
        .add(defineSummary())
        .add(arrayOfObjects("project", "projects", object()))
        .add(arrayOfStrings("pathway", ImmutableSet.of("pathways", "pathwayId", "gene.pathwayId")))
        .add(arrayOfStrings("curated_set", ImmutableSet.of("curatedSetId", "gene.curatedSetId")))
        .add(arrayOfStrings("drug", "gene.compoundId"))
        .add(arrayOfObjects("transcripts", "transcripts", object()))
        .add(object("go_term", GENE_GO_TERM,
            arrayOfStrings("biological_process"),
            arrayOfStrings("cellular_component"),
            arrayOfStrings("molecular_function")))

        // Fake fields for GeneSetFilterVisitor
        .add(string(GENE_GO_TERM_ID, GENE_GO_TERM_ID))
        .add(string(GENE_SET_ID, GENE_SET_ID))

        // Fake fields for LocationFilterVisitor
        .add(string(GENE_LOCATION, GENE_LOCATION))
        .add(string(MUTATION_LOCATION, MUTATION_LOCATION))

        .add(string(SCORE, ImmutableSet.of(SCORE, "affectedDonorCountFiltered")))

        // NOTE: Centric and non-centric are modelled different. Set only exists in gene and not gene-centric
        .add(arrayOfObjects("sets", "sets", object()))

        .build();
  }

  private static ObjectFieldModel defineExternalDbIds() {
    return object("external_db_ids", "externalDbIds",
        arrayOfStrings("entrez_gene"));
  }

  private static ObjectFieldModel defineSummary() {
    return object("_summary",
        long_("_affected_donor_count", "affectedDonorCountTotal"),
        long_("_affected_project_count"),
        arrayOfStrings("_affected_transcript_id", "affectedTranscriptIds"),
        long_("_total_mutation_count"),
        long_("_unique_mutation_count"));
  }

  private static ArrayFieldModel defineDonor() {
    val element = object(
        identifiableString("_donor_id", "donor.id"),
        defineDonorSummary(),
        string("disease_status_last_followup", "donor.diseaseStatusLastFollowup"),
        string("donor_relapse_type", "donor.relapseType"),
        string("donor_sex", "donor.gender"),
        // FIXME: This field has different spelling in different types
        string("donor_tumour_stage_at_diagnosis", "donor.tumourStageAtDiagnosis"),
        string("donor_vital_status", "donor.vitalStatus"),
        string("_summary._state", "donor.state"),
        arrayOfStrings("_summary._studies", "donor.studies"),
        object("project",
            string("_project_id", "donor.projectId"),
            string("primary_site", "donor.primarySite"),
            string("project_name", "donor.projectName")),
        defineSsm());

    return nestedArrayOfObjects("donor", element);
  }

  private static ObjectFieldModel defineDonorSummary() {
    return object("_summary",
        string("_age_at_diagnosis_group", "donor.ageAtDiagnosisGroup"),
        string("_available_data_type", "donor.availableDataTypes"),
        string("experimental_analysis_performed", "donor.analysisTypes"));
  }

  private static ArrayFieldModel defineSsm() {
    val element = object(
        identifiableString("_mutation_id", "mutation.id"),
        string("mutation_type", "mutation.type"),
        string("chromosome", "mutation.chromosome"),
        long_("chromosome_end", "mutation.end"),
        long_("chromosome_start", "mutation.start"),
        defineConsequence(),
        defineObservation());

    return nestedArrayOfObjects("ssm", element);
  }

  private static ArrayFieldModel defineConsequence() {
    val element = object(
        string("consequence_type", "mutation.consequenceType"),
        string("functional_impact_prediction_summary", "mutation.functionalImpact"));

    return nestedArrayOfObjects("consequence", element);
  }

  private static ArrayFieldModel defineObservation() {
    val element = object(
        string("platform", "mutation.platform"),
        string("sequencing_strategy", "mutation.sequencingStrategy"),
        string("verification_status", "mutation.verificationStatus"));

    return nestedArrayOfObjects("observation", element);
  }

  private static Map<String, String> defineInternalAliases() {
    return new ImmutableMap.Builder<String, String>()
        .put(BIOLOGICAL_PROCESS, "go_term.biological_process")
        .put(CELLULAR_COMPONENT, "go_term.cellular_component")
        .put(MOLECULAR_FUNCTION, "go_term.molecular_function")
        .put(LOOKUP_TYPE, GENE_LOOKUP)
        .build();
  }

  @Override
  public Type getType() {
    return Type.GENE_CENTRIC;
  }

}
