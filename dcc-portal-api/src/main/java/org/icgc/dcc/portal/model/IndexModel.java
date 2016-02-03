package org.icgc.dcc.portal.model;

import static com.google.common.base.Preconditions.checkState;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.dcc.portal.pql.meta.IndexModel.getRepositoryFileTypeModel;
import static org.dcc.portal.pql.meta.TypeModel.ENTITY_SET_ID;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dcc.portal.pql.meta.RepositoryFileTypeModel;
import org.dcc.portal.pql.meta.TypeModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

@Component
public class IndexModel {

  /**
   * Constants.
   */
  public static final int MAX_FACET_TERM_COUNT = 1024;
  public static final String IS = "is";
  public static final String ALL = "all";
  public static final String NOT = "not";
  public static final String MISSING = "_missing";

  public static final Set<String> GENES_SORT_FIELD_NAMES = ImmutableSet.of("symbol", "name", "start", "type",
      "affectedDonorCountFiltered");

  /**
   * Special cases for term lookups
   */
  public static final String API_ENTITY_LIST_ID_FIELD_NAME = ENTITY_SET_ID;

  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  public static enum Kind {
    PROJECT("project"),
    DONOR("donor"),
    GENE("gene"),
    MUTATION("mutation"),
    PATHWAY("pathway"),

    GENE_SET("geneSet"),
    REPOSITORY_FILE("file"),

    CONSEQUENCE("consequence"),
    TRANSCRIPT("transcript"),
    OCCURRENCE("occurrence"),
    EMB_OCCURRENCE("embOccurrence"),
    OBSERVATION("observation"),
    RELEASE("release"),
    KEYWORD(""),
    SPECIMEN(""),
    SAMPLE(""),
    SEQ_DATA(""),
    DOMAIN(""),
    EXON(""),
    DIAGRAM("diagram"),
    FAMILY(""),
    EXPOSURE(""),
    THERAPY("");

    private final String id;
  }

  // Index document type
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  public static enum Type {
    PROJECT("project"),
    DONOR("donor"),
    GENE("gene"),
    DRUG("drug"),
    MUTATION("mutation"),
    RELEASE("release"),
    PATHWAY("pathway"),
    GENE_SET("gene-set"),
    DONOR_CENTRIC("donor-centric"),
    GENE_CENTRIC("gene-centric"),
    MUTATION_CENTRIC("mutation-centric"),
    OCCURRENCE_CENTRIC("observation-centric"),
    REPOSITORY_FILE("file"),
    REPOSITORY_FILE_CENTRIC("file-centric"),

    DONOR_TEXT("donor-text"),
    GENE_TEXT("gene-text"),
    MUTATION_TEXT("mutation-text"),
    PATHWAY_TEXT("pathway-text"),
    GENESET_TEXT("gene-set-text"),
    DIAGRAM("diagram"),
    REPOSITORY_FILE_TEXT("file-text"),
    REPOSITORY_FILE_DONOR_TEXT("donor-text"),
    DRUG_TEXT("drug-text"),
    PROJECT_TEXT("project-text");

    @NonNull
    private final String id;
  }

  private static final TypeModel REPO_FILE_TYPE_MODEL = getRepositoryFileTypeModel();

