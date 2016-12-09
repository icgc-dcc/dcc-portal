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

package org.icgc.dcc.portal.server.security;

import static org.icgc.dcc.common.client.api.daco.DACOClient.UserType.CUD;
import static org.icgc.dcc.common.client.api.daco.DACOClient.UserType.OPENID;

import java.util.Optional;

import org.icgc.dcc.common.client.api.ICGCEntityNotFoundException;
import org.icgc.dcc.common.client.api.ICGCException;
import org.icgc.dcc.common.client.api.cud.CUDClient;
import org.icgc.dcc.common.client.api.cud.User;
import org.icgc.dcc.common.client.api.daco.DACOClient;
import org.icgc.dcc.common.client.api.daco.DACOClient.UserType;
import org.icgc.dcc.portal.server.config.ServerProperties.ICGCProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides authentication services against the Central User Directory (CUD) and utilities to check if the user is a
 * DACO approved user.
 * 
 * @see https://wiki.oicr.on.ca/display/icgcweb/CUD-LOGIN
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class AuthService {

  /**
   * Configuration.
   */
  @NonNull
  private ICGCProperties icgcConfig;

  /**
   * Dependencies.
   */
  @NonNull
  private CUDClient cudClient;
  @NonNull
  private DACOClient dacoClient;

  public Optional<org.icgc.dcc.common.client.api.daco.User> getDacoUser(@NonNull UserType userType,
      @NonNull String userId) {
    log.debug("Verifying DACO access for user '{}'", userId);
    if (userType.equals(OPENID)) {
      return getDacoUserByOpenId(userId);
    } else {
      return getDacoUserByCUD(userId);
    }
  }

  /**
   * Checks Central User Directory (CUD) if <tt>username</tt> has DACO access.
   * 
   * @throws ICGCException and its sub-classes
   */
  public boolean hasDacoAccess(String userId, UserType userType) {
    log.debug("Checking DACO access for user: '{}'. User type: {}", userId, userType);
    val result = dacoClient.hasDacoAccess(userId, userType);
    log.debug("Does {} have DACO access? - {}", userId, result);

    return result;
  }

  /**
   * Checks Central User Directory (CUD) if <tt>username</tt> has DACO cloud access.
   * 
   * @throws ICGCException and its sub-classes
   */
  public boolean hasDacoCloudAccess(String userId) {
    log.debug("Checking DACO cloud access for user: '{}'. User type: {}", userId);
    val result = dacoClient.hasCloudAccess(userId);
    log.debug("Does {} have DACO cloud access? - {}", userId, result);

    return result;
  }

  /**
   * Logins <tt>username</tt> to Central User Directory (CUD).
   * 
   * @throws ICGCException and its sub-classes
   */
  public String loginUser(String username, String password) {
    log.debug("Login user. Username: {}. Password: {}", username, password);
    val result = cudClient.login(username, password);
    log.debug("Logged in user '{}'. Login token '{}'", username, result);

    return result;
  }

  /**
   * Get user info from Central User Directory (CUD).
   * 
   * @throws ICGCException and its sub-classes
   */
  public User getCudUserInfo(@NonNull String userToken) {
    log.debug("Getting CUD info for user token {}", userToken);
    val authToken = getAuthToken();
    val result = cudClient.getUserInfo(authToken, userToken);
    log.debug("User information: {}", result);

    return result;
  }

  /**
   * Login as the service user defined in the configuration file.
   * 
   * @return session token
   * @throws ICGCException and its sub-classes
   */
  public String getAuthToken() {
    return loginUser(icgcConfig.getCudUser(), icgcConfig.getCudPassword());
  }

  private Optional<org.icgc.dcc.common.client.api.daco.User> getDacoUserByCUD(String userId) {
    try {
      // Resolve openId for this user.
      val openIdUsers = dacoClient.getUsersByType(CUD, userId);
      if (openIdUsers.isEmpty()) {
        return Optional.empty();
      }

      if (openIdUsers.size() > 1) {
        log.warn("CUD user '{}' has multiple openIDs. Using the first one. OpenIDs: {}", userId, openIdUsers);
      }

      val user = openIdUsers.get(0);

      return getDacoUserByOpenId(user.getOpenid());
    } catch (ICGCEntityNotFoundException e) {
      log.debug("User '{}' does not have DACO access", userId);

      return Optional.empty();
    }
  }

  private Optional<org.icgc.dcc.common.client.api.daco.User> getDacoUserByOpenId(String userId) {
    try {
      val dacoUsers = dacoClient.getUser(userId);
      if (dacoUsers.isEmpty()) {
        return Optional.empty();
      }

      if (dacoUsers.size() > 1) {
        log.warn("OpenID '{}' is shared between multiple users. Returning the first one. Users: {}", userId, dacoUsers);
      }

      return Optional.of(dacoUsers.get(0));
    } catch (ICGCEntityNotFoundException e) {
      log.debug("User '{}' does not have DACO access", userId);

      return Optional.empty();
    }
  }

}
