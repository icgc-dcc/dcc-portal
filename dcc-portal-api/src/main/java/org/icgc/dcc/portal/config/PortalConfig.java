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

package org.icgc.dcc.portal.config;

import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.query.QueryEngine;
import org.elasticsearch.client.Client;
import org.icgc.dcc.common.core.mail.Mailer;
import org.icgc.dcc.portal.auth.UserAuthProvider;
import org.icgc.dcc.portal.auth.UserAuthenticator;
import org.icgc.dcc.portal.config.PortalProperties.AuthProperties;
import org.icgc.dcc.portal.config.PortalProperties.CacheProperties;
import org.icgc.dcc.portal.config.PortalProperties.CrowdProperties;
import org.icgc.dcc.portal.config.PortalProperties.DownloadProperties;
import org.icgc.dcc.portal.config.PortalProperties.ElasticSearchProperties;
import org.icgc.dcc.portal.config.PortalProperties.HazelcastProperties;
import org.icgc.dcc.portal.config.PortalProperties.ICGCProperties;
import org.icgc.dcc.portal.config.PortalProperties.MailProperties;
import org.icgc.dcc.portal.config.PortalProperties.OAuthProperties;
import org.icgc.dcc.portal.config.PortalProperties.WebProperties;
import org.icgc.dcc.portal.model.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import com.yammer.dropwizard.db.DatabaseConfiguration;

@EnableAsync
@Configuration
public class PortalConfig {

  /**
   * Dependencies.
   */
  @Autowired
  private PortalProperties properties;

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
    val crowd = properties.getCrowd();
    val release = properties.getRelease();
    val download = properties.getDownload();
    val auth = properties.getAuth();
    val setAnalysis = properties.getSetOperation();
    val features = properties.getFeatures();

    return Settings.builder()
        .ssoUrl(crowd.getSsoUrl())
        .ssoUrlGoogle(crowd.getSsoUrlGoogle())
        .releaseDate(release.getReleaseDate())
        .dataVersion(release.getDataVersion())
        .downloadEnabled(download.isEnabled())
        .authEnabled(auth.isEnabled())
        .maxNumberOfHits(setAnalysis.maxNumberOfHits)
        .maxMultiplier(setAnalysis.maxMultiplier)
        .featureFlags(features)
        .build();
  }

  /**
   * Properties
   */

  @Bean
  public MailProperties mailProperties() {
    return properties.getMail();
  }

  @Bean
  public ICGCProperties icgcProperties() {
    return properties.getIcgc();
  }

  @Bean
  public DownloadProperties downloadProperties() {
    return properties.getDownload();
  }

  @Bean
  public ElasticSearchProperties elasticSearchProperties() {
    return properties.getElastic();
  }

  @Bean
  public DatabaseConfiguration databaseProperties() {
    return properties.getDatabase();
  }

  @Bean
  public CrowdProperties crowdProperties() {
    return properties.getCrowd();
  }

  @Bean
  public CacheProperties cacheProperties() {
    return properties.getCache();
  }

  @Bean
  public WebProperties webProperties() {
    return properties.getWeb();
  }

  @Bean
  public HazelcastProperties hazelcastProperties() {
    return properties.getHazelcast();
  }

  @Bean
  public OAuthProperties oauthProperties() {
    return properties.getOauth();
  }

  @Bean
  public AuthProperties authProperties() {
    return properties.getAuth();
  }

}
