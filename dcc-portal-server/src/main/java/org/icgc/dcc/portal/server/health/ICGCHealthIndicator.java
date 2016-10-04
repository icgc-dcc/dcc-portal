/*
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.portal.server.health;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.icgc.dcc.common.client.api.daco.DACOClient.UserType.CUD;

import org.icgc.dcc.portal.server.security.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ICGCHealthIndicator extends AbstractHealthIndicator {

  /**
   * Constants.
   */
  private static final String DACO_ENABLED_CUD_USER = "btiernay";

  /**
   * Dependencies
   */
  private final AuthService authService;

  @Override
  protected void doHealthCheck(Builder builder) throws Exception {
    log.info("Checking the health of ICGC...");
    val token = authService.getAuthToken();
    if (isNullOrEmpty(token)) {
      builder.down().withDetail("message", "Token empty").build();
      return;
    }

    if (!authService.hasDacoAccess(DACO_ENABLED_CUD_USER, CUD)) {
      builder.down().withDetail("message", "Invalid DACO account").build();
      return;
    }

    builder.up().withDetail("message", "CUD and DACO valid").build();
  }

}
