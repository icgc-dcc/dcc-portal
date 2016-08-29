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

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;

import org.icgc.dcc.portal.server.config.ServerProperties.AuthProperties;
import org.icgc.dcc.portal.server.model.User;

import com.google.common.net.HttpHeaders;
import com.sun.jersey.api.core.HttpContext;
import com.sun.jersey.server.impl.inject.AbstractHttpContextInjectable;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * An {@code Injectable} which provides the following to {@link UserAuthProvider}:
 * <ul>
 * <li>Performs decode from HTTP request</li>
 * <li>Carries OpenID authentication data</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
class UserAuthInjectable extends AbstractHttpContextInjectable<User> {

  /**
   * Constants.
   */
  public static final String AUTH_BEARER_TYPE = "Bearer";

  /**
   * The Authenticator that will compare credentials
   */
  @Getter
  @NonNull
  private final UserAuthenticator authenticator;

  /**
   * The authentication realm
   */
  @Getter
  @NonNull
  private final String realm;

  /**
   * Is an authenticated user required?
   */
  private final boolean required;

  @Override
  public User getValue(HttpContext httpContext) {
    // First try session
    val sessionToken = resolveSessionToken(httpContext);
    if (sessionToken != null) {
      val result = handleAuthentication(Optional.of(sessionToken), Optional.empty());
      if (result != null) {
        return result;
      }
    }

    // Next try OAuth
    val accessToken = resolveAccessToken(httpContext);
    if (accessToken != null) {
      val result = handleAuthentication(Optional.empty(), Optional.of(accessToken));
      if (result != null) {
        return result;
      }
    }

    handleUnauthenticated();

    return null;
  }

  private static UUID resolveSessionToken(HttpContext httpContext) {
    Map<String, Cookie> cookies = httpContext.getRequest().getCookies();

    UUID token = null;

    try {
      if (cookies.containsKey(AuthProperties.SESSION_TOKEN_NAME)) {
        token = UUID.fromString(cookies.get(AuthProperties.SESSION_TOKEN_NAME).getValue());
      }
    } catch (IllegalArgumentException e) {
      log.debug("Invalid session token passed in request");
    } catch (NullPointerException e) {
      log.debug("No session token passed in request");
    }

    return token;
  }

  private static String resolveAccessToken(HttpContext httpContext) {
    val headers = httpContext.getRequest().getRequestHeader(HttpHeaders.AUTHORIZATION);

    String token = null;

    try {
      // Typically there is only one (most servers enforce that)
      for (val value : headers)
        if ((value.toLowerCase().startsWith(AUTH_BEARER_TYPE.toLowerCase()))) {
          val authHeaderValue = value.substring(AUTH_BEARER_TYPE.length()).trim();
          int commaIndex = authHeaderValue.indexOf(',');
          if (commaIndex > 0) {
            token = authHeaderValue.substring(0, commaIndex);
          } else {
            token = authHeaderValue;
          }
        }
    } catch (NullPointerException e) {
      log.debug("No OAuth access token passed in request");
    } catch (Exception e) {
      log.debug("Invalid OAuth access token passed in request");
    }

    return token;
  }

  private User handleAuthentication(Optional<UUID> sessionToken, Optional<String> accessToken) {
    val credentials = new UserCredentials(sessionToken, accessToken);

    try {
      val result = authenticator.authenticate(credentials);
      if (result.isPresent()) {
        return result.get();
      }
    } catch (Exception e) {
      log.warn("Problem authenticating '" + credentials + "':", e);
    }

    return null;
  }

  private void handleUnauthenticated() {
    if (required) {
      throw new WebApplicationException(Response.status(UNAUTHORIZED)
          .entity("A valid Session token is required to access this resource.")
          .type(TEXT_PLAIN_TYPE)
          .build());
    }
  }

}
