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

import static java.util.Collections.emptyMap;
import static org.dcc.portal.pql.meta.field.ArrayFieldModel.arrayOfStrings;
import static org.dcc.portal.pql.meta.field.LongFieldModel.long_;
import static org.dcc.portal.pql.meta.field.ObjectFieldModel.object;
import static org.dcc.portal.pql.meta.field.StringFieldModel.string;

import java.util.List;

import lombok.val;

import org.dcc.portal.pql.meta.field.FieldModel;
import org.dcc.portal.pql.meta.field.ObjectFieldModel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class ProjectTypeModel extends TypeModel {

  private final static String TYPE_PREFIX = "project";
  private static final List<String> INCLUDE_FIELDS = ImmutableList.of(
      "_summary.experimental_analysis_performed_sample_count",
      "_summary.experimental_analysis_performed_donor_count");

  private final static List<String> AVAILABLE_FACETS = ImmutableList.of(
      "primaryCountries",
      "primarySite",
      "availableDataTypes",
      "state",
      "tumourType");

  private final static List<String> PUBLIC_FIELDS = ImmutableList.of(
      "id",
      "icgcId",
      "primarySite",
      "name",
      "tumourType",
      "tumourSubtype",
      "primaryCountries",
      "partnerCountries",
      "availableDataTypes",
      "ssmTestedDonorCount",
      "cnsmTestedDonorCount",
      "stsmTestedDonorCount",
      "sgvTestedDonorCount",
      "methSeqTestedDonorCount",
      "methArrayTestedDonorCount",
      "expSeqTestedDonorCount",
      "expArrayTestedDonorCount",
      "pexpTestedDonorCount",
      "mirnaSeqTestedDonorCount",
      "jcnTestedDonorCount",
      "totalDonorCount",
      "totalLiveDonorCount",
      "affectedDonorCount",
      "experimentalAnalysisPerformedDonorCounts",
      "experimentalAnalysisPerformedSampleCounts",
      "pubmedIds",
      "repository",
      "state");

  public ProjectTypeModel() {
    super(defineFields(), emptyMap(), PUBLIC_FIELDS, INCLUDE_FIELDS);
  }

  @Override
  public Type getType() {
    return Type.PROJECT;
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
    val fields = new ImmutableList.Builder<FieldModel>();
    fields.add(string("_project_id", ImmutableSet.of("id", "donor.projectId")));
    fields.add(string("icgc_id", "icgcId"));
    fields.add(string("primary_site", ImmutableSet.of("primarySite", "donor.primarySite")));
    fields.add(string("project_name", ImmutableSet.of("name", "donor.projectName")));
    fields.add(string("tumour_type", "tumourType"));
    fields.add(string("tumour_subtype", "tumourSubtype"));
    fields.add(arrayOfStrings("primary_countries", "primaryCountries"));
    fields.add(arrayOfStrings("partner_countries", "partnerCountries"));
    fields.add(arrayOfStrings("pubmed_ids", "pubmedIds"));
    fields.add(defineSummary());

    return fields.build();
  }

  private static ObjectFieldModel defineSummary() {
    return object("_summary",
        arrayOfStrings("_available_data_type", "availableDataTypes"),
        long_("_ssm_tested_donor_count", "ssmTestedDonorCount"),
        long_("_cnsm_tested_donor_count", "cnsmTestedDonorCount"),
        long_("_stsm_tested_donor_count", "stsmTestedDonorCount"),
        long_("_sgv_tested_donor_count", "sgvTestedDonorCount"),
        long_("_meth_seq_tested_donor_count", "methSeqTestedDonorCount"),
        long_("_meth_array_tested_donor_count", "methArrayTestedDonorCount"),
        long_("_exp_seq_tested_donor_count", "expSeqTestedDonorCount"),
        long_("_exp_array_tested_donor_count", "expArrayTestedDonorCount"),
        long_("_pexp_tested_donor_count", "pexpTestedDonorCount"),
        long_("_mirna_seq_tested_donor_count", "mirnaSeqTestedDonorCount"),
        long_("_jcn_tested_donor_count", "jcnTestedDonorCount"),
        long_("_total_donor_count", "totalDonorCount"),
        long_("_total_live_donor_count", "totalLiveDonorCount"),
        long_("_affected_donor_count", "affectedDonorCount"),
        string("_state", "state"),
        object("experimental_analysis_performed_donor_count", "experimentalAnalysisPerformedDonorCounts"),
        object("experimental_analysis_performed_sample_count", "experimentalAnalysisPerformedSampleCounts"),
        arrayOfStrings("repository", "repository"));
  }

}
