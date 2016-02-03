package org.icgc.dcc.portal.resource;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.SET_COOKIE;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.icgc.dcc.portal.resource.ResourceUtils.checkRequest;
import static org.icgc.dcc.portal.util.AuthUtils.createSessionCookie;
import static org.icgc.dcc.portal.util.AuthUtils.deleteCookie;
import static org.icgc.dcc.portal.util.AuthUtils.stringToUuid;
import static org.icgc.dcc.portal.util.AuthUtils.throwAuthenticationException;

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
import org.icgc.dcc.portal.config.PortalProperties.CrowdProperties;
import org.icgc.dcc.portal.model.User;
import org.icgc.dcc.portal.service.AuthService;
import org.icgc.dcc.portal.service.CmsAuthService;
import org.icgc.dcc.portal.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Component
@Path("/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }) )
public class AuthResource extends BaseResource {

  private static final String DACO_ACCESS_KEY = "daco";
  private static final String CLOUD_ACCESS_KEY = "cloudAccess";
  private static final String TOKEN_KEY = "token";
  private static final String PASSWORD_KEY = "password";
  private static final String USERNAME_KEY = "username";

  @NonNull
  private final AuthService authService;
  @NonNull
  private final SessionService sessionService;
  @NonNull
  private final CmsAuthService cmsService;

  @Getter(lazy = true)
  private final Collection<String> cookiesToDelete = initCookiesToDelete();

  /**
   * This is only used by UI.
   */
  @GET
  @Path("/verify")
  public Response verify(@Context HttpHeaders requestHeaders) {
    val cookies = requestHeaders.getCookies();
    val sessionToken = getCookieValue(cookies.get(CrowdProperties.SESSION_TOKEN_NAME));
    val cudToken = getCookieValue(cookies.get(CrowdProperties.CUD_TOKEN_NAME));
    val cmsToken = getCookieValue(cookies.get(cmsService.getSessionName()));
    log.info("Received an authorization request. Session token: '{}'. CUD token: '{}'", sessionToken, cudToken);

    // Already logged in and knows credentials
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
      val userType = resolveUserType(cudToken, cmsToken);

      val dccUser = createUser(icgcUser.getUserName(), token, userType);
      val verifiedResponse = verifiedResponse(dccUser);
      log.info("[{}] Finished authorization for user '{}'. DACO access: '{}'",
          new Object[] { token, icgcUser.getUserName(), dccUser.getDaco() });

      return verifiedResponse;
    }

    val userMessage = "Authorization failed due to missing token";
    val logMessage = "Couldn't authorize the user. No session token found.";
    throwAuthenticationException(userMessage, logMessage, getCookiesToDelete());

    // Will not come to this point because of throwAuthenticationException()
    return null;
  }

  private static UserType resolveUserType(String cudToken, String cmsToken) {
    return isNullOrEmpty(cudToken) ? UserType.OPENID : UserType.CUD;
  }

  private static String getCookieValue(javax.ws.rs.core.Cookie cookie) {
    return cookie == null ? null : cookie.getValue();
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
  private User createUser(String userName, String cudToken, UserType userType) {
    val sessionToken = randomUUID();
    val sessionTokenString = cudToken == null ? sessionToken.toString() : cudToken;
    log.info("[{}] Creating and persisting user '{}' in the cache.", sessionTokenString, userName);
    val user = new User(null, sessionToken);
    user.setEmailAddress(userName);
    log.debug("[{}] Created user: {}", sessionTokenString, user);

    try {
      log.debug("[{}] Checking if the user has DACO and cloud access", sessionTokenString);
      val dacoUserOpt = authService.getDacoUser(userType, userName);
      if (dacoUserOpt.isPresent()) {
        val dacoUser = dacoUserOpt.get();
        log.info("[{}] Granted DACO access to the user", sessionTokenString);
        user.setDaco(true);

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

    return verifiedResponse(createUser(username, null, UserType.CUD));
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

  /**
   * Extracts session token from cookies or HTTP header.
   * 
   * @return sessionToken or <tt>null</tt> if no token found
   */
  private static UUID getSessionToken(HttpServletRequest request) {
    for (val cookie : request.getCookies()) {
      if (isSessionTokenCookie(cookie)) {
        return stringToUuid(cookie.getValue());
      }
    }

    return null;
  }

  private static boolean isSessionTokenCookie(Cookie cookie) {
    return cookie.getName().equals(CrowdProperties.SESSION_TOKEN_NAME);
  }

  private static Response verifiedResponse(User user) {
    log.debug("Creating successful verified response for user: {}", user);
    val cookie = createSessionCookie(CrowdProperties.SESSION_TOKEN_NAME, user.getSessionToken().toString());

    return Response.ok(ImmutableMap.of(
        TOKEN_KEY, user.getSessionToken(),
        USERNAME_KEY, user.getEmailAddress(),
        DACO_ACCESS_KEY, user.getDaco(),
        CLOUD_ACCESS_KEY, user.getCloudAccess()))
        .header(SET_COOKIE, cookie.toString())
        .build();
  }

  private Response createLogoutResponse(Status status, String message) {
    val dccCookie = deleteCookie(CrowdProperties.SESSION_TOKEN_NAME);
    val crowdCookie = deleteCookie(CrowdProperties.CUD_TOKEN_NAME);
    val cmsCookie = deleteCookie(cmsService.getSessionName());

    return status(status)
        .header(SET_COOKIE, dccCookie.toString())
        .header(SET_COOKIE, crowdCookie.toString())
        .header(SET_COOKIE, cmsCookie.toString())
        .entity(new org.icgc.dcc.portal.model.Error(status, message))
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

}
