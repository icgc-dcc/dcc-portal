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
public class StorageClientService {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Map<String, String> PARAMS = ImmutableMap.of(
      "groupId", "org.icgc.dcc",
      "artifactId", "icgc-storage-client",
      "classifier", "dist",
      "repository", "dcc-release");

  /**
   * Configuration.
   */
  private String mavenRepositoryUrl = "http://seqwaremaven.oicr.on.ca/artifactory";

  @SneakyThrows
  public List<MavenArtifactVersion> getVersions() {
    val versionsUrl = mavenRepositoryUrl + "/api/search/versions?g=" +
        PARAMS.get("groupId") + "&a=" +
        PARAMS.get("artifactId") + "&repos=" + PARAMS.get("repository");

    val url = new URL(versionsUrl);
    val response = MAPPER.readValue(url, MavenArtifactResults.class);
    return response.results;
  }

  public String getLatestVersionUrl() {
    return getVersionUrl("%5BRELEASE%5D");
  }

  public String getVersionChecksumUrl(String version) {
    return getVersionUrl(version) + ".md5";
  }

  public String getVersionSignatureUrl(String version) {
    return getVersionUrl(version) + ".asc";
  }

  public String getVersionUrl(String version) {
    val artifactId = PARAMS.get("artifactId");
    val classifier = PARAMS.get("classifier");
    return getGroupUrl() + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + "-"
        + classifier + ".tar.gz";
  }

  private String getGroupUrl() {
    val groupId = PARAMS.get("groupId");
    return getRepositoryUrl() + "/" + groupId.replaceAll("\\.", "/");
  }

  private String getRepositoryUrl() {
    val repository = PARAMS.get("repository");
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

}