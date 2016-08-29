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
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.Date;

import lombok.NonNull;
import lombok.val;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.openid4java.association.Association;
import org.openid4java.consumer.ConsumerAssociationStore;

import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;

/**
 * Implements ConsumerAssociationStore using Hazelcast as persistence storage. Will be injected in ConsumerManager
 */
public class DistributedConsumerAssociationStore implements ConsumerAssociationStore {

  public static final String ASSOCIATIONS_CACHE_NAME = "sharedAssociations";
  private static final int EXTRA_TTL_TIME_SECONDS = 60;

  private final MultiMap<String, Association> associations;
  private final MapConfig mapConfig;

  public DistributedConsumerAssociationStore(@NonNull HazelcastInstance hazelcast) {
    this.associations = hazelcast.getMultiMap(ASSOCIATIONS_CACHE_NAME);
    this.mapConfig = hazelcast.getConfig().getMapConfig(ASSOCIATIONS_CACHE_NAME);
  }

  @Override
  public void save(String opUrl, @NonNull Association association) {
    checkOpUrlArgument(opUrl);
    configureRetentionPolicy(association.getExpiry());

    associations.put(opUrl, association);
  }

  @Override
  public Association load(String opUrl, String handle) {
    checkOpUrlArgument(opUrl);
    checkHandleArgument(handle);

    return getValue(opUrl, handle);
  }

  @Override
  public Association load(String opUrl) {
    checkOpUrlArgument(opUrl);

    return getValueByMaxExpiryDate(opUrl);
  }

  private Association getValueByMaxExpiryDate(String opUrl) {
    // Initialize to something older than any association date could be
    Date maxExpiryDate = new Date(0L);
    Association result = null;

    for (val association : associations.get(opUrl)) {
      val expiryDate = association.getExpiry();
      if (expiryDate.after(maxExpiryDate)) {
        maxExpiryDate = expiryDate;
        result = association;
      }
    }

    return result;
  }

  @Override
  public void remove(String opUrl, String handle) {
    checkOpUrlArgument(opUrl);
    checkHandleArgument(handle);

    associations.remove(opUrl, getValue(opUrl, handle));
  }

  private Association getValue(String opUrl, String handle) {
    for (val association : associations.get(opUrl)) {
      if (association.getHandle().equals(handle)) return association;
    }

    return null;
  }

  private static void checkOpUrlArgument(String opUrl) {
    checkArgument(!isNullOrEmpty(opUrl), "OpenId Provider's URL can't be null or empty");
  }

  private static void checkHandleArgument(String handle) {
    checkArgument(!isNullOrEmpty(handle), "Association handle can't be null or empty");
  }

  /**
   * Sets association time-to-live(TTL) to maximum of all received so far plus <tt>EXTRA_TTL_TIME</tt>.
   */
  private void configureRetentionPolicy(Date expiryDate) {
    val newExpiryDate = Seconds.secondsBetween(new DateTime(expiryDate), new DateTime()).getSeconds();
    if (newExpiryDate > mapConfig.getTimeToLiveSeconds()) {
      mapConfig.setTimeToLiveSeconds(newExpiryDate + EXTRA_TTL_TIME_SECONDS);
    }
  }

}
