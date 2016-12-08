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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.util.Joiners.WHITESPACE;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.icgc.dcc.portal.server.model.AccessToken;
import org.icgc.dcc.portal.server.model.AccessTokenScopes.AccessTokenScope;
import org.icgc.dcc.portal.server.model.User;
import org.icgc.dcc.portal.server.service.ForbiddenAccessException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableSet;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TokenServiceTest {

  /**
   * Scope supported by default. <br>
   * // TODO: Make sure scope is correctly resolved.
   */
  private static final Set<String> USER_SCOPES = ImmutableSet.of("aws.download", "aws.upload", "collab.download",
      "collab.upload", "id.create");
  private static final String USER_ID = "userId";
  private static final String TOKEN_ID = "123";
  private static final int EXPIRES = 10;

  @InjectMocks
  TokenService tokenService;

  @Mock
  OAuthClient client;

  @Test
  public void createTest_successful() {
    when(client.createToken(USER_ID, createScope(), "")).thenReturn(createAccessToken());
    when(client.getUserScopes(USER_ID)).thenReturn(createUserScopesInternal(USER_SCOPES));

    val result = tokenService.create(createUser(USER_ID, TRUE), createScope(), "");
    assertThat(result).isEqualTo(TOKEN_ID);
  }

  @Test(expected = ForbiddenAccessException.class)
  public void createTest_noDaco() {
    tokenService.create(createUser(USER_ID, FALSE), createScope(), "");
  }

  @Test
  public void deleteTest() {
    tokenService.delete(TOKEN_ID);
    verify(client).revokeToken(TOKEN_ID);
  }

  @Test
  public void listTest() {
    tokenService.list(createUser(USER_ID, TRUE));
    verify(client).listTokens(USER_ID);
  }

  @Test
  public void userScopesTest() {
    when(client.getUserScopes(USER_ID)).thenReturn(createUserScopesInternal(USER_SCOPES));
    val result = tokenService.getUserScopes(createUser(USER_ID, TRUE));
    val resultScopes = convertScopes(result.getScopes());
    log.info("{}", resultScopes);
    assertThat(resultScopes).containsOnlyElementsOf(USER_SCOPES);
  }

  private Set<String> convertScopes(Set<AccessTokenScope> scopes) {
    return scopes.stream()
        .map(s -> s.getName())
        .collect(toImmutableSet());
  }

  @Test
  public void userScopesTest_unrecognizedScope() {
    when(client.getUserScopes(USER_ID)).thenReturn(createUserScopesInternal(singleton("fake")));
    val result = tokenService.getUserScopes(createUser(USER_ID, TRUE));
    val resultScopes = convertScopes(result.getScopes());
    assertThat(resultScopes).containsExactly("fake");
  }

  private static UserScopesResponse createUserScopesInternal(Set<String> scopes) {
    return new UserScopesResponse(scopes);
  }

  private static User createUser(String userId, Boolean hasDaco) {
    val user = new User(USER_ID, null);
    user.setOpenIDIdentifier(USER_ID);
    user.setEmailAddress(USER_ID);
    user.setDaco(hasDaco);

    return user;
  }

  private static AccessToken createAccessToken() {
    return new AccessToken(TOKEN_ID, "", EXPIRES, USER_SCOPES);
  }

  private static String createScope() {
    return WHITESPACE.join(USER_SCOPES);
  }

}
