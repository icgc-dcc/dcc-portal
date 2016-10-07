/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.server.model;

import static org.icgc.dcc.portal.server.model.IndexModel.FIELDS_MAPPING;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getLong;
import static org.icgc.dcc.portal.server.util.ElasticsearchResponseUtils.getString;

import java.util.Map;

import lombok.Value;
import lombok.val;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel(value = "Occurrence")
public class EmbOccurrence {

  @ApiModelProperty(value = "Affected Donor ID", required = true)
  String donorId;
  @ApiModelProperty(value = "Mutation ID", required = true)
  String mutationId;
  @ApiModelProperty(value = "Chromosome", required = true)
  String chromosome;
  @ApiModelProperty(value = "Start Position", required = true)
  Long start;
  @ApiModelProperty(value = "End Position", required = true)
  Long end;
  @ApiModelProperty(value = "submitted Mutation ID", required = true)
  String submittedMutationId;
  @ApiModelProperty(value = "Matched Sample ID", required = true)
  String matchedSampleId;
  @ApiModelProperty(value = "Submitted Matched Sample ID", required = true)
  String submittedMatchedSampleId;
  @ApiModelProperty(value = "Affected Project Identifiable", required = true)
  String projectId;
  @ApiModelProperty(value = "Sample ID", required = true)
  String sampleId;
  @ApiModelProperty(value = "Specimen ID", required = true)
  String specimenId;
  @ApiModelProperty(value = "Analysis ID", required = true)
  String analysisId;
  @ApiModelProperty(value = "Analyzed Sample ID", required = true)
  String analyzedSampleId;
  @ApiModelProperty(value = "Base Calling Algorithm", required = true)
  String baseCallingAlgorithm;
  @ApiModelProperty(value = "Strand", required = true)
  Long strand;
  @ApiModelProperty(value = "Control Genotype", required = true)
  String controlGenotype;
  @ApiModelProperty(value = "Experimental Protocol", required = true)
  String experimentalProtocol;
  @ApiModelProperty(value = "Expressed Allele", required = true)
  String expressedAllele;
  @ApiModelProperty(value = "Platform", required = true)
  String platform;
  @ApiModelProperty(value = "Probability", required = true)
  Double probability;
  @ApiModelProperty(value = "Quality Score", required = true)
  Double qualityScore;
  @ApiModelProperty(value = "Raw Data Accession", required = true)
  String rawDataAccession;
  @ApiModelProperty(value = "Raw Data Repository", required = true)
  String rawDataRepository;
  @ApiModelProperty(value = "Read Count", required = true)
  Double readCount;
  @ApiModelProperty(value = "Ref Snp Allele", required = true)
  String refsnpAllele;
  @ApiModelProperty(value = "Sequence Coverage", required = true)
  Double seqCoverage;
  @ApiModelProperty(value = "Sequencing Strategy", required = true)
  String sequencingStrategy;
  @ApiModelProperty(value = "SSM M Db Xref", required = true)
  String ssmMDbXref;
  @ApiModelProperty(value = "SSM M URI", required = true)
  String ssmMUri;
  @ApiModelProperty(value = "SSM P URI", required = true)
  String ssmPUri;
  @ApiModelProperty(value = "Tumour Genotype", required = true)
  String tumourGenotype;
  @ApiModelProperty(value = "Variation Calling Algorithm", required = true)
  String variationCallingAlgorithm;
  @ApiModelProperty(value = "Verification Platform", required = true)
  String verificationPlatform;
  @ApiModelProperty(value = "Verification Status", required = true)
  String verificationStatus;
  @ApiModelProperty(value = "xref Ensembl VarId", required = true)
  String xrefEnsemblVarId;
  @ApiModelProperty(value = "Cancer Project of Affected Donor", required = true)
  Project project;
  @ApiModelProperty(value = "Affected Donor", required = true)
  Donor donor;