  // These names are used by the client
  private static final List<String> REPOSITORY_FILE_CLIENT_FIELD_ALIAS_MAPPING = ImmutableList.<String> builder()
      .add(RepositoryFileTypeModel.Fields.ID)
      .add(RepositoryFileTypeModel.Fields.REPO_NAME)
      .add(RepositoryFileTypeModel.Fields.REPO_CODE)
      .add(RepositoryFileTypeModel.Fields.FILE_NAME)
      .add(RepositoryFileTypeModel.Fields.FILE_SIZE)
      .add(RepositoryFileTypeModel.Fields.PROJECT_CODE)
      .add(RepositoryFileTypeModel.Fields.PRIMARY_SITE)
      .add(RepositoryFileTypeModel.Fields.STUDY)
      .add(RepositoryFileTypeModel.Fields.DONOR_STUDY)
      .add(RepositoryFileTypeModel.Fields.DATA_TYPE)
      .add(RepositoryFileTypeModel.Fields.EXPERIMENTAL_STRATEGY)
      .add(RepositoryFileTypeModel.Fields.FILE_FORMAT)
      .add(RepositoryFileTypeModel.Fields.ACCESS)
      .add(RepositoryFileTypeModel.Fields.DONOR_ID)
      .add(RepositoryFileTypeModel.Fields.SUBMITTED_DONOR_ID)
      .add(RepositoryFileTypeModel.Fields.SPECIMEN_ID)
      .add(RepositoryFileTypeModel.Fields.SPECIMEN_TYPE)
      .add(RepositoryFileTypeModel.Fields.SAMPLE_ID)
      .add(RepositoryFileTypeModel.Fields.PROGRAM)
      .add(RepositoryFileTypeModel.Fields.ANALYSIS_SOFTWARE)
      .build();
  private static final ImmutableMap<String, String> REPOSITORY_FILE_FIELDS_MAPPING = ImmutableMap
      .<String, String> builder()
      .putAll(Maps.toMap(REPOSITORY_FILE_CLIENT_FIELD_ALIAS_MAPPING, alias -> REPO_FILE_TYPE_MODEL.getField(alias)))
      .put(API_ENTITY_LIST_ID_FIELD_NAME, API_ENTITY_LIST_ID_FIELD_NAME)
      .build();

  private static final ImmutableMap<String, String> FAMILY_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("donorHasRelativeWithCancerHistory", "donor_has_relative_with_cancer_history")
          .put("relationshipType", "relationship_type")
          .put("relationshipTypeOther", "relationship_type_other")
          .put("relationshipSex", "relationship_sex")
          .put("relationshipAge", "relationship_age")
          .put("relationshipDiseaseICD10", "relationship_disease_icd10")
          .put("relationshipDisease", "relationship_disease")
          .build();

  private static final ImmutableMap<String, String> EXPOSURE_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("exposureType", "exposure_type")
          .put("exposureIntensity", "exposure_intensity")
          .put("tobaccoSmokingHistoryIndicator", "tobacco_smoking_history_indicator")
          .put("tobaccoSmokingIntensity", "tobacco_smoking_intensity")
          .put("alcoholHistory", "alcohol_history")
          .put("alcoholHistoryIntensity", "alcohol_history_intensity")
          .build();

  private static final ImmutableMap<String, String> THERAPY_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("firstTherapyType", "first_therapy_type")
          .put("firstTherapyTherapeuticIntent", "first_therapy_therapeutic_intent")
          .put("firstTherapyStartInterval", "first_therapy_start_interval")
          .put("firstTherapyDuration", "first_therapy_duration")
          .put("firstTherapyResponse", "first_therapy_response")
          .put("secondTherapyType", "second_therapy_type")
          .put("secondTherapyTherapeuticIntent", "second_therapy_therapeutic_intent")
          .put("secondTherapyStartInterval", "second_therapy_start_interval")
          .put("secondTherapyDuration", "second_therapy_duration")
          .put("secondTherapyResponse", "second_therapy_response")
          .put("otherTherapy", "other_therapy")
          .put("otherTherapyResponse", "other_therapy_response")
          .build();

