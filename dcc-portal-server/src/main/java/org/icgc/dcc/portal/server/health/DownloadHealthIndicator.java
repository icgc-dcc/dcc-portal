package org.icgc.dcc.portal.server.health;

import org.icgc.dcc.download.client.DownloadClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DownloadHealthIndicator implements HealthIndicator {

  /**
   * Dependencies
   */
  private final DownloadClient downloader;
  
  @Override
  public Health health() {
    log.info("Checking the health of Downloader...");
    if (downloader == null) {
      return Health.down().withDetail("message", "Service missing").build();
    }

    if (!downloader.isServiceAvailable()) {
      return Health.down().withDetail("message", "Service unavailable").build();
    }

    return  Health.up().withDetail("message", "Service is available").build();
  }

}