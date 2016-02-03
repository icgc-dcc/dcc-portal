/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;
import java.util.UUID;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.portal.model.User;
import org.openid4java.discovery.DiscoveryInformation;

import com.google.common.base.Optional;

public class SessionService {

  public static final String USERS_CACHE_NAME = "users";
  public static final String DISCOVERY_INFO_CACHE_NAME = "openId_auth";

  // Stores DiscoveryInformation received from OpenID Provider for further verification requests
  private final Map<UUID, DiscoveryInformation> discoveryInfoCache;

  // Stores session information about logged in users
  private final Map<UUID, User> usersCache;

  public SessionService() {
    this.usersCache = newHashMap();
    this.discoveryInfoCache = newHashMap();
  }

  public SessionService(Map<UUID, User> usersCache, Map<UUID, DiscoveryInformation> discoveryInfoCache) {
    this.usersCache = usersCache;
    this.discoveryInfoCache = discoveryInfoCache;
  }

  public void putUser(@NonNull UUID sessionToken, @NonNull User user) {
    usersCache.put(sessionToken, user);
  }

  public void removeUser(@NonNull User user) {
    checkNotNull(user.getSessionToken());

    usersCache.remove(user.getSessionToken());
  }

  public Optional<User> getUserBySessionToken(@NonNull UUID sessionToken) {
    return Optional.fromNullable(usersCache.get(sessionToken));
  }

  public Optional<User> getUserByOpenidIdentifier(String openIDIdentifier) {
    checkArgument(!isNullOrEmpty(openIDIdentifier));

    for (val user : usersCache.values()) {
      if (user.getOpenIDIdentifier().equals(openIDIdentifier)) {
        return Optional.of(user);
      }
    }

    return Optional.absent();
  }

  public Optional<User> getUserByEmail(String emailAddress) {
    if (emailAddress == null) return Optional.absent();

    for (val user : usersCache.values()) {
      if (user.getEmailAddress().equals(emailAddress)) {
        return Optional.of(user);
      }
    }

    return Optional.absent();
  }

  public void putDiscoveryInfo(@NonNull UUID sessionToken, @NonNull DiscoveryInformation discoveryInfo) {
    discoveryInfoCache.put(sessionToken, discoveryInfo);
  }

  public Optional<DiscoveryInformation> getDiscoveryInfo(@NonNull UUID sessionToken) {
    return Optional.fromNullable(discoveryInfoCache.get(sessionToken));
  }

  public void removeDiscoveryInfo(@NonNull UUID sessionToken) {
    discoveryInfoCache.remove(sessionToken);
  }

}
