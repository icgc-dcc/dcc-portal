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
package org.icgc.dcc.portal.server.security.oauth;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Sets.difference;
import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.icgc.dcc.common.core.util.Separators.EMPTY_STRING;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.portal.server.security.AuthUtils.throwForbiddenException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.icgc.dcc.common.core.util.Splitters;
import org.icgc.dcc.portal.server.model.AccessToken;
import org.icgc.dcc.portal.server.model.AccessTokenScopes;
import org.icgc.dcc.portal.server.model.AccessTokenScopes.AccessTokenScope;
import org.icgc.dcc.portal.server.service.BadRequestException;
import org.icgc.dcc.portal.server.model.Tokens;
import org.icgc.dcc.portal.server.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth access tokens management service.
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TokenService {

  private static final String DEFAULT_SCOPE_DESCRIPTION = EMPTY_STRING;
  private static final Map<String, String> SCOPE_DESCRIPTIONS = ImmutableMap.<String, String> builder()
      .put("portal.download", "Allows secure downloads from the Data Portal")
      .put("portal.export", "Allows to download controlled export data from the download server")
      .put("aws.upload", "Allows uploads to AWS S3")
      .put("aws.download", "Allows secure downloads from AWS S3")
      .put("collab.upload", "Allows uploads to the Collaboratory cloud")
      .put("collab.download", "Allows secure downloads from the Collaboratory cloud")
      .put("id.create", "Allows to create new ICGC IDs at https://id.icgc.org")
      .build();

  @NonNull
  private final OAuthClient client;

  public String create(User user, String scope, String description) {
    log.debug("Creating access token of scope '{}' for user '{}'...", scope, user);
    val userId = user.getEmailAddress();
    if (user.getDaco().equals(FALSE)) {
      throwForbiddenException("The user is not DACO approved",
          format("User %s is not DACO approved to access the create access token resource", userId));
    }

    validateScope(user, scope);
    val token = client.createToken(userId, scope, description);
    log.debug("Created token '{}' for '{}'", token, userId);

    return token.getId();
  }

  public Tokens list(@NonNull User user) {
    // See getUserScopes(User) method commends on why user's email is used.
    return client.listTokens(user.getEmailAddress());
  }

  public void delete(@NonNull String tokenId) {
    client.revokeToken(tokenId);
  }

  public AccessToken getToken(@NonNull String tokenId) {
    val token = client.getToken(tokenId);
    if (token == null) {
      throw new BadRequestException("Invalid token");
    }

    return token;
  }

  public AccessTokenScopes getUserScopes(User user) {
    // In ICGC a CUD user's name and email do not match.
    // When using the ICGC API CUD users are always identified by user name
    // On the DCC Portal _all_ users are identified by their emails. For this reason we always user users's email when
    // communicating with the Auth server.
    val userId = user.getEmailAddress();
    if (userId == null) {
      log.warn("User has no OpenID but trying to access tokens: {}", user);
      return new AccessTokenScopes(emptySet());
    }

    val userScopes = client.getUserScopes(userId);
    val scopesResult = ImmutableSet.<AccessTokenScope> builder();

    for (val scope : userScopes.getScopes()) {
      val scopeDescription = firstNonNull(SCOPE_DESCRIPTIONS.get(scope), DEFAULT_SCOPE_DESCRIPTION);
      scopesResult.add(new AccessTokenScope(scope, scopeDescription));
    }

    return new AccessTokenScopes(scopesResult.build());
  }

  private void validateScope(User user, String scope) {
    val requestScopes = parseScopes(scope);
    val userScopes = extractScopeNames(getUserScopes(user));

    val scopeDiff = difference(requestScopes, userScopes);
    if (!scopeDiff.isEmpty()) {
      throwForbiddenException("The user is not allowed to create tokens of this scope",
          format("User '%s' is not allowed to create tokens of scope '%s'.", user.getEmailAddress(), scope));
    }
  }

  private static HashSet<String> parseScopes(String scope) {
    return Sets.newHashSet(Splitters.WHITESPACE.split(scope));
  }

  private static Set<String> extractScopeNames(AccessTokenScopes userScopes) {
    return userScopes.getScopes().stream()
        .map(s -> s.getName())
        .collect(toImmutableSet());
  }

}
