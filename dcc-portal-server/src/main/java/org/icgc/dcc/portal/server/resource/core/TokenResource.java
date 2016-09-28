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
package org.icgc.dcc.portal.server.resource.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.hibernate.validator.constraints.NotEmpty;
import org.icgc.dcc.portal.server.model.AccessToken;
import org.icgc.dcc.portal.server.model.AccessTokenScopes;
import org.icgc.dcc.portal.server.model.Tokens;
import org.icgc.dcc.portal.server.model.User;
import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.security.jersey.Auth;
import org.icgc.dcc.portal.server.security.oauth.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.swagger.annotations.Api;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * This endpoint is used to manage access tokens handled by the
 * <a href="https://github.com/icgc-dcc/dcc-auth">Authentication Server</a>
 */
@Component
@Api("/settings")
@Path("/v1/settings/tokens")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TokenResource extends Resource {

  /**
   * Dependencies.
   */
  @NonNull
  private final TokenService tokenService;

  @POST
  @Produces(TEXT_PLAIN)
  public String create(
      @Auth(required = true) User user,
      @Valid @NotEmpty @FormParam("scope") String scope,
      @Valid @Size(min = 0, max = 200) @FormParam("desc") String description) {
    checkArgument(!isNullOrEmpty(scope), "scope is null or empty");

    return tokenService.create(user, scope, description);
  }

  @GET
  @Produces(APPLICATION_JSON)
  public Tokens list(@Auth(required = true) User user) {
    return tokenService.list(user);
  }

  @GET
  @Path("/{tokenId}")
  @Produces(APPLICATION_JSON)
  public AccessToken get(@PathParam("tokenId") String tokenId) {
    return tokenService.getToken(tokenId);
  }

  @DELETE
  @Path("/{tokenId}")
  public Response delete(@Auth(required = true) User user, @PathParam("tokenId") String tokenId) {
    checkArgument(!isNullOrEmpty(tokenId), "tokenId is null or empty");
    tokenService.delete(tokenId);

    return Response.ok().build();
  }

  @GET
  @Path("/scopes")
  @Produces(APPLICATION_JSON)
  public AccessTokenScopes scopes(@Auth(required = true) User user) {
    return tokenService.getUserScopes(user);
  }

}
