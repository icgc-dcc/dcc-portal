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
package org.icgc.dcc.portal.server.test;

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.GroupProperties;
import com.hazelcast.instance.HazelcastInstanceFactory;

@NoArgsConstructor(access = PRIVATE)
public final class HazelcastFactory {

  /**
   * Creates an instance of Hazelcast that does not communicate over the network.
   */
  public static HazelcastInstance createLocalHazelcastInstance() {
    return HazelcastInstanceFactory.newHazelcastInstance(initConfig(new Config()));
  }

  private static Config initConfig(Config config) {
    config.setProperty(GroupProperties.PROP_WAIT_SECONDS_BEFORE_JOIN, "0");
    config.setProperty(GroupProperties.PROP_GRACEFUL_SHUTDOWN_MAX_WAIT, "120");
    config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);

    return config;
  }

}
