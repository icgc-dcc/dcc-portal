/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.server.config;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.google.common.collect.ImmutableMap;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ServerProperties {

  @Valid
  @JsonProperty
  CrowdProperties crowd = new CrowdProperties();

  @Valid
  @JsonProperty
  ElasticSearchProperties elastic = new ElasticSearchProperties();

  @Valid
  @JsonProperty
  MailProperties mail = new MailProperties();

  @Valid
  @JsonProperty
  DownloadProperties download = new DownloadProperties();

  @Valid
  @JsonProperty
  ICGCProperties icgc = new ICGCProperties();

  @Valid
  @JsonProperty
  HazelcastProperties hazelcast = new HazelcastProperties();

  @Valid
  @JsonProperty
  CacheProperties cache = new CacheProperties();

  @Valid
  @JsonProperty
  WebProperties web = new WebProperties();

  @Valid
  @JsonProperty
  ReleaseProperties release = new ReleaseProperties();

  @Valid
  @JsonProperty
  SetOperationProperties setOperation = new SetOperationProperties();

  @Valid
  @JsonProperty
  OAuthProperties oauth = new OAuthProperties();

  @Valid
  @JsonProperty
  AuthProperties auth = new AuthProperties();

  @Valid
  @JsonProperty
  SoftwareProperties software = new SoftwareProperties();

  @Valid
  @JsonProperty
  MirrorProperties mirror = new MirrorProperties();

  @Valid
  @JsonProperty
  Map<String, Boolean> features = newHashMap();

  @Valid
  @JsonProperty
  JupyterProperties jupyter = new JupyterProperties();

  @Valid
  @JsonProperty
  BannerProperties banner = new BannerProperties();

  @Data
  public static class CacheProperties {

    @JsonProperty
    boolean enableLastModified;

    @JsonProperty
    List<String> excludeLastModified = newArrayList();

    @JsonProperty
    boolean enableETag;

    @JsonProperty
    List<String> excludeETag = newArrayList();

  }

  @Data
  public static class CrowdProperties {

    /**
     * The cookie name generated by the ICGC SSO authenticator.
     */
    public static final String CUD_TOKEN_NAME = "crowd.token_key";

    @JsonProperty
    String egoUrl;

    @JsonProperty
    String egoClientId;
  }

  @Data
  public static class DownloadProperties {

    @JsonProperty
    boolean enabled = true;

    @JsonProperty
    String serverUrl = "";

    @JsonProperty
    String publicServerUrl = "";

    @JsonProperty
    String user = "";

    @JsonProperty
    String password = "";

    @JsonProperty
    boolean strictSSLCertificates = true;

    @JsonProperty
    boolean requestLoggingEnabled;

    @JsonProperty
    String sharedSecret;

    @JsonProperty
    String aesKey;

    @JsonProperty
    int ttlHours = 1;

  }

  @Data
  public static class ElasticSearchProperties {

    public static final String SNIFF_MODE_KEY = "client.transport.sniff";

    @JsonProperty
    String indexName = "dcc-release-release5";

    @JsonProperty
    String repoIndexName = "icgc-repository";

    @Valid
    @JsonProperty
    List<ElasticSearchNodeAddress> nodeAddresses = newArrayList();

    @JsonProperty
    Map<String, String> client = newHashMap();

    @Data
    public static class ElasticSearchNodeAddress {

      @JsonProperty
      String host = "localhost";

      @Min(value = 1, message = "Must be greater than or equal to {value} but was '${validatedValue}'")
      @Max(value = 65535, message = "'Must be less than than or equal to {value} but was '${validatedValue}'")
      @JsonProperty
      int port = 9300;

    }

  }

  @Data
  public static class HazelcastProperties {

    @JsonProperty
    boolean enabled = false;

    @JsonProperty
    String groupName;

    @JsonProperty
    String groupPassword;

    @JsonProperty
    int usersCacheTTL;

    @JsonProperty
    int openidAuthTTL;

    @JsonProperty
    boolean multicast = false;

    @JsonProperty
    List<String> hosts = newArrayList();

  }

  @Data
  public static class ICGCProperties {

    @JsonProperty
    String cgpUrl;

    @JsonProperty
    String shortUrl;

    @JsonProperty
    String cudUrl;

    @JsonProperty
    String cmsUrl;

    @JsonProperty
    String cudAppId;

    @JsonProperty
    String cudUser;

    @JsonProperty
    String cudPassword;

    @JsonProperty
    String consumerKey;

    @JsonProperty
    String consumerSecret;

    @JsonProperty
    String accessToken;

    @JsonProperty
    String accessSecret;

    @JsonProperty
    Boolean enableHttpLogging;

    @JsonProperty
    Boolean enableStrictSSL;

  }

  @Data
  public static class MailProperties {

    boolean enabled = false;

    String smtpServer = "";
    int smtpPort = 25;

    @Email(message = "Must be a valid email address but was '${validatedValue}")
    String senderEmail = "no-reply@oicr.on.ca";

    String senderName = "DCC Portal";

    String recipientEmail = "";
  }

  @Data
  public static class ReleaseProperties {

    @JsonProperty
    @NotEmpty
    String releaseDate;

    @JsonProperty
    @Min(value = 1, message = "Must be greater than or equal to {value} but was '${validatedValue}'")
    int dataVersion;
  }

  @Data
  public static class SetOperationProperties {

    @JsonProperty
    int maxPreviewNumberOfHits;
    @JsonProperty
    int maxNumberOfHits;
    @JsonProperty
    int maxMultiplier;
  }

  @Data
  public static class WebProperties {

    /**
     * The base URL of the application. Used to verify short URLs are using the appropriate domain and protocol.
     */
    @JsonProperty
    @URL(message = "Must be a valid URL but was '${validatedValue}'")
    @NotEmpty
    String baseUrl;

    @JsonProperty
    String gaAccount;

  }

  @Data
  public static class OAuthProperties {

    @URL(message = "Must be a valid URL but was '${validatedValue}'")
    @NotEmpty
    @JsonProperty
    String serviceUrl;

    @NotEmpty
    @JsonProperty
    String clientId;

    @JsonProperty
    String clientSecret;

    @JsonProperty
    boolean enableStrictSSL;

    @JsonProperty
    boolean enableHttpLogging;

  }

  @Data
  public static class AuthProperties {

    /**
     * The cookie name for the session token generated by the portal-api
     */
    public static final String SESSION_TOKEN_NAME = "dcc_portal_token";

    /**
     * Whether to allow logins or not.
     */
    @JsonProperty
    boolean enabled = true;

  }

  @Data
  public static class MirrorProperties {

    /**
     * Whether to enable mirror features.
     */
    @JsonProperty
    boolean enabled;

    @JsonProperty
    String countryCode;

    @JsonProperty
    String name;

    @JsonProperty
    String countryLocation;

  }

  @Data
  public static class SoftwareProperties {

    @JsonProperty
    String groupId = "org.icgc.dcc";

    @JsonProperty
    String artifactId = "icgc-storage-client";

    @JsonProperty
    String classifier = "dist";

    @JsonProperty
    String repository = "dcc-release";

    @JsonProperty
    String icgcRepository = "dcc-binaries";

    @JsonProperty
    String icgcGroupId = "org/icgc";

    @JsonProperty
    String icgcArtifactId = "icgc-get";

    @JsonProperty
    String osxClassifier = "osx_x64";

    @JsonProperty
    String linuxClassifier = "linux_x64";

    @JsonProperty
    String mavenRepositoryUrl = "https://artifacts.oicr.on.ca/artifactory";

  }

  @Data
  public static class JupyterProperties {

    /**
     * Whether to enable Jupyter Notebooks in analysis.
     */
    @JsonProperty
    boolean enabled;

    @JsonProperty
    String url;

  }

  @Data
  public static class BannerProperties {

    @JsonProperty
    String message;

    @JsonProperty
    boolean alwaysShow;

    @JsonProperty
    String link;

    @JsonProperty
    String linkText;

    public Map<String, Object> getJsonMessage() {
      return ImmutableMap.of("message", message,
        "alwaysShow", alwaysShow,
        "link", link,
        "linkText", linkText);
    }

  }

}
