package org.icgc.dcc.portal.test;

import java.io.File;
import java.io.IOException;

import org.icgc.dcc.portal.PortalMain;
import org.icgc.dcc.portal.config.PortalConfig;
import org.icgc.dcc.portal.config.PortalProperties;
import org.icgc.dcc.portal.config.RepositoryConfig;
import org.icgc.dcc.portal.config.SearchConfig;
import org.icgc.dcc.portal.test.AbstractSpringIntegrationTest.TestConfig;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.yammer.dropwizard.config.ConfigurationException;
import com.yammer.dropwizard.config.ConfigurationFactory;
import com.yammer.dropwizard.validation.Validator;

import lombok.val;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestConfig.class, PortalConfig.class, RepositoryConfig.class, SearchConfig.class })
public abstract class AbstractSpringIntegrationTest {

  @Configuration
  @ComponentScan(basePackageClasses = PortalMain.class)
  public static class TestConfig {

    @Bean
    public PortalProperties portalProperties() throws IOException, ConfigurationException {
      val factory = ConfigurationFactory.forClass(PortalProperties.class, new Validator());
      return factory.build(new File("src/test/conf/settings.yml"));
    }

  }

}
