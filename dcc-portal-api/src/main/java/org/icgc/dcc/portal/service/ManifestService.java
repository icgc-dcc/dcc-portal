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

import static com.google.common.collect.Iterables.getFirst;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.defaultString;
import static org.dcc.portal.pql.meta.Type.REPOSITORY_FILE;
import static org.icgc.dcc.common.core.util.Joiners.DOT;
import static org.icgc.dcc.common.core.util.stream.Streams.stream;
import static org.icgc.dcc.portal.repository.RepositoryFileRepository.toRawFieldName;
import static org.icgc.dcc.portal.util.ElasticsearchResponseUtils.getString;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.dcc.portal.pql.meta.RepositoryFileTypeModel.Fields;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.icgc.dcc.portal.manifest.GNOSManifest;
import org.icgc.dcc.portal.manifest.GenericManifest;
import org.icgc.dcc.portal.manifest.ICGCStorageManifest;
import org.icgc.dcc.portal.manifest.ManfiestArchive;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.model.RepositoryFile;
import org.icgc.dcc.portal.model.RepositoryFile.Donor;
import org.icgc.dcc.portal.model.RepositoryFile.FileCopy;
import org.icgc.dcc.portal.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.repository.RepositoryFileRepository;
import org.icgc.dcc.portal.repository.TermsLookupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class ManifestService {

  /**
   * Constants.
   */
  private static final int BUFFER_SIZE = 1024 * 100;

  /**
   * Dependencies.
   */
  @NonNull
  private final RepositoryFileRepository repositoryFileRepository;
  @NonNull
  private final TermsLookupRepository termsLookupRepository;

  public void generateManifestArchive(OutputStream output, Date timestamp, Query query, List<String> repoCodes)
      throws IOException {
    // Get requested file copies
    val watch = Stopwatch.createStarted();

    log.info("Reading manifest archive files...");
    val searchResult = findFiles(query);
    log.info("Read manifest archive files in {}", watch);

    @Cleanup
    val archive = new ManfiestArchive(output);

    // Write a manifest for each repository in turn
    log.info("Writing manifest archive...");
    eachRepository(repoCodes, searchResult, output, (repoCode, repoType, bundles) -> {
      @Cleanup
      ByteArrayOutputStream fileContents = new ByteArrayOutputStream(BUFFER_SIZE);
      writeManifest(repoType, timestamp, bundles, fileContents);

      String fileName = formatFileName(repoCode, repoType, timestamp);
      archive.addManifest(fileName, fileContents);
    });

    log.info("Finsished creating manifest archive in {}", watch);
  }

  public void generateManifestFile(OutputStream output, Date timestamp, Query query, String repoCode) {
    // Get requested file copies
    val searchResult = findFiles(query);
    generateManifestFile(output, repoCode, searchResult, timestamp);
  }

  // TODO: Remove and use {@link generateManifestFile}
  public void generateManifestFile(OutputStream output, Date timestamp, String setId) {
    String repoCode;
    val repoName = termsLookupRepository.getRepoName(setId);
    if ("AWS - Virginia".equals(repoName)) {
      repoCode = "aws-virginia";
    } else if ("Collaboratory".equals(repoName)) {
      repoCode = "collaboratory";
    } else {
      throw new IllegalArgumentException("Only Collaboratory and AWS - Virginia are supported for this operation.");
    }

    // Get requested file copies
    val searchResult = findFiles(setId);
    generateManifestFile(output, repoCode, searchResult, timestamp);
  }

  @SneakyThrows
  private void generateManifestFile(OutputStream output, String repoCode, SearchResponse searchResult, Date timestamp) {
    eachRepository(singleton(repoCode), searchResult, output, (repoCode1, repoType, bundles) -> {
      @Cleanup
      OutputStream fileContents = new BufferedOutputStream(output);
      writeManifest(repoType, timestamp, bundles, fileContents);
    });
  }

  private void eachRepository(Collection<String> repoCodes, SearchResponse searchResult, OutputStream output,
      RespositoryBundlesCallback callback)
      throws IOException {
    val fileCopies = FileCopyMapper.map(searchResult);

    // Index
    val repoCodeFileCopies = Multimaps.index(fileCopies, fc -> fc.get(Fields.REPO_CODE));

    for (val repoCode : repoCodeFileCopies.keySet()) {
      val excluded = !repoCodes.contains(repoCode);
      if (excluded) {
        continue;
      }

      val repoFileCopies = repoCodeFileCopies.get(repoCode);
      if (repoFileCopies.isEmpty()) {
        continue;
      }

      // Entries with the same repoCode should & must have the same repoType.
      val repoType = getFirst(repoFileCopies, null).get(Fields.REPO_TYPE);
      val bundles = Multimaps.index(repoFileCopies, fileCopy -> formatFileURL(fileCopy));

      callback.handle(repoCode, repoType, bundles);
    }
  }

  private SearchResponse findFiles(Query query) {
    val converter = Jql2PqlConverter.getInstance();
    val pql = converter.convert(query, REPOSITORY_FILE);
    log.debug("Received JQL: '{}'; converted to PQL: '{}'.", query.getFilters(), pql);

    return repositoryFileRepository.findDownloadInfo(pql);
  }

  private SearchResponse findFiles(String setId) {
    return repositoryFileRepository.findDownloadInfoFromSet(setId);
  }

  private static void writeManifest(String repoType, Date timestamp,
      ListMultimap<String, Map<String, String>> downloadUrlGroups, OutputStream out) {
    if (isGnos(repoType)) {
      GNOSManifest.write(out, downloadUrlGroups, timestamp);
    } else if (isS3(repoType)) {
      ICGCStorageManifest.write(out, downloadUrlGroups);
    } else {
      // Not sure who consumes this one...
      GenericManifest.write(out, downloadUrlGroups);
    }
  }

  private static String formatFileURL(String baseUrl, String dataPath, String id) {
    return Stream.of(baseUrl, dataPath, id)
        .map(part -> part.replaceAll("^/+|/+$", ""))
        .collect(joining("/"));
  }

  private static String formatFileURL(@NonNull Map<String, String> fileCopy) {
    val repoType = fileCopy.get(Fields.REPO_TYPE);
    if (isGnos(repoType)) {
      return formatFileURL(
          fileCopy.get(Fields.REPO_BASE_URL),
          fileCopy.get(Fields.REPO_DATA_PATH),
          fileCopy.get(Fields.DATA_BUNDLE_ID));
    } else {
      return formatFileURL(
          fileCopy.get(Fields.REPO_BASE_URL),
          fileCopy.get(Fields.REPO_DATA_PATH),
          fileCopy.get(Fields.FILE_NAME));
    }
  }

  private static String formatFileName(String repoCode, String repoType, Date timestamp) {
    return DOT.join(Arrays.asList(
        "manifest",
        repoCode,
        timestamp.getTime(),
        isGnos(repoType) ? "xml" : "txt"));
  }

  private static boolean isGnos(String repoType) {
    return "GNOS".equalsIgnoreCase(repoType);
  }

  private static boolean isS3(String repoType) {
    return "S3".equalsIgnoreCase(repoType);
  }

  /**
   * Callback for processing a grouped set of files by url (a.k.a a bundle).
   */
  private interface RespositoryBundlesCallback {

    void handle(String repoCode, String repoType, ListMultimap<String, Map<String, String>> bundles)
        throws IOException;

  }

  /**
   * Mapping logic from hits to file copy thingies?
   */
  private static class FileCopyMapper {

    /**
     * Constants.
     */
    private static final List<String> FILE_FIELDS = ImmutableList.of(
        Fields.FILE_UUID,
        Fields.FILE_ID,
        Fields.STUDY,
        Fields.DATA_BUNDLE_ID);

    public static List<Map<String, String>> map(SearchResponse searchResult) {
      return stream(searchResult.getHits()).flatMap(hit -> map(hit)).collect(toList());
    }

    /**
     * Merge the fields with flattened file copies fields.
     */
    private static Stream<Map<String, String>> map(SearchHit hit) {
      val file = RepositoryFile.parse(hit.sourceAsString());
      val fileCopies = file.getFileCopies();

      if (isEmpty(fileCopies)) {
        return Stream.empty();
      }

      // Collect common data
      val fileFields = mapFile(hit, file);

      // Map and combine with common data
      return fileCopies.stream()
          .map(fileCopy -> mapFileCopy(fileCopy))
          .map(fileCopy -> fileCopy.putAll(fileFields))
          .map(fileCopy -> fileCopy.build());
    }

    private static Builder<String, String> mapFileCopy(FileCopy fileCopy) {
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
          .put(Fields.REPO_DATA_PATH, defaultString(fileCopy.getRepoDataPath()));
    }

    private static Map<String, String> mapFile(SearchHit hit, RepositoryFile file) {
      val fileFields = Maps.<String, String> newHashMap();
      for (val fieldName : FILE_FIELDS) {
        fileFields.put(fieldName, mapValue(hit, fieldName));
      }

      val donors = file.getDonors() == null ? Collections.<Donor> emptyList() : file.getDonors();
      val donorCount = donors.size();
      val projectCount = donors.stream().map(Donor::getProjectCode).distinct().count();

      fileFields.put(Fields.DONOR_ID,
          donorCount == 0 ? "" : donorCount == 1 ? donors.get(0).getDonorId() : donorCount + " donors");
      fileFields.put(Fields.PROJECT_CODE,
          projectCount == 0 ? "" : donorCount == 1 ? donors.get(0).getProjectCode() : projectCount + " projects");

      return fileFields;
    }

    private static String mapValue(SearchHit hit, String fieldName) {
      val rawFieldName = toRawFieldName(fieldName);
      val resultField = hit.getFields().get(rawFieldName);

      return null == resultField ? "" : defaultString(getString(resultField.getValues()));
    }

  }

}
