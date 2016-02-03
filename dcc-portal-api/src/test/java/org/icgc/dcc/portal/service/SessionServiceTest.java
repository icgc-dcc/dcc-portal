package org.icgc.dcc.portal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.portal.service.SessionService.DISCOVERY_INFO_CACHE_NAME;

import java.net.URL;
import java.util.Map;
import java.util.UUID;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.portal.model.User;
import org.icgc.dcc.portal.test.HazelcastFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;

import com.hazelcast.core.HazelcastInstance;

public class SessionServiceTest {

  private static final String OPEN_ID_IDENTIFIER = "openIDIdentifier";
  private static final String EMAIL_ADDRESS = "test@email.com";

  private final UUID sessionToken = UUID.randomUUID();
  private final User user = new User(null, sessionToken);

  private DiscoveryInformation discoveryInfo = getDiscoveryInfo();
  private SessionService sessionService;
  private HazelcastInstance hazelcast;

  @Before
  public void setUp() throws Exception {
    hazelcast = HazelcastFactory.createLocalHazelcastInstance();
    Map<UUID, User> usersCache = hazelcast.getMap(SessionService.USERS_CACHE_NAME);
    Map<UUID, DiscoveryInformation> discoveryInfoCache = hazelcast.getMap(DISCOVERY_INFO_CACHE_NAME);

    sessionService = new SessionService(usersCache, discoveryInfoCache);
    sessionService.putUser(sessionToken, user);
    sessionService.putDiscoveryInfo(sessionToken, discoveryInfo);
  }

  @After
  public void tearDown() throws Exception {
    hazelcast.shutdown();
  }

  @Test
  public void testGetBySessionToken() throws Exception {
    val userOptional = sessionService.getUserBySessionToken(sessionToken);

    assertThat(userOptional.isPresent()).isEqualTo(true);
    assertThat(userOptional.get()).isEqualTo(user);
  }

  @Test
  public void testGetBySessionTokenInvalid() throws Exception {
    assertThat(sessionService.getUserBySessionToken(UUID.randomUUID()).isPresent()).isEqualTo(false);
  }

  @Test
  public void testRemoveUser() throws Exception {
    sessionService.removeUser(user);
    assertThat(sessionService.getUserBySessionToken(sessionToken).isPresent()).isEqualTo(false);
  }

  @Test
  public void testGetUserByOpenidIdentifierFound() throws Exception {
    user.setOpenIDIdentifier(OPEN_ID_IDENTIFIER);

    // must be pushed otherwise local changes won't be reflected in the cluster
    sessionService.putUser(sessionToken, user);
    val userOptional = sessionService.getUserByOpenidIdentifier(OPEN_ID_IDENTIFIER);

    assertThat(userOptional.isPresent()).isEqualTo(true);
    assertThat(userOptional.get()).isEqualTo(user);
  }

  @Test
  public void testGetUserByOpenidIdentifierNotFound() throws Exception {
    assertThat(sessionService.getUserByOpenidIdentifier(OPEN_ID_IDENTIFIER).isPresent()).isEqualTo(false);
  }

  @Test
  public void testGetUserByEmailFound() throws Exception {
    user.setEmailAddress(EMAIL_ADDRESS);
    sessionService.putUser(sessionToken, user);
    val userOptional = sessionService.getUserByEmail(EMAIL_ADDRESS);

    assertThat(userOptional.isPresent()).isEqualTo(true);
    assertThat(userOptional.get()).isEqualTo(user);
  }

  @Test
  public void testGetUserByEmailNotFound() throws Exception {
    assertThat(sessionService.getUserByEmail(EMAIL_ADDRESS).isPresent()).isEqualTo(false);
  }

  @Test
  public void testGetDiscoveryInfoFound() {
    val discoveryInfoOptional = sessionService.getDiscoveryInfo(sessionToken);

    assertThat(discoveryInfoOptional.isPresent()).isEqualTo(true);
    assertThat(discoveryInfoOptional.get()).isEqualTo(discoveryInfo);
  }

  @Test
  public void testGetDiscoveryInfoNotFound() {
    assertThat(sessionService.getDiscoveryInfo(UUID.randomUUID()).isPresent()).isEqualTo(false);
  }

  @Test
  public void testRemoveDiscoveryInfo() {
    assertThat(sessionService.getDiscoveryInfo(sessionToken).isPresent()).isEqualTo(true);
    sessionService.removeDiscoveryInfo(sessionToken);
    assertThat(sessionService.getDiscoveryInfo(UUID.randomUUID()).isPresent()).isEqualTo(false);
  }

  @SneakyThrows
  private static DiscoveryInformation getDiscoveryInfo() {
    // DiscoveryInformation does not override equals. All objects retrieved from hazelcast do not point to the same
    // local objects
    return new DiscoveryInfoMock(new URL("http://test.org"));
  }

  @EqualsAndHashCode(callSuper = false)
  public static class DiscoveryInfoMock extends DiscoveryInformation {

    public DiscoveryInfoMock(URL opEndpoint) throws DiscoveryException {
      super(opEndpoint);
    }

  }

}
