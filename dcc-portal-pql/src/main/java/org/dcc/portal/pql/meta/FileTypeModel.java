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

/**
 * TypeModel for Repository File index type
 */
public class FileTypeModel extends TypeModel {

  private static final Type MY_TYPE = Type.FILE;
  private static final String TYPE_PREFIX = MY_TYPE.getPrefix();

  public FileTypeModel() {
    super(Fields.MAPPINGS, INTERNAL_ALIASES, PUBLIC_FIELDS, INCLUDE_FIELDS);
  }

  @Override
  public Type getType() {
    return MY_TYPE;
  }

  @Override
  public List<String> getFacets() {
    return AVAILABLE_FACETS;
  }

  @Override
  public String prefix() {
    return TYPE_PREFIX;
  }

  /**
   * Raw ES fields
   */
  public static class EsFields {

    public static final String ID = "id";
    public static final String FILE_COPIES = "file_copies";
    public static final String DONORS = "donors";
    public static final String DONOR_ID = "donor_id";
    public static final String DATA_BUNDLE = "data_bundle";
    public static final String DATA_BUNDLE_ID = "data_bundle.data_bundle_id";
    public static final String OBJECT_ID = "object_id";
    public static final String ANALYSIS_METHOD = "analysis_method";
    public static final String STUDY = "study";
    public static final String ACCESS = "access";
    public static final String DATA_CATEGORIZATION = "data_categorization";
    public static final String REFERENCE_GENOME = "reference_genome";

  }

  /**
   * Field aliases
   */
  public static class Fields {

    // Sub-object aliases
    public static final String DATA_CATEGORIZATION = "dataCategorization";
    public static final String DATA_BUNDLE = "dataBundle";
    public static final String FILE_COPIES = "fileCopies";
    public static final String INDEX_FILE = "indexFile";
    public static final String DONORS = "donors";
    public static final String OTHER_IDENTIFIERS = "otherIdentifiers";
    public static final String ANALYSIS_METHOD = "analysisMethod";
    public static final String REFERENCE_GENOME = "referenceGenome";

    // Individual field aliases
    public static final String DATA_TYPE = "dataType";
    public static final String EXPERIMENTAL_STRATEGY = "experimentalStrategy";
    public static final String DATA_BUNDLE_ID = "dataBundleId";

    public static final String REPO_CODE = "repoCode";
    public static final String REPO_ORG = "repoOrg";
    public static final String REPO_NAME = "repoName";
    public static final String REPO_TYPE = "repoType";
    public static final String REPO_COUNTRY = "repoCountry";
    public static final String REPO_BASE_URL = "repoBaseUrl";
    public static final String REPO_DATA_PATH = "repoDataPath";
    public static final String REPO_METADATA_PATH = "repoMetadataPath";

    public static final String FILE_NAME = "fileName";
    public static final String FILE_FORMAT = "fileFormat";
    public static final String FILE_MD5SUM = "fileMd5sum";
    public static final String FILE_SIZE = "fileSize";
    public static final String LAST_MODIFIED = "lastModified";
    public static final String LAST_UPDATED = "lastUpdated";

    public static final String INDEX_FILE_ID = INDEX_FILE + ".id";
    public static final String INDEX_OBJECT_UUID = INDEX_FILE + ".objectId";
    public static final String INDEX_FILE_NAME = INDEX_FILE + ".fileName";
    public static final String INDEX_FILE_FORMAT = INDEX_FILE + ".fileFormat";
    public static final String INDEX_FILE_MD5SUM = INDEX_FILE + ".fileMd5sum";
    public static final String INDEX_FILE_SIZE = INDEX_FILE + ".fileSize";

    public static final String PROGRAM = "program";
    public static final String DONOR_STUDY = "donorStudy";
    public static final String DONOR_ID = "donorId";
    public static final String PRIMARY_SITE = "primarySite";
    public static final String PROJECT_CODE = "projectCode";
    public static final String SAMPLE_ID = "sampleId";
    public static final String SPECIMEN_ID = "specimenId";
    public static final String SPECIMEN_TYPE = "specimenType";
    public static final String SUBMITTED_DONOR_ID = "submittedDonorId";
    public static final String SUBMITTED_SAMPLE_ID = "submittedSampleId";
    public static final String SUBMITTED_SPECIMEN_ID = "submittedSpecimenId";
    public static final String MATCHED_CONTROL_SAMPLE_ID = "matchedControlSampleId";

    public static final String TCGA_ALIQUOT_BARCODE = "tcgaAliquotBarcode";
    public static final String TCGA_PARTICIPANT_BARCODE = "tcgaParticipantBarcode";
    public static final String TCGA_SAMPLE_BARCODE = "tcgaSampleBarcode";

    public static final String ANALYSIS_TYPE = "analysisType";
    public static final String ANALYSIS_SOFTWARE = "software";

    public static final String REFERENCE_NAME = "referenceName";
    public static final String GENOME_BUILD = "genomeBuild";
    public static final String DOWNLOAD_URL = "downloadUrl";

    public static final String ID = "id";
    public static final String FILE_UUID = "fileUuid";
    public static final String OBJECT_ID = "objectId";
    public static final String FILE_OBJECT_ID = "fileObjectId";
    public static final String FILE_ID = "fileId";
    public static final String ACCESS = "access";
    public static final String STUDY = "study";

    // Sub-object mappings
    private static ObjectFieldModel dataCategorization() {
      return object("data_categorization", DATA_CATEGORIZATION,
          string("data_type", DATA_TYPE),
          string("experimental_strategy", EXPERIMENTAL_STRATEGY));
    }

