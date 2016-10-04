package org.icgc.dcc.portal.server.health;

import static org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus.RED;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ElasticsearchHealthIndicator extends AbstractHealthIndicator {

  /**
   * Dependencies
   */
  private final Client client;

  @Override
  protected void doHealthCheck(Builder builder) throws Exception {
    log.info("Checking the health of ElasticSearch...");
    if (client == null) {
      builder.down().withDetail("message", "Service missing").build();
      return;
    }

    val status = getStatus();
    if (status == RED) {
      builder.down().withDetail("message", String.format("Cluster health status is %s", status.name())).build();
      return;
    }

    builder.up().withDetail("message", String.format("Cluster health status is %s", status.name())).build();
  }

  private ClusterHealthStatus getStatus() {
    return client.admin().cluster().prepareHealth().execute().actionGet().getStatus();
  }

}