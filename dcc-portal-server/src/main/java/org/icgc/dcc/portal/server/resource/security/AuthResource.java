/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.server.resource.security;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.SET_COOKIE;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.OK;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.portal.server.security.AuthUtils.createSessionCookie;
import static org.icgc.dcc.portal.server.security.AuthUtils.deleteCookie;
import static org.icgc.dcc.portal.server.security.AuthUtils.stringToUuid;
import static org.icgc.dcc.portal.server.security.AuthUtils.throwAuthenticationException;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.icgc.dcc.common.client.api.ICGCException;
import org.icgc.dcc.common.client.api.daco.DACOClient.UserType;
import org.icgc.dcc.portal.server.config.ServerProperties.AuthProperties;
import org.icgc.dcc.portal.server.config.ServerProperties.CrowdProperties;
import org.icgc.dcc.portal.server.model.User;
import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.security.AuthService;
import org.icgc.dcc.portal.server.service.BadRequestException;
import org.icgc.dcc.portal.server.service.CmsAuthService;
import org.icgc.dcc.portal.server.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.swagger.annotations.Api;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Component
@Api("/auth")
@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class AuthResource extends Resource {

  /**
   * Constants.
   */
  private static final String DACO_ACCESS_KEY = "daco";
  private static final String CLOUD_ACCESS_KEY = "cloudAccess";
  private static final String TOKEN_KEY = "token";
  private static final String PASSWORD_KEY = "password";
  private static final String USERNAME_KEY = "username";

  /**
   * Configuration.
   */
  @NonNull
  private final AuthProperties properties;

  /**
   * Dependencies.
   */
  @NonNull
  private final AuthService authService;
  @NonNull
  private final SessionService sessionService;
  @NonNull
  private final CmsAuthService cmsService;

  /**
   * State.
   */
  @Getter(lazy = true, value = PRIVATE)
  private final Collection<String> cookiesToDelete = initCookiesToDelete();

  /**
   * This is only used by UI.
   */
  @GET
  @Path("/verify")
  public Response verify(@Context HttpHeaders requestHeaders) {
    // First check to see if logins are enabled
    if (!properties.isEnabled()) {
      throwAuthenticationException("Login disabled");
    }

    // Resolve tokens from cookies
    val cookies = requestHeaders.getCookies();
    val sessionToken = getCookieValue(cookies.get(AuthProperties.SESSION_TOKEN_NAME));
    val cudToken = getCookieValue(cookies.get(CrowdProperties.CUD_TOKEN_NAME));
    val cmsToken = getCookieValue(cookies.get(cmsService.getSessionName()));
    log.info("Received an authorization request. Session token: '{}'. CUD token: '{}', CMS token: {}",
        sessionToken, cudToken, cmsToken);

    // Already logged in and credentials available
    if (sessionToken != null) {
      log.info("[{}] Looking for already authenticated user in the cache", sessionToken);
      val user = getAuthenticatedUser(sessionToken);

      val verifiedResponse = verifiedResponse(user);
      log.info("[{}] Finished authorization for user '{}'. DACO access: '{}'",
          new Object[] { sessionToken, user.getOpenIDIdentifier(), user.getDaco() });

      return verifiedResponse;
    }

    if (!isNullOrEmpty(cudToken) || !isNullOrEmpty(cmsToken)) {
      log.info("[{}, {}] The user has been authenticated by the ICGC authenticator.", cudToken, cmsToken);
      val tokenUserEntry = resolveIcgcUser(cudToken, cmsToken);
      val token = tokenUserEntry.getKey();
      val icgcUser = tokenUserEntry.getValue();
      log.debug("Resolved ICGC user: {}", icgcUser);

      val userType = resolveUserType(cudToken, cmsToken);
      val userId = icgcUser.getUserName();
      val userEmail = userType == UserType.CUD ? icgcUser.getEmail() : icgcUser.getUserName();

      val dccUser = createUser(userType, userId, userEmail, token);
      val verifiedResponse = verifiedResponse(dccUser);
      log.info("[{}] Finished authorization for user {} {}", token, userType.name(), dccUser);

      return verifiedResponse;
    }

    val userMessage = "Authorization failed due to missing token";
    val logMessage = "Couldn't authorize the user. No session token found.";

    throwAuthenticationException(userMessage, logMessage, getCookiesToDelete());

    // Will not come to this point because of throwAuthenticationException()
    return null;
  }

  /**
   * This is only used by command-line utilities whose principal is not tied to OpenID, but rather CUD authentication.
   * It is not used by the UI.
   */
  @POST
  @Path("/login")
  public Response login(Map<String, String> creds) {
    checkRequest((creds == null || creds.isEmpty() || !creds.containsKey(USERNAME_KEY)), "Null or empty argument");
    val username = creds.get(USERNAME_KEY);
    log.info("Logging into CUD as {}", username);

    log.info("[{}] Checking if the user has been already authenticated.", username);
    val userOptional = sessionService.getUserByEmail(username);
    if (userOptional.isPresent()) {
      log.info("[{}] The user is already authenticated.", username);

      return verifiedResponse(userOptional.get());
    }

    // Login user.
    try {
      log.info("[{}] The user is not authenticated yet. Authenticating...", username);
      authService.loginUser(username, creds.get(PASSWORD_KEY));
    } catch (ICGCException e) {
      throwAuthenticationException("Username and password are incorrect",
          String.format("[%s] Failed to login the user. Exception %s", username, e.getMessage()));
    }

    return verifiedResponse(createUser(UserType.CUD, username, username, null));
  }

  @POST
  @Path("/logout")
  public Response logout(@Context HttpServletRequest request) {
    val sessionToken = getSessionToken(request);
    log.info("[{}] Terminating session", sessionToken);

    if (sessionToken != null) {
      val userOptional = sessionService.getUserBySessionToken(sessionToken);

      if (userOptional.isPresent()) {
        sessionService.removeUser(userOptional.get());
      }
      log.info("[{}] Successfully terminated session", sessionToken);

      return createLogoutResponse(OK, "");
    }

    return createLogoutResponse(NOT_MODIFIED, "Did not find a user to log out");
  }

  private SimpleImmutableEntry<String, org.icgc.dcc.common.client.api.cud.User> resolveIcgcUser(String cudToken,
      String cmsToken) {
    org.icgc.dcc.common.client.api.cud.User user = null;
    String token = null;

    log.debug("[{}, {}] Looking for user info in the CUD", cudToken, cmsToken);
    try {
      if (!isNullOrEmpty(cudToken)) {
        user = authService.getCudUserInfo(cudToken);
        token = cudToken;
      } else {
        user = cmsService.getUserInfo(cmsToken);
        token = cmsToken;
      }
    } catch (ICGCException e) {
      log.warn("[{}, {}] Failed to authorize ICGC user. Exception: {}",
          new Object[] { cudToken, cmsToken, e.getMessage() });
      throwAuthenticationException("Authorization failed due to expired token", getCookiesToDelete());
    }

    log.debug("[{}] Retrieved user information: {}", token, user);

    return new SimpleImmutableEntry<String, org.icgc.dcc.common.client.api.cud.User>(token, user);
  }

  /**
   * Gets already authenticated user.
   * 
   * @throws AuthenticationException
   */
  private User getAuthenticatedUser(String sessionToken) {
    val token = stringToUuid(sessionToken);
    val tempUserOptional = sessionService.getUserBySessionToken(token);

    if (!tempUserOptional.isPresent()) {
      throwAuthenticationException(
          "Authentication failed due to no User matching session token: " + sessionToken,
          String.format("[%s] Could not find any user in the cache. The session must have expired.", sessionToken),
          getCookiesToDelete());
    }
    log.info("[{}] Found user in the cache: {}", sessionToken, tempUserOptional.get());

    return tempUserOptional.get();
  }

  /**
   * Create a valid session user or throws {@link AuthenticationException} in case of failure.
   * 
   * @throws AuthenticationException
   */
  private User createUser(UserType userType, String userId, String userEmail, String cudToken) {
    val sessionToken = randomUUID();
    val sessionTokenString = cudToken == null ? sessionToken.toString() : cudToken;
    log.info("[{}] Creating and persisting user '{}' in the cache.", sessionTokenString, userId);
    val user = new User(userId, sessionToken);
    user.setEmailAddress(userEmail);
    log.debug("[{}] Created user: {}", sessionTokenString, user);

    try {
      log.debug("[{}] Checking if the user has DACO and cloud access", sessionTokenString);
      val dacoUserOpt = authService.getDacoUser(userType, userId);
      if (dacoUserOpt.isPresent()) {
        val dacoUser = dacoUserOpt.get();
        log.info("[{}] Granted DACO access to the user", sessionTokenString);
        user.setDaco(true);
        user.setOpenIDIdentifier(dacoUser.getOpenid());

        if (dacoUser.isCloudAccess()) {
          log.info("[{}] Granted DACO cloud access to the user", sessionTokenString);
          user.setCloudAccess(true);
        }
      }
    } catch (ICGCException e) {
      throwAuthenticationException("Failed to grant DACO access to the user",
          format("[%s] Failed to grant DACO access to the user. Exception: %s", sessionTokenString, e.getMessage()));
    }

    log.debug("[{}] Saving the user in the cache", sessionTokenString);
    sessionService.putUser(sessionToken, user);
    log.debug("[{}] Saved the user in the cache", sessionTokenString);

    return user;
  }

  private Response createLogoutResponse(Status status, String message) {
    val dccCookie = deleteCookie(AuthProperties.SESSION_TOKEN_NAME);
    val crowdCookie = deleteCookie(CrowdProperties.CUD_TOKEN_NAME);
    val cmsCookie = deleteCookie(cmsService.getSessionName());

    return status(status)
        .header(SET_COOKIE, dccCookie.toString())
        .header(SET_COOKIE, crowdCookie.toString())
        .header(SET_COOKIE, cmsCookie.toString())
        .entity(new org.icgc.dcc.portal.server.model.Error(status, message)) // Not really an error
        .build();
  }

  private Collection<String> initCookiesToDelete() {
    val result = ImmutableList.<String> builder();
    result.add(CrowdProperties.CUD_TOKEN_NAME);
    if (cmsService == null) {
      log.warn(
          "Can't properly define all cookies to be deleted on a failed authentication request. CmsService is not initialized");
    } else {
      result.add(cmsService.getSessionName());
    }

    return result.build();
  }

  private static UserType resolveUserType(String cudToken, String cmsToken) {
    return isNullOrEmpty(cudToken) ? UserType.OPENID : UserType.CUD;
  }

  private static String getCookieValue(javax.ws.rs.core.Cookie cookie) {
    return cookie == null ? null : cookie.getValue();
  }

  /**
   * Extracts session token from cookies or HTTP header.
   * 
   * @return sessionToken or <tt>null</tt> if no token found
   */
  private static UUID getSessionToken(HttpServletRequest request) {
    val cookies = request.getCookies();
    if (cookies != null) {
      for (val cookie : cookies) {
        if (isSessionTokenCookie(cookie)) {
          return stringToUuid(cookie.getValue());
        }
      }
    } else {
      throw new BadRequestException("Cookies must be set for this resource.");
    }

    return null;
  }

  private static boolean isSessionTokenCookie(Cookie cookie) {
    return cookie.getName().equals(AuthProperties.SESSION_TOKEN_NAME);
  }

  private static Response verifiedResponse(User user) {
    log.debug("Creating successful verified response for user: {}", user);
    val cookie = createSessionCookie(AuthProperties.SESSION_TOKEN_NAME, user.getSessionToken().toString());

    return Response.ok(ImmutableMap.of(
        TOKEN_KEY, user.getSessionToken(),
        USERNAME_KEY, user.getEmailAddress(),
        DACO_ACCESS_KEY, user.getDaco(),
        CLOUD_ACCESS_KEY, user.getCloudAccess()))
        .header(SET_COOKIE, cookie.toString())
        .build();
  }

}
