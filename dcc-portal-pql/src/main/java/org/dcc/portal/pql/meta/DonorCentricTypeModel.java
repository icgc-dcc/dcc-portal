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
import static org.dcc.portal.pql.meta.field.BooleanFieldModel.bool;
import static org.dcc.portal.pql.meta.field.DoubleFieldModel.double_;
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

public class DonorCentricTypeModel extends TypeModel {

  private final static String TYPE_PREFIX = "donor";

  /**
   * Field aliases
   */
  public static class Fields {

    public static final String PROJECT_ID = "projectId";

  }

  // Including real fields, not aliases. Because after the AST is built by PqlParseTreeVisitor includes are resolved to
  // the real fields
  private static final List<String> INCLUDE_FIELDS = ImmutableList.of();
  private final static List<String> AVAILABLE_FACETS = ImmutableList.of(
      Fields.PROJECT_ID,
      "primarySite",
      "gender",
      "tumourStageAtDiagnosis",
      "vitalStatus",
      "diseaseStatusLastFollowup",
      "relapseType",
      "ageAtDiagnosisGroup",
      "availableDataTypes",
      "analysisTypes",
      "projectName",
      "studies",
      "state");

  private final static List<String> PUBLIC_FIELDS = ImmutableList.of(
      "id",
      "submittedDonorId",
      Fields.PROJECT_ID,
      "primarySite",
      "projectName",
      "tumourType",
      "tumourSubtype",
      "ssmCount",
      "cnsmExists",
      "stsmExists",
      "sgvExists",
      "pexpExists",
      "mirnaSeqExists",
      "methSeqExists",
      "methArrayExists",
      "expSeqExists",
      "expArrayExists",
      "jcnExists",
      "ageAtDiagnosis",
      "ageAtDiagnosisGroup",
      "ageAtEnrollment",
      "ageAtLastFollowup",
      "diagnosisIcd10",
      "diseaseStatusLastFollowup",
      "intervalOfLastFollowup",
      "gender",
      "vitalStatus",
      "tumourStageAtDiagnosis",
      "tumourStagingSystemAtDiagnosis",
      "tumourStageAtDiagnosisSupplemental",
      "relapseType",
      "relapseInterval",
      "survivalTime",
      "availableDataTypes",
      "analysisTypes",
      "studies",
      "ssmAffectedGenes",
      "state",
      "priorMalignancy",
      "cancerTypePriorMalignancy",
      "cancerHistoryFirstDegreeRelative");

  public DonorCentricTypeModel() {
    super(initFields(), defineInternalAliases(), PUBLIC_FIELDS, INCLUDE_FIELDS);
  }

  @Override
  public List<String> getFacets() {
    return AVAILABLE_FACETS;
  }

  @Override
  public String prefix() {
    return TYPE_PREFIX;
  }

  private static List<FieldModel> initFields() {
    val fields = new ImmutableList.Builder<FieldModel>();
    fields.add(identifiableString("_donor_id", "id"));
    fields.add(string("_project_id"));
    fields.add(initSummary());
    fields.add(string("disease_status_last_followup", "diseaseStatusLastFollowup"));
    fields.add(long_("donor_age_at_diagnosis", "ageAtDiagnosis"));
    fields.add(long_("donor_age_at_enrollment", "ageAtEnrollment"));
    fields.add(long_("donor_age_at_last_followup", "ageAtLastFollowup"));
    fields.add(string("donor_diagnosis_icd10", "diagnosisIcd10"));
    fields.add(string("donor_id", "submittedDonorId"));
    fields.add(long_("donor_interval_of_last_followup", "intervalOfLastFollowup"));
    fields.add(long_("donor_relapse_interval", "relapseInterval"));
    fields.add(string("donor_relapse_type", "relapseType"));
    fields.add(string("donor_sex", "gender"));
    fields.add(long_("donor_survival_time", "survivalTime"));
    fields.add(string("donor_tumour_stage_at_diagnosis", "tumourStageAtDiagnosis"));
    fields.add(string("donor_tumour_stage_at_diagnosis_supplemental", "tumourStageAtDiagnosisSupplemental"));
    fields.add(string("donor_tumour_staging_system_at_diagnosis", "tumourStagingSystemAtDiagnosis"));
    fields.add(string("donor_vital_status", "vitalStatus"));
    fields.add(initGene());
    fields.add(initProject());

    // This is a fake field to which is resolved by GeneSetFilterVisitor
    fields.add(string(GENE_GO_TERM_ID, GENE_GO_TERM_ID));
    fields.add(string(GENE_SET_ID, GENE_SET_ID));

    // This is a fake field to which is resolved by LocationFilterVisitor
    fields.add(string(GENE_LOCATION, GENE_LOCATION));
    fields.add(string(MUTATION_LOCATION, MUTATION_LOCATION));

    fields.add(string(SCORE, ImmutableSet.of(SCORE, "ssmAffectedGenes")));

    fields.add(string("prior_malignancy", "priorMalignancy"));
    fields.add(string("cancer_type_prior_malignancy", "cancerTypePriorMalignancy"));
    fields.add(string("cancer_history_first_degree_relative", "cancerHistoryFirstDegreeRelative"));

    return fields.build();
  }

