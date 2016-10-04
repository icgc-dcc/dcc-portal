package org.icgc.dcc.portal.server.health;

import org.icgc.dcc.download.client.DownloadClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DownloadHealthIndicator extends AbstractHealthIndicator {

  /**
   * Dependencies
   */
  private final DownloadClient client;

  @Override
  protected void doHealthCheck(Builder builder) throws Exception {
    log.info("Checking the health of Downloader...");
    if (client == null) {
      builder.down().withDetail("message", "Service missing");
      return;
    }

    if (!client.isServiceAvailable()) {
      builder.down().withDetail("message", "Service unavailable");
      return;
    }

    builder.up().withDetail("message", "Service is available");
  }

}