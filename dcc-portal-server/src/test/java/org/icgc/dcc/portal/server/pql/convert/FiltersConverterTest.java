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
package org.icgc.dcc.portal.server.pql.convert;

import static com.google.common.collect.ImmutableList.of;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.dcc.portal.pql.meta.IndexModel.getDonorCentricTypeModel;
import static org.dcc.portal.pql.meta.IndexModel.getMutationCentricTypeModel;
import static org.dcc.portal.pql.meta.Type.DONOR_CENTRIC;
import static org.dcc.portal.pql.meta.Type.FILE;
import static org.dcc.portal.pql.meta.Type.GENE_CENTRIC;
import static org.dcc.portal.pql.meta.Type.MUTATION_CENTRIC;
import static org.icgc.dcc.portal.server.pql.convert.FiltersConverter.createFilterByNestedPath;
import static org.icgc.dcc.portal.server.pql.convert.FiltersConverter.groupFieldsByNestedPath;
import static org.icgc.dcc.portal.server.pql.convert.FiltersConverter.groupNestedPaths;
import static org.icgc.dcc.portal.server.pql.convert.FiltersConverter.isEncloseWithCommonParent;
import static org.icgc.dcc.portal.server.pql.convert.model.Operation.IS;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.dcc.portal.pql.meta.IndexModel;
import org.dcc.portal.pql.meta.Type;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.pql.convert.model.JqlField;
import org.icgc.dcc.portal.server.pql.convert.model.JqlFilters;
import org.icgc.dcc.portal.server.pql.convert.model.JqlSingleValue;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@Slf4j
public class FiltersConverterTest {

  FiltersConverter converter = new FiltersConverter();
  private final static ObjectMapper mapper = createObjectMapper();