  private static ObjectFieldModel initSummary() {
    return object("_summary",
        long_("_affected_gene_count"),
        string("_age_at_diagnosis_group", "ageAtDiagnosisGroup"),
        arrayOfStrings("_available_data_type", "availableDataTypes"),
        bool("_cngv_exists"),
        bool("_cnsm_exists", "cnsmExists"),
        bool("_exp_array_exists", "expArrayExists"),
        bool("_exp_seq_exists", "expSeqExists"),
        bool("_jcn_exists", "jcnExists"),
        bool("_meth_array_exists", "methArrayExists"),
        bool("_meth_seq_exists", "methSeqExists"),
        bool("_mirna_seq_exists", "mirnaSeqExists"),
        bool("_pexp_exists", "pexpExists"),
        bool("_sgv_exists", "sgvExists"),
        bool("_ssm_count", "ssmCount"),
        bool("_stgv_exists"),
        bool("_stsm_exists", "stsmExists"),
        string("_state", "state"),
        arrayOfStrings("_studies", "studies"),
        arrayOfStrings("experimental_analysis_performed", "analysisTypes"),
        initExperimentalAnalysisPerformedSampleCount(),
        arrayOfStrings("repository"));
  }

  private static ObjectFieldModel initExperimentalAnalysisPerformedSampleCount() {
    return object("experimental_analysis_performed_sample_count",
        long_("AMPLICON"),
        long_("Bisulfite-Seq"),
        long_("RNA-Seq"),
        long_("WGA"),
        long_("WGS"),
        long_("WXS"),
        long_("miRNA-Seq"),
        long_("non-NGS"));
  }

  private static ArrayFieldModel initGene() {
    val element = object(
        identifiableString("_gene_id", "gene.id"),
        object("_summary", long_("_ssm_count")),
        string("biotype", "gene.type"),
        string("chromosome", "gene.chromosome"),
        long_("end", "gene.end"),
        long_("start", "gene.start"),
        string("symbol", "gene.symbol"),
        arrayOfStrings("pathway", ImmutableSet.of("gene.pathways", "gene.pathwayId")),
        arrayOfStrings("curated_set", "gene.curatedSetId"),
        arrayOfStrings("drug", "gene.compoundId"),
        object("go_term", "gene.GoTerm",
            arrayOfStrings("biological_process"),
            arrayOfStrings("cellular_component"),
            arrayOfStrings("molecular_function")),
        nestedArrayOfObjects("ssm", initSmm()));

    return nestedArrayOfObjects("gene", element);
  }

  private static ObjectFieldModel initSmm() {
    return object(
        identifiableString("_mutation_id", "mutation.id"),
        string("_type"),
        string("chromosome", "mutation.chromosome"),
        long_("chromosome_end", "mutation.end"),
        long_("chromosome_start", "mutation.start"),
        nestedArrayOfObjects(
            "consequence",
            object(
                string("consequence_type", "mutation.consequenceType"),
                string("functional_impact_prediction_summary", "mutation.functionalImpact"))),
        string("mutation_type", "mutation.type"),
        nestedArrayOfObjects("observation", initObservation()));
  }

  private static ObjectFieldModel initObservation() {
    return object(
        string("_matched_sample_id"),
        string("_sample_id"),
        string("_specimen_id"),
        string("alignment_algorithm"),
        string("analysis_id"),
        string("analyzed_sample_id"),
        string("base_calling_algorithm"),
        string("biological_validation_platform"),
        string("biological_validation_status"),
        string("experimental_protocol"),
        string("marking"),
        string("matched_sample_id"),
        long_("mutant_allele_read_count"),
        string("observation_id"),
        string("other_analysis_algorithm"),
        string("platform", "mutation.platform"),
        double_("probability"),
        double_("quality_score"),
        string("raw_data_accession"),
        string("raw_data_repository"),
        double_("seq_coverage"),
        string("sequencing_strategy", "mutation.sequencingStrategy"),
        long_("total_read_count"),
        string("variation_calling_algorithm"),
        string("verification_platform"),
        string("verification_status", "mutation.verificationStatus"));
  }

  private static ObjectFieldModel initProject() {
    return object("project",
        string("_project_id", Fields.PROJECT_ID),
        string("primary_site", "primarySite"),
        string("project_name", "projectName"),
        string("tumour_type", "tumourType"),
        string("tumour_subtype", "tumourSubtype"));
  }

  private static Map<String, String> defineInternalAliases() {
    return new ImmutableMap.Builder<String, String>()
        .put(BIOLOGICAL_PROCESS, "gene.go_term.biological_process")
        .put(CELLULAR_COMPONENT, "gene.go_term.cellular_component")
        .put(MOLECULAR_FUNCTION, "gene.go_term.molecular_function")
        .put(LOOKUP_TYPE, DONOR_LOOKUP)
        .build();
  }

  @Override
  public Type getType() {
    return Type.DONOR_CENTRIC;
  }

}