  private static final ImmutableMap<String, String> SPECIMEN_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "_specimen_id")
          .put("submittedId", "specimen_id")
          .put("available", "specimen_available")
          .put("digitalImageOfStainedSection", "digital_image_of_stained_section")
          .put("dbXref", "specimen_db_xref")
          .put("biobank", "specimen_biobank")
          .put("biobankId", "specimen_biobank_id")
          .put("treatmentType", "specimen_donor_treatment_type")
          .put("treatmentTypeOther", "specimen_donor_treatment_type_other")
          .put("processing", "specimen_processing")
          .put("processingOther", "specimen_processing_other")
          .put("storage", "specimen_storage")
          .put("storageOther", "specimen_storage_other")
          .put("type", "specimen_type")
          .put("typeOther", "specimen_type_other")
          .put("uri", "specimen_uri")
          .put("tumourConfirmed", "tumour_confirmed")
          .put("tumourGrade", "tumour_grade")
          .put("tumourGradeSupplemental", "tumour_grade_supplemental")
          .put("tumourHistologicalType", "tumour_histological_type")
          .put("tumourStage", "tumour_stage")
          .put("tumourStageSupplemental", "tumour_stage_supplemental")
          .put("tumourStageSystem", "tumour_stage_system")
          .put("percentCellularity", "percent_cellularity")
          .put("levelOfCellularity", "level_of_cellularity")
          .build();

  private static final ImmutableMap<String, String> SAMPLE_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "_sample_id")
          .put("analyzedId", "analyzed_sample_id")
          .put("analyzedInterval", "analyzed_sample_interval")
          .put("availableRawSequenceData", "available_raw_sequence_data")
          .put("study", "study")
          .build();

  private static final ImmutableMap<String, String> RAWSEQDATA_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "analysis_id")
          .put("platform", "platform")
          .put("state", "state")
          .put("type", "sample_type")
          .put("libraryStrategy", "library_strategy")
          .put("analyteCode", "analyte_code")
          .put("dataUri", "analysis_data_uri")
          .put("repository", "repository")
          .put("filename", "filename")
          .put("rawDataAccession", "raw_data_accession")
          .build();

  private static final ImmutableMap<String, String> TRANSCRIPT_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "id")
          .put("name", "name")
          .put("type", "biotype")
          .put("isCanonical", "is_canonical")
          .put("start", "start")
          .put("end", "end")
          .put("cdnaCodingStart", "cdna_coding_start")
          .put("cdnaCodingEnd", "cdna_coding_end")
          .put("codingRegionStart", "coding_region_start")
          .put("codingRegionEnd", "coding_region_end")
          .put("startExon", "start_exon")
          .put("endExon", "end_exon")
          .put("length", "length")
          .put("lengthAminoAcid", "length_amino_acid")
          .put("lengthCds", "length_cds")
          .put("numberOfExons", "number_of_exons")
          .put("seqExonStart", "seq_exon_start")
          .put("seqExonEnd", "seq_exon_end")
          .put("translationId", "translation_id")
          .put("functionalImpact", "functional_impact_prediction_summary")
          .build();

  private static final ImmutableMap<String, String> CONSEQUENCE_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("geneAffectedId", "gene_affected")
          .put("geneAffectedSymbol", "gene_symbol")
          .put("transcriptAffected", "transcript_affected")
          .put("aaMutation", "aa_mutation")
          .put("geneStrand", "gene_strand")
          .put("cdsMutation", "cds_mutation")
          .put("type", "consequence_type")
          .put("functionalImpact", "functional_impact_prediction_summary")
          .build();

  private static final ImmutableMap<String, String> DOMAIN_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("interproId", "interpro_id")
          .put("hitName", "hit_name")
          .put("description", "description")
          .put("gffSource", "gff_source")
          .put("start", "start")
          .put("end", "end")
          .build();

  private static final ImmutableMap<String, String> EXON_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("start", "start")
          .put("end", "end")
          .put("cdnaCodingStart", "cdna_coding_start")
          .put("cdnaCodingEnd", "cdna_coding_end")
          .put("genomicCodingStart", "genomic_coding_start")
          .put("genomicCodingEnd", "genomic_coding_end")
          .put("cdnaStart", "cdna_start")
          .put("cdnaEnd", "cdna_end")
          .put("endPhase", "end_phase")
          .put("startPhase", "startPhase")
          .build();

  // This need to be used for ssm_occurrence in Mutations
  // FIXME: remove extra fields that are now in observations
  private static final ImmutableMap<String, String> EMB_OCCURRENCE_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("donorId", "_donor_id")
          .put("submittedMutationId", "mutation_id")
          .put("mutationId", "_mutation_id")
          .put("matchedSampleId", "_matched_sample_id")
          .put("submittedMatchedSampleId", "matched_sample_id")
          .put("projectId", "_project_id")
          .put("primarySite", "project.primary_site")
          .put("tumourType", "project.tumour_type")
          .put("tumourSubtype", "project.tumour_subtype")
          .put("sampleId", "_sample_id")
          .put("specimenId", "_specimen_id")
          .put("analysisId", "analysis_id")
          .put("analyzedSampleId", "analyzed_sample_id")
          .put("baseCallingAlgorithm", "base_calling_algorithm")
          .put("strand", "chromosome_strand")
          .put("chromosome", "chromosome")
          .put("start", "chromosome_start")
          .put("end", "chromosome_end")
          .put("controlGenotype", "control_genotype")
          .put("experimentalProtocol", "experimental_protocol")
          .put("expressedAllele", "expressed_allele")
          .put("platform", "platform")
          .put("probability", "probability")
          .put("qualityScore", "quality_score")
          .put("rawDataAccession", "raw_data_accession")
          .put("rawDataRepository", "raw_data_repository")
          .put("readCount", "read_count")
          .put("refsnpAllele", "refsnp_allele")
          .put("seqCoverage", "seq_coverage")
          .put("sequencingStrategy", "sequencing_strategy")
          .put("ssmMDbXref", "ssm_m_db_xref")
          .put("ssmMUri", "ssm_m_uri")
          .put("ssmPUri", "ssm_p_uri")
          .put("tumourGenotype", "tumour_genotype")
          .put("variationCallingAlgorithm", "variation_calling_algorithm")
          .put("verificationPlatform", "verification_platform")
          .put("verificationStatus", "verification_status")
          .put("xrefEnsemblVarId", "xref_ensembl_var_id")
          .build();

  private static final ImmutableMap<String, String> OBSERVATION_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("alignmentAlgorithm", "alignment_algorithm")
          .put("analysisId", "analysis_id")
          .put("baseCallingAlgorithm", "base_calling_algorithm")
          .put("biologicalValidationStatus", "biological_validation_status")
          .put("experimentalProtocol", "experimental_protocol")
          .put("icgcSampleId", "_sample_id")
          .put("icgcSpecimenId", "_specimen_id")
          .put("matchedICGCSampleId", "_matched_sample_id")
          .put("mutantAlleleReadCount", "mutant_allele_read_count")
          .put("otherAnalysisAlgorithm", "other_analysis_algorithm")
          .put("platform", "platform")
          .put("probability", "probability")
          .put("rawDataAccession", "raw_data_accession")
          .put("rawDataRepository", "raw_data_repository")
          .put("sequencingStrategy", "sequencing_strategy")
          .put("submittedMatchedSampleId", "matched_sample_id")
          .put("submittedSampleId", "analyzed_sample_id")
          .put("totalReadCount", "total_read_count")
          .put("variationCallingAlgorithm", "variation_calling_algorithm")
          .put("verificationStatus", "verification_status")
          .put(API_ENTITY_LIST_ID_FIELD_NAME, API_ENTITY_LIST_ID_FIELD_NAME)

          .build();

  private static final ImmutableMap<String, String> RELEASE_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "_release_id")
          .put("name", "name")
          .put("releasedOn", "date")
          .put("donorCount", "donor_count")
          .put("liveDonorCount", "live_donor_count")
          .put("mutationCount", "mutation_count")
          .put("sampleCount", "sample_count")
          .put("projectCount", "project_count")
          .put("liveProjectCount", "live_project_count")
          .put("specimenCount", "specimen_count")
          .put("ssmCount", "ssm_count")
          .put("primarySiteCount", "primary_site_count")
          .put("livePrimarySiteCount", "live_primary_site_count")
          .put("mutatedGeneCount", "mutated_gene_count")
          .put("releaseNumber", "number")
          .build();

  private static final ImmutableMap<String, String> DIAGRAM_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "diagram_id")
          .put("xml", "xml")
          .put("proteinMap", "protein_map")
          .put("highlights", "highlights")
          .build();

  private static Map<String, String> DRUG_KEYWORD_FIELDS = ImmutableList.of(
      "inchikey", "drug_class", "atc_codes_code", "atc_codes_description", "atc_level5_codes",
      "trials_description", "trials_conditions_name", "external_references_drugbank", "external_references_chembl")
      .stream().collect(
          toMap(field -> Type.DRUG_TEXT.id + "." + field, identity()));

  private static final ImmutableMap<String, String> KEYWORD_FIELDS_MAPPING = ImmutableMap.<String, String> builder()
      // Common
      .put("id", "id")
      .put("type", "type")

      // Gene and project and pathway
      .put("name", "name")

      // Gene
      .put("symbol", "symbol")
      .put("ensemblTranscriptId", "ensemblTranscriptId")
      .put("ensemblTranslationId", "ensemblTranslationId")
      .put("synonyms", "synonyms")
      .put("uniprotkbSwissprot", "uniprotkbSwissprot")
      .put("omimGene", "omimGene")
      .put("entrezGene", "entrezGene")
      .put("hgnc", "hgnc")

      // Mutation
      .put("mutation", "mutation")
      .put("geneMutations", "geneMutations")
      .put("start", "start")

      // Project
      .put("tumourType", "tumourType")
      .put("tumourSubtype", "tumourSubtype")
      .put("primarySite", "primarySite")

      // Donor
      .put("specimenIds", "specimenIds")
      .put("submittedSpecimenIds", "submittedSpecimenIds")
      .put("sampleIds", "sampleIds")
      .put("submittedSampleIds", "submittedSampleIds")
      .put("projectId", "projectId")

      // Donor-file, these are derived from file
      .put("submittedId", "submittedId")
      .put("TCGAParticipantBarcode", "TCGAParticipantBarcode")
      .put("TCGASampleBarcode", "TCGASampleBarcode")
      .put("TCGAAliquotBarcode", "TCGAAliquotBarcode")

      // GO Term
      .put("altIds", "altIds")

      // File Repo
      .put("file_name", "file_name")
      .put("donor_id", "donor_id")
      .put("object_id", "object_id")
      .put("data_type", "data_type")
      .put("project_code", "project_code")

      // Drug-text
      .putAll(DRUG_KEYWORD_FIELDS)

      .build();

  private static final ImmutableMap<String, String> PATHWAY_FIELDS_MAPPING =
      new ImmutableMap.Builder<String, String>()
          .put("id", "pathway_id")
          .put("name", "pathway_name")
          .put("summation", "summation")
          .put("source", "source")
          .put("species", "species")
          .put("uniprotId", "uniprotId")
          .put("url", "url")
          .put("evidenceCode", "evidence_code")
          .put("projects", "projects")
          .put("parentPathways", "parent_pathways")
          .put("linkOut", "link_out")
          .put("geneCount", "gene_count")
          .build();

  private static final ImmutableMap<Kind, TypeModel> TYPE_MODEL_BY_KIND = ImmutableMap.<Kind, TypeModel> builder()
      .put(Kind.DONOR, org.dcc.portal.pql.meta.IndexModel.getDonorCentricTypeModel())
      .put(Kind.MUTATION, org.dcc.portal.pql.meta.IndexModel.getMutationCentricTypeModel())
      .put(Kind.OCCURRENCE, org.dcc.portal.pql.meta.IndexModel.getObservationCentricTypeModel())
      .put(Kind.GENE, org.dcc.portal.pql.meta.IndexModel.getGeneCentricTypeModel())
      .put(Kind.GENE_SET, org.dcc.portal.pql.meta.IndexModel.getGeneSetTypeModel())
      .put(Kind.PROJECT, org.dcc.portal.pql.meta.IndexModel.getProjectTypeModel())
      .build();

  /*
   * private static final ImmutableMap<String, String> GO_SET_FIELDS_MAPPING = new ImmutableMap.Builder<String,
   * String>() .put("ontology", "ontology") .put("altIds", "alt_ids") .put("synonyms", "synonyms") .put("inferredTree",
   * "inferred_tree") .build();
   * 
   * private static final ImmutableMap<String, String> PATHWAY_SET_FIELDS_MAPPING = new ImmutableMap.Builder<String,
   * String>() .put("hierarchy", "hierarchy") .build();
   */

  public static final EnumMap<Kind, ImmutableMap<String, String>> FIELDS_MAPPING =
      new EnumMap<Kind, ImmutableMap<String, String>>(Kind.class);

  static {
    FIELDS_MAPPING.put(Kind.PROJECT, createFieldsMapping(Kind.PROJECT));
    FIELDS_MAPPING.put(Kind.DONOR, createFieldsMapping(Kind.DONOR));
    FIELDS_MAPPING.put(Kind.SPECIMEN, SPECIMEN_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.FAMILY, FAMILY_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.EXPOSURE, EXPOSURE_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.THERAPY, THERAPY_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.SAMPLE, SAMPLE_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.SEQ_DATA, RAWSEQDATA_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.GENE, createFieldsMapping(Kind.GENE));
    FIELDS_MAPPING.put(Kind.MUTATION, createFieldsMapping(Kind.MUTATION));
    FIELDS_MAPPING.put(Kind.TRANSCRIPT, TRANSCRIPT_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.CONSEQUENCE, CONSEQUENCE_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.DOMAIN, DOMAIN_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.EXON, EXON_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.EMB_OCCURRENCE, EMB_OCCURRENCE_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.OBSERVATION, OBSERVATION_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.OCCURRENCE, createFieldsMapping(Kind.OCCURRENCE));
    FIELDS_MAPPING.put(Kind.RELEASE, RELEASE_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.KEYWORD, KEYWORD_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.PATHWAY, PATHWAY_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.GENE_SET, createFieldsMapping(Kind.GENE_SET));
    FIELDS_MAPPING.put(Kind.DIAGRAM, DIAGRAM_FIELDS_MAPPING);
    FIELDS_MAPPING.put(Kind.REPOSITORY_FILE, REPOSITORY_FILE_FIELDS_MAPPING);
  }

  /**
   * Internal gene set types
   */
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  public static enum GeneSetType {
    GENE_SET_TYPE_ALL("geneSetId"),
    GENE_SET_TYPE_GO("go_term"),
    GENE_SET_TYPE_PATHWAY("pathway"),
    GENE_SET_TYPE_CURATED("curated_set");

    private final String type;
  }

  /**
   * Mapping query ID fields to internal types
   */
  public static final Map<String, String> GENE_SET_QUERY_ID_FIELDS = ImmutableMap.<String, String> builder()
      .put(GeneSetType.GENE_SET_TYPE_ALL.getType(), "geneSetId")
      .put(GeneSetType.GENE_SET_TYPE_CURATED.getType(), "curatedSetId")
      .put(GeneSetType.GENE_SET_TYPE_PATHWAY.getType(), "pathwayId")
      .put(GeneSetType.GENE_SET_TYPE_GO.getType(), "goTermId").build();

  /**
   * Mapping query type fields to internal type
   */
  public static final Map<String, String> GENE_SET_QUERY_TYPE_FIELDS = ImmutableMap.<String, String> of(
      GeneSetType.GENE_SET_TYPE_CURATED.getType(), "hasCuratedSet",
      GeneSetType.GENE_SET_TYPE_PATHWAY.getType(), "hasPathway",
      GeneSetType.GENE_SET_TYPE_GO.getType(), "hasGoTerm");

  private String index;
  private String repoIndexName;

  @Autowired
  public IndexModel(@Value("#{indexName}") String index, @Value("#{repoIndexName}") String repoIndexName) {
    super();
    this.index = index;
    this.repoIndexName = repoIndexName;
  }

  public String getIndex() {
    return this.index;
  }

  public String getRepoIndex() {
    return this.repoIndexName;
  }

  public static String[] getFields(Query query, Kind kind) {
    val typeFieldsMap = FIELDS_MAPPING.get(kind);
    val result = Lists.<String> newArrayList();

    if (query.hasFields()) {
      for (val field : query.getFields()) {
        if (typeFieldsMap.containsKey(field)) {
          result.add(typeFieldsMap.get(field));
        }
      }
    } else {
      result.addAll(typeFieldsMap.values().asList());
    }
    clearInvalidFields(result, kind);
    return result.toArray(new String[result.size()]);
  }

  /**
   * Remove fields that are objects in ES. They must be retrieved from source
   */
  private static void clearInvalidFields(List<String> fields, Kind kind) {
    val typeFieldsMap = FIELDS_MAPPING.get(kind);

    switch (kind) {
    case GENE:
      fields.remove(typeFieldsMap.get("externalDbIds"));
      fields.remove(typeFieldsMap.get("pathways"));
      break;
    case PROJECT:
      fields.remove(typeFieldsMap.get("experimentalAnalysisPerformedDonorCounts"));
      fields.remove(typeFieldsMap.get("experimentalAnalysisPerformedSampleCounts"));
      break;
    case OCCURRENCE:
      fields.remove(typeFieldsMap.get("observation"));
      break;
    case GENE_SET:
      fields.remove(typeFieldsMap.get("hierarchy"));
      fields.remove(typeFieldsMap.get("inferredTree"));
      fields.remove(typeFieldsMap.get("synonyms"));
      fields.remove(typeFieldsMap.get("altIds"));
      break;
    default:
      break;
    }
  }

  private static ImmutableMap<String, String> createFieldsMapping(Kind kind) {
    val typeModel = TYPE_MODEL_BY_KIND.get(kind);
    checkState(typeModel != null, "TypeModel is not defined for kind '%s'", kind);
    val result = typeModel.getAliases().stream()
        .collect(toMap(k -> k, v -> typeModel.getField(v)));

    return ImmutableMap.copyOf(result);
  }

}
