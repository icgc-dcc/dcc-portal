package org.icgc.dcc.portal.server.test;

import java.io.IOException;

import org.icgc.dcc.portal.server.config.PortalConfig;
import org.icgc.dcc.portal.server.config.PortalProperties;
import org.icgc.dcc.portal.server.config.RepositoryConfig;
import org.icgc.dcc.portal.server.config.SearchConfig;
import org.icgc.dcc.portal.server.test.AbstractSpringIntegrationTest.TestConfig;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestConfig.class, PortalConfig.class, RepositoryConfig.class, SearchConfig.class })
public abstract class AbstractSpringIntegrationTest {

  @Configuration
  @ComponentScan(basePackages = "org.icgc.dcc.portal.server")
  public static class TestConfig {

    @Bean
    public PortalProperties portalProperties() throws IOException {
      return new PortalProperties();
    }

  }

}
