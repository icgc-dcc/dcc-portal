/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.portal.server.security.jersey;

import org.icgc.dcc.portal.server.model.User;
import org.icgc.dcc.portal.server.security.oauth.OAuthClient;
import org.icgc.dcc.portal.server.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Authenticator which verifies if the provided OpenID credentials are valid.
 */
@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class UserAuthenticator {

  /**
   * Constants.
   */
  private static final String PORTAL_DOWNLOAD_SCOPE = "portal.download";

  /**
   * Dependencies.
   */
  @NonNull
  private final SessionService sessionService;
  @NonNull
  private final OAuthClient oauthClient;

  public Optional<User> authenticate(UserCredentials credentials) {
    if (credentials.isWebSession()) {
      val sessionToken = credentials.getSessionToken().get();
      log.debug("Looking up user by session token '{}'...", sessionToken);

      // Get the User referred to by the API key
      val user = sessionService.getUserBySessionToken(sessionToken);
      if (user.isPresent() && user.get().getDaco()) {
        return user;
      }
    } else if (credentials.isAPI()) {
      val accessToken = credentials.getAccessToken().get();
      log.debug("Looking up user by access token '{}'...", accessToken);

      if (oauthClient.checkToken(accessToken, PORTAL_DOWNLOAD_SCOPE)) {
        val user = new User();

        user.setDaco(true);

        return Optional.of(user);
      }
    }

    return Optional.absent();
  }

}
