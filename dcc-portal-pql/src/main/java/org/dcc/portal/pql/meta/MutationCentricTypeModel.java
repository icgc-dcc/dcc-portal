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

import static org.dcc.portal.pql.meta.field.ArrayFieldModel.arrayOfStrings;
import static org.dcc.portal.pql.meta.field.ArrayFieldModel.nestedArrayOfObjects;
import static org.dcc.portal.pql.meta.field.LongFieldModel.long_;
import static org.dcc.portal.pql.meta.field.ObjectFieldModel.object;
import static org.dcc.portal.pql.meta.field.StringFieldModel.identifiableString;
import static org.dcc.portal.pql.meta.field.StringFieldModel.string;

import java.util.List;
import java.util.Map;

import org.dcc.portal.pql.meta.field.ArrayFieldModel;
import org.dcc.portal.pql.meta.field.FieldModel;
import org.dcc.portal.pql.meta.field.ObjectFieldModel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class MutationCentricTypeModel extends TypeModel {

  private final static String TYPE_PREFIX = "mutation";

  // Including real fields, not aliases. Because after the AST is built by PqlParseTreeVisitor includes are resolved to
  // the real fields
  private static final List<String> INCLUDE_FIELDS = ImmutableList.of("transcript", "ssm_occurrence");
  private static final List<String> AVAILABLE_FACETS = ImmutableList.of(
      "type",
      "consequenceType",
      "platform",
      "verificationStatus",
      "functionalImpact",
      "sequencingStrategy");

  private static final List<String> PUBLIC_FIELDS = ImmutableList.of(
      "id",
      "mutation",
      "type",
      "chromosome",
      "start",
      "end",
      "affectedDonorCountTotal",
      "testedDonorCount",
      "consequenceType",
      // "consequenceTypeNested",
      "platform",
      // "platformNested",
      "verificationStatus",
      // "verificationStatusNested",
      "assemblyVersion",
      "referenceGenomeAllele",
      "affectedProjectCount",
      "affectedProjectIds",
      "affectedDonorCountFiltered",
      "transcriptId",
      "functionalImpact",
      // "functionalImpactNested",
      "mutation.location",
      "sequencingStrategy"
      // "sequencingStrategyNested"
      );

  public MutationCentricTypeModel() {
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
        .add(string("assembly_version", "assemblyVersion"))
        .add(string("chromosome", ImmutableSet.of("chromosome", "mutation.chromosome")))
        .add(long_("chromosome_start", ImmutableSet.of("start", "mutation.start")))
        .add(long_("chromosome_end", ImmutableSet.of("end", "mutation.end")))
        .add(identifiableString("_mutation_id", "id"))
        .add(string("mutation", "mutation"))
        .add(string("mutation_type", "type"))
        .add(string("reference_genome_allele", "referenceGenomeAllele"))
        .add(defineSsmOccurrence())
        .add(defineTranscript())
        // .add(string("platform", "platform"))
        // .add(arrayOfStrings("verification_status", "verificationStatus"))
        // .add(arrayOfStrings("sequencing_strategy", "sequencingStrategy"))
        // .add(arrayOfStrings("consequence_type", "consequenceType"))
        // .add(arrayOfStrings("functional_impact_prediction_summary", "functionalImpact"))
        .add(defineSummary())

        // Fake fields for GeneSetFilterVisitor
        .add(string(GENE_GO_TERM_ID, GENE_GO_TERM_ID))
        .add(string(GENE_SET_ID, GENE_SET_ID))

        // Fake fields for LocationFilterVisitor
        .add(string(GENE_LOCATION, GENE_LOCATION))
        .add(string(MUTATION_LOCATION, MUTATION_LOCATION))

        .add(string(SCORE, ImmutableSet.of(SCORE, "affectedDonorCountFiltered")))
        .build();
  }

  private static ObjectFieldModel defineSummary() {
    return object("_summary",
        long_("_affected_donor_count", "affectedDonorCountTotal"),
        string("_tested_donor_count", "testedDonorCount"),
        string("_affected_project_count", "affectedProjectCount"),
        arrayOfStrings("_affected_project_ids", "affectedProjectIds"));
  }

  private static ArrayFieldModel defineTranscript() {
    return nestedArrayOfObjects("transcript", ImmutableSet.of("transcripts", "consequences"),
        object(
            string("id", "transcriptId"),
            string("functional_impact_prediction_summary", "functionalImpact"),
            object("consequence", string("consequence_type", "consequenceType")),
            object("gene",
                identifiableString("_gene_id", "gene.id"),
                string("biotype", "gene.type"),
                string("chromosome", "gene.chromosome"),
                long_("end", "gene.end"),
                long_("start", "gene.start"),
                string("symbol", "gene.symbol"),
                arrayOfStrings("pathway", "gene.pathwayId"),
                arrayOfStrings("curated_set", "gene.curatedSetId"),
                arrayOfStrings("drug", "gene.compoundId"),
                object("go_term", "gene.GoTerm",
                    arrayOfStrings("biological_process"),
                    arrayOfStrings("cellular_component"),
                    arrayOfStrings("molecular_function")))));
  }

  private static ArrayFieldModel defineSsmOccurrence() {
    return nestedArrayOfObjects("ssm_occurrence", "occurrences",
        object(
            defineDonor(),
            defineProject(),
            nestedArrayOfObjects("observation", object(
                string("platform", "platform"),
                string("verification_status", "verificationStatus"),
                string("sequencing_strategy", "sequencingStrategy")))));
  }

  private static ObjectFieldModel defineProject() {
    return object("project",
        string("_project_id", "donor.projectId"),
        string("primary_site", "donor.primarySite"),
        string("project_name", "donor.projectName"));
  }

  private static ObjectFieldModel defineDonor() {
    return object("donor",
        identifiableString("_donor_id", "donor.id"),
        string("donor_sex", "donor.gender"),
        string("donor_tumour_stage_at_diagnosis", "donor.tumourStageAtDiagnosis"),
        string("donor_vital_status", "donor.vitalStatus"),
        string("disease_status_last_followup", "donor.diseaseStatusLastFollowup"),
        string("donor_relapse_type", "donor.relapseType"),
        object("_summary",
            string("_age_at_diagnosis_group", "donor.ageAtDiagnosisGroup"),
            string("_state", "donor.state"),
            arrayOfStrings("_available_data_type", "donor.availableDataTypes"),
            arrayOfStrings("_studies", "donor.studies"),
            arrayOfStrings("experimental_analysis_performed", "donor.analysisTypes")));
  }

  private static Map<String, String> defineInternalAliases() {
    return new ImmutableMap.Builder<String, String>()
        .put(BIOLOGICAL_PROCESS, "transcript.gene.go_term.biological_process")
        .put(CELLULAR_COMPONENT, "transcript.gene.go_term.cellular_component")
        .put(MOLECULAR_FUNCTION, "transcript.gene.go_term.molecular_function")
        .put(LOOKUP_TYPE, MUTATION_LOOKUP)
        .build();
  }

  @Override
  public Type getType() {
    return Type.MUTATION_CENTRIC;
  }

}
