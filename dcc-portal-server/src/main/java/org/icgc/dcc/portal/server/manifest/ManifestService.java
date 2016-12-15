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
package org.icgc.dcc.portal.server.manifest;

import static com.google.common.collect.Ordering.explicit;
import static com.google.common.collect.Ordering.natural;
import static com.sun.jersey.core.header.ContentDisposition.type;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static org.dcc.portal.pql.meta.Type.FILE;
import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;
import static org.icgc.dcc.common.core.util.Formats.formatCount;
import static org.icgc.dcc.common.core.util.Joiners.COMMA;
import static org.icgc.dcc.common.core.util.Joiners.DOT;
import static org.icgc.dcc.common.core.util.function.Predicates.distinctByKey;
import static org.icgc.dcc.portal.server.model.EntitySetDefinition.SortOrder.DESCENDING;
import static org.icgc.dcc.portal.server.util.SearchResponses.getTotalHitCount;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.stream.Stream;

import org.elasticsearch.action.search.SearchResponse;
import org.icgc.dcc.portal.server.config.ServerProperties;
import org.icgc.dcc.portal.server.manifest.model.Manifest;
import org.icgc.dcc.portal.server.manifest.model.ManifestField;
import org.icgc.dcc.portal.server.manifest.model.ManifestFile;
import org.icgc.dcc.portal.server.manifest.model.ManifestFormat;
import org.icgc.dcc.portal.server.manifest.writer.EGAManifestWriter;
import org.icgc.dcc.portal.server.manifest.writer.GDCManifestWriter;
import org.icgc.dcc.portal.server.manifest.writer.GNOSManifestWriter;
import org.icgc.dcc.portal.server.manifest.writer.GenericManifestWriter;
import org.icgc.dcc.portal.server.manifest.writer.ICGCManifestWriter;
import org.icgc.dcc.portal.server.manifest.writer.PDCManifestWriter;
import org.icgc.dcc.portal.server.model.BaseEntitySet.Type;
import org.icgc.dcc.portal.server.model.EntitySetDefinition;
import org.icgc.dcc.portal.server.model.Query;
import org.icgc.dcc.portal.server.model.Repository;
import org.icgc.dcc.portal.server.pql.convert.Jql2PqlConverter;
import org.icgc.dcc.portal.server.repository.FileRepository;
import org.icgc.dcc.portal.server.repository.ManifestRepository;
import org.icgc.dcc.portal.server.repository.RepositoryRepository;
import org.icgc.dcc.portal.server.service.EntitySetService;
import org.icgc.dcc.portal.server.service.NotFoundException;
import org.icgc.dcc.portal.server.util.MultiPartOutputStream;
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
  private static final String FILE_NAME_PREFIX = "manifest";
  private static final String MANIFEST_SET_NAME = "Saved manifest";

  /**
   * Configuration.
   */
  @NonNull
  private final ServerProperties properties;

  /**
   * Dependencies.
   */
  @NonNull
  private final RepositoryRepository repositories;
  @NonNull
  private final ManifestRepository manifestRepository;
  @NonNull
  private final FileRepository fileRepository;
  @NonNull
  private final EntitySetService entitySetService;

  public String getFileName(@NonNull Manifest manifest) {
    val repoCode = manifest.getRepos().size() == 1 ? manifest.getRepos().get(0) : null;

    val timestamp = manifest.getTimestamp();
    if (manifest.getFormat() == ManifestFormat.TARBALL) {
      // Archive
      return FILE_NAME_PREFIX + "." + timestamp + ".tar.gz";
    } else if (repoCode != null) {
      // Single repo
      val repo = repositories.findOne(repoCode);
      return formatFileName(repo, timestamp);
    } else {
      // Concatenated manifest
      return FILE_NAME_PREFIX + ".concatenated." + timestamp + ".txt";
    }
  }

  @NonNull
  public Manifest getManifest(@NonNull UUID manifestId) {
    val manifest = manifestRepository.find(manifestId);
    if (manifest == null) {
      throw new NotFoundException(manifestId.toString(), "manifest");
    }

    return manifest;
  }

  @SneakyThrows
  public void saveManifest(@NonNull Manifest manifest) {
    val dataVersion = properties.getRelease().getDataVersion();

    val entitySetDefinition =
        new EntitySetDefinition(
            manifest.getFilters(),
            "id",
            DESCENDING,
            MANIFEST_SET_NAME,
            COMMA.join(manifest.getRepos()),
            Type.FILE,
            200000,
            false);

    val entitySet = entitySetService.createFileEntitySet(entitySetDefinition);

    manifest.setId(entitySet.getId());
    manifest.setVersion(dataVersion);

    manifestRepository.save(manifest, dataVersion);
  }

  public void generateManifests(@NonNull ManifestContext context) throws IOException {
    val watch = Stopwatch.createStarted();

    // Get requested file copies
    log.info("Finding files to include in manifest using query: {} ...", context.getQuery());
    val searchResponse = findFiles(context.getQuery());
    log.info("Found {} manifest files in {}", formatCount(getTotalHitCount(searchResponse)), watch);

    try {
      switch (context.getManifest().getFormat()) {
      case TARBALL:
        generateManifestArchive(searchResponse, context);
        break;
      case FILES:
        generateManifestFiles(searchResponse, context);
        break;
      case JSON:
        generateManifestJSON(searchResponse, context);
        break;
      }
    } catch (Exception e) {
      log.error("Error generating manifests: ", e);
      throw e;
    }

    log.info("Finished creating manifest in {}", watch);
  }

  public void generateManifestArchive(SearchResponse searchResponse, ManifestContext context) throws IOException {
    @Cleanup
    val archive = new ManifestArchive(context.getOutput());
    val timestamp = context.getManifest().getTimestamp();

    // Write a manifest for each repository in turn
    log.info("Writing manifest archive...");
    eachRepository(context, searchResponse, (repo, bundles) -> {
      ByteArrayOutputStream fileContents = new ByteArrayOutputStream(BUFFER_SIZE);
      writeManifest(repo, timestamp, bundles, fileContents);

      String fileName = formatFileName(repo, timestamp);
      archive.addManifest(fileName, fileContents);
    });
  }

  private void generateManifestFiles(SearchResponse searchResponse, ManifestContext context) throws IOException {
    val timestamp = context.getManifest().getTimestamp();
    if (context.getManifest().isMultipart()) {
      val boundary = "boundary_" + timestamp;
      @Cleanup
      val output = new MultiPartOutputStream(boundary, context.getOutput());

      eachRepository(context, searchResponse, (repo, bundles) -> {
        String fileName = formatFileName(repo, timestamp);
        String fileType = DefaultMediaTypePredictor.getInstance().getMediaTypeFromFileName(fileName).toString();
        output.startPart(fileType, new String[] { "ContentDisposition: " +
            type("attachment").fileName(fileName).build().toString() });
        writeManifest(repo, timestamp, bundles, output);
      });
    } else {
      val output = context.getOutput();
      eachRepository(context, searchResponse, (repo, bundles) -> {
        writeManifest(repo, timestamp, bundles, output);
      });
    }
  }

  private void generateManifestJSON(SearchResponse searchResponse, ManifestContext context) throws IOException {
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
    eachRepository(context, searchResponse, (repo, bundles) -> {
      generator.writeStartObject();
      generator.writeStringField("repo", repo.getCode());

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
          if (fields.contains(ManifestField.REPOFILEID)) {
            generator.writeStringField(ManifestField.REPOFILEID.getKey(), file.getRepoFileId());
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
        writeManifest(repo, manifest.getTimestamp(), bundles, fileContents);

        generator.writeBinaryField(ManifestField.CONTENT.getKey(), fileContents.toByteArray());
      }
      generator.writeEndObject();
    });
    generator.writeEndArray();
    generator.writeEndObject();

    generator.flush();
  }

  private void eachRepository(ManifestContext context, SearchResponse searchResponse, BundlesCallback callback)
      throws IOException {
    // Map and filter
    Stream<ManifestFile> files = new ManifestMapper(repositories.findAll())
        .map(searchResponse)
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
      val repo = repositories.findOne(repoCode);
      val repoFiles = repoCodeFiles.get(repoCode);

      // Index
      val bundles = Multimaps.index(repoFiles, file -> formatFileURL(repo, file));

      // Hand off
      callback.handle(repo, bundles);
    }
  }

  private SearchResponse findFiles(Query query) {
    val converter = Jql2PqlConverter.getInstance();
    val pql = converter.convert(query, FILE);
    log.debug("Received JQL: '{}'; converted to PQL: '{}'.", query.getFilters(), pql);

    return fileRepository.findFileInfoPQL(pql);
  }

  private static void writeManifest(Repository repo, long timestamp,
      ListMultimap<String, ManifestFile> downloadUrlGroups, OutputStream out) {
    if (repo.isGNOS()) {
      GNOSManifestWriter.write(out, downloadUrlGroups, timestamp);
    } else if (repo.isS3()) {
      ICGCManifestWriter.write(out, downloadUrlGroups);
    } else if (repo.isGDC()) {
      GDCManifestWriter.write(out, downloadUrlGroups);
    } else if (repo.isPDC()) {
      PDCManifestWriter.write(out, downloadUrlGroups);
    } else if (repo.isEGA()) {
      EGAManifestWriter.write(out, downloadUrlGroups);
    } else {
      // e.g TCGA
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

  private static String formatFileName(Repository repo, long timestamp) {
    val ext = repo.isGNOS() ? "xml" : repo.isEGA() || repo.isPDC() ? "sh" : "tsv";
    return DOT.join(FILE_NAME_PREFIX, repo.getCode(), timestamp, ext);
  }

  private static String formatFileURL(String... parts) {
    return Stream.of(parts)
        .map(part -> part.replaceAll("^/+|/+$", ""))
        .collect(joining("/"));
  }

  private static String formatFileURL(@NonNull Repository repo, @NonNull ManifestFile file) {
    if (repo.isGNOS()) {
      return formatFileURL(
          file.getRepoBaseUrl(),
          file.getRepoDataPath(),
          file.getDataBundleId());
    } else if (repo.isGDC()) {
      return file.getRepoFileId();
    } else if (repo.isPDC()) {
      return formatFileURL(
          file.getRepoBaseUrl(),
          file.getRepoDataPath());
    } else {
      return formatFileURL(
          file.getRepoBaseUrl(),
          file.getRepoDataPath(),
          file.getName());
    }
  }

  /**
   * Callback for processing a grouped set of files by url (a.k.a a bundle).
   */
  private interface BundlesCallback {

    void handle(Repository repo, ListMultimap<String, ManifestFile> bundles)
        throws IOException;

  }

}
