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
import static javax.ws.rs.core.HttpHeaders.SET_COOKIE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.icgc.dcc.portal.server.security.AuthUtils.createSessionCookie;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.icgc.dcc.portal.server.config.ServerProperties.AuthProperties;
import org.icgc.dcc.portal.server.model.User;
import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.security.openid.OpenIDAuthService;
import org.icgc.dcc.portal.server.service.BadRequestException;
import org.openid4java.message.ParameterList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.swagger.annotations.Api;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Resource that performs OpenID authentication.
 */
@Component
@Api("/auth")
@Path("/v1/auth/openid")
@Produces(TEXT_PLAIN)
@Slf4j
@RequiredArgsConstructor(onConstructor = @__({ @Autowired }))
public class OpenIDResource extends Resource {

  /**
   * Constants.
   */
  private static final String IDENTIFIER_PARAM_NAME = "identifier";
  private static final String CURRENT_URL_PARAM_NAME = "currentUrl";
  private static final String TOKEN_PARAM_NAME = "token";
  private static final String REDIRECT_PARAM_NAME = "redirect";
  private static final String RESPONSE_HEADER_VALUE_TEMPLATE = "%s;HttpOnly";

  /**
   * Dependencies
   */
  @NonNull
  private final OpenIDAuthService openidService;

  /**
   * Handles the authentication request from the user after they select their OpenID Provider
   * 
   * @param identifier The identifier for the OpenID Provider
   * @param currentUrl The current URL path of the page calling this resource
   * @return A redirection or a form view containing user-specific permissions
   */
  @POST
  @Path("/provider")
  public Response authenticationRequest(
      @Context HttpServletRequest request,
      @QueryParam(IDENTIFIER_PARAM_NAME) String identifier,
      @QueryParam(CURRENT_URL_PARAM_NAME) String currentUrl) {

    try {
      val authRequest = openidService.createAuthRequest(
          request.getServerName(),
          request.getServerPort(),
          identifier,
          currentUrl);

      // Redirect the user to their OpenID Provider
      return Response.ok(authRequest.getDestinationUrl(true)).build();
    } catch (Exception e) {
      log.info("OpenID Authentication Exception: ", e);
      throw new BadRequestException("An error has occurred please try again");
    }
  }

  /**
   * Handles the OpenID Provider response to the earlier authentication request
   * 
   * @return The OpenID identifier for this user if verification was successful
   */
  @GET
  @Path("/verify")
  public Response verifyOpenIdServerResponse(
      @Context HttpServletRequest request,
      @QueryParam(TOKEN_PARAM_NAME) String token,
      @QueryParam(REDIRECT_PARAM_NAME) URI redirect) {

    // Extract the parameters from the authentication response which comes in as a HTTP request from the OpenID provider
    val parameterList = new ParameterList(request.getParameterMap());
    val receivingUrl = getReceivingUrl(request);
    log.debug("[{}] ReceivingUrl - {}", token, receivingUrl);
    val user = openidService.verify(token, receivingUrl, parameterList, redirect);

    return Response
        .seeOther(redirect)
        .header(SET_COOKIE, String.format(RESPONSE_HEADER_VALUE_TEMPLATE, setSessionCookie(user).toString()))
        .build();
  }

  private static String getReceivingUrl(HttpServletRequest request) {
    val receivingURL = request.getRequestURL();
    val queryString = request.getQueryString();

    if (!isNullOrEmpty(queryString)) {
      receivingURL.append("?").append(request.getQueryString());
    }

    // If the portal is behind a load-balancer the response will always come through HTTP. Rewrite to HTTPS to match the
    // return_to URL generated in the previous step
    return receivingURL.toString().replaceFirst("http://", "https://");
  }

  private static NewCookie setSessionCookie(User user) {
    val sessionToken = user.getSessionToken().toString();

    return createSessionCookie(AuthProperties.SESSION_TOKEN_NAME, sessionToken);
  }

}
