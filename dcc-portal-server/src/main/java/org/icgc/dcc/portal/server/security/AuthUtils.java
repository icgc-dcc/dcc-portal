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
package org.icgc.dcc.portal.server.security;

import static java.util.Collections.emptyList;
import static lombok.AccessLevel.PRIVATE;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;

import javax.ws.rs.core.NewCookie;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.portal.server.config.ServerProperties.AuthProperties;
import org.icgc.dcc.portal.server.service.ForbiddenAccessException;

/**
 * Contains helper methods to assist in the authentication process.
 */
@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class AuthUtils {

  private static final String ICGC_DOMAIN = ".icgc.org";
  private static final String DEFAULT_DOMAIN = "";
  private static final String COOKIE_PATH = "/";
  private static final String COOKIE_COMMENT = null;
  private static final String FAILED_AUTH_COOKIE_VALUE = null;
  private static final int FAILED_AUTH_COOKIE_AGE = 0; // Expire immediately
  private static final int SUCCESSFUL_AUTH_COOKIE_AGE = 60 * 30; // 30 mins
  private static final boolean SECURE = true;
  private static final String DEFAULT_USER_MESSAGE = "An error occurred while trying to log in.";

  public static NewCookie deleteCookie(@NonNull String cookieName) {
    return new NewCookie(
        cookieName,
        FAILED_AUTH_COOKIE_VALUE,
        COOKIE_PATH,
        (cookieName.equals(AuthProperties.SESSION_TOKEN_NAME)) ? DEFAULT_DOMAIN : ICGC_DOMAIN,
        COOKIE_COMMENT,
        FAILED_AUTH_COOKIE_AGE,
        SECURE);
  }

  public static NewCookie createSessionCookie(@NonNull String cookieName, @NonNull String cookieValue) {
    log.debug("Creating new session cookie: {}={}", cookieName, cookieValue);
    return new NewCookie(
        cookieName,
        cookieValue,
        COOKIE_PATH,
        DEFAULT_DOMAIN,
        COOKIE_COMMENT,
        SUCCESSFUL_AUTH_COOKIE_AGE,
        SECURE);
  }

  /**
   * Throws an {@link AuthenticationException} that will be mapped to the <tt>Unauthorized</tt> {@link Response}
   * 
   * @param userMessage returned in response's body and logged to the log file
   * @throws AuthenticationException
   */
  public static void throwAuthenticationException(@NonNull String userMessage) {
    throwAuthenticationException(userMessage, userMessage);
  }

  /**
   * Throws an {@link AuthenticationException} that will be mapped to the <tt>Unauthorized</tt> {@link Response}
   * 
   * @param userMessage returned in response's body and logged to the log file
   * @param invalidateCookie indicates if <tt>crowd_cookie</tt> must be invalidated in the response
   * @throws AuthenticationException
   */
  public static void throwAuthenticationException(@NonNull String userMessage, Collection<String> invalidateCookie) {
    throwAuthenticationException(userMessage, userMessage, invalidateCookie);
  }

  /**
   * Throws an {@link AuthenticationException} that will be mapped to the <tt>Unauthorized</tt> {@link Response}
   * 
   * @param userMessage returned in response's body
   * @param logMessage is logged to the log file
   * @throws AuthenticationException
   */
  public static void throwAuthenticationException(@NonNull String userMessage, @NonNull String logMessage) {
    throwAuthenticationException(userMessage, logMessage, emptyList());
  }

  /**
   * Throws an {@link AuthenticationException} that will be mapped to the <tt>Unauthorized</tt> {@link Response}
   * 
   * @param userMessage returned in response's body
   * @param logMessage is logged to the log file
   * @param invalidateCookie indicates if <tt>crowd_cookie</tt> must be invalidated in the response
   * @throws AuthenticationException
   */
  public static void throwAuthenticationException(@NonNull String userMessage, @NonNull String logMessage,
      Collection<String> invalidateCookies) {

    log.info(logMessage);
    throw new AuthenticationException(userMessage, null, invalidateCookies);
  }

  /**
   * Throws an {@link AuthenticationException} that will be mapped to the <tt>POST/redirect/GET</tt> {@link Response}
   * 
   * @param userMessage returned in "openid_error" cookie
   * @param logMessage is logged to the log file
   * @param redirect - redirection URL
   * @throws AuthenticationException
   */
  public static void throwRedirectException(@NonNull String userMessage, @NonNull String logMessage, URI redirect) {
    log.info(logMessage);
    throw new AuthenticationException(userMessage, redirect, emptyList());
  }

  /**
   * Throws an {@link AuthenticationException} that will be mapped to the <tt>POST/redirect/GET</tt> {@link Response}
   * 
   * @param userMessage returned in "openid_error" cookie and logged to the log file
   * @param redirect - redirection URL
   * @throws AuthenticationException
   */
  public static void throwRedirectException(@NonNull String userMessage, URI redirect) {
    throwRedirectException(userMessage, userMessage, redirect);
  }

  /**
   * Throws an {@link ForbiddenAccessException}.
   * @param userMessage returned to the user
   * @param logMessage logged to the log file
   */
  public static void throwForbiddenException(String userMessage, String logMessage) {
    log.warn(logMessage);
    // entityName field is not used anywhere. Passing an empty string
    throw new ForbiddenAccessException(userMessage, "");
  }

  /**
   * Converts a {@link String} to {@link UUID}
   * 
   * @throws AuthenticationException if <tt>source</tt> can't be parsed
   */
  public static UUID stringToUuid(@NonNull String source) {
    return stringToUuid(source, null);
  }

  /**
   * Converts a {@link String} to {@link UUID}
   * 
   * @param redirect indicates redirection URI for the end-user
   * @throws AuthenticationException if <tt>source</tt> can't be parsed
   */
  public static UUID stringToUuid(@NonNull String source, URI redirect) {
    UUID result = null;

    try {
      result = UUID.fromString(source);
    } catch (IllegalArgumentException e) {
      throwRedirectException(DEFAULT_USER_MESSAGE,
          String.format("Failed to convert string '%s' to UUID. Exception: %s", source, e.getMessage()), redirect);
    }

    return result;
  }

}
