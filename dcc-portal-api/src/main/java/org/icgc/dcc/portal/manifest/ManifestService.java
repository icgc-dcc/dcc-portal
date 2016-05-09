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
package org.icgc.dcc.portal.manifest;

import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Ordering.explicit;
import static com.google.common.collect.Ordering.natural;
import static com.sun.jersey.core.header.ContentDisposition.type;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static org.dcc.portal.pql.meta.Type.REPOSITORY_FILE;
import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;
import static org.icgc.dcc.common.core.util.Joiners.DOT;
import static org.icgc.dcc.common.core.util.function.Predicates.distinctByKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.stream.Stream;

import org.elasticsearch.action.search.SearchResponse;
import org.icgc.dcc.portal.config.PortalProperties;
import org.icgc.dcc.portal.manifest.model.Manifest;
import org.icgc.dcc.portal.manifest.model.ManifestField;
import org.icgc.dcc.portal.manifest.model.ManifestFile;
import org.icgc.dcc.portal.manifest.writer.GNOSManifestWriter;
import org.icgc.dcc.portal.manifest.writer.GenericManifestWriter;
import org.icgc.dcc.portal.manifest.writer.ICGCManifestWriter;
import org.icgc.dcc.portal.model.Query;
import org.icgc.dcc.portal.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.repository.ManifestRepository;
import org.icgc.dcc.portal.repository.RepositoryFileRepository;
import org.icgc.dcc.portal.repository.TermsLookupRepository;
import org.icgc.dcc.portal.service.NotFoundException;
import org.icgc.dcc.portal.util.MultiPartOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.sun.jersey.multipart.file.DefaultMediaTypePredictor;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
   * Configuration.
   */
  @NonNull
  private final PortalProperties properties;

  /**
   * Dependencies.
   */
  @NonNull
  private final ManifestRepository manifestRepository;
  @NonNull
  private final RepositoryFileRepository repositoryFileRepository;
  @NonNull
  private final TermsLookupRepository termsLookupRepository;

  @NonNull
  public Manifest getManifest(@NonNull UUID manifestId) {
    val manifest = manifestRepository.find(manifestId);
    if (manifest == null) {
      throw new NotFoundException(manifestId.toString(), "manifest");
    }

    return manifest;
  }

  public void saveManifest(@NonNull Manifest manifest) {
    val dataVersion = properties.getRelease().getDataVersion();

    val manifestId = createManifestId();
    manifest.setId(manifestId);
    manifest.setVersion(dataVersion);

    manifestRepository.save(manifest, dataVersion);
  }

  public void generateManifests(@NonNull ManifestContext context) throws IOException {
    // Get requested file copies
    val watch = Stopwatch.createStarted();

    log.info("Finding manifest files...");
    val searchResult = findFiles(context.getQuery());
    log.info("Read manifest files in {}", watch);

    try {
      switch (context.getManifest().getFormat()) {
      case TARBALL:
        generateManifestArchive(searchResult, context);
        break;
      case FILES:
        generateManifestFiles(searchResult, context);
        break;
      case JSON:
        generateManifestJSON(searchResult, context);
        break;
      }
    } catch (Exception e) {
      log.error("Error generating manifests: ", e);
      throw e;
    }

    log.info("Finsished creating manifest in {}", watch);
  }

  public void generateManifestArchive(SearchResponse searchResult, ManifestContext context) throws IOException {
    @Cleanup
    val archive = new ManifestArchive(context.getOutput());
    val timestamp = context.getManifest().getTimestamp();

    // Write a manifest for each repository in turn
    log.info("Writing manifest archive...");
    eachRepository(context, searchResult, (repoCode, repoType, bundles) -> {
      ByteArrayOutputStream fileContents = new ByteArrayOutputStream(BUFFER_SIZE);
      writeManifest(repoType, timestamp, bundles, fileContents);

      String fileName = formatFileName(repoCode, repoType, timestamp);
      archive.addManifest(fileName, fileContents);
    });
  }

  private void generateManifestFiles(SearchResponse searchResult, ManifestContext context) throws IOException {
    val timestamp = context.getManifest().getTimestamp();
    if (context.getManifest().isMultipart()) {
      val boundary = "boundary_" + timestamp;
      val output = new MultiPartOutputStream(boundary, context.getOutput());

      eachRepository(context, searchResult, (repoCode1, repoType, bundles) -> {
        String fileName = formatFileName(repoCode1, repoType, timestamp);
        String fileType = DefaultMediaTypePredictor.getInstance().getMediaTypeFromFileName(fileName).toString();
        output.startPart(fileType, new String[] { "ContentDisposition: " + type("attachment")
            .fileName(formatFileName(repoCode1, repoType, timestamp)).build().toString() });
        writeManifest(repoType, timestamp, bundles, output);
      });
    } else {
      val output = context.getOutput();
      eachRepository(context, searchResult, (repoCode1, repoType, bundles) -> {
        writeManifest(repoType, timestamp, bundles, output);
      });
    }
  }

  private void generateManifestJSON(SearchResponse searchResult, ManifestContext context) throws IOException {
    val output = context.getOutput();
    val manifest = context.getManifest();
    val generator = DEFAULT.getFactory().createGenerator(output);
    val fields = manifest.getFields();
    val files = fields.contains(ManifestField.ID) ||
        fields.contains(ManifestField.MD5SUM) ||
        fields.contains(ManifestField.SIZE);

    // This is too big to fit in a {@link Manifest} so we stream instead
    generator.writeStartObject();
    if (manifest.getId() != null) {
      generator.writeStringField("id", manifest.getId().toString());
    }
    generator.writeObjectField("repos", manifest.getRepos());
    generator.writeNumberField("timestamp", manifest.getTimestamp());
    generator.writeNumberField("version", manifest.getVersion());
    generator.writeObjectField("filters", manifest.getFilters());
    generator.writeObjectField("fields", manifest.getFields());
    generator.writeObjectField("format", manifest.getFormat());
    generator.writeBooleanField("unique", manifest.isUnique());
    generator.writeBooleanField("multipart", manifest.isMultipart());
    generator.writeFieldName("entries");

    generator.writeStartArray();
    eachRepository(context, searchResult, (repoCode1, repoType, bundles) -> {
      generator.writeStartObject();
      generator.writeStringField("repo", repoCode1);

      // Files
      if (files) {
        generator.writeArrayFieldStart("files");
        for (ManifestFile file : bundles.values()) {
          generator.writeStartObject();
          if (fields.contains(ManifestField.ID)) {
            generator.writeStringField(ManifestField.ID.getKey(), file.getId());
          }
          if (fields.contains(ManifestField.MD5SUM)) {
            generator.writeStringField(ManifestField.MD5SUM.getKey(), file.getMd5sum());
          }
          if (fields.contains(ManifestField.SIZE)) {
            generator.writeNumberField(ManifestField.SIZE.getKey(), file.getSize());
          }
          generator.writeEndObject();
        }
        generator.writeEndArray();
      }

      // Contents
      if (fields.contains(ManifestField.CONTENT)) {
        ByteArrayOutputStream fileContents = new ByteArrayOutputStream(BUFFER_SIZE);
        writeManifest(repoType, manifest.getTimestamp(), bundles, fileContents);

        generator.writeBinaryField(ManifestField.CONTENT.getKey(), fileContents.toByteArray());
      }
      generator.writeEndObject();
    });
    generator.writeEndArray();
    generator.writeEndObject();

    generator.flush();
  }

  private void eachRepository(ManifestContext context, SearchResponse searchResult, BundlesCallback callback)
      throws IOException {

    // Map and filter
    Stream<ManifestFile> files = ManifestMapper
        .map(searchResult)
        .filter(file -> context.isActive(file.getRepoCode()));

    if (context.getManifest().isUnique()) {
      // Remove duplicates by file id by choosing the one with the higest priority
      files = files
          .sorted(fileIdOrder().thenComparing(priorityFileCopyOrder(context.getManifest().getRepos())))
          .filter(distinctByKey(file -> file.getId()));
    }

    // Group files from each repo together
    val repoCodeFiles = files.collect(groupingBy(file -> file.getRepoCode()));

    // Iterate in order of priority
    for (val repoCode : prioritizeRepoCodes(context.getManifest().getRepos(), repoCodeFiles.keySet())) {
      val repoFiles = repoCodeFiles.get(repoCode);

      // Entries with the same repoCode must have the same repoType
      val repoType = getFirst(repoFiles, null).getRepoType();

      // Index
      val bundles = Multimaps.index(repoFiles, file -> formatFileURL(file));

      // Hand off
      callback.handle(repoCode, repoType, bundles);
    }
  }

  private SearchResponse findFiles(Query query) {
    val converter = Jql2PqlConverter.getInstance();
    val pql = converter.convert(query, REPOSITORY_FILE);
    log.debug("Received JQL: '{}'; converted to PQL: '{}'.", query.getFilters(), pql);

    return repositoryFileRepository.findDownloadInfo(pql);
  }

  private static void writeManifest(String repoType, long timestamp,
      ListMultimap<String, ManifestFile> downloadUrlGroups, OutputStream out) {
    if (isGnos(repoType)) {
      GNOSManifestWriter.write(out, downloadUrlGroups, timestamp);
    } else if (isS3(repoType)) {
      ICGCManifestWriter.write(out, downloadUrlGroups);
    } else {
      // Not sure who consumes this one...
      GenericManifestWriter.write(out, downloadUrlGroups);
    }
  }

  private static Comparator<ManifestFile> fileIdOrder() {
    return comparing(file -> file.getId());
  }

  private static SortedSet<String> prioritizeRepoCodes(List<String> priorities, Set<String> repoCodes) {
    val order = priorityRepoOrder(priorities);
    return ImmutableSortedSet.orderedBy(order).addAll(repoCodes).build();
  }

  private static Comparator<ManifestFile> priorityFileCopyOrder(List<String> priorities) {
    val order = priorityRepoOrder(priorities);
    return comparing(file -> file.getRepoCode(), order);
  }

  private static Ordering<String> priorityRepoOrder(List<String> priorities) {
    val all = priorities.isEmpty();
    return all ? natural() : explicit(priorities);
  }

  private static boolean isGnos(String repoType) {
    return "GNOS".equalsIgnoreCase(repoType);
  }

  private static boolean isS3(String repoType) {
    return "S3".equalsIgnoreCase(repoType);
  }

  private static String formatFileURL(String baseUrl, String dataPath, String id) {
    return Stream.of(baseUrl, dataPath, id)
        .map(part -> part.replaceAll("^/+|/+$", ""))
        .collect(joining("/"));
  }

  private static String formatFileURL(@NonNull ManifestFile file) {
    val repoType = file.getRepoType();
    if (isGnos(repoType)) {
      return formatFileURL(
          file.getRepoBaseUrl(),
          file.getRepoDataPath(),
          file.getDataBundleId());
    } else {
      return formatFileURL(
          file.getRepoBaseUrl(),
          file.getRepoDataPath(),
          file.getName());
    }
  }

  private static String formatFileName(String repoCode, String repoType, long timestamp) {
    return DOT.join(Arrays.asList(
        "manifest",
        repoCode,
        timestamp,
        isGnos(repoType) ? "xml" : "txt"));
  }

  private static UUID createManifestId() {
    // Prevent "browser scanning" by using an opaque id
    return UUID.randomUUID();
  }

  /**
   * Callback for processing a grouped set of files by url (a.k.a a bundle).
   */
  private interface BundlesCallback {

    void handle(String repoCode, String repoType, ListMultimap<String, ManifestFile> bundles)
        throws IOException;

  }

}
