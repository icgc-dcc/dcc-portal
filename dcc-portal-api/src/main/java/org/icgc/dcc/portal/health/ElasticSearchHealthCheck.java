/*
 * Copyright 2015(c) The Ontario Institute for Cancer Research. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.health;

import static org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus.RED;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.yammer.metrics.core.HealthCheck;

@Slf4j
@Component
public final class ElasticSearchHealthCheck extends HealthCheck {

  /**
   * Constants.
   */
  private static final String CHECK_NAME = "elasticsearch";

  /**
   * Dependencies
   */
  private final Client client;

  @Autowired
  public ElasticSearchHealthCheck(Client client) {
    super(CHECK_NAME);
    this.client = client;
  }

  @Override
  protected Result check() throws Exception {
    log.info("Checking the health of ElasticSearch...");
    if (client == null) {
      return Result.unhealthy("Service missing");
    }

    val status = getStatus();
    if (status == RED) {
      return Result.unhealthy("Cluster health status is %s", status.name());
    }

    return Result.healthy("Cluster health status is %s", status.name());
  }

  private ClusterHealthStatus getStatus() {
    return client.admin().cluster().prepareHealth().execute().actionGet().getStatus();
  }

}
