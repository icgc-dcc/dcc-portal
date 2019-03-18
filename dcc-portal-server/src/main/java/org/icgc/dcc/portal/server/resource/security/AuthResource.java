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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.dcc.common.client.api.ICGCException;
import org.icgc.dcc.common.client.api.daco.DACOClient.UserType;
import org.icgc.dcc.portal.server.config.ServerProperties.AuthProperties;
import org.icgc.dcc.portal.server.config.ServerProperties.CrowdProperties;
import org.icgc.dcc.portal.server.model.Error;
import org.icgc.dcc.portal.server.model.User;
import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.security.AuthService;
import org.icgc.dcc.portal.server.security.AuthenticationException;
import org.icgc.dcc.portal.server.service.BadRequestException;
import org.icgc.dcc.portal.server.service.CmsAuthService;
import org.icgc.dcc.portal.server.service.EgoAuthService;
import org.icgc.dcc.portal.server.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.SET_COOKIE;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.portal.server.security.AuthUtils.*;

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
  @NonNull
  private final EgoAuthService egoAuthService;


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

    String jwtToken = null;
    if (requestHeaders.getRequestHeader("token") != null) {
      jwtToken = requestHeaders.getRequestHeader("token").get(0);
    }

    if (!isNullOrEmpty(jwtToken)) {
      val optionalUser = egoAuthService.getUserInfo(jwtToken);
      if (optionalUser.isPresent()) {
        val user = optionalUser.get();
        val dccUser = createUser(user.getUserName(), user.getEmail());
        dccUser.setDaco(egoAuthService.hasDacoAccess(jwtToken));
        dccUser.setCloudAccess(egoAuthService.hasCloudAccess(jwtToken));

        val verifiedResponse = verifiedResponse(dccUser);
        log.info("[{}] Finished authorization for user {} {}", jwtToken, UserType.OPENID, dccUser);
        return verifiedResponse;
      } else {
        return null;
      }
    }

    val cookies = requestHeaders.getCookies();
    val sessionToken = Optional.ofNullable(cookies.get(AuthProperties.SESSION_TOKEN_NAME)).map(javax.ws.rs.core.Cookie::getValue).orElse(null);
    // Already logged in and credentials available
    if (sessionToken != null) {
      log.info("[{}] Looking for already authenticated user in the cache", sessionToken);
      val user = getAuthenticatedUser(sessionToken);

      val verifiedResponse = verifiedResponse(user);
      log.info("[{}] Finished authorization for user '{}'. DACO access: '{}'",
              sessionToken, user.getOpenIDIdentifier(), user.getDaco());

      return verifiedResponse;
    }

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

    return verifiedResponse(createUser(username, username));
  }

  @POST
  @Path("/logout")
  public Response logout(@Context HttpServletRequest request) {
    val sessionToken = getSessionToken(request);
    log.info("[{}] Terminating session", sessionToken);

    if (sessionToken != null) {
      val userOptional = sessionService.getUserBySessionToken(sessionToken);

      userOptional.ifPresent(sessionService::removeUser);
      log.info("[{}] Successfully terminated session", sessionToken);

      return createLogoutResponse(OK, "");
    }

    return createLogoutResponse(NOT_MODIFIED, "Did not find a user to log out");
  }

  /**
   * Create a valid session user or throws {@link AuthenticationException} in case of failure.
   * 
   */
  private User createUser(String userId, String userEmail) {
    val sessionToken = randomUUID();
    val sessionTokenString = sessionToken.toString();
    log.info("[{}] Creating and persisting user '{}' in the cache.", sessionTokenString, userId);
    val user = new User(userId, sessionToken);
    user.setEmailAddress(userEmail);
    log.debug("[{}] Created user: {}", sessionTokenString, user);
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
        .entity(new Error(status, message)) // Not really an error
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
}
