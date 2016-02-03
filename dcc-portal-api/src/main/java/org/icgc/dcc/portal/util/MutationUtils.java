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
package org.icgc.dcc.portal.util;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static java.lang.Boolean.FALSE;
import static java.util.Collections.singletonList;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.portal.util.MapUtils.map;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.NoArgsConstructor;
import lombok.val;

import com.google.common.collect.TreeMultimap;

@NoArgsConstructor(access = PRIVATE)
public final class MutationUtils {

  /**
   * Extracts the donor id from the supplied ssm occurrence.
   * 
   * @param ssmOccurrence - the ssm occurrence
   * @return the extracted donor id
   */
  public static String donorId(Map<String, Object> ssmOccurrence) {
    return ssmOccurrence == null ? null : (String) donor(ssmOccurrence).get("_donor_id");
  }

  /**
   * Extracts the donor from the supplied ssm occurrence.
   * 
   * @param ssmOccurrence - the ssm occurrence
   * @return the extracted donor
   */
  public static Map<String, Object> donor(Map<String, Object> ssmOccurrence) {
    return ssmOccurrence == null ? null : map(ssmOccurrence.get("donor"));
  }

  /**
   * Determines the unique affected project count from the supplied ssm occurrences.
   * 
   * @param ssmOccurrences - the list of ssm occurrences of the related mutation
   * @return the affected project count
   */
  public static Set<String> affectedProjectIds(List<Map<String, Object>> ssmOccurrences) {
    // Sorted for stability
    Set<String> uniqueProjectIds = newTreeSet();

    for (val ssmOccurrence : ssmOccurrences) {
      uniqueProjectIds.add(projectId(ssmOccurrence));
    }

    return uniqueProjectIds;
  }

  /**
   * Determines the unique affected donor counts by project from the supplied ssm occurrences.
   * 
   * @param ssmOccurrences - the list of ssm occurrences of the related mutation
   * @return the affected donor counts by project
   */
  public static List<Map<String, Object>> affectedProjectDonorCounts(List<Map<String, Object>> ssmOccurrences) {
    // Sorted for stability
    TreeMultimap<String, String> projectDonorIds = TreeMultimap.create();
    Map<String, String> projectNames = newHashMap();

    for (val ssmOccurrence : ssmOccurrences) {
      String projectId = projectId(ssmOccurrence);
      projectNames.put(projectId, projectName(ssmOccurrence));
      projectDonorIds.put(projectId, donorId(ssmOccurrence));
    }

    List<Map<String, Object>> records = newArrayList();
    for (String projectId : projectDonorIds.keySet()) {
      // Attributes
      String projectName = projectNames.get(projectId);
      Integer projectDonorCount = projectDonorIds.get(projectId).size();

      // Assemble
      Map<String, Object> record = newLinkedHashMap();
      record.put("_project_id", projectId);
      record.put("project_name", projectName);
      record.put("count", projectDonorCount);

      records.add(record);
    }

    return records;
  }

  /**
   * Extracts the project from the supplied ssm occurrence.
   * 
   * @param ssmOccurrence - the ssm occurrence
   * @return the project
   */
  public static Map<String, Object> project(Map<String, Object> ssmOccurrence) {
    return map(ssmOccurrence.get("project"));
  }

  /**
   * Extracts the project id from the supplied ssm occurrence.
   * 
   * @param ssmOccurrence - the ssm occurrence to extract from
   * @return the extracted project id
   */
  public static String projectId(Map<String, Object> ssmOccurrence) {
    return (String) project(ssmOccurrence).get("_project_id");
  }

  /**
   * Extracts the project name from the supplied ssm occurrence.
   * 
   * @param ssmOccurrence - the ssm occurrence to extract from
   * @return the extracted project name
   */
  public static String projectName(Map<String, Object> ssmOccurrence) {
    return (String) project(ssmOccurrence).get("project_name");
  }

  /**
   * Extracts the {@code validation_status} from the supplied ssm occurrence.
   * 
   * @param ssmOccurrence - the ssm occurrence to extract from
   * @return the extracted validation_status
   */
  public static String validationStatus(Map<String, Object> ssmOccurrence) {
    String missingValue = "";
    return ssmOccurrence == null ? missingValue : firstNonNull((String) ssmOccurrence.get("validation_status"),
        missingValue);
  }