  @SuppressWarnings("unchecked")
  public EmbOccurrence(Map<String, Object> fieldMap) {
    val fields = FIELDS_MAPPING.get(EntityType.EMB_OCCURRENCE);
    mutationId = getString(fieldMap.get(fields.get("mutationId")));
    chromosome = getString(fieldMap.get(fields.get("chromosome")));
    start = getLong(fieldMap.get(fields.get("start")));
    end = getLong(fieldMap.get(fields.get("end")));
    submittedMutationId = getString(fieldMap.get(fields.get("submittedMutationId")));
    matchedSampleId = getString(fieldMap.get(fields.get("matchedSampleId")));
    submittedMatchedSampleId = getString(fieldMap.get(fields.get("submittedMatchedSampleId")));
    sampleId = getString(fieldMap.get(fields.get("sampleId")));
    specimenId = getString(fieldMap.get(fields.get("specimenId")));
    analysisId = getString(fieldMap.get(fields.get("analysisId")));
    analyzedSampleId = getString(fieldMap.get(fields.get("analyzedSampleId")));
    baseCallingAlgorithm = getString(fieldMap.get(fields.get("baseCallingAlgorithm")));
    strand = getLong(fieldMap.get(fields.get("strand")));
    controlGenotype = getString(fieldMap.get(fields.get("controlGenotype")));
    experimentalProtocol = getString(fieldMap.get(fields.get("experimentalProtocol")));
    expressedAllele = getString(fieldMap.get(fields.get("expressedAllele")));
    platform = getString(fieldMap.get(fields.get("platform")));
    probability = (Double) fieldMap.get(fields.get("probability"));
    qualityScore = (Double) fieldMap.get(fields.get("qualityScore"));
    rawDataAccession = getString(fieldMap.get(fields.get("rawDataAccession")));
    rawDataRepository = getString(fieldMap.get(fields.get("rawDataRepository")));
    readCount = (Double) fieldMap.get(fields.get("readCount"));
    refsnpAllele = getString(fieldMap.get(fields.get("refsnpAllele")));
    seqCoverage = (Double) fieldMap.get(fields.get("seqCoverage"));
    sequencingStrategy = getString(fieldMap.get(fields.get("sequencingStrategy")));
    ssmMDbXref = getString(fieldMap.get(fields.get("ssmMDbXref")));
    ssmMUri = getString(fieldMap.get(fields.get("ssmMUri")));
    ssmPUri = getString(fieldMap.get(fields.get("ssmPUri")));
    tumourGenotype = getString(fieldMap.get(fields.get("tumourGenotype")));
    variationCallingAlgorithm = getString(fieldMap.get(fields.get("variationCallingAlgorithm")));
    verificationPlatform = getString(fieldMap.get(fields.get("verificationPlatform")));
    verificationStatus = getString(fieldMap.get(fields.get("verificationStatus")));
    xrefEnsemblVarId = getString(fieldMap.get(fields.get("xrefEnsemblVarId")));
    Map<String, Object> p = (Map<String, Object>) fieldMap.get("project");
    p.put("_summary._total_donor_count", ((Map<String, Object>) p.get("_summary")).get("_total_donor_count"));
    p.put("_summary._affected_donor_count", ((Map<String, Object>) p.get("_summary")).get("_affected_donor_count"));
    p.put("_summary._available_data_type", ((Map<String, Object>) p.get("_summary")).get("_available_data_type"));
    p.put("_summary._ssm_tested_donor_count", ((Map<String, Object>) p.get("_summary")).get("_ssm_tested_donor_count"));
    project = new Project(p);
    projectId = getProjectId(p);
    val d = (Map<String, Object>) fieldMap.get("donor");
    donorId = getDonorId(d);
    donor = new Donor(d);
  }

  private static String getDonorId(Map<String, Object> donor) {
    val fields = FIELDS_MAPPING.get(EntityType.DONOR);

    return getString(donor.get(fields.get("id")));
  }

  private static String getProjectId(Map<String, Object> project) {
    val fields = FIELDS_MAPPING.get(EntityType.PROJECT);

    return getString(project.get(fields.get("id")));
  }

}
