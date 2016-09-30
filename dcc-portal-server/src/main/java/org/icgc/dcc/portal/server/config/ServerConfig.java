/*
 * Copyright 2016(c) The Ontario Institute for Cancer Research. All rights reserved.
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

import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.client.Client;
import org.icgc.dcc.common.core.mail.Mailer;
import org.icgc.dcc.portal.server.config.ServerProperties.AuthProperties;
import org.icgc.dcc.portal.server.config.ServerProperties.CacheProperties;
import org.icgc.dcc.portal.server.config.ServerProperties.CrowdProperties;
import org.icgc.dcc.portal.server.config.ServerProperties.DownloadProperties;
import org.icgc.dcc.portal.server.config.ServerProperties.ElasticSearchProperties;
import org.icgc.dcc.portal.server.config.ServerProperties.HazelcastProperties;
import org.icgc.dcc.portal.server.config.ServerProperties.ICGCProperties;
import org.icgc.dcc.portal.server.config.ServerProperties.MailProperties;
import org.icgc.dcc.portal.server.config.ServerProperties.OAuthProperties;
import org.icgc.dcc.portal.server.config.ServerProperties.SoftwareProperties;
import org.icgc.dcc.portal.server.config.ServerProperties.WebProperties;
import org.icgc.dcc.portal.server.model.Settings;
import org.icgc.dcc.portal.server.security.jersey.UserAuthProvider;
import org.icgc.dcc.portal.server.security.jersey.UserAuthenticator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import lombok.NonNull;
import lombok.val;

@EnableAsync
@Configuration
public class ServerConfig {

  /**
   * Properties.
   */

  @Bean
  @ConfigurationProperties
  public ServerProperties properties() {
    return new ServerProperties();
  }

  /**
   * Infrastructure.
   */

  @Bean
  public UserAuthProvider openIdProvider(UserAuthenticator authenticator) {
    return new UserAuthProvider(authenticator, "OpenID");
  }

  @Bean
  public QueryEngine queryEngine(@NonNull Client client, @Value("#{indexName}") String index) {
    return new QueryEngine(client, index);
  }

  @Bean
  public Mailer mailer(MailProperties mail) {
    return Mailer.builder()
        .enabled(mail.isEnabled())
        .recipient(mail.getRecipientEmail())
        .from(mail.getSenderEmail())
        .host(mail.getSmtpServer())
        .port(Integer.toString(mail.getSmtpPort()))
        .build();
  }

  /**
   * Settings
   */

  @Bean
  public Settings settings() {
    val crowd = properties().getCrowd();
    val release = properties().getRelease();
    val download = properties().getDownload();
    val auth = properties().getAuth();
    val setAnalysis = properties().getSetOperation();
    val features = properties().getFeatures();
    val mirror = properties().getMirror();

    return Settings.builder()
        .ssoUrl(crowd.getSsoUrl())
        .ssoUrlGoogle(crowd.getSsoUrlGoogle())
        .releaseDate(release.getReleaseDate())
        .dataVersion(release.getDataVersion())
        .downloadEnabled(download.isEnabled())
        .authEnabled(auth.isEnabled())
        .maxNumberOfHits(setAnalysis.maxNumberOfHits)
        .maxMultiplier(setAnalysis.maxMultiplier)
        .mirror(mirror)
        .featureFlags(features)
        .build();
  }

  /**
   * Properties
   */

  @Bean
  public MailProperties mailProperties() {
    return properties().getMail();
  }

  @Bean
  public ICGCProperties icgcProperties() {
    return properties().getIcgc();
  }

  @Bean
  public DownloadProperties downloadProperties() {
    return properties().getDownload();
  }

  @Bean
  public ElasticSearchProperties elasticSearchProperties() {
    return properties().getElastic();
  }

  @Bean
  public CrowdProperties crowdProperties() {
    return properties().getCrowd();
  }

  @Bean
  public CacheProperties cacheProperties() {
    return properties().getCache();
  }

  @Bean
  public WebProperties webProperties() {
    return properties().getWeb();
  }

  @Bean
  public HazelcastProperties hazelcastProperties() {
    return properties().getHazelcast();
  }

  @Bean
  public OAuthProperties oauthProperties() {
    return properties().getOauth();
  }

  @Bean
  public AuthProperties authProperties() {
    return properties().getAuth();
  }

  @Bean
  public SoftwareProperties softProperties() {
    return properties().getSoftware();
  }

}
