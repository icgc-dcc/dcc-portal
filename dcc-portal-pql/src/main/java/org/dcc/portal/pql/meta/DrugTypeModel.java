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

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static org.dcc.portal.pql.meta.field.ArrayFieldModel.arrayOfObjects;
import static org.dcc.portal.pql.meta.field.ArrayFieldModel.arrayOfStrings;
import static org.dcc.portal.pql.meta.field.LongFieldModel.long_;
import static org.dcc.portal.pql.meta.field.ObjectFieldModel.object;
import static org.dcc.portal.pql.meta.field.StringFieldModel.string;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import org.dcc.portal.pql.meta.field.ArrayFieldModel;
import org.dcc.portal.pql.meta.field.FieldModel;
import org.dcc.portal.pql.meta.field.LongFieldModel;
import org.dcc.portal.pql.meta.field.ObjectFieldModel;
import org.dcc.portal.pql.meta.field.StringFieldModel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Type model of Drug index type
 */
public class DrugTypeModel extends TypeModel {

  private static final Type MY_TYPE = Type.DRUG;
  private static final String TYPE_PREFIX = MY_TYPE.getPrefix();

  public DrugTypeModel() {
    super(Fields.MAPPINGS, INTERNAL_ALIASES, PUBLIC_FIELDS, INCLUDE_FIELDS);
  }

  @Override
  public Type getType() {
    return MY_TYPE;
  }

  @Override
  public String prefix() {
    return TYPE_PREFIX;
  }

  @Override
  public List<String> getFacets() {
    return AVAILABLE_FACETS;
  }

  /**
   * Field aliases and mappings
   */
  @UtilityClass
  public class Fields {

    // Aliases
    public final String ID = "id";
    public final String ZINC_ID = "zincId";
    public final String NAME = "name";
    public final String SMALL_IMAGE_URL = "smallImageUrl";
    public final String LARGE_IMAGE_URL = "largeImageUrl";
    public final String INCHIKEY = "inchikey";
    public final String DRUG_CLASS = "drugClass";

    public final String CANCER_TRIAL_COUNT = "cancerTrialCount";
    public final String SYNONYMS = "synonyms";

    public final String EXTERNAL_REFERENCES = "externalReferences";
    public final String CHEMBL = fullAlias(EXTERNAL_REFERENCES, "chembl");
    public final String DRUG_BANK = "drugbank";

    public final String ATC_CODES = "atcCodes";
    public final String ATC_CODE = fullAlias(ATC_CODES, "code");
    public final String ATC_DESCRIPTION = fullAlias(ATC_CODES, "description");
    public final String ATC_LEVEL5_CODES = "atcLevel5Codes";

    public final String GENES = "genes";
    // public final String GENES_CHEMBL = fullAlias(GENES, "chembl");
    // public final String GENES_DESCRIPTION = fullAlias(GENES, "description");
    public final String UNIPROT = "uniprot";
    public final String ENSEMBL_GENE_ID = "ensemblGeneId";

    public final String TRIALS = "trials";
    public final String TRIALS_CODE = fullAlias(TRIALS, "code");
    public final String TRIALS_DESCRIPTION = fullAlias(TRIALS, "description");
    public final String TRIALS_PHASE_NAME = "phaseName";
    public final String TRIALS_START_DATE = "startDate";
    public final String TRIALS_STATUS_NAME = "statusName";

    public final String TRIALS_CONDITIONS = fullAlias(TRIALS, "conditions");
    public final String TRIALS_CONDITIONS_NAME = fullAlias(TRIALS_CONDITIONS, "name");
    public final String TRIALS_CONDITIONS_SHORT_NAME = fullAlias(TRIALS_CONDITIONS, "shortName");

    public final String TRIALS_DRUGS_MAPPINGS = fullAlias(TRIALS, "drugMappings");
    public final String TRIALS_DRUGS_MAPPINGS_DESCRIPTION = fullAlias(TRIALS_DRUGS_MAPPINGS, "description");
    public final String TRIALS_DRUGS_MAPPINGS_IDS = fullAlias(TRIALS_DRUGS_MAPPINGS, "ids");

    // Sub-object mappings
    private ArrayFieldModel synonyms() {
      return arrayOfStrings(esField(SYNONYMS), SYNONYMS);
    }

    private ObjectFieldModel externalReferences() {
      return object(esField(EXTERNAL_REFERENCES), EXTERNAL_REFERENCES,
          arrayOfStrings("chembl", CHEMBL),
          arrayOfStrings(esField(DRUG_BANK), multiAliases(EXTERNAL_REFERENCES, DRUG_BANK)));
    }

    private ArrayFieldModel atcCodes() {
      return arrayOfObjects(esField(ATC_CODES), ATC_CODES, object(
          string("code", ATC_CODE),
          string("description", ATC_DESCRIPTION),
          stringField(ATC_CODES, ATC_LEVEL5_CODES)));
    }

    private ArrayFieldModel genes() {
      return arrayOfObjects(esField(GENES), GENES, object(
          // string("chembl", GENES_CHEMBL),
          // string("description", GENES_DESCRIPTION),
          stringField(GENES, UNIPROT),
          stringField(GENES, ENSEMBL_GENE_ID)));
    }

    private ArrayFieldModel trialsConditions() {
      return arrayOfObjects("conditions", TRIALS_CONDITIONS, object(
          string("name", TRIALS_CONDITIONS_NAME),
          string("short_name", TRIALS_CONDITIONS_SHORT_NAME)));
    }

    private ArrayFieldModel trialsDrugMappings() {
      return arrayOfObjects("drug_mappings", TRIALS_DRUGS_MAPPINGS, object(
          string("description", TRIALS_DRUGS_MAPPINGS_DESCRIPTION),
          arrayOfStrings("ids", TRIALS_DRUGS_MAPPINGS_IDS)));
    }

    private ArrayFieldModel trials() {
      return arrayOfObjects(esField(TRIALS), TRIALS, object(
          string("code", TRIALS_CODE),
          string("description", TRIALS_DESCRIPTION),
          stringField(TRIALS, TRIALS_PHASE_NAME),
          stringField(TRIALS, TRIALS_START_DATE),
          stringField(TRIALS, TRIALS_STATUS_NAME),
          trialsConditions(),
          trialsDrugMappings()));
    }

    // Top-level fields
    private final List<StringFieldModel> stringFields = transform(
        newArrayList(NAME, SMALL_IMAGE_URL, LARGE_IMAGE_URL, INCHIKEY, DRUG_CLASS),
        DrugTypeModel::stringField);

    private final List<FieldModel> otherFields = newArrayList(primaryKey(ZINC_ID), numberField(CANCER_TRIAL_COUNT),
        synonyms(), externalReferences(), atcCodes(), genes(), trials());

    // Main mapping
    private final List<FieldModel> MAPPINGS = combine(Stream.of(stringFields, otherFields));

  }

  // FIXME: do we need to bother with this when most of the time we want all fields?
  public static final List<String> PUBLIC_FIELDS = ImmutableList.of();

  private static final Map<String, String> INTERNAL_ALIASES = ImmutableMap.<String, String> of();
  private static final List<String> INCLUDE_FIELDS = ImmutableList.of();
  private static final List<String> AVAILABLE_FACETS = ImmutableList.of();

  // Helpers
  private static String esField(String name) {
    return LOWER_CAMEL.to(LOWER_UNDERSCORE, name);
  }

  private static StringFieldModel primaryKey(String alias) {
    return string(esField(alias), ImmutableSet.of(Fields.ID, alias));
  }

  private static StringFieldModel stringField(String alias) {
    return string(esField(alias), alias);
  }

  private static StringFieldModel stringField(String parentAlias, String childAlias) {
    return string(esField(childAlias), multiAliases(parentAlias, childAlias));
  }

  private static LongFieldModel numberField(String alias) {
    return long_(esField(alias), alias);
  }

  @NonNull
  private static String fullAlias(String parentAlias, String childAlias) {
    return parentAlias + "." + childAlias;
  }

  private static Set<String> multiAliases(String parentAlias, String childAlias) {
    return ImmutableSet.of(childAlias, fullAlias(parentAlias, childAlias));
  }

  private static List<FieldModel> combine(Stream<List<? extends FieldModel>> stream) {
    return stream.flatMap(Collection::stream)
        .collect(toImmutableList());
  }

}
