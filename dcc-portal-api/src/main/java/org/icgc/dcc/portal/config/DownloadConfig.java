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

import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.download.client.DownloadClient;
import org.icgc.dcc.download.client.DownloadClientConfig;
import org.icgc.dcc.download.client.impl.HttpDownloadClient;
import org.icgc.dcc.download.client.impl.NoOpDownloadClient;
import org.icgc.dcc.download.core.jwt.JwtConfig;
import org.icgc.dcc.download.core.jwt.JwtService;
import org.icgc.dcc.portal.config.PortalProperties.DownloadProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Lazy
@Configuration
public class DownloadConfig {

  @Bean
  @SneakyThrows
  public DownloadClient downloadClient(DownloadProperties properties) {
    if (properties.isEnabled() == false) {
      return new NoOpDownloadClient();
    }

    val clientConfig = new DownloadClientConfig()
        .baseUrl(properties.getServerUrl())
        .user(properties.getUser())
        .password(properties.getPassword())
        .requestLoggingEnabled(properties.isRequestLoggingEnabled())
        .strictSSLCertificates(properties.isStrictSSLCertificates());

    return new HttpDownloadClient(clientConfig);
  }

  @Bean
  public JwtService jwtService(DownloadProperties properties) {
    val config = new JwtConfig();
    config.setSharedSecret(properties.getSharedSecret());
    config.setAesKey(properties.getAesKey());
    config.setTtlHours(properties.getTtlHours());

    return new JwtService(config);
  }

}
