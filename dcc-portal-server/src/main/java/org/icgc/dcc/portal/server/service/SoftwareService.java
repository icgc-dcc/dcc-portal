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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.icgc.dcc.portal.server.config.ServerProperties.SoftwareProperties;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.val;

@Service
public class SoftwareService {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final SoftwareProperties config = new SoftwareProperties();

  /**
   * Configuration.
   */

  @SneakyThrows
  public List<ArtifactFolder> getVersions(String artifactId) {
    String versionsUrl;
    versionsUrl = config.getMavenRepositoryUrl() + "/api/storage/" + config.getIcgcRepository() + "/" +
        artifactId;
    val url = new URL(versionsUrl);
    val response = MAPPER.readValue(url, ArtifactFolderResults.class);
    return response.children;
  }

  @SneakyThrows
  public List<MavenArtifactVersion> getMavenVersions(String artifactId) {
    val versionsUrl = config.getMavenRepositoryUrl() + "/api/search/versions?g=" + config.getGroupId() + "&a=" +
        artifactId + "&repos=" + config.getRepository();
    val url = new URL(versionsUrl);
    val response = MAPPER.readValue(url, MavenArtifactResults.class);
    return response.results;
  }

  public String getLatestVersionUrl() {
    return getVersionUrl("%5BRELEASE%5D", config.getArtifactId());
  }

  public String getLatestICGCGetVersionUrl(String os) {
    List<ArtifactFolder> result = getVersions(config.getIcgcArtifactId());
    List<String> versions = result.stream().map(ArtifactFolder::getUri).collect(Collectors.toList());
    List<Version> versionObjects = makeVersions(versions);
    Version version = versionObjects.stream().sorted().findFirst().orElse(null);
    return getICGCGetVersionUrl(version.get(), os);
  }

  public String getVersionChecksumUrl(String version) {
    return getVersionUrl(version, config.getArtifactId()) + ".md5";
  }

  public String getVersionSignatureUrl(String version) {
    return getVersionUrl(version, config.getArtifactId()) + ".asc";
  }

  public String getICGCGetVersionUrl(String version, String os) {
    String classifier;
    if (os.equals("linux")) {
      classifier = config.getLinuxClassifier();
    } else {
      classifier = config.getOsxClassifier();
    }
    return getVersionUrl(version, config.getIcgcArtifactId()) + classifier + ".zip";
  }

  public String getVersionUrl(String version, String artifactId) {
    if (artifactId.equals(config.getIcgcArtifactId())) {
      return getRepositoryUrl(artifactId) + "/" + artifactId + "/" + version + "/" + artifactId + "_v" + version
          + "_";
    }
    val classifier = config.getClassifier();
    return getGroupUrl() + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + "-"
        + classifier + ".tar.gz";
  }

  private String getGroupUrl() {
    val groupId = config.getGroupId();
    return getRepositoryUrl(config.getArtifactId()) + "/" + groupId.replaceAll("\\.", "/");
  }

  private String getRepositoryUrl(String artifactId) {
    String repository;
    if (artifactId.equals(config.getIcgcArtifactId())) {
      repository = config.getIcgcRepository();
    } else {
      repository = config.getRepository();
    }
    return config.getMavenRepositoryUrl() + "/simple/" + repository;
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

  public String[] parseVersion(String uri) {
    String[] letters = uri.split("");
    return letters;
  }

  public List<Version> makeVersions(List<String> strings) {
    List<Version> results = new ArrayList<Version>();
    for (String uri : strings) {
      results.add(new Version(uri));
    }
    return results;
  }

  public class Version implements Comparable<Version> {

    private String version;

    public final String get() {
      return this.version;
    }

    public Version(String version) {
      if (version == null) throw new IllegalArgumentException("Version can not be null");
      if (!version.matches("/[0-9]+(\\.[0-9]+)*")) throw new IllegalArgumentException("Invalid version format");
      this.version = version.substring(1);
    }

    @Override
    public int compareTo(Version that) {
      if (that == null) return 1;
      String[] thisParts = this.get().split("\\.");
      String[] thatParts = that.get().split("\\.");
      int length = Math.max(thisParts.length, thatParts.length);
      for (int i = 0; i < length; i++) {
        int thisPart = i < thisParts.length ? Integer.parseInt(thisParts[i]) : 0;
        int thatPart = i < thatParts.length ? Integer.parseInt(thatParts[i]) : 0;
        if (thisPart < thatPart) return 1;
        if (thisPart > thatPart) return -1;
      }
      return 0;
    }

    @Override
    public boolean equals(Object that) {
      if (this == that) return true;
      if (that == null) return false;
      if (this.getClass() != that.getClass()) return false;
      return this.compareTo((Version) that) == 0;
    }

  }

}
