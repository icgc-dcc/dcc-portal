package org.icgc.dcc.portal.server.health;

import static org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus.RED;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ElasticsearchHealthIndicator implements HealthIndicator {

  /**
   * Dependencies
   */
  private final Client client;

  @Override
  public Health health() {
    log.info("Checking the health of ElasticSearch...");
    if (client == null) {
      return Health.down().withDetail("message", "Service missing").build();
    }

    val status = getStatus();
    if (status == RED) {
      return Health.down().withDetail("message", String.format("Cluster health status is %s", status.name())).build();
    }

    return  Health.up().withDetail("message", String.format("Cluster health status is %s", status.name())).build();
  }

  private ClusterHealthStatus getStatus() {
    return client.admin().cluster().prepareHealth().execute().actionGet().getStatus();
  }

}