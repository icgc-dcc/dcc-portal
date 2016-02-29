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
package org.icgc.dcc.portal.service;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.transformEntries;
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.intersection;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.union;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_GNU;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.left;
import static org.apache.commons.lang.StringUtils.right;
import static org.apache.commons.lang.math.NumberUtils.toLong;
import static org.apache.commons.lang.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
import static org.apache.commons.lang.time.DateFormatUtils.formatUTC;
import static org.dcc.portal.pql.meta.Type.REPOSITORY_FILE;
import static org.icgc.dcc.common.core.util.Joiners.COMMA;
import static org.icgc.dcc.common.core.util.Joiners.DOT;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.portal.model.RepositoryFile.parse;
import static org.icgc.dcc.portal.repository.RepositoryFileRepository.toRawFieldName;
import static org.icgc.dcc.portal.repository.RepositoryFileRepository.toStringArray;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getString;
import static org.icgc.dcc.portal.util.SearchResponses.hasHits;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.dcc.portal.pql.meta.RepositoryFileTypeModel.Fields;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.collect.Iterables;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.icgc.dcc.portal.model.Keyword;
import org.icgc.dcc.portal.model.Keywords;
import org.icgc.dcc.portal.model.Pagination;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.RepositoryFile;
import org.icgc.dcc.portal.model.RepositoryFile.Donor;
import org.icgc.dcc.portal.model.RepositoryFile.FileCopy;
import org.icgc.dcc.portal.model.RepositoryFiles;
import org.icgc.dcc.portal.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.repository.RepositoryFileRepository;
import org.icgc.dcc.portal.repository.TermsLookupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.CsvMapWriter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }) )
public class RepositoryFileService {

  private static final Jql2PqlConverter PQL_CONVERTER = Jql2PqlConverter.getInstance();
  private static final String DATE_FORMAT_PATTERN = ISO_DATETIME_TIME_ZONE_FORMAT.getPattern();
  private static final String UTF_8 = StandardCharsets.UTF_8.name();
  private static final int BUFFER_SIZE = 1024 * 100;
  private static final List<String> MANIFEST_SIMPLE_FIELDS = ImmutableList.of(
      Fields.FILE_UUID,
      Fields.FILE_ID,
      Fields.STUDY,
      Fields.DATA_BUNDLE_ID);

  private static final Map<String, String> DATA_TABLE_EXPORT_MAP = ImmutableMap.<String, String> builder()
      .put(Fields.ACCESS, "Access")
      .put(Fields.FILE_ID, "File ID")
      .put(Fields.DONOR_ID, "ICGC Donor")
      .put(Fields.REPO_NAME, "Repository")
      .put(Fields.PROJECT_CODE, "Project")
      .put(Fields.STUDY, "Study")
      .put(Fields.DATA_TYPE, "Data Type")
      .put(Fields.EXPERIMENTAL_STRATEGY, "Experimental Strategy")
      .put(Fields.FILE_FORMAT, "Format")
      .put(Fields.FILE_SIZE, "Size (bytes)")
      .build();
  private static final Set<String> DATA_TABLE_EXPORT_MAP_FIELD_KEYS = toRawFieldSet(
      DATA_TABLE_EXPORT_MAP.keySet());
  private static final String[] DATA_TABLE_EXPORT_MAP_FIELD_ARRAY = toStringArray(DATA_TABLE_EXPORT_MAP_FIELD_KEYS);

  private static final Set<String> DATA_TABLE_EXPORT_SUMMARY_FIELDS = toRawFieldSet(newArrayList(
      Fields.DONOR_ID, Fields.PROJECT_CODE));
  private static final Set<String> DATA_TABLE_EXPORT_AVERAGE_FIELDS = toRawFieldSet(newArrayList(
      Fields.FILE_SIZE));
  private static final Set<String> DATA_TABLE_EXPORT_OTHER_FIELDS = difference(DATA_TABLE_EXPORT_MAP_FIELD_KEYS,
      union(DATA_TABLE_EXPORT_SUMMARY_FIELDS, DATA_TABLE_EXPORT_AVERAGE_FIELDS));
  private static final Map<Collection<String>, Function<SearchHitField, String>> DATA_TABLE_EXPORT_FIELD_PROCESSORS =
      ImmutableMap.<Collection<String>, Function<SearchHitField, String>> of(
          DATA_TABLE_EXPORT_SUMMARY_FIELDS, RepositoryFileService::toSummarizedString,
          DATA_TABLE_EXPORT_AVERAGE_FIELDS, RepositoryFileService::toAverageSizeString,
          DATA_TABLE_EXPORT_OTHER_FIELDS, RepositoryFileService::toStringValue);

