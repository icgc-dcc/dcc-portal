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
import java.util.List;

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

  /**
   * Configuration.
   */
  private String mavenRepositoryUrl = "https://artifacts.oicr.on.ca/artifactory";

  @SneakyThrows
  public List<ArtifactFolder> getVersions(String artifactId, SoftwareProperties config) {
    String versionsUrl;
    versionsUrl = mavenRepositoryUrl + "/api/storage/" + config.getIcgcRepository() + "/" +
        artifactId;
    val url = new URL(versionsUrl);
    val response = MAPPER.readValue(url, ArtifactFolderResults.class);
    return response.children;
  }

  @SneakyThrows
  public List<MavenArtifactVersion> getMavenVersions(String artifactId, SoftwareProperties config) {
    val versionsUrl = mavenRepositoryUrl + "/api/search/versions?g=" + config.getGroupId() + "&a=" +
        artifactId + "&repos=" + config.getArtifactId();
    val url = new URL(versionsUrl);
    val response = MAPPER.readValue(url, MavenArtifactResults.class);
    return response.results;
  }

  public String getLatestVersionUrl(SoftwareProperties config) {
    return getVersionUrl("%5BRELEASE%5D", config.getArtifactId(), config);
  }

  public String getLatestICGCGetVersionUrl(String os, SoftwareProperties config) {
    List<ArtifactFolder> result = getVersions("icgc-get", config);

    return getICGCGetVersionUrl("0.2.10", os, config);
  }

  public String getVersionChecksumUrl(String version, SoftwareProperties config) {
    return getVersionUrl(version, config.getArtifactId(), config) + ".md5";
  }

  public String getVersionSignatureUrl(String version, SoftwareProperties config) {
    return getVersionUrl(version, config.getArtifactId(), config) + ".asc";
  }

  public String getICGCGetVersionUrl(String version, String os, SoftwareProperties config) {
    String classifier;
    if (os.equals("linux")) {
      classifier = config.getLinuxClassifier();
    } else {
      classifier = config.getOsxClassifier();
    }
    return getVersionUrl(version, config.getIcgcArtifactId(), config) + classifier + ".zip";
  }

  public String getVersionUrl(String version, String artifactId, SoftwareProperties config) {
    if (artifactId.equals("icgc-get")) {
      return getRepositoryUrl(artifactId, config) + "/" + artifactId + "/" + version + "/" + artifactId + "_v" + version
          + "_";
    }
    val classifier = config.getClassifier();
    return getGroupUrl(config) + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + "-"
        + classifier + ".tar.gz";
  }

  private String getGroupUrl(SoftwareProperties config) {
    val groupId = config.getGroupId();
    return getRepositoryUrl("icgc-storage-client", config) + "/" + groupId.replaceAll("\\.", "/");
  }

  private String getRepositoryUrl(String artifactId, SoftwareProperties config) {
    String repository;
    if (artifactId.equals("icgc-get")) {
      repository = config.getIcgcRepository();
    } else {
      repository = config.getRepository();
    }
    return mavenRepositoryUrl + "/simple/" + repository;
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

  public int compareVersion(String[] arg0, String[] arg1) {
    int length = arg0.length;
    if (arg1.length > arg0.length) length = arg1.length;

    for (int i = 0; i < length; i++) {
      String s0 = null;
      if (i < arg0.length) s0 = arg0[i];
      Integer i0 = (s0 == null) ? 0 : Integer.parseInt(s0);
      String s1 = null;
      if (i < arg1.length) s1 = arg1[i];
      Integer i1 = (s1 == null) ? 0 : Integer.parseInt(s1);
      if (i0.compareTo(i1) < 0) return -1;
      else if (i1.compareTo(i0) < 0) return 1;
    }
    return 0;
  };
}
