package org.icgc.dcc.portal.server.security.oauth;

import lombok.val;
import org.icgc.dcc.common.core.util.Splitters;
import org.icgc.dcc.portal.server.config.ServerProperties;
import org.icgc.dcc.portal.server.model.AccessToken;
import org.icgc.dcc.portal.server.model.Tokens;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class EgoIntegrationTest {

  private static final String SERVICE_URL = "http://localhost:8081";
  private static final String AUTH = "88f90598-fd85-4e91-b049-041ce44ed604";
  private static final String USER_ID = "9b0cb230-24de-47b4-a92a-5cc67438ed45";
  private static final String SCOPES = "Policy001.READ Policy002.READ";
  private static final String DESCRIPTION = "Test Ego Token";

  private OAuthClient client;

  @Before
  public void setUp() {
    this.client = new OAuthClient(createOAuthProperties());
  }

  @Test
  public void createToken() {
    AccessToken result = client.createEgoToken(UUID.fromString(USER_ID), SCOPES, DESCRIPTION);
    validateToken(result, parseScopes(SCOPES));
    client.revokeEgoToken(result.getId());
  }

  @Test
  public void checkToken(){
    val tokenName = "841e00ae-55d3-4f74-91fa-9cc1a5a68b52";
    val result = client.checkEgoToken(tokenName, SCOPES);
    assertThat(result).isEqualTo(true);
  }

  @Test
  public void revokeToken(){
    val tokenName = "841e00ae-55d3-4f74-91fa-9cc1a5a68b52";
    client.revokeEgoToken(tokenName);
  }

  @Test
  public void listToken(){
    Tokens result = client.listEgoTokens(USER_ID);
    assertThat(result.getTokens().size()).isGreaterThan(1);
  }

  private Set<String> parseScopes(String scopes) {
    return newHashSet(Splitters.WHITESPACE.split(scopes));
  }

  private static void validateToken(AccessToken token, Set<String> scope) {
    assertThat(token.getId()).isNotEmpty();
    assertThat(token.getExpiresIn()).isGreaterThan(1);
    assertThat(token.getScope()).isEqualTo(scope);
    assertThat(token.getDescription()).isEqualTo(DESCRIPTION);
  }

  private static ServerProperties.OAuthProperties createOAuthProperties() {
    val result = new ServerProperties.OAuthProperties();
    result.setServiceUrl(SERVICE_URL);
    result.setClientId("sample-data-portal");
    result.setClientSecret("sample-data-portal-secret");
    result.setEnableStrictSSL(false);
    result.setEnableHttpLogging(true);
    return result;
  }

}
