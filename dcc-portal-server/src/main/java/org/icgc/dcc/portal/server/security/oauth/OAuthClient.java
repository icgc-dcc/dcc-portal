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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Sets.newTreeSet;
import static com.sun.jersey.api.client.Client.create;
import static com.sun.jersey.api.client.ClientResponse.Status.FORBIDDEN;
import static com.sun.jersey.api.client.ClientResponse.Status.INTERNAL_SERVER_ERROR;
import static com.sun.jersey.api.client.ClientResponse.Status.OK;
import static com.sun.jersey.api.json.JSONConfiguration.FEATURE_POJO_MAPPING;
import static com.sun.jersey.client.urlconnection.HTTPSProperties.PROPERTY_HTTPS_PROPERTIES;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SERVER_ERROR;

import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.core.MultivaluedMap;

import org.icgc.dcc.common.core.security.DumbHostnameVerifier;
import org.icgc.dcc.common.core.security.DumbX509TrustManager;
import org.icgc.dcc.common.core.util.Splitters;
import org.icgc.dcc.portal.server.config.ServerProperties.OAuthProperties;
import org.icgc.dcc.portal.server.model.AccessToken;
import org.icgc.dcc.portal.server.model.Tokens;
import org.icgc.dcc.portal.server.service.BadGatewayException;
import org.icgc.dcc.portal.server.service.BadRequestException;
import org.icgc.dcc.portal.server.service.ForbiddenAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OAuthClient {

  /**
   * Constants
   */
  private static final String PASSWORD_GRANT_TYPE = "password";
  private static final int MAX_DESCRIPTION_LENGTH = 200;

  /**
   * Constants - Params.
   */
  private static final String SCOPE_PARAM = "scope";
  private static final String DESCRIPTION_PARAM = "desc";
  private static final String USERNAME_PARAM = "username";
  private static final String GRANT_TYPE_PARAM = "grant_type";
  private static final String TOKEN_PARAM = "token";

  /**
   * Constants - URL.
   */
  private static final String TOKENS_URL = "tokens";
  private static final String USERS_URL = "users";
  private static final String SCOPES_URL = "scopes";
  private static final String CREATE_TOKEN_URL = "oauth/token";
  private static final String CHECK_TOKEN_URL = "oauth/check_token";

  /**
   * Configuration.
   */
  private final WebResource resource;

  @Autowired
  public OAuthClient(@NonNull OAuthProperties config) {
    val jerseyClient = create(getClientConfig(config));
    configureFilters(jerseyClient, config);

    this.resource = jerseyClient.resource(config.getServiceUrl());
  }

  public AccessToken createToken(@NonNull String userId, @NonNull String scope) {
    return createToken(userId, scope, null);
  }

  public AccessToken createToken(@NonNull String userId, @NonNull String scope, String description) {
    checkArguments(userId);
    val response = resource.path(CREATE_TOKEN_URL)
        .type(APPLICATION_FORM_URLENCODED_TYPE)
        .accept(APPLICATION_JSON_TYPE)
        .post(ClientResponse.class, createParameters(userId, scope, description));
    validateResponse(response);
    val accessToken = response.getEntity(AccessTokenResponse.class);

    return convertToAccessToken(accessToken);
  }

  public Tokens listTokens(@NonNull String userId) {
    checkArguments(userId);
    val response = resource.path(USERS_URL).path(userId).path(TOKENS_URL).get(ClientResponse.class);
    validateResponse(response);

    return response.getEntity(Tokens.class);
  }

  public void revokeToken(@NonNull String token) {
    checkArguments(token);
    val response = resource.path(TOKENS_URL).path(token).delete(ClientResponse.class);
    validateResponse(response);
  }

  public boolean checkToken(@NonNull String tokenId, String scope) {
    val token = getToken(tokenId);
    if (token == null) {
      return false;
    }

    return token.getScope().contains(scope);
  }

  public AccessToken getToken(@NonNull String token) {
    checkArguments(token);

    val params = new MultivaluedMapImpl();
    params.add(TOKEN_PARAM, token);

    val response = resource.path(CHECK_TOKEN_URL)
        .type(APPLICATION_FORM_URLENCODED_TYPE)
        .accept(APPLICATION_JSON_TYPE)
        .post(ClientResponse.class, params);

    checkState(response.getClientResponseStatus().getFamily() != SERVER_ERROR, "Error checking token: %s", response);
    if (response.getClientResponseStatus() != OK) {
      return null;
    }

    val checkResponse = response.getEntity(CheckTokenResponse.class);
    return new AccessToken(token, checkResponse.getDesc(), checkResponse.getExp(),
        newTreeSet(checkResponse.getScope()));
  }

  public UserScopesResponse getUserScopes(@NonNull String userId) {
    checkArguments(userId);
    val response = resource.path(USERS_URL).path(userId).path(SCOPES_URL).get(ClientResponse.class);
    validateResponse(response);

    return response.getEntity(UserScopesResponse.class);
  }

  private static MultivaluedMap<String, String> createParameters(String userId, String scope, String description) {
    val params = new MultivaluedMapImpl();
    params.add(GRANT_TYPE_PARAM, PASSWORD_GRANT_TYPE);
    params.add(USERNAME_PARAM, userId);
    params.add(SCOPE_PARAM, scope);

    if (!isNullOrEmpty(description)) {
      if (description.length() > MAX_DESCRIPTION_LENGTH) {
        throw new BadRequestException(format("Token description length is more than %d characters.",
            MAX_DESCRIPTION_LENGTH));
      }

      params.add(DESCRIPTION_PARAM, description);
    }

    return params;
  }

  private static void validateResponse(ClientResponse response) {
    if (response.getClientResponseStatus() == FORBIDDEN) {
      throw new ForbiddenAccessException("Invalid Token Scope", "auth");
    } else if (response.getClientResponseStatus() == INTERNAL_SERVER_ERROR) {
      throw new BadGatewayException("Auth server experienced an error.");
    }

    checkState(response.getClientResponseStatus() == OK, "Expected a valid response. Got: %s",
        response.getClientResponseStatus());
  }

  private static ClientConfig getClientConfig(OAuthProperties config) {
    val cc = new DefaultClientConfig();
    cc.getFeatures().put(FEATURE_POJO_MAPPING, TRUE);
    cc.getClasses().add(JacksonJsonProvider.class);

    return configureSSLCertificatesHandling(cc, config);
  }

  @SneakyThrows
  private static ClientConfig configureSSLCertificatesHandling(ClientConfig config, OAuthProperties oauthConfig) {
    if (!oauthConfig.isEnableStrictSSL()) {
      log.debug("Setting up SSL context");
      val context = SSLContext.getInstance("TLS");
      context.init(null, new TrustManager[] { new DumbX509TrustManager() }, null);
      config.getProperties().put(PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(new DumbHostnameVerifier(), context));
    }

    return config;
  }

  private static void configureFilters(Client jerseyClient, OAuthProperties config) {
    jerseyClient.addFilter(new HTTPBasicAuthFilter(config.getClientId(), config.getClientSecret()));

    if (config.isEnableHttpLogging()) {
      jerseyClient.addFilter(new LoggingFilter());
    }
  }

  private static AccessToken convertToAccessToken(AccessTokenResponse token) {
    return new AccessToken(token.getId(), token.getDescription(), token.getExpiresIn(), convertScope(token.getScope()));
  }

  private static Set<String> convertScope(String scope) {
    return copyOf(Splitters.WHITESPACE.split(scope));
  }

  private static void checkArguments(String... args) {
    for (val arg : args) {
      checkArgument(!isNullOrEmpty(arg));
    }
  }

}