  private static final Joiner COMMA_JOINER = COMMA.skipNulls();
  private static final Keywords NO_MATCH_KEYWORD_SEARCH_RESULT = new Keywords(emptyList());
  private static final Map<String, String> KEYWORD_SEARCH_TYPE_ENTRY = ImmutableMap.of(
      "type", "donor");
  private static final Map<String, String> FILE_DONOR_INDEX_TYPE_TO_KEYWORD_FIELD_MAPPING =
      ImmutableMap.<String, String> builder()
          .put("id", "id")
          .put("specimen_id", "specimenIds")
          .put("sample_id", "sampleIds")
          .put("submitted_donor_id", "submittedId")
          .put("submitted_specimen_id", "submittedSpecimenIds")
          .put("submitted_sample_id", "submittedSampleIds")
          .put("tcga_participant_barcode", "TCGAParticipantBarcode")
          .put("tcga_sample_barcode", "TCGASampleBarcode")
          .put("tcga_aliquot_barcode", "TCGAAliquotBarcode")
          .build();

  // Manifest column definitions
  private static final String[] TSV_HEADERS = { "url", "file_name", "file_size", "md5_sum", "study" };
  private static final List<String> TSV_COLUMN_FIELD_NAMES = ImmutableList.of(
      Fields.FILE_NAME,
      Fields.FILE_SIZE,
      Fields.FILE_MD5SUM,
      Fields.STUDY);
  private static final Map<String, String> COUNT_FIELD_FORMAT_STRINGS = ImmutableMap.<String, String> of(
      Fields.DONOR_ID, "%d donors",
      Fields.PROJECT_CODE, "%d projects");
  private static final Map<String, Function<Donor, String>> COUNT_FIELD_GETTERS =
      ImmutableMap.<String, Function<Donor, String>> of(
          Fields.DONOR_ID, Donor::getDonorId,
          Fields.PROJECT_CODE, Donor::getProjectCode);

  private static final String[] AWS_TSV_HEADERS =
      { "repo_code", "file_id", "object_id", "file_format", "file_name", "file_size", "md5_sum", "index_object_id", "donor_id/donor_count", "project_id/project_count", "study" };
  private static final List<String> AWS_TSV_COLUMN_FIELD_NAMES = ImmutableList.of(
      Fields.REPO_CODE,
      Fields.FILE_ID,
      Fields.FILE_UUID,
      Fields.FILE_FORMAT,
      Fields.FILE_NAME,
      Fields.FILE_SIZE,
      Fields.FILE_MD5SUM,
      Fields.INDEX_OBJECT_UUID,
      Fields.DONOR_ID,
      Fields.PROJECT_CODE,
      Fields.STUDY);

  private static final BiFunction<Collection<Map<String, String>>, String, String> CONCAT_WITH_COMMA =
      (fileInfo, fieldName) -> COMMA_JOINER.join(transform(fileInfo, map -> map.get(fieldName)));

  private final RepositoryFileRepository repositoryFileRepository;
  private final TermsLookupRepository termsLookupRepository;

  public Map<String, String> getIndexMetadata() {
    return repositoryFileRepository.getIndexMetaData();
  }

  public Map<String, String> getRepositoryMap() {
    return repositoryFileRepository.getRepositoryMap();
  }

  @NonNull
  public void exportTableData(OutputStream output, Query query) {
    val prepResponse = repositoryFileRepository.prepareDataExport(query, DATA_TABLE_EXPORT_MAP_FIELD_ARRAY);

    generateTabDelimitedData(output, prepResponse, DATA_TABLE_EXPORT_MAP_FIELD_ARRAY);
  }

  @NonNull
  public void exportTableDataFromSet(OutputStream output, String setId) {
    val prepResponse = repositoryFileRepository.prepareSetDataExport(setId, DATA_TABLE_EXPORT_MAP_FIELD_ARRAY);

    generateTabDelimitedData(output, prepResponse, DATA_TABLE_EXPORT_MAP_FIELD_ARRAY);
  }

