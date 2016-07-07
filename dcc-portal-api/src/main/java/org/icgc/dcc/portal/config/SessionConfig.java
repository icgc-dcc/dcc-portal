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

import static com.google.common.collect.Maps.newHashMap;
import static org.icgc.dcc.portal.service.SessionService.DISCOVERY_INFO_CACHE_NAME;

import java.util.Map;
import java.util.UUID;

import org.icgc.dcc.portal.auth.openid.DistributedConsumerAssociationStore;
import org.icgc.dcc.portal.auth.openid.DistributedNonceVerifier;
import org.icgc.dcc.portal.config.PortalProperties.HazelcastProperties;
import org.icgc.dcc.portal.model.User;
import org.icgc.dcc.portal.service.SessionService;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.InMemoryConsumerAssociationStore;
import org.openid4java.consumer.InMemoryNonceVerifier;
import org.openid4java.discovery.DiscoveryInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Lazy
@Slf4j
@Configuration
public class SessionConfig {

  /**
   * Dependencies.
   */
  @Autowired
  private HazelcastProperties hazelcast;

  @Bean
  public HazelcastInstance hazelcastInstance() {
    if (isDistributed()) {
      return Hazelcast.newHazelcastInstance(getHazelcastConfig(hazelcast));
    } else {
      return null;
    }
  }

  @Bean
  public ConsumerManager consumerManager() {
    val consumerManager = new ConsumerManager();

    if (isDistributed()) {
      consumerManager.setAssociations(new DistributedConsumerAssociationStore(hazelcastInstance()));
      consumerManager.setNonceVerifier(new DistributedNonceVerifier(hazelcastInstance()));
    } else {
      consumerManager.setAssociations(new InMemoryConsumerAssociationStore());
      consumerManager.setNonceVerifier(new InMemoryNonceVerifier());
    }

    return consumerManager;
  }

  @Bean
  public SessionService sessionService() {
    if (isDistributed()) {
      Map<UUID, User> usersCache = hazelcastInstance().getMap(SessionService.USERS_CACHE_NAME);
      Map<UUID, DiscoveryInformation> discoveryInfoCache = hazelcastInstance().getMap(DISCOVERY_INFO_CACHE_NAME);

      return new SessionService(usersCache, discoveryInfoCache);
    } else {
      Map<UUID, User> usersCache = newHashMap();
      Map<UUID, DiscoveryInformation> discoveryInfoCache = newHashMap();

      return new SessionService(usersCache, discoveryInfoCache);
    }
  }

  /**
   * Utilities.
   */

  private boolean isDistributed() {
    return hazelcast.isEnabled();
  }

  private static Config getHazelcastConfig(HazelcastProperties hazelcastConfig) {
    val config = new Config();
    config.setProperty("hazelcast.logging.type", "slf4j");
    config.setGroupConfig(new GroupConfig(hazelcastConfig.getGroupName(), hazelcastConfig.getGroupPassword()));
    if (!hazelcastConfig.isMulticast()) {
      log.info("Disabling multicast and using TCP/IP for Hazecast");
      val joinConfig = config.getNetworkConfig().getJoin();

      // Disable multicast
      val multicastConfig = new MulticastConfig()
          .setEnabled(false);
      joinConfig.setMulticastConfig(multicastConfig);

      // Enable TCP/IP
      val tcpIpConfig = new TcpIpConfig()
          .setEnabled(true)
          .setMembers(hazelcastConfig.getHosts());
      joinConfig.setTcpIpConfig(tcpIpConfig);
    }

    configureMapConfigs(hazelcastConfig, config.getMapConfigs());

    return config;
  }

  private static void configureMapConfigs(HazelcastProperties hazelcastConfig, Map<String, MapConfig> mapConfigs) {
    val usersMapConfig = new MapConfig();
    usersMapConfig.setName(SessionService.USERS_CACHE_NAME);
    usersMapConfig.setTimeToLiveSeconds(hazelcastConfig.getUsersCacheTTL());
    mapConfigs.put(SessionService.USERS_CACHE_NAME, usersMapConfig);

    val openidAuthMapConfig = new MapConfig();
    openidAuthMapConfig.setName(SessionService.DISCOVERY_INFO_CACHE_NAME);
    openidAuthMapConfig.setTimeToLiveSeconds(hazelcastConfig.getOpenidAuthTTL());
    mapConfigs.put(SessionService.DISCOVERY_INFO_CACHE_NAME, openidAuthMapConfig);
  }

}