  /**
   * Extracts the {@code verification_status} from the supplied ssm occurrence.
   * 
   * @param ssmOccurrence - the ssm occurrence to extract from
   * @return the extracted verification_status
   */
  public static String verificationStatus(Map<String, Object> ssmOccurrence) {
    String missingValue = "";
    return ssmOccurrence == null ? missingValue : firstNonNull((String) ssmOccurrence.get("verification_status"),
        missingValue);
  }

  /**
   * Extracts the project donor count from the supplied project.
   * 
   * @param ssmOccurrence - the project
   * @return the total donor count
   */
  public static Integer projectTotalDonorCount(Map<String, Object> project) {
    return (Integer) map(project.get("_summary")).get("_total_donor_count");
  }

  /**
   * Determines the unique affected project donor count from the supplied ssm occurrences.
   * 
   * @param projectId - the target project id
   * @param ssmOccurrences - the list of ssm occurrences of the related mutation
   * @return the affected donor count
   */
  public static int projectAffectedDonorCount(String projectId, List<Map<String, Object>> ssmOccurrences) {
    return projectsAffectedDonorCount(singletonList(projectId), ssmOccurrences);
  }

  /**
   * Determines the total unique donor count from the supplied ssm occurrences.
   * 
   * @param ssmOccurrences - the list of ssm occurrences of the related mutation
   * @return the total affected donor count
   */
  public static int projectsAffectedDonorCount(List<Map<String, Object>> ssmOccurrences) {
    return projectsAffectedDonorCount(Collections.<String> emptyList(), ssmOccurrences);
  }

  /**
   * Determines the total unique donor count from the supplied ssm occurrences.
   * 
   * @param projectIds - the target project ids
   * @param ssmOccurrences - the list of ssm occurrences of the related mutation
   * @return the total affected donor count
   */
  public static int projectsAffectedDonorCount(Collection<String> projectIds, List<Map<String, Object>> ssmOccurrences) {
    // Consider all projects?
    boolean anyProject = projectIds.isEmpty();

    Set<String> uniqueDonorIds = newHashSet();

    for (Map<String, Object> ssmOccurrence : ssmOccurrences) {
      if (anyProject || projectIds.contains(projectId(ssmOccurrence))) {
        uniqueDonorIds.add(donorId(ssmOccurrence));
      }
    }

    return uniqueDonorIds.size();
  }

  /**
   * Determines the total project donor count from the supplied ssm occurrences.
   * 
   * @param projectId - the target project id
   * @param ssmOccurrences - the list of ssm occurrences of the related mutation
   * @return the total donor count
   */
  public static int projectTotalDonorCount(String projectId, List<Map<String, Object>> ssmOccurrences) {
    for (Map<String, Object> ssmOccurrence : ssmOccurrences) {
      boolean target = projectId.equals(projectId(ssmOccurrence));
      if (target) {
        return projectTotalDonorCount(project(ssmOccurrence));
      }
    }

    throw new RuntimeException("Could not find project with project id " + projectId + " in mutation.");
  }

  /**
   * Determines the total projects donor count from the supplied ssm occurrences.
   * 
   * @param ssmOccurrences - the list of ssm occurrences of the related mutation
   * @return the total donor count
   */
  public static int projectsTotalDonorCount(List<Map<String, Object>> ssmOccurrences) {
    Set<String> uniqueProjectIds = newHashSet();

    int totalDonorCount = 0;
    for (Map<String, Object> ssmOccurrence : ssmOccurrences) {
      String projectId = projectId(ssmOccurrence);

      if (!uniqueProjectIds.contains(projectId)) {
        // Accumulate newly encountered project donor count
        int projectTotalDonorCount = projectTotalDonorCount(project(ssmOccurrence));
        totalDonorCount += projectTotalDonorCount;

        // Remember to ignore this next time we encounter it
        uniqueProjectIds.add(projectId);
      }
    }

    return totalDonorCount;
  }

  /**
   * Extracts the transcript id from the supplied transcript.
   * 
   * @param transcript - the transcript to extract the id from
   * @return the extracted transcript id
   */
  public static String transcriptId(Map<String, Object> transcript) {
    return transcript == null ? null : (String) transcript.get("id");
  }

  /**
   * Extracts the transcript name from the supplied transcript.
   * 
   * @param transcript - the transcript to extract the id from
   * @return the extracted transcript name
   */
  public static String transcriptName(Map<String, Object> transcript) {
    return transcript == null ? null : (String) transcript.get("name");
  }