  @SneakyThrows
  private void generateTabDelimitedData(OutputStream output, SearchResponse prepResponse, String[] keys) {
    @Cleanup
    val writer = new CsvMapWriter(new BufferedWriter(new OutputStreamWriter(output, UTF_8)), TAB_PREFERENCE);
    writer.writeHeader(toStringArray(DATA_TABLE_EXPORT_MAP.values()));

    String scrollId = prepResponse.getScrollId();

    while (true) {
      val response = repositoryFileRepository.fetchSearchScrollData(scrollId);

      if (!hasHits(response)) {
        break;
      }

      for (val hit : response.getHits()) {
        writer.write(toRowValueMap(hit), keys);
      }

      scrollId = response.getScrollId();
    }

  }

  private static Set<String> toRawFieldSet(Collection<String> aliases) {
    return aliases.stream().map(k -> toRawFieldName(k))
        .collect(toImmutableSet());
  }

  private static String combineUniqueItemsToString(SearchHitField hitField, Function<Set<Object>, String> combiner) {
    return (null == hitField) ? "" : combiner.apply(newHashSet(hitField.getValues()));
  }

  private static String toStringValue(SearchHitField hitField) {
    return combineUniqueItemsToString(hitField, COMMA_JOINER::join);
  }

  private static String toSummarizedString(SearchHitField hitField) {
    return combineUniqueItemsToString(hitField, RepositoryFileService::toSummarizedString);
  }

  private static String toSummarizedString(Set<Object> values) {
    if (isEmpty(values)) {
      return "";
    }

    val count = values.size();

    // Get the value if there is only one element; otherwise get the count or empty string if empty.
    return (count > 1) ? String.valueOf(count) : Iterables.get(values, 0).toString();
  }

  private static String toAverageSizeString(SearchHitField hitField) {
    if (null == hitField) {
      return "0";
    }

    val average = hitField.getValues().stream()
        .mapToLong(o -> toLong(o.toString(), 0))
        .average();

    return String.valueOf(average.orElse(0));
  }

  private static <K, V> Map<K, V> combineMaps(Stream<? extends Map<K, V>> source) {
    return source.map(Map::entrySet)
        .flatMap(Collection::stream)
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static <T> List<T> combineCollections(Stream<? extends Collection<T>> source) {
    return source.flatMap(Collection::stream)
        .collect(toImmutableList());
  }

  @NonNull
  private static Map<String, String> toRowValueMap(SearchHit hit) {
    val valueMap = hit.getFields();
    val maps = DATA_TABLE_EXPORT_FIELD_PROCESSORS.entrySet().stream().map(fieldsProcessorPair -> {
      // Takes a collection of fields and its processor and produces a map of field and its value.
      return Maps.<String, String> toMap(fieldsProcessorPair.getKey(),
          field -> fieldsProcessorPair.getValue().apply(valueMap.get(field)));
    });

    return combineMaps(maps);
  }

  /**
   * Emulating keyword search, but without prefix/ngram analyzers..ie: exact match
   */
  public Keywords findRepoDonor(@NonNull Query query) {
    val response = repositoryFileRepository.findRepoDonor(
        FILE_DONOR_INDEX_TYPE_TO_KEYWORD_FIELD_MAPPING.keySet(), query.getQuery());
    val hits = response.getHits();

    if (hits.totalHits() < 1) {
      return NO_MATCH_KEYWORD_SEARCH_RESULT;
    }

    val keywords = transform(hits,
        hit -> new Keyword(toKeywordFieldMap(hit)));

    return new Keywords(newArrayList(keywords));
  }

  public RepositoryFile findOne(@NonNull String fileId) {
    log.info("External repository file id is: '{}'.", fileId);

    val response = repositoryFileRepository.findOne(fileId);
    return parse(response.getSourceAsString());
  }

  public long getDonorCount(@NonNull Query query) {
    return repositoryFileRepository.getDonorCount(query);
  }

  public RepositoryFiles findAll(@NonNull Query query) {
    val response = repositoryFileRepository.findAll(query);
    val hits = response.getHits();
    val externalFiles = new RepositoryFiles(convertHitsToRepoFiles(hits));

    externalFiles.setTermFacets(
        repositoryFileRepository.convertAggregationsToFacets(response.getAggregations(), query));
    externalFiles.setPagination(Pagination.of(hits.getHits().length, hits.getTotalHits(), query));

    return externalFiles;
  }

  @NonNull
  public void generateManifestArchive(OutputStream output, Date timestamp, Query query, List<String> repoList)
      throws JsonProcessingException, IOException {
    val data = getData(query);

    @Cleanup
    val tar = new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(output)));
    tar.setLongFileMode(LONGFILE_GNU);

