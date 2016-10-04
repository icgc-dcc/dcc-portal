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
package org.icgc.dcc.portal.server.service;

import static com.google.common.base.Preconditions.checkState;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.icgc.dcc.portal.server.config.ServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SoftwareService {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Configuration.
   */
  @NonNull
  private final ServerProperties properties;

  @SneakyThrows
  public List<Version> getIcgcGetVersions() {
    String versionsUrl;
    versionsUrl = properties.getSoftware().getMavenRepositoryUrl() + "/api/storage/"
        + properties.getSoftware().getIcgcRepository() + "/" + properties.getSoftware().getIcgcGroupId();
    val url = new URL(versionsUrl);
    val response = MAPPER.readValue(url, ArtifactFolderResults.class);
    for (Iterator<ArtifactFolder> i = response.children.iterator(); i.hasNext();) {
      ArtifactFolder child = i.next();
      if (!child.folder) {
        i.remove();
      }
    }
    val uris = response.children.stream().map(ArtifactFolder::getUri);
    val versions = uris.map(Version::new);
    val sortedVersions = versions.sorted().collect(Collectors.toList());
    return sortedVersions;
  }

  @SneakyThrows
  public List<MavenArtifactVersion> getMavenVersions() {
    val versionsUrl = properties.getSoftware().getMavenRepositoryUrl() + "/api/search/versions?g="
        + properties.getSoftware().getGroupId() + "&a=" +
        properties.getSoftware().getArtifactId() + "&repos=" + properties.getSoftware().getRepository();
    val url = new URL(versionsUrl);
    val response = MAPPER.readValue(url, MavenArtifactResults.class);
    return response.results;
  }

  public String getLatestVersionUrl() {
    return getIcgcStorageClientVersionUrl("%5BRELEASE%5D");
  }

  public String getLatestIcgcGetVersionUrl(String os) {
    val result = getIcgcGetVersions();
    val latestVersion = result.stream().findFirst().orElse(null);
    return getIcgcGetVersionUrl(latestVersion.version, os);
  }

  public String getVersionChecksumUrl(String version) {
    return getIcgcStorageClientVersionUrl(version) + ".md5";
  }

  public String getVersionSignatureUrl(String version) {
    return getIcgcStorageClientVersionUrl(version) + ".asc";
  }

  public String getIcgcGetVersionUrl(String version, String os) {
    String classifier;
    if (os.equals("linux")) {
      classifier = properties.getSoftware().getLinuxClassifier();
    } else {
      classifier = properties.getSoftware().getOsxClassifier();
    }
    return getRepositoryUrl(properties.getSoftware().getIcgcArtifactId()) + "/"
        + properties.getSoftware().getIcgcGroupId() + "/" + version + "/"
        + properties.getSoftware().getIcgcArtifactId() + "_v" + version
        + "_" + classifier + ".zip";
  }

  public String getIcgcStorageClientVersionUrl(String version) {
    val classifier = properties.getSoftware().getClassifier();
    return getGroupUrl() + "/" + properties.getSoftware().getArtifactId() + "/" + version + "/"
        + properties.getSoftware().getArtifactId() + "-" + version
        + "-"
        + classifier + ".tar.gz";
  }

  private String getGroupUrl() {
    val groupId = properties.getSoftware().getGroupId();
    return getRepositoryUrl(properties.getSoftware().getArtifactId()) + "/" + groupId.replaceAll("\\.", "/");
  }

  private String getRepositoryUrl(String artifactId) {
    String repository;
    if (artifactId.equals(properties.getSoftware().getIcgcArtifactId())) {
      repository = properties.getSoftware().getIcgcRepository();
    } else {
      repository = properties.getSoftware().getRepository();
    }
    return properties.getSoftware().getMavenRepositoryUrl() + "/simple/" + repository;
  }

  @Data
  public static class MavenArtifactResults {

    List<MavenArtifactVersion> results;

  }

  @Data
  public static class MavenArtifactVersion {

    String version;
    boolean integration;

  }

  @Data
  public static class ArtifactFolderResults {

    String repo;
    String Path;
    String created;
    String createdBy;
    String lastModified;
    String modifiedBy;
    String lastUpdated;
    List<ArtifactFolder> children;
    String uri;

  }

  @Data
  public static class ArtifactFolder {

    String uri;
    boolean folder;

  }

  @EqualsAndHashCode
  public class Version implements Comparable<Version> {

    @Getter
    private String version;

    public Version(String version) {
      checkState(version != null);
      this.version = version.substring(1);
    }

    @Override
    public int compareTo(Version that) {
      if (that == null) return 1;
      String[] thisParts = this.version.split("\\.");
      String[] thatParts = that.version.split("\\.");
      int length = Math.max(thisParts.length, thatParts.length);
      for (int i = 0; i < length; i++) {
        int thisPart = i < thisParts.length ? Integer.parseInt(thisParts[i]) : 0;
        int thatPart = i < thatParts.length ? Integer.parseInt(thatParts[i]) : 0;
        if (thisPart < thatPart) return 1;
        if (thisPart > thatPart) return -1;
      }

      return 0;
    }

  }

}