    private static ObjectFieldModel dataBundle() {
      return object("data_bundle", DATA_BUNDLE,
          string("data_bundle_id", DATA_BUNDLE_ID));
    }

    private static ArrayFieldModel fileCopies() {
      return nestedArrayOfObjects(EsFields.FILE_COPIES, FILE_COPIES, object(
          string("repo_code", REPO_CODE),
          string("repo_org", REPO_ORG),
          string("repo_name", REPO_NAME),
          string("repo_type", REPO_TYPE),
          string("repo_country", REPO_COUNTRY),
          string("repo_base_url", REPO_BASE_URL),
          string("repo_data_path", REPO_DATA_PATH),
          string("repo_metadata_path", REPO_METADATA_PATH),
          indexFile(),
          string("file_name", FILE_NAME),
          string("file_format", FILE_FORMAT),
          string("file_md5sum", FILE_MD5SUM),
          long_("file_size", FILE_SIZE),
          long_("last_modified", ImmutableSet.of(LAST_MODIFIED, LAST_UPDATED))));
    }

    private static ObjectFieldModel indexFile() {
      return object("index_file", INDEX_FILE,
          string("id", INDEX_FILE_ID),
          string("object_id", INDEX_OBJECT_UUID),
          string("file_name", INDEX_FILE_NAME),
          string("file_format", INDEX_FILE_FORMAT),
          string("file_md5sum", INDEX_FILE_MD5SUM),
          long_("file_size", INDEX_FILE_SIZE));
    }

    private static ArrayFieldModel donors() {
      return nestedArrayOfObjects(EsFields.DONORS, DONORS, object(
          identifiableString(EsFields.DONOR_ID, ImmutableSet.of(DONOR_ID, "donor.id")),
          string("program", PROGRAM),
          string("primary_site", PRIMARY_SITE),
          string("project_code", PROJECT_CODE),
          string("study", DONOR_STUDY),
          string("sample_id", SAMPLE_ID),
          string("specimen_id", SPECIMEN_ID),
          string("specimen_type", SPECIMEN_TYPE),
          string("submitted_donor_id", SUBMITTED_DONOR_ID),
          string("submitted_sample_id", SUBMITTED_SAMPLE_ID),
          string("submitted_specimen_id", SUBMITTED_SPECIMEN_ID),
          string("matched_control_sample_id", MATCHED_CONTROL_SAMPLE_ID),
          otherIdentifiers()));
    }

    private static ObjectFieldModel otherIdentifiers() {
      return object("other_identifiers", OTHER_IDENTIFIERS,
          string("tcga_sample_barcode", TCGA_SAMPLE_BARCODE),
          string("tcga_aliquot_barcode", TCGA_ALIQUOT_BARCODE),
          string("tcga_participant_barcode", TCGA_PARTICIPANT_BARCODE));
    }

    private static ObjectFieldModel analysisMethod() {
      return object("analysis_method", ANALYSIS_METHOD,
          string("analysis_type", ANALYSIS_TYPE),
          string("software", ANALYSIS_SOFTWARE));
    }

    private static ObjectFieldModel referenceGenome() {
      return object("reference_genome", REFERENCE_GENOME,
          string("reference_name", REFERENCE_NAME),
          string("genome_build", GENOME_BUILD),
          string("download_url", DOWNLOAD_URL));
    }

    // Main mapping
    private static final List<FieldModel> MAPPINGS = ImmutableList.<FieldModel> builder()
        .add(identifiableString("id", ImmutableSet.of(ID, FILE_ID, "file.id")))
        .add(string("object_id", ImmutableSet.of(FILE_OBJECT_ID, FILE_UUID, OBJECT_ID)))
        .add(string("access", ACCESS))
        .add(string("study", STUDY))
        .add(dataCategorization())
        .add(dataBundle())
        .add(fileCopies())
        .add(donors())
        .add(analysisMethod())
        .add(referenceGenome())
        .build();
  }

  // Used for select(*) - default projection
  public static final List<String> PUBLIC_FIELDS = ImmutableList.of(
      Fields.OBJECT_ID,
      Fields.FILE_UUID,
      Fields.ID,
      Fields.FILE_ID,
      Fields.FILE_COPIES,
      Fields.DONORS,
      Fields.ANALYSIS_METHOD,
      Fields.STUDY,
      Fields.ACCESS,
      Fields.REFERENCE_GENOME,
      Fields.DATA_CATEGORIZATION);

  public static final List<String> AVAILABLE_FACETS = ImmutableList.<String> builder()
      .add(Fields.PROJECT_CODE)
      .add(Fields.PRIMARY_SITE)
      .add(Fields.SPECIMEN_TYPE)
      .add(Fields.REPO_NAME)
      .add(Fields.DATA_TYPE)
      .add(Fields.EXPERIMENTAL_STRATEGY)
      .add(Fields.STUDY)
      .add(Fields.FILE_FORMAT)
      .add(Fields.ACCESS)
      .add(Fields.DONOR_STUDY)
      .add(Fields.ANALYSIS_SOFTWARE)
      .build();

  private static final List<String> INCLUDE_FIELDS = ImmutableList.of(
      EsFields.ID,
      EsFields.OBJECT_ID,
      EsFields.STUDY,
      EsFields.ACCESS,
      EsFields.ANALYSIS_METHOD,
      EsFields.DATA_BUNDLE_ID,
      EsFields.FILE_COPIES,
      EsFields.DONORS,
      EsFields.DATA_CATEGORIZATION,
      EsFields.REFERENCE_GENOME);

  private static final Map<String, String> INTERNAL_ALIASES = ImmutableMap.<String, String> of(
      LOOKUP_TYPE, FILE_LOOKUP);

}