    val repoIncludes = removeEmptyString(repoList);
    val repoCodeGroups = Multimaps.index(data, m -> m.get(Fields.REPO_CODE));

    // This writes out the results to the tar archive.
    for (val repoCode : repoCodeGroups.keySet()) {
      if (shouldRepositoryBeExcluded(repoIncludes, repoCode)) {
        continue;
      }

      val entries = repoCodeGroups.get(repoCode);

      if (isEmpty(entries)) {
        continue;
      }

      // Entries with the same repoCode should & must have the same repoType.
      val repoType = entries.get(0).get(Fields.REPO_TYPE);

      generateTarEntry(tar, entries, repoCode, repoType, timestamp);
    }
  }

  @SneakyThrows
  private void generateRepoManifestFile(OutputStream output, String repoCode, SearchResponse searchResult,
      Date timestamp) {
    val data = FluentIterable.from(searchResult.getHits())
        .transformAndConcat(hit -> toValueMap(hit));

    @Cleanup
    val buffer = new BufferedOutputStream(output);
    val repoCodeGroups = Multimaps.index(data, m -> m.get(Fields.REPO_CODE));

    for (val code : repoCodeGroups.keySet()) {
      // Make sure we process the target repo only.
      if (!repoCode.equals(code)) {
        continue;
      }

      val repoData = repoCodeGroups.get(repoCode);

      if (isEmpty(repoData)) {
        break;
      }

      // Entries with the same repoCode should & must have the same repoType.
      val repoType = repoData.get(0).get(Fields.REPO_TYPE);
      val downloadUrlGroups = Multimaps.index(repoData, entry -> buildDownloadUrl(entry));

      if (RepoTypes.isGnos(repoType)) {
        generateXmlFile(buffer, downloadUrlGroups, timestamp);
      } else if (RepoTypes.isAws(repoType)) {
        generateAwsTextFile(buffer, downloadUrlGroups);
      } else {
        generateTextFile(buffer, downloadUrlGroups);
      }
    }
  }

  @NonNull
  public void generateManifestFile(OutputStream output, Date timestamp, Query query, String repoCode) {
    val pql = PQL_CONVERTER.convert(query, REPOSITORY_FILE);
    log.debug("Received JQL: '{}'; converted to PQL: '{}'.", query.getFilters(), pql);

    val searchResult = repositoryFileRepository.findDownloadInfo(pql);

    generateRepoManifestFile(output, repoCode, searchResult, timestamp);
  }

  @NonNull
  @SneakyThrows
  public void generateManifestFileFromSet(OutputStream output, Date timestamp, String setId) {
    String repoCode;
    val repoName = termsLookupRepository.getRepoName(setId);
    if ("AWS - Virginia".equals(repoName)) {
      repoCode = "aws-virginia";
    } else if ("Collaboratory".equals(repoName)) {
      repoCode = "collaboratory";
    } else {
      throw new BadRequestException("Only Collaboratory and AWS - Virginia are supported for this operation.");
    }

    val searchResult = repositoryFileRepository.findDownloadInfoFromSet(setId);

    generateRepoManifestFile(output, repoCode, searchResult, timestamp);
  }

  private Iterable<Map<String, String>> getData(@NonNull final Query query) {
    val pql = PQL_CONVERTER.convert(query, REPOSITORY_FILE);
    log.debug("Received JQL: '{}'; converted to PQL: '{}'.", query.getFilters(), pql);

    val searchResult = repositoryFileRepository.findDownloadInfo(pql);

    return FluentIterable.from(searchResult.getHits())
        .transformAndConcat(hit -> toValueMap(hit));
  }

  private List<String> removeEmptyString(@NonNull Iterable<String> strings) {
    return FluentIterable.from(strings)
        .filter(s -> !isBlank(s))
        .toList();
  }

  private static Map<String, Object> toKeywordFieldMap(@NonNull SearchHit hit) {
    val valueMap = hit.getSource();
    val commonKeys = intersection(FILE_DONOR_INDEX_TYPE_TO_KEYWORD_FIELD_MAPPING.keySet(), valueMap.keySet());

    val result = commonKeys.stream().collect(toMap(
        key -> FILE_DONOR_INDEX_TYPE_TO_KEYWORD_FIELD_MAPPING.get(key),
        key -> valueMap.get(key)));
    result.putAll(KEYWORD_SEARCH_TYPE_ENTRY);

    return result;
  }

  private static List<RepositoryFile> convertHitsToRepoFiles(SearchHits hits) {
    return FluentIterable.from(hits)
        .transform(hit -> parse(hit.getSourceAsString()))
        .toList();
  }

  private static boolean shouldRepositoryBeExcluded(List<String> repoIncludes, String repoCode) {
    return !(isEmpty(repoIncludes) || repoIncludes.contains(repoCode));
  }

  // Merge the fields with flattened file copies fields.
  private static Iterable<Map<String, String>> toValueMap(SearchHit hit) {
    val repoFile = parse(hit.sourceAsString());
    val fileCopies = repoFile.getFileCopies();

    if (isEmpty(fileCopies)) {
      return emptyList();
    }

    val fields = asValueMap(hit);
    val donorInfoMap = getDonorValueMap(repoFile.getDonors());

    return transform(fileCopies, fileCopy -> combineMaps(Stream.of(fields, donorInfoMap, getFileCopyMap(fileCopy))));
  }

  private static Map<String, String> getDonorValueMap(List<Donor> donors) {
    val count = isEmpty(donors) ? 0 : donors.size();
    val donor = (count > 0) ? donors.get(0) : null;

    val counts = transformValues(COUNT_FIELD_FORMAT_STRINGS,
        formatString -> (count > 0) ? format(formatString, count) : "");

    // Get the value if there is only one element; otherwise get the formatted count.
    return transformEntries(COUNT_FIELD_GETTERS,
        (key, getter) -> (count == 1) ? getter.apply(donor) : counts.get(key));
  }

  @NonNull
  private static SearchHitField getResultByFieldAlias(Map<String, SearchHitField> valueMap, String alias) {
    return valueMap.get(toRawFieldName(alias));
  }

  private static Map<String, String> asValueMap(SearchHit hit) {
    val valueMap = hit.getFields();

    return Maps.toMap(MANIFEST_SIMPLE_FIELDS, alias -> {
      final SearchHitField resultField = getResultByFieldAlias(valueMap, alias);

      return (null == resultField) ? "" : defaultString(getString(resultField.getValues()));
    });
  }

  private static Map<String, String> getFileCopyMap(FileCopy fileCopy) {
    val indexFile = fileCopy.getIndexFile();
    val indexObjectId = (null == indexFile) ? "" : defaultString(indexFile.getObjectId());

    return ImmutableMap.<String, String> builder()
        .put(Fields.FILE_NAME, defaultString(fileCopy.getFileName()))
        .put(Fields.FILE_FORMAT, defaultString(fileCopy.getFileFormat()))
        .put(Fields.FILE_MD5SUM, defaultString(fileCopy.getFileMd5sum()))
        .put(Fields.FILE_SIZE, String.valueOf(fileCopy.getFileSize()))
        .put(Fields.INDEX_OBJECT_UUID, indexObjectId)
        .put(Fields.REPO_CODE, defaultString(fileCopy.getRepoCode()))
        .put(Fields.REPO_TYPE, defaultString(fileCopy.getRepoType()))
        .put(Fields.REPO_BASE_URL, defaultString(fileCopy.getRepoBaseUrl()))
        .put(Fields.REPO_DATA_PATH, defaultString(fileCopy.getRepoDataPath()))
        .build();
  }

  @UtilityClass
  class RepoTypes {

    final String GNOS = "GNOS";
    final String AWS_S3 = "S3";

    boolean isGnos(String repoType) {
      return GNOS.equalsIgnoreCase(repoType);
    }

    boolean isAws(String repoType) {
      return AWS_S3.equalsIgnoreCase(repoType);
    }
  }

  @SneakyThrows
  @NonNull
  private static void generateTarEntry(TarArchiveOutputStream tar, List<Map<String, String>> allFileInfoOfOneRepo,
      String repoCode, String repoType, Date timestamp) {
    val isGnos = RepoTypes.isGnos(repoType);
    val downloadUrlGroups =
        Multimaps.index(allFileInfoOfOneRepo, entry -> isGnos ? buildGnosDownloadUrl(entry) : buildDownloadUrl(entry));

    @Cleanup
    val buffer = new ByteArrayOutputStream(BUFFER_SIZE);

    if (isGnos) {
      generateXmlFile(buffer, downloadUrlGroups, timestamp);
    } else if (RepoTypes.isAws(repoType)) {
      generateAwsTextFile(buffer, downloadUrlGroups);
    } else {
      generateTextFile(buffer, downloadUrlGroups);
    }

    addFileToTar(buildFileName(repoCode, repoType, timestamp), buffer, tar);
  }

  @SneakyThrows
  @NonNull
  private static void generateXmlFile(OutputStream buffer, ListMultimap<String, Map<String, String>> downloadUrlGroups,
      Date timestamp) {
    int rowCount = 0;
    // If this is thread-safe, perhaps we can make this static???
    val factory = XMLOutputFactory.newInstance();
    @Cleanup
    val writer = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(buffer, UTF_8));

    startXmlDocument(writer, timestamp);

    for (val url : downloadUrlGroups.keySet()) {
      val rowInfo = downloadUrlGroups.get(url);

      if (isEmpty(rowInfo)) {
        continue;
      }

      // TODO: is this still true that same url has the same data_bundle_id??
      val repoId = rowInfo.get(0).get(Fields.DATA_BUNDLE_ID);

      writeXmlEntry(writer, repoId, url, rowInfo, ++rowCount);
    }

    endXmlDocument(writer);
  }

  @SneakyThrows
  private static CsvListWriter createTsv(OutputStream stream) {
    return new CsvListWriter(new OutputStreamWriter(stream, UTF_8), TAB_PREFERENCE);
  }

  @SneakyThrows
  @NonNull
  private static void generateTextFile(OutputStream buffer, Multimap<String, Map<String, String>> downloadUrlGroups) {
    @Cleanup
    val tsv = createTsv(buffer);
    tsv.writeHeader(TSV_HEADERS);

    for (val url : downloadUrlGroups.keySet()) {
      val fileInfo = downloadUrlGroups.get(url);
      val otherColumns = Lists.transform(TSV_COLUMN_FIELD_NAMES,
          fieldName -> CONCAT_WITH_COMMA.apply(fileInfo, fieldName));
      val row = combineCollections(Stream.of(newArrayList(url), otherColumns));

      tsv.write(row);
    }

    tsv.flush();
  }

  @SneakyThrows
  @NonNull
  private static void generateAwsTextFile(OutputStream buffer,
      Multimap<String, Map<String, String>> downloadUrlGroups) {
    @Cleanup
    val tsv = createTsv(buffer);
    tsv.writeHeader(AWS_TSV_HEADERS);

    for (val url : downloadUrlGroups.keySet()) {
      val fileInfo = downloadUrlGroups.get(url);
      val row = Lists.transform(AWS_TSV_COLUMN_FIELD_NAMES,
          fieldName -> CONCAT_WITH_COMMA.apply(fileInfo, fieldName));

      tsv.write(row);
    }

    tsv.flush();
  }

  private static String getFileExtensionOf(String repoType) {
    return RepoTypes.isGnos(repoType) ? "xml" : "txt";
  }

  @NonNull
  private static String buildFileName(String repoCode, String repoType, Date timestamp) {
    return DOT.join(Arrays.asList(
        "manifest",
        repoCode,
        timestamp.getTime(),
        getFileExtensionOf(repoType)));
  }

  @NonNull
  private static void addFileToTar(String fileName, ByteArrayOutputStream content, TarArchiveOutputStream tar)
      throws IOException {
    val tarEntry = new TarArchiveEntry(fileName);

    tarEntry.setSize(content.size());
    tar.putArchiveEntry(tarEntry);

    content.writeTo(tar);
    tar.closeArchiveEntry();
  }

  @NonNull
  private static String removeBookendingCharacter(String input, String characterToRemove) {
    val lengthToRemove = characterToRemove.length();

    if (lengthToRemove < 1) {
      return input;
    }

    val result = input.startsWith(characterToRemove) ? right(input, input.length() - lengthToRemove) : input;

    if (null == result) {
      return null;
    }

    return result.endsWith(characterToRemove) ? left(result, result.length() - lengthToRemove) : result;
  }

  @NonNull
  private static String buildDownloadUrl(String baseUrl, String dataPath, String id) {
    return Stream.of(baseUrl, dataPath, id)
        .map(part -> part.replaceAll("^/+|/+$", ""))
        .collect(joining("/"));
  }

  private static String buildDownloadUrl(@NonNull Map<String, String> valueMap) {
    return buildDownloadUrl(
        valueMap.get(Fields.REPO_BASE_URL),
        valueMap.get(Fields.REPO_DATA_PATH),
        valueMap.get(Fields.FILE_NAME));
  }

  private static String buildGnosDownloadUrl(@NonNull Map<String, String> valueMap) {
    return buildDownloadUrl(
        valueMap.get(Fields.REPO_BASE_URL),
        valueMap.get(Fields.REPO_DATA_PATH),
        valueMap.get(Fields.DATA_BUNDLE_ID));
  }

  private static String formatToUtc(@NonNull Date timestamp) {
    return formatUTC(timestamp, DATE_FORMAT_PATTERN);
  }

  @UtilityClass
  private class XmlTags {

    final String ROOT = "ResultSet";
    final String RECORD = "Result";
    final String RECORD_ID = "analysis_id";
    final String RECORD_URI = "analysis_data_uri";
    final String FILES = "files";
    final String FILE = "file";
    final String FILE_NAME = "filename";
    final String FILE_SIZE = "filesize";
    final String CHECK_SUM = "checksum";

  }

  @NonNull
  private static void startXmlDocument(XMLStreamWriter writer, Date timestamp) throws XMLStreamException {
    writer.writeStartDocument(UTF_8, "1.0");
    writer.writeStartElement(XmlTags.ROOT);
    writer.writeAttribute("date", formatToUtc(timestamp));
  }

  @NonNull
  private static void endXmlDocument(XMLStreamWriter writer) throws XMLStreamException {
    // Closes the root element - XmlTags.ROOT
    writer.writeEndElement();
    writer.writeEndDocument();
  }

  @NonNull
  private static void addDownloadUrlEntryToXml(XMLStreamWriter writer, String id, String downloadUrl, int rowCount)
      throws XMLStreamException {
    writer.writeStartElement(XmlTags.RECORD);
    writer.writeAttribute("id", String.valueOf(rowCount));

    addXmlElement(writer, XmlTags.RECORD_ID, id);
    addXmlElement(writer, XmlTags.RECORD_URI, downloadUrl);

    writer.writeStartElement(XmlTags.FILES);
  }

  private static void closeDownloadUrlElement(@NonNull XMLStreamWriter writer) throws XMLStreamException {
    // Close off XmlTags.FILES ("files") element.
    writer.writeEndElement();
    // Close off XmlTags.RECORD ("Result") element.
    writer.writeEndElement();
  }

  @NonNull
  private static void addFileInfoEntriesToXml(XMLStreamWriter writer, Iterable<Map<String, String>> info)
      throws XMLStreamException {
    for (val fileInfo : info) {
      writer.writeStartElement(XmlTags.FILE);

      addXmlElement(writer, XmlTags.FILE_NAME, fileInfo.get(Fields.FILE_NAME));
      addXmlElement(writer, XmlTags.FILE_SIZE, fileInfo.get(Fields.FILE_SIZE));

      writer.writeStartElement(XmlTags.CHECK_SUM);
      writer.writeAttribute("type", "md5");
      writer.writeCharacters(fileInfo.get(Fields.FILE_MD5SUM));
      writer.writeEndElement();

      writer.writeEndElement();
    }
  }

  @NonNull
  private static void addXmlElement(XMLStreamWriter writer, String elementName, String elementValue)
      throws XMLStreamException {
    writer.writeStartElement(elementName);
    writer.writeCharacters(elementValue);
    writer.writeEndElement();
  }

  @NonNull
  private static void writeXmlEntry(XMLStreamWriter writer, String id, String downloadUrl,
      Iterable<Map<String, String>> fileInfo, final int rowCount) throws XMLStreamException {
    addDownloadUrlEntryToXml(writer, id, downloadUrl, rowCount);
    addFileInfoEntriesToXml(writer, fileInfo);
    closeDownloadUrlElement(writer);
  }

  public Map<String, Long> getSummary(@NonNull Query query) {
    return repositoryFileRepository.getSummary(query);
  }

  public Map<String, Map<String, Map<String, Object>>> getStudyStats(String study) {
    return repositoryFileRepository.getStudyStats(study);
  }

  public Map<String, Map<String, Map<String, Object>>> getRepoStats(String repoName) {
    return repositoryFileRepository.getRepoStats(repoName);
  }

}
