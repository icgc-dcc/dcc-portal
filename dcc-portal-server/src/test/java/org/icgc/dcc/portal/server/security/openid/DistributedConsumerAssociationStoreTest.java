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
import static org.icgc.dcc.portal.server.security.openid.DistributedConsumerAssociationStore.ASSOCIATIONS_CACHE_NAME;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import lombok.val;

import org.icgc.dcc.portal.server.security.openid.DistributedConsumerAssociationStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openid4java.association.Association;

import com.google.common.collect.Lists;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;

@RunWith(MockitoJUnitRunner.class)
public class DistributedConsumerAssociationStoreTest {

  private static final String OP_URL = "http://test.org";
  private static final String OLDER_ASSOCIATION_HANDLE = "abc";
  private static final String NEWER_ASSOCIATION_HANDLE = "xyz";

  @Mock
  HazelcastInstance hazelcast;

  @Mock
  MapConfig mapConfig;

  @Mock
  Config config;

  @Mock
  Association olderAssociation, newerAssociation;

  @Mock
  MultiMap<Object, Object> map;

  DistributedConsumerAssociationStore associationStore;

  @Before
  public void setUp() {
    when(hazelcast.getConfig()).thenReturn(config);
    when(hazelcast.getMultiMap(ASSOCIATIONS_CACHE_NAME)).thenReturn(map);
    when(config.getMapConfig(ASSOCIATIONS_CACHE_NAME)).thenReturn(mapConfig);

    setupAssociations();
    associationStore = new DistributedConsumerAssociationStore(hazelcast);
  }

  @Test
  public void loadTest() {
    assertThat(associationStore.load(OP_URL)).isEqualTo(newerAssociation);
    assertThat(associationStore.load(OP_URL, OLDER_ASSOCIATION_HANDLE)).isEqualTo(olderAssociation);
    assertThat(associationStore.load(OP_URL, NEWER_ASSOCIATION_HANDLE)).isEqualTo(newerAssociation);
    verify(map, times(3)).get(OP_URL);
  }

  @Test
  public void saveTest() {
    associationStore.save(OP_URL, olderAssociation);
    verify(map).put(OP_URL, olderAssociation);
  }

  @Test
  public void removeTest() {
    associationStore.remove(OP_URL, OLDER_ASSOCIATION_HANDLE);
    verify(map).remove(OP_URL, olderAssociation);
  }

  private void setupAssociations() {
    when(olderAssociation.getExpiry()).thenReturn(new Date(0L));
    when(olderAssociation.getHandle()).thenReturn(OLDER_ASSOCIATION_HANDLE);

    when(newerAssociation.getExpiry()).thenReturn(new Date(1000L));
    when(newerAssociation.getHandle()).thenReturn(NEWER_ASSOCIATION_HANDLE);

    when(map.get(OP_URL)).thenReturn(getAssociationsList());
  }

  private List<Object> getAssociationsList() {
    val list = Lists.newArrayList();
    list.add(olderAssociation);
    list.add(newerAssociation);

    return list;
  }

}
