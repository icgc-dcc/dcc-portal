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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import org.openid4java.consumer.NonceVerifier;
import org.openid4java.util.InternetDateFormat;

import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;

import lombok.val;

/**
 * Implements NonceVerifier using Hazelcast as persistence storage. Will be injected in ConsumerManager
 */
public class DistributedNonceVerifier implements NonceVerifier {

  public static final String NONCE_CACHE_NAME = "sharedNonces";
  private static final int DEFAULT_MAX_AGE_SECONDS = 60;

  // Can't use Set because Hazelcast's implementation does not support eviction policy on Sets
  private final Map<String, Boolean> nonces;
  private final InternetDateFormat nonceParser = new InternetDateFormat();

  // Max age of nonce in seconds.
  private int maxAgeSeconds;
  private final MapConfig mapConfig;

  public DistributedNonceVerifier(HazelcastInstance hazelcast) {
    this(hazelcast, DEFAULT_MAX_AGE_SECONDS);
  }

  public DistributedNonceVerifier(HazelcastInstance hazelcast, int maxAge) {
    this.nonces = hazelcast.getMap(NONCE_CACHE_NAME);
    this.mapConfig = hazelcast.getConfig().getMapConfig(NONCE_CACHE_NAME);
    this.maxAgeSeconds = maxAge;
  }

  @Override
  public int seen(String opUrl, String nonce) {
    checkArgument(!isNullOrEmpty(opUrl), "OpenId Provider's URL can't be null or empty");
    checkArgument(!isNullOrEmpty(nonce), "Nonce can't be null or empty");

    try {
      checkMaxAge(nonce);
    } catch (ParseException e) {
      e.printStackTrace();

      return INVALID_TIMESTAMP;
    } catch (IllegalStateException e) {
      return TOO_OLD;
    }

    val key = opUrl + nonce;
    val hasNonce = nonces.get(key);
    if (hasNonce == null) {
      nonces.put(key, Boolean.TRUE);

      return OK;
    } else {
      return SEEN;
    }
  }

  private void checkMaxAge(String nonce) throws ParseException {
    val nonceAge = new Date().getTime() - nonceParser.parse(nonce).getTime();
    checkState(nonceAge < maxAgeSeconds * 1000);
  }

  @Override
  public int getMaxAge() {
    return maxAgeSeconds;
  }

  @Override
  public void setMaxAge(int maxAge) {
    this.maxAgeSeconds = maxAge;
    updateRetentionPolicy();
  }

  private void updateRetentionPolicy() {
    mapConfig.setTimeToLiveSeconds(maxAgeSeconds);
  }

}