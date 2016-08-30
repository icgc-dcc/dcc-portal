package org.icgc.dcc.portal.server.security.openid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;

import org.icgc.dcc.portal.server.security.AuthService;
import org.icgc.dcc.portal.server.security.openid.OpenIDAuthService;
import org.icgc.dcc.portal.server.service.SessionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;

import com.google.common.collect.ImmutableList;

import lombok.val;

@RunWith(MockitoJUnitRunner.class)
public class OpenIDServiceTest {

  /**
   * Test data.
   */
  private static final String OPENID_IDENTIFIER = "https://me.yahoo.com";
  private static final String OP_ENDPOINT = "https://open.login.yahooapis.com/openid/op/auth";

  /**
   * Dependencies.
   */
  @Mock
  private SessionService sessionService;
  @Mock
  private ConsumerManager consumerManager;
  @Mock
  private AuthService authService;

  /**
   * Class under test.
   */
  @InjectMocks
  private OpenIDAuthService openidAuthService;

  @Before
  public void setUp() throws Exception {
    val discoveryInfo = new DiscoveryInformation(new URL(OP_ENDPOINT));
    val discoveries = ImmutableList.<Object> of();
    when(consumerManager.discover(OPENID_IDENTIFIER)).thenReturn(discoveries);
    when(consumerManager.associate(discoveries)).thenReturn(discoveryInfo);
    when(consumerManager.authenticate(eq(discoveryInfo), anyString())).thenReturn(mock(AuthRequest.class));
  }

  @Test
  public void testCreateAuthRequest() throws Exception {
    val serverName = "server";
    val serverPort = 0;
    val identifier = "id";
    val currentUrl = "http://localhost";

    val request = openidAuthService.createAuthRequest(serverName, serverPort, identifier, currentUrl);

    // TODO: This really isn't testing anything. We should add better assertions and exercising of logic.
    assertThat(request).isNotNull();
  }

}
