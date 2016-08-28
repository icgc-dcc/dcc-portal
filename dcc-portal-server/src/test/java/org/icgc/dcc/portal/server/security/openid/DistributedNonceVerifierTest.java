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
package org.icgc.dcc.portal.server.security.openid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.portal.server.security.openid.DistributedNonceVerifier.NONCE_CACHE_NAME;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openid4java.consumer.NonceVerifier.INVALID_TIMESTAMP;
import static org.openid4java.consumer.NonceVerifier.OK;
import static org.openid4java.consumer.NonceVerifier.SEEN;
import static org.openid4java.consumer.NonceVerifier.TOO_OLD;

import java.util.Date;

import org.icgc.dcc.portal.server.security.openid.DistributedNonceVerifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openid4java.util.InternetDateFormat;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

@RunWith(MockitoJUnitRunner.class)
public class DistributedNonceVerifierTest {

  private static final InternetDateFormat NONCE_PARSER = new InternetDateFormat();
  private static final int MAX_AGE_SECONDS = 60;
  private static final String URL = "http://test.org";
  private static final String VALID_NONCE = NONCE_PARSER.format(new Date());
  private static final String OLD_NONCE = "2005-05-15T17:11:51Z";
  private static final String INVALID_NONCE = "abc";
  private static final String VALID_KEY = URL + VALID_NONCE;

  private DistributedNonceVerifier nonceVerifier;

  @Mock
  Config config;

  @Mock
  HazelcastInstance hazelcast;

  @Mock
  MapConfig mapConfig;

  @Mock
  IMap<Object, Object> map;

  @Before
  public void setUp() {
    when(config.getMapConfig(NONCE_CACHE_NAME)).thenReturn(mapConfig);
    when(hazelcast.getConfig()).thenReturn(config);
    when(map.get(VALID_KEY)).thenReturn(null).thenReturn(Boolean.TRUE);
    when(hazelcast.getMap(NONCE_CACHE_NAME)).thenReturn(map);

    nonceVerifier = new DistributedNonceVerifier(hazelcast, MAX_AGE_SECONDS);
  }

  @Test
  public void getMaxAgeTest() {
    nonceVerifier.setMaxAge(MAX_AGE_SECONDS);
    assertThat(nonceVerifier.getMaxAge()).isEqualTo(MAX_AGE_SECONDS);
    verify(mapConfig).setTimeToLiveSeconds(MAX_AGE_SECONDS);
  }

  @Test
  public void seenTest() {
    // The assert order is important because of the setUp
    assertThat(nonceVerifier.seen(URL, OLD_NONCE)).isEqualTo(TOO_OLD);
    assertThat(nonceVerifier.seen(URL, VALID_NONCE)).isEqualTo(OK);
    verify(map).get(VALID_KEY);
    assertThat(nonceVerifier.seen(URL, VALID_NONCE)).isEqualTo(SEEN);
    assertThat(nonceVerifier.seen(URL, INVALID_NONCE)).isEqualTo(INVALID_TIMESTAMP);
  }

}
