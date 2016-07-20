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
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.val;

@Service
public class SoftwareService {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Map<String, String> PARAMS = ImmutableMap.of(
      "groupId", "org.icgc.dcc",
      "classifier", "dist",
      "repository", "dcc-release",
      );
  private static final Map<String, String> ICGCPARAMS = ImmutableMap.of(
		  "osxClassifier", "osx_x64",
		  "linuxClassifier", "linux_x64",
		  "repository", "dcc-binaries");

  /**
   * Configuration.
   */
  private String mavenRepositoryUrl = "https://artifacts.oicr.on.ca/artifactory";

  @SneakyThrows
  public List<ArtifactFolder> getVersions(String artifactId) {
    String versionsUrl;
    versionsUrl = mavenRepositoryUrl + "/api/storage/" + ICGCPARAMS.get("repository") + "/" +
        artifactId;
    val url = new URL(versionsUrl);
    val response = MAPPER.readValue(url, ArtifactFolderResults.class);
    return response.children;
  }

  @SneakyThrows
  public List<MavenArtifactVersion> getMavenVersions(String artifactId) {
    val versionsUrl = mavenRepositoryUrl + "/api/search/versions?g=" + PARAMS.get("groupId") + "&a=" +
        artifactId + "&repos=" + PARAMS.get("repository");
    val url = new URL(versionsUrl);
    val response = MAPPER.readValue(url, MavenArtifactResults.class);
    return response.results;
  }

  public String getLatestVersionUrl(String artifactId) {
    if (artifactId.equals("icgc-get")) {
      List<ArtifactFolder> Versions = getVersions(artifactId);
      return getVersionUrl("0.2.10", artifactId);
    }
    return getVersionUrl("%5BRELEASE%5D", artifactId);
  }

  public String getVersionChecksumUrl(String version, String artifactId) {
    return getVersionUrl(version, artifactId) + ".md5";
  }

  public String getVersionSignatureUrl(String version, String artifactId) {
    return getVersionUrl(version, artifactId) + ".asc";
  }
  
  public String getLinuxVersionURL(String version, String artifactId){
  	return getVersionUrl(version, artifactId) + ICGCPARAMS.get("linuxClassifier") + ".zip"
  }
  
  public String getMacVersionURL(String version, String artifactId){
	  	return getVersionUrl(version, artifactId) + ICGCPARAMS.get("osxClassifier") + ".zip"
  }
  
  public String getVersionUrl(String version, String artifactId) {
    if (artifactId.equals("icgc-get")) {
         return getRepositoryUrl(artifactId) + "/" + artifactId + "/" + version + "/" + artifactId + "_v" + version + "_";
    }
    val classifier = PARAMS.get("classifier");
    return getGroupUrl() + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + "-"
        + classifier + ".tar.gz";
  }

  private String getGroupUrl() {
    val groupId = PARAMS.get("groupId");
    return getRepositoryUrl("icgc-storage-client") + "/" + groupId.replaceAll("\\.", "/");
  }

  private String getRepositoryUrl(String artifactId) {
    String repository;
    if (artifactId.equals("icgc-get")) {
      repository = PARAMS.get("ICGCRepository");
    } else {
      repository = PARAMS.get("repository");
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

}
