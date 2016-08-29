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
package org.icgc.dcc.portal.server.jersey.mapper;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.icgc.dcc.portal.server.security.AuthUtils.deleteCookie;

import java.util.Collection;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.icgc.dcc.portal.server.config.ServerProperties.AuthProperties;
import org.icgc.dcc.portal.server.model.Error;
import org.icgc.dcc.portal.server.resource.security.AuthResource;
import org.icgc.dcc.portal.server.resource.security.OpenIDResource;
import org.icgc.dcc.portal.server.security.AuthenticationException;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import lombok.NonNull;
import lombok.val;

@Component
@Provider
public class AuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException> {

  private static final Status STATUS = UNAUTHORIZED;
  private static final String COOKIE_NAME = "openid_error";
  private static final String COOKIE_PATH = "";
  private static final String DOMAIN = "";
  private static final String COOKIE_COMMENT = null;
  private static final int REDIRECT_COOKIE_AGE = 10;
  private static final boolean SECURE = true;

  @Context
  private HttpHeaders headers;

  @Override
  public Response toResponse(AuthenticationException e) {
    return (e.getRedirect() == null) ? createUnauthenticatedResponse(e, headers) : createRedirectResponse(e);
  }

  /**
   * Is used by {@link AuthResource}
   */
  private static Response createUnauthenticatedResponse(AuthenticationException e, HttpHeaders headers) {
    val response = status(STATUS)
        .type(APPLICATION_JSON_TYPE)
        .cookie(deleteCookie(AuthProperties.SESSION_TOKEN_NAME))
        .entity(errorResponse(e));

    // dcc cookie is always deleted at a failed authentication because it's managed by the portal
    // crowd cookie is deleted ONLY when it's expired. I.e. we tried to authenticate with it but was rejected by the
    // ICGC API
    val cookiesToDelete = e.getInvalidCookies();

    return (cookiesToDelete.isEmpty()) ? response.build() : invalidateCrowdCookie(response, cookiesToDelete).build();
  }

  private static Error errorResponse(AuthenticationException e) {
    return new Error(STATUS, e.getMessage());
  }

  private static ResponseBuilder invalidateCrowdCookie(ResponseBuilder responseBuilder,
      Collection<String> invalidCookies) {
    val cookiesToDelete = Lists.<NewCookie> newArrayList();
    for (val cookieName : invalidCookies) {
      cookiesToDelete.add(deleteCookie(cookieName));
    }

    return responseBuilder.cookie(cookiesToDelete.toArray(new NewCookie[cookiesToDelete.size()]));
  }

  /**
   * Is used by {@link OpenIDResource}
   */
  private static Response createRedirectResponse(AuthenticationException e) {
    return Response
        .seeOther(e.getRedirect())
        .cookie(getErrorCookie(e.getMessage()))
        .build();
  }

  private static NewCookie getErrorCookie(@NonNull String message) {
    return new NewCookie(COOKIE_NAME, message, COOKIE_PATH, DOMAIN, COOKIE_COMMENT, REDIRECT_COOKIE_AGE, SECURE);
  }

}