  @Test
  public void convertToEqualTest() {
    val result = converter.convertFilters(createFilters("{donor:{id:{is:'DO1'}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("eq(donor.id,'DO1')");
  }

  @Test
  public void convertToArrayTest() {
    val result = converter.convertFilters(createFilters("{donor:{id:{is:['DO1']}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("in(donor.id,'DO1')");
  }

  @Test
  public void convertToArrayTest_multivalue() {
    val result = converter.convertFilters(createFilters("{donor:{id:{is:['DO1', 'DO2']}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("in(donor.id,'DO1','DO2')");
  }

  @Test
  public void notTest() {
    val result = converter.convertFilters(createFilters("{donor:{id:{not:'DO1'}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("not(eq(donor.id,'DO1'))");
  }

  @Test
  public void notTest_array() {
    val result = converter.convertFilters(createFilters("{donor:{id:{not:['DO1','DO2']}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("not(in(donor.id,'DO1','DO2'))");
  }

  @Test
  public void existsTest() {
    val result = converter.convertFilters(createFilters("{donor:{hasCuratedSet:true}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("exists(gene.curatedSetId)");
  }

  @Test
  public void missingTest() {
    val result = converter.convertFilters(createFilters("{donor:{id:{not:'DO1'}, hasPathway:false}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("missing(gene.pathwayId),not(eq(donor.id,'DO1'))");
  }

  @Test
  public void missingValueTest() {
    val result = converter.convertFilters(createFilters("{donor:{id:{is:['_missing']}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("missing(donor.id)");
  }

  @Test
  public void missingValueTest_array() {
    val result = converter.convertFilters(createFilters("{donor:{id:{is:['_missing', 'DO1']}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("or(missing(donor.id),in(donor.id,'DO1'))");
  }

  @Test
  public void missingValueNotTest() {
    val result = converter.convertFilters(createFilters("{donor:{id:{not:['_missing']}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("not(missing(donor.id))");
  }

  @Test
  public void missingValueNotTest_array() {
    val result = converter.convertFilters(createFilters("{donor:{id:{not:['_missing', 'DO1']}}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("not(or(missing(donor.id),in(donor.id,'DO1')))");
  }

  @Test
  public void mutationNestedFiltersTest_gene() {
    val filters = createFilters("{donor:{id:{is:'DO1'}},mutation:{id:{is:'MU1'},consequenceType:{is:'start_lost'}}}");
    val result = converter.convertFilters(filters, GENE_CENTRIC);
    log.debug("PQL: {}", result);
    assertThat(result)
        .isEqualTo(
            "nested(donor,"
                + "and(nested(donor.ssm,and("
                + "nested(donor.ssm.consequence,eq(mutation.consequenceType,'start_lost')),"
                + "eq(mutation.id,'MU1'))),eq(donor.id,'DO1')))");
  }

  @Test
  public void pathwayId_And_HasPathway() {
    val jql = "{gene: {hasPathway: true, pathwayId: {is :['REACT_13797']}}}";
    val filters = createFilters(jql);
    val pql = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(pql).isEqualTo(
        "or(in(gene.pathwayId,'REACT_13797'),exists(gene.pathwayId))");
  }

  @Test
  public void pathwayId_And_NoHasPathway() {
    val jql = "{gene: {type: {is: ['antisense']}, pathwayId: {is: ['REACT_13797']}}}";
    val filters = createFilters(jql);
    val pql = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(pql).isEqualTo(
        "in(gene.type,'antisense'),in(gene.pathwayId,'REACT_13797')");
  }

  @Test
  public void noPathwayId_And_HasPathway() {
    val jql = "{gene: {type: {is: ['antisense']}, hasPathway: true}}";
    val filters = createFilters(jql);
    val pql = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(pql).isEqualTo(
        "in(gene.type,'antisense'),exists(gene.pathwayId)");
  }

  @Test
  public void entitySetId_And_Id() {
    val jql =
        "{gene: {id: {is: ['ES:ab263008-db02-4631-a5c5-c4914a0fe6c8', 'ENSG00000155657']}}}";
    val filters = createFilters(jql);
    val pql = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(pql).isEqualTo(
        "in(gene.id,'ES:ab263008-db02-4631-a5c5-c4914a0fe6c8','ENSG00000155657')");
  }

  @Test
  public void entitySetId_And_Id_TwoCategories_GeneCentric() {
    val jql =
        "{gene: {id: {is: ['ES:ab263008-db02-4631-a5c5-c4914a0fe6c8', 'ENSG00000155657']}}," +
            "donor: {id: {is: ['ES:ab263008-db02-4631-a5c5-c4914a0fe6c8', 'some_donor_id']}}}";
    val filters = createFilters(jql);
    val pql = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(pql)
        .isEqualTo(
            "in(gene.id,'ES:ab263008-db02-4631-a5c5-c4914a0fe6c8','ENSG00000155657'),nested(donor,in(donor.id,'ES:ab263008-db02-4631-a5c5-c4914a0fe6c8','some_donor_id'))");
  }

  @Test
  public void entitySetId_And_Id_TwoCategories_DonorCentric() {
    val jql =
        "{gene: {id: {is: ['ES:ab263008-db02-4631-a5c5-c4914a0fe6c8','ENSG00000155657']}}," +
            "donor: {id: {is: ['ES:ab263008-db02-4631-a5c5-c4914a0fe6c8','some_donor_id']}}}";
    val filters = createFilters(jql);
    val pql = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(pql)
        .isEqualTo(
            "in(donor.id,'ES:ab263008-db02-4631-a5c5-c4914a0fe6c8','some_donor_id'),nested(gene,in(gene.id,'ES:ab263008-db02-4631-a5c5-c4914a0fe6c8','ENSG00000155657'))");
  }

  @Test
  public void donorFiltersTest_donor() {
    val filters = createFilters("{donor:{primarySite:{is:['Brain']},ageAtDiagnosisGroup:{is:['60 - 69']}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("in(donor.primarySite,'Brain'),in(donor.ageAtDiagnosisGroup,'60 - 69')");
  }

  @Test
  public void donorFiltersTest_mutation() {
    val filters = createFilters("{donor:{primarySite:{is:['Brain']},ageAtDiagnosisGroup:{is:['60 - 69']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result)
        .isEqualTo("nested(ssm_occurrence,in(donor.primarySite,'Brain'),in(donor.ageAtDiagnosisGroup,'60 - 69'))");
  }

  @Test
  public void fileFiltersTest() {
    val testJql = "{file: {id: {is: 'test1'}}}";
    val expectedPql = "eq(file.id,'test1')";

    val testPql = converter.convertFilters(createFilters(testJql), FILE);
    assertThat(testPql)
        .isEqualTo(expectedPql);
  }

  @Test
  public void fileFiltersTest2() {
    val testJql = "{file: {study: {is: 'test1'}}}";
    val expectedPql = "eq(file.study,'test1')";

    val testPql = converter.convertFilters(createFilters(testJql), FILE);
    assertThat(testPql)
        .isEqualTo(expectedPql);
  }

  @Test
  public void mutationNestedAndNonNestedTest() {
    val filters = createFilters("{mutation:{platform:{is:['M1','M2']},id:{is:['M1','M2']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).isEqualTo("in(mutation.id,'M1','M2'),nested(ssm_occurrence.observation,in(platform,'M1','M2'))");
  }

  @Test
  public void multifilterWithMissingKeywordTest() {
    val filters = createFilters("{donor:{gender:{is:['_missing','male']}},gene:{type:{is:['antisense']}},"
        + "mutation:{id:{is:'MU1'}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).isEqualTo("eq(mutation.id,'MU1'),"
        + "nested(transcript,in(gene.type,'antisense')),"
        + "nested(ssm_occurrence,or(missing(donor.gender),in(donor.gender,'male')))");
  }

  @Test
  public void mutationLocationTest_donor() {
    val result = converter.convertFilters(createFilters("{mutation:{location:{is:['chr12:43566-3457633']}}}"),
        DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene.ssm,in(mutation.location,'chr12:43566-3457633'))");
  }

  @Test
  public void mutationLocationTest_gene() {
    val result = converter.convertFilters(createFilters("{mutation:{location:{is:['chr12:43566-3457633']}}}"),
        GENE_CENTRIC);
    assertThat(result).isEqualTo("nested(donor.ssm,in(mutation.location,'chr12:43566-3457633'))");
  }

  @Test
  public void mutationLocationTest_mutation() {
    val result = converter.convertFilters(createFilters("{mutation:{location:{is:['chr12:43566-3457633']}}}"),
        MUTATION_CENTRIC);
    assertThat(result).isEqualTo("in(mutation.location,'chr12:43566-3457633')");
  }

  @Test
  public void geneLocationTest_donor() {
    val result = converter.convertFilters(createFilters("{gene:{location:{is:['chr12:43566-3457633']}}}"),
        DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene,in(gene.location,'chr12:43566-3457633'))");
  }

  @Test
  public void geneLocationTest_gene() {
    val result = converter.convertFilters(createFilters("{gene:{location:{is:['chr12:43566-3457633']}}}"),
        GENE_CENTRIC);
    assertThat(result).isEqualTo("in(gene.location,'chr12:43566-3457633')");
  }

  @Test
  public void geneLocationTest_mutation() {
    val result = converter.convertFilters(createFilters("{gene:{location:{is:['chr12:43566-3457633']}}}"),
        MUTATION_CENTRIC);
    assertThat(result).isEqualTo("nested(transcript,in(gene.location,'chr12:43566-3457633'))");
  }

  @Test
  public void pathwayTest() {
    val result = converter.convertFilters(createFilters("{gene:{hasPathway:false}}"), GENE_CENTRIC);
    assertThat(result).isEqualTo("missing(gene.pathwayId)");
  }

  @Test
  public void pathwayTest_observation() {
    val result = converter.convertFilters(createFilters("{gene:{hasPathway:false}}"), Type.OBSERVATION_CENTRIC);
    val nestedPath = IndexModel.getObservationCentricTypeModel().getNestedPath("gene.pathwayId");
    assertThat(result).isEqualTo(format("nested(%s,missing(gene.pathwayId))", nestedPath));
  }

  @Test
  public void pathwayTest_otherType() {
    val result = converter.convertFilters(createFilters("{gene:{hasPathway:false}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene,missing(gene.pathwayId))");
  }

  @Test
  public void compoundTest() {
    val result = converter.convertFilters(createFilters("{gene:{hasCompound:false}}"), GENE_CENTRIC);
    assertThat(result).isEqualTo("missing(gene.compoundId)");
  }

  @Test
  public void hasCompoundTest() {
    val result = converter.convertFilters(createFilters("{gene:{hasCompound:true}}"), GENE_CENTRIC);
    assertThat(result).isEqualTo("exists(gene.compoundId)");
  }

  @Test
  public void hasCompoundTest_donor() {
    val result = converter.convertFilters(createFilters("{gene:{hasCompound:true}}"), DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene,exists(gene.compoundId))");
  }

  @Test
  public void hasCompoundTest_mutation() {
    val result = converter.convertFilters(createFilters("{gene:{hasCompound:true}}"), MUTATION_CENTRIC);
    assertThat(result).isEqualTo("nested(transcript,exists(gene.compoundId))");
  }

  @Test
  public void hasCompoundAndFilterTest() {
    val result =
        converter.convertFilters(createFilters("{gene:{compoundId:{is:['123']},hasCompound:true}}"), GENE_CENTRIC);
    assertThat(result).isEqualTo("or(in(gene.compoundId,'123'),exists(gene.compoundId))");
  }

  @Test
  public void hasCompoundAndPathwayIdTest() {
    val result =
        converter.convertFilters(createFilters("{gene:{pathwayId:{is:['123']},hasCompound:true}}"), GENE_CENTRIC);
    assertThat(result).isEqualTo("exists(gene.compoundId),in(gene.pathwayId,'123')");
  }

  @Test
  public void compoundAndPathwayTest() {
    val result = converter.convertFilters(createFilters("{gene:{compoundId:{is:['ZINC01']},hasCompound:true,"
        + "pathwayId:{is:['123']},hasPathway:false}}"), GENE_CENTRIC);
    assertThat(result).isEqualTo("or(in(gene.compoundId,'ZINC01'),exists(gene.compoundId)),"
        + "or(in(gene.pathwayId,'123'),missing(gene.pathwayId))");
  }

  @Test
  public void goTermAndFilterTest_mutation() {
    val filters = createFilters("{mutation:{id:{is:['M1','M2']}},gene:{goTermId:{is:['321']}}}");
    val result = converter.convertFilters(filters, Type.MUTATION_CENTRIC);
    log.info("{}", result);
    assertThat(result).isEqualTo("in(mutation.id,'M1','M2'),nested(transcript,in(gene.goTermId,'321'))");
  }

  @Test
  public void curratedSetTest_gene() {
    val filters = createFilters("{gene:{curatedSetId:{is:['GS1']},hasPathway:true}}");
    val result = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(result).isEqualTo("in(gene.curatedSetId,'GS1'),exists(gene.pathwayId)");
  }

  @Test
  public void curratedSetTest_mutation() {
    val filters = createFilters("{gene:{curatedSetId:{is:['GS1']},hasPathway:true}}");
    val result = converter.convertFilters(filters, Type.MUTATION_CENTRIC);
    assertThat(result).isEqualTo("nested(transcript,in(gene.curatedSetId,'GS1'),exists(gene.pathwayId))");
  }

  @Test
  public void hasPathwayAndOtherFilterTest() {
    val filters = createFilters("{donor:{analysisTypes:{is:['non-NGS']}},gene:{hasPathway:true}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("in(donor.analysisTypes,'non-NGS'),nested(gene,exists(gene.pathwayId))");
  }

  @Test
  public void pathwayAndGoTermTest_donor() {
    val filters = createFilters("{gene:{hasPathway:true,goTermId:{is:['123']}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene,in(gene.goTermId,'123'),exists(gene.pathwayId))");
  }

  @Test
  public void pathwayAndGoTermTest_gene() {
    val filters = createFilters("{gene:{hasPathway:true,goTermId:{is:['123']}}}");
    val result = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(result).isEqualTo("in(gene.goTermId,'123'),exists(gene.pathwayId)");
  }

  @Test
  public void pathwayAndGoTermTest_mutation() {
    val filters = createFilters("{gene:{hasPathway:true,goTermId:{is:['123']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).isEqualTo("nested(transcript,in(gene.goTermId,'123'),exists(gene.pathwayId))");
  }

  @Test
  public void pathwayAndNestedFilterTest_donor() {
    val filters = createFilters("{gene:{hasPathway:true},mutation:{consequenceType:{is:['sl']}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene,"
        + "and(nested(gene.ssm.consequence,in(mutation.consequenceType,'sl')),exists(gene.pathwayId)))");
  }

  @Test
  public void curratedSetAndNestedFilterTest_mutation() {
    val filters = createFilters("{gene:{curatedSetId:{is:['GS1']}},mutation:{consequenceType:{is:['sl']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).isEqualTo("nested(transcript,in(gene.curatedSetId,'GS1'),in(consequenceType,'sl'))");
  }

  @Test
  public void twoLevelsNestingTest() {
    val filters = createFilters("{gene:{id:{is:'G1'}},mutation:{id:{is:'M1'}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene,and(nested(gene.ssm,eq(mutation.id,'M1')),eq(gene.id,'G1')))");
  }

  /**
   * Filters at different levels of nesting must be nested at the closest parent nesting level.<br>
   * E.g. In the DonorCentric model mutation.platform is nested at gene.ssm.observation, mutation.functionalImpact is
   * nested at gene.ssm.consequence nested path. These two siblings must be enclosed at gene.ssm level. There is another
   * nested path 'gene'. However, as gene.ssm is closest parent to the siblings it must be used.
   */
  @Test
  public void differentLevelsNestingTest() {
    val filters = createFilters("{mutation:{platform:{is:['Illumina GA sequencing']},functionalImpact:{is:['High']}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene.ssm,nested(gene.ssm.consequence,in(mutation.functionalImpact,'High')),"
        + "nested(gene.ssm.observation,in(mutation.platform,'Illumina GA sequencing')))");
  }

  @Test
  public void differentLevelsNestingTest_mutationCentric() {
    val filters = createFilters("{mutation:{platform:{is:['p']},functionalImpact:{is:['f']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    log.warn("{}", result);
    assertThat(result).isEqualTo("nested(transcript,in(functionalImpact,'f')),"
        + "nested(ssm_occurrence.observation,in(platform,'p'))");
  }

  @Test
  public void differentLevelsNestingTest_withCommonParent() {
    val filters = createFilters("{mutation:{"
        + "platform:{is:['Illumina GA sequencing']},"
        + "functionalImpact:{is:['High']},"
        + "id:{is:'M1'}}}");

    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene.ssm,"
        + "and("
        + "nested(gene.ssm.consequence,in(mutation.functionalImpact,'High')),"
        + "nested(gene.ssm.observation,in(mutation.platform,'Illumina GA sequencing')),"
        + "eq(mutation.id,'M1')))");
  }

  @Test
  public void nestedTest_single() {
    val filters = createFilters("{gene:{id:{is:'G1'}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene,eq(gene.id,'G1'))");
  }

  @Test
  public void nestedTest_two() {
    val filters = createFilters("{gene:{id:{is:'G1'},start:{is:123}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene,eq(gene.id,'G1'),eq(gene.start,123))");
  }

  @Test
  public void nestedInNestedTest() {
    val filters = createFilters("{gene:{id:{is:'G1'}},mutation:{id:{is:'M1'}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene,and(nested(gene.ssm,eq(mutation.id,'M1')),eq(gene.id,'G1')))");
  }

  @Test
  public void groupNestedPathsTest_single() {
    assertThat(groupNestedPaths(of("gene"), getDonorCentricTypeModel())
        .get("gene"))
        .containsExactly("gene");
  }

  @Test
  public void groupNestedPathsTest_differentLevels_mutationCentric() {
    val result = groupNestedPaths(of("ssm_occurrence.observation", "transcript"), getMutationCentricTypeModel());
    log.debug("Result: {}", result);
    assertThat(result.get("transcript")).containsExactly("transcript");
    assertThat(result.get("ssm_occurrence.observation")).containsExactly("ssm_occurrence.observation");

  }

  @Test
  public void groupNestedPathsTest_two() {
    assertThat(groupNestedPaths(of("gene.ssm", "gene.ssm.observation"), getDonorCentricTypeModel())
        .get("gene.ssm"))
        .containsExactly("gene.ssm", "gene.ssm.observation");
  }

  @Test
  public void groupNestedPathsTest_withEmptyPath() {
    assertThat(groupNestedPaths(of("", "gene.ssm", "gene.ssm.observation"), getDonorCentricTypeModel())
        .get("gene.ssm"))
        .containsExactly("gene.ssm", "gene.ssm.observation");
  }

  @Test
  public void groupNestedPathsTest_noCommonParent() {
    assertThat(groupNestedPaths(of("gene.ssm.consequence", "gene.ssm.observation"), getDonorCentricTypeModel())
        .get("gene.ssm"))
        .containsExactly("gene.ssm.consequence", "gene.ssm.observation");
  }

  @Test
  public void groupNestedPathsTest_withCommonParent() {
    val result = groupNestedPaths(of("gene.ssm", "gene.ssm.consequence", "gene.ssm.observation"),
        getDonorCentricTypeModel());
    assertThat(result.size()).isEqualTo(3);
    assertThat(result.get("gene.ssm")).containsExactly("gene.ssm", "gene.ssm.consequence", "gene.ssm.observation");
  }

  @Test
  public void groupNestedPathsTest_withCommonParentAtHigherLevel() {
    val result = groupNestedPaths(of("gene", "gene.ssm.consequence", "gene.ssm.observation"),
        getDonorCentricTypeModel());
    assertThat(result.size()).isEqualTo(3);
    assertThat(result.get("gene")).containsExactly("gene", "gene.ssm.consequence", "gene.ssm.observation");
  }

  @Test
  public void groupNestedPathsTest_multiple() {
    val result = groupNestedPaths(of("ssm_occurrence", "ssm_occurrence.observation", "transcript"),
        getMutationCentricTypeModel());
    assertThat(result.size()).isEqualTo(3);
    assertThat(result.get("ssm_occurrence")).containsExactly("ssm_occurrence", "ssm_occurrence.observation");
    assertThat(result.get("transcript")).containsExactly("transcript");
  }

  @Test
  public void isEncloseWithCommonParentTest() {
    assertThat(isEncloseWithCommonParent(of("gene", "gene.ssm"))).isFalse();
    assertThat(isEncloseWithCommonParent(of("gene.ssm.consequence", "gene.ssm.observation"))).isTrue();
    assertThat(isEncloseWithCommonParent(of("gene.ssm", "gene.ssm.consequence", "gene.ssm.observation"))).isFalse();
    assertThat(isEncloseWithCommonParent(of("gene", "gene.ssm.consequence", "gene.ssm.observation"))).isFalse();
  }

  @Test
  public void createFilterByNestedPathTest_single() {
    val values = ArrayListMultimap.<String, JqlField> create();

    // gene:{id:{is:'G'}}
    values.put("gene", new JqlField("id", IS, new JqlSingleValue("G"), "gene"));
    assertThat(createFilterByNestedPath(DONOR_CENTRIC, values,
        Lists.newArrayList("gene"), false))
        .isEqualTo("nested(gene,eq(gene.id,'G'))");
  }

  @Test
  public void createFilterByNestedPathTest_twoOnSameLevel() {
    val values = ArrayListMultimap.<String, JqlField> create();

    // gene:{id:{is:'G'}, start:{is:1}}
    values.put("gene", new JqlField("id", IS, new JqlSingleValue("G"), "gene"));
    values.put("gene", new JqlField("start", IS, new JqlSingleValue(1), "gene"));
    assertThat(createFilterByNestedPath(DONOR_CENTRIC, values,
        Lists.newArrayList("gene"), false))
        .isEqualTo("nested(gene,eq(gene.id,'G'),eq(gene.start,1))");
  }

  @Test
  public void createFilterByNestedPathTest_differentLevelsWithoutCommonParent() {
    val values = ArrayListMultimap.<String, JqlField> create();

    // {mutation:{platform:{is:'p'},functionalImpact:{is:'fi'}}}
    values.put("gene.ssm.observation", new JqlField("platform", IS, new JqlSingleValue("p"), "mutation"));
    values.put("gene.ssm.consequence", new JqlField("functionalImpact", IS, new JqlSingleValue("fi"), "mutation"));

    assertThat(createFilterByNestedPath(DONOR_CENTRIC, values,
        Lists.newArrayList("gene.ssm.observation", "gene.ssm.consequence"), false))
        .isEqualTo("nested(gene.ssm.consequence,eq(mutation.functionalImpact,'fi')),"
            + "nested(gene.ssm.observation,eq(mutation.platform,'p'))");

  }

  @Test
  public void createFilterByNestedPathTest_withCommonParent() {
    val values = ArrayListMultimap.<String, JqlField> create();

    // gene:{id:{is:'G'}}, mutation:{id:{is:'M'}}
    values.put("gene", new JqlField("id", IS, new JqlSingleValue("G"), "gene"));
    values.put("gene.ssm", new JqlField("id", IS, new JqlSingleValue("M"), "mutation"));
    assertThat(createFilterByNestedPath(DONOR_CENTRIC, values,
        Lists.newArrayList("gene.ssm", "gene"), false))
        .isEqualTo("nested(gene,and(nested(gene.ssm,eq(mutation.id,'M')),eq(gene.id,'G')))");
  }

  @Test
  public void groupFieldsByNestedPathTest() {
    val fields = ImmutableList.of(
        new JqlField("id", IS, new JqlSingleValue("M"), "mutation"),
        new JqlField("platform", IS, new JqlSingleValue("p"), "mutation"),
        new JqlField("functionalImpact", IS, new JqlSingleValue("fi"), "mutation"));

    val result = groupFieldsByNestedPath("mutation", fields, DONOR_CENTRIC);
    assertThat(result.size()).isEqualTo(3);
  }

  @Test
  public void mutationFiltersInProjectTypeModelTest() {
    val filters = createFilters("{project:{id:{is:['p']}},mutation:{consequenceType:{is:['sl']}}}");
    val result = converter.convertFilters(filters, Type.PROJECT);
    assertThat(result).isEqualTo("in(project.id,'p')");
  }

  @Test
  public void projectTest() {
    val filters = createFilters("{project:{primaryCountries:{is:['US']},id:{is:'P1'},primarySite:{is:'Blood'},"
        + "availableDataTypes:{is:'SSM'}}}");
    val result = converter.convertFilters(filters, Type.PROJECT);
    assertThat(result).isEqualTo("in(project.primaryCountries,'US'),"
        + "eq(project.id,'P1'),eq(project.primarySite,'Blood'),eq(project.availableDataTypes,'SSM')");
  }

  @Test
  public void allTest() {
    val filters = createFilters("{gene:{id:{all:['G1','G2','G3']}}}");
    val result = converter.convertFilters(filters, GENE_CENTRIC);
    assertThat(result).isEqualTo("and(in(gene.id,'G1'),in(gene.id,'G2'),in(gene.id,'G3'))");
  }

  @Test
  public void geneSetIdTest_donor() {
    val filters = createFilters("{gene:{geneSetId:{is:['REACT_111155']}}}");
    val result = converter.convertFilters(filters, DONOR_CENTRIC);
    assertThat(result).isEqualTo("nested(gene,in(gene.geneSetId,'REACT_111155'))");
  }

  @Test
  public void geneSetIdTest_mutation() {
    val filters = createFilters("{gene:{geneSetId:{is:['REACT_111155']}}}");
    val result = converter.convertFilters(filters, MUTATION_CENTRIC);
    assertThat(result).isEqualTo("nested(transcript,in(gene.geneSetId,'REACT_111155'))");
  }

  @SneakyThrows
  public static JqlFilters createFilters(String filters) {
    return mapper.readValue(new FiltersParam(filters).get().toString(), JqlFilters.class);
  }

  private static ObjectMapper createObjectMapper() {
    return registerJqlDeserializer(new ObjectMapper());
  }

  private static ObjectMapper registerJqlDeserializer(ObjectMapper mapper) {
    val module = new SimpleModule();
    module.addDeserializer(JqlFilters.class, new JqlFiltersDeserializer());
    mapper.registerModule(module);

    return mapper;
  }

}