  /**
   * Extracts the transcript canonical status from the supplied transcript.
   * 
   * @param transcript - the transcript to extract the status from
   * @return the extracted transcript canonical status
   */
  public static Boolean isCanonical(Map<String, Object> transcript) {
    return transcript == null ? FALSE : firstNonNull((Boolean) transcript.get("is_canonical"), FALSE);
  }

  /**
   * Extracts the gene from the supplied transcript.
   * 
   * @param transcript - the transcript to extract the gene from
   * @return the extracted gene
   */
  public static Map<String, Object> gene(Map<String, Object> transcript) {
    return transcript == null ? null : map(transcript.get("gene"));
  }

  /**
   * Extracts the gene id from the supplied gene.
   * 
   * @param gene - the gene to extract from
   * @return the gene id
   */
  public static Object geneId(Map<String, Object> gene) {
    return gene == null ? null : gene.get("_gene_id");
  }

  /**
   * Extracts the gene symbol from the supplied gene.
   * 
   * @param gene - the gene to extract from
   * @return the gene id
   */
  public static Object geneSymbol(Map<String, Object> gene) {
    return gene == null ? null : gene.get("symbol");
  }

  /**
   * Extracts the gene strand from the supplied gene.
   * 
   * @param gene - the gene to extract from
   * @return the gene strand
   */
  public static Object geneStrand(Map<String, Object> gene) {
    return gene == null ? null : gene.get("strand");
  }

  /**
   * Extracts the consequences for the supplied mutation transcripts.
   * 
   * @param transcripts - the mutation transcripts
   * @return the extracted consequences
   */
  public static List<Map<String, Object>> consequences(List<Map<String, Object>> transcripts) {
    if (transcripts == null || transcripts.isEmpty()) {
      return null;
    }

    // Transform
    List<Map<String, Object>> records = newArrayList();
    for (Map<String, Object> transcript : transcripts) {
      Map<String, Object> consequence = newLinkedHashMap();

      // Data
      consequence.put("_gene_id", geneId(gene(transcript)));
      consequence.put("gene_symbol", geneSymbol(gene(transcript)));
      consequence.put("consequence_type", consequenceType(transcript));
      consequence.put("aa_mutation", aaMutation(transcript));
      consequence.put("protein_domain_affected", proteinDomainAffected(transcript));
      consequence.put("is_canonical", isCanonical(transcript));
      consequence.put("transcript_id", transcriptId(transcript));
      consequence.put("name", transcriptName(transcript));

      records.add(consequence);
    }

    return records;
  }

  /**
   * Extracts the consequence from the supplied transcript.
   * 
   * @param transcript - the transcript to extract the consequence from
   * @return the consequence
   */
  public static Map<String, Object> consequence(Map<String, Object> transcript) {
    return transcript == null ? null : map(transcript.get("consequence"));
  }

  /**
   * Extracts the amino acid mutation from the supplied transcript.
   * 
   * @param transcript - the transcript to extract the amino acid mutation from
   * @return the amino acid mutation
   */
  public static String aaMutation(Map<String, Object> transcript) {
    val consequence = consequence(transcript);
    if (consequence == null) {
      return null;
    }

    return transcript == null ? null : (String) consequence.get("aa_mutation");
  }

  /**
   * Extracts the cds mutation from the supplied transcript.
   * 
   * @param transcript - the transcript to extract the cds mutation from
   * @return the cds mutation
   */
  public static String cdsMutation(Map<String, Object> transcript) {
    val consequence = consequence(transcript);
    if (consequence == null) {
      return null;
    }

    return transcript == null ? null : (String) consequence.get("cds_mutation");
  }

  /**
   * Extracts the consequence type from the supplied transcript.
   * 
   * @param transcript - the transcript to extract the consequence type from
   * @return the consequence type
   */
  public static String consequenceType(Map<String, Object> transcript) {
    val consequence = consequence(transcript);
    if (consequence == null) {
      return null;
    }

    return transcript == null ? null : (String) consequence.get("consequence_type");
  }

  /**
   * Extracts the protein domain affected from the supplied transcript.
   * 
   * @param transcript - the transcript to extract the protein domain affected from
   * @return the protein domain affected
   */
  public static String proteinDomainAffected(Map<String, Object> transcript) {
    val consequence = consequence(transcript);
    if (consequence == null) {
      return null;
    }

    return transcript == null ? null : (String) consequence.get("protein_domain_affected");
  }

}
