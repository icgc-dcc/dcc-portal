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

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import lombok.val;

import org.icgc.dcc.common.core.util.Splitters;
import org.icgc.dcc.portal.server.config.ServerProperties;
import org.icgc.dcc.portal.server.config.ServerProperties.OAuthProperties;
import org.icgc.dcc.portal.server.model.AccessToken;
import org.icgc.dcc.portal.server.security.oauth.OAuthClient;
import org.icgc.dcc.portal.server.service.BadRequestException;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Strings;

public class OAuthClientIntegrationTest {

  private static final String SERVICE_URL = "http://localhost:8443";
  private static final String USERNAME = "fakeId@a.com";
  private static final String SCOPES = "os.download os.upload";

  OAuthClient client;

  @Before
  public void setUp() {
    this.client = new OAuthClient(createOAuthProperties());
  }

  @Test
  public void createTokenTest() {
    val result = client.createToken(USERNAME, SCOPES);
    validateToken(result, parseScopes(SCOPES));
    client.revokeToken(result.getId());
  }

  private Set<String> parseScopes(String scopes) {
    return newHashSet(Splitters.WHITESPACE.split(scopes));
  }

  @Test
  public void createTokenTest_withDescription() {
    val result = client.createToken(USERNAME, SCOPES, "Description");
    assertThat(result.getDescription()).isEqualTo("Description");
    client.revokeToken(result.getId());
  }

  @Test(expected = BadRequestException.class)
  public void createTokenTest_longDescription() {
    client.createToken(USERNAME, SCOPES, Strings.repeat("a", 201));
  }

  @Test
  public void listTokens() {
    client.createToken(USERNAME, SCOPES);
    val result = client.listTokens(USERNAME);
    assertThat(result.getTokens()).hasSize(1);
  }

  @Test
  public void revokeTest() {
    val token = client.createToken(USERNAME, SCOPES);
    client.revokeToken(token.getId());

    for (val tok : client.listTokens(USERNAME).getTokens()) {
      assertThat(tok).isNotEqualTo(token);
    }
  }

  @Test
  public void getUserScopesTest() {
    val scopes = client.getUserScopes(USERNAME);
    assertThat(scopes.getScopes()).containsOnlyElementsOf(parseScopes(SCOPES));
  }

  private static void validateToken(AccessToken token, Set<String> scope) {
    assertThat(token.getId()).isNotEmpty();
    assertThat(token.getExpiresIn()).isGreaterThan(1);
    assertThat(token.getScope()).isEqualTo(scope);
  }

  private static OAuthProperties createOAuthProperties() {
    val result = new ServerProperties.OAuthProperties();
    result.setServiceUrl(SERVICE_URL);
    result.setClientId("mgmt");
    result.setClientSecret("pass");
    result.setEnableStrictSSL(false);
    result.setEnableHttpLogging(true);

    return result;
  }

}
