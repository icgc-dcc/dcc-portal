package org.icgc.dcc.portal.server.config;

import java.io.File;

import org.icgc.dcc.portal.config.PortalProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.yammer.dropwizard.config.ConfigurationFactory;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jersey.JacksonMessageBodyProvider;
import com.yammer.dropwizard.json.ObjectMapperFactory;
import com.yammer.dropwizard.validation.Validator;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class DropwizardConfig extends WebMvcConfigurerAdapter {

  @Bean
  public Validator validator() {
    return new Validator();
  }

  @Bean
  public ObjectMapperFactory objectMapperFactory() {
    val factory = new ObjectMapperFactory();
    factory.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    return factory;
  }

  @Bean
  public JacksonMessageBodyProvider jacksonMessageBodyProvider() {
    return new JacksonMessageBodyProvider(objectMapperFactory().build(), validator());
  }

  @Bean
  @SneakyThrows
  public PortalProperties configuration(ApplicationArguments args) {
    if (args.getSourceArgs().length < 2) {
      log.error("Missing args. Expected: 'server path/to/settings.yml'");
      System.exit(1);
    }

    val settings = new File(args.getSourceArgs()[1]);
    if (!settings.exists()) {
      log.error("Settings file does not exist: {}", settings.getCanonicalPath());
      System.exit(1);
    }

    return ConfigurationFactory.forClass(PortalProperties.class, validator(), objectMapperFactory()).build(settings);
  }

  @Bean
  public Environment env(PortalProperties configuration, ObjectMapperFactory factory, Validator validator) {
    return new Environment("portal", configuration, factory, new Validator());
  }

}
