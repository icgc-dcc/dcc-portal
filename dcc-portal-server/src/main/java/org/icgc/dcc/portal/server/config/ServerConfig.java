package org.icgc.dcc.portal.server.config;

import static com.sun.jersey.api.container.filter.LoggingFilter.FEATURE_LOGGING_DISABLE_ENTITY;
import static com.sun.jersey.api.core.ResourceConfig.FEATURE_DISABLE_WADL;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS;

import java.io.File;
import java.util.Set;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.icgc.dcc.portal.filter.CachingFilter;
import org.icgc.dcc.portal.filter.CrossOriginFilter;
import org.icgc.dcc.portal.filter.DownloadFilter;
import org.icgc.dcc.portal.filter.VersionFilter;
import org.icgc.dcc.portal.resource.Resource;
import org.icgc.dcc.portal.spring.SpringComponentProviderFactory;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import com.google.common.collect.ImmutableList;
import com.sun.jersey.api.container.filter.LoggingFilter;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.inject.InjectableProvider;
import com.yammer.dropwizard.jersey.InvalidEntityExceptionMapper;
import com.yammer.dropwizard.jersey.OptionalQueryParamInjectableProvider;
import com.yammer.dropwizard.jersey.OptionalResourceMethodDispatchAdapter;
import com.yammer.dropwizard.jersey.caching.CacheControlledResourceMethodDispatchAdapter;
import com.yammer.metrics.jersey.InstrumentedResourceMethodDispatchAdapter;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableWebMvc
@Configuration
public class ServerConfig extends WebMvcConfigurerAdapter {

  @Override
  @SneakyThrows
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/**").addResourceLocations(getResourceLocation());
  }

  @Bean
  public FilterRegistrationBean urlRewrite() {
    val urlRewrite = new FilterRegistrationBean();
    urlRewrite.setFilter(new UrlRewriteFilter());
    urlRewrite.addInitParameter("confPath", "urlrewrite.xml");
    urlRewrite.addInitParameter("statusEnabled", "false");

    return urlRewrite;
  }

  @Bean
  public ServletRegistrationBean jersey(ResourceConfig resourceConfig) {
    val jersey = new ServletRegistrationBean();
    jersey.setServlet(new ServletContainer(resourceConfig));
    jersey.addUrlMappings("/api/*");

    return jersey;
  }

  @Bean
  public ResourceConfig resourceConfig(GenericApplicationContext context, 
      Set<Resource> resources, 
      Set<ExceptionMapper<?>> mappers, 
      Set<InjectableProvider<?, ?>> injectables,
      Set<MessageBodyReader<?>> readers, 
      Set<MessageBodyWriter<?>> writers) {
    for (val resource : resources) {
      log.info(" - Registering resource: {}", resource.getClass().getSimpleName());
    }
    for (val mapper : mappers) {
      log.info(" - Registering mapper: {}", mapper.getClass().getSimpleName());
    }
    for (val injectable : injectables) {
      log.info(" - Registering injectable: {}", injectable.getClass().getSimpleName());
    }
    for (val reader : readers) {
      log.info(" - Registering reader: {}", reader.getClass().getSimpleName());
    }
    for (val writer : writers) {
      log.info(" - Registering writer: {}", writer.getClass().getSimpleName());
    }

    val config = new DefaultResourceConfig();
    config.getFeatures().put(FEATURE_DISABLE_WADL, Boolean.TRUE);
    val classes = config.getClasses();
    classes.add(InstrumentedResourceMethodDispatchAdapter.class);
    classes.add(CacheControlledResourceMethodDispatchAdapter.class);
    classes.add(OptionalResourceMethodDispatchAdapter.class);
    classes.add(OptionalQueryParamInjectableProvider.class);
    
    val singletons = config.getSingletons();
    singletons.addAll(resources);
    singletons.addAll(mappers);
    singletons.addAll(injectables);
    singletons.addAll(readers);
    singletons.addAll(writers);
    singletons.add(new InvalidEntityExceptionMapper());
    singletons.add(new SpringComponentProviderFactory(config, context));

    // Don't log entities because our JSONs can be huge
    config.getFeatures().put(FEATURE_LOGGING_DISABLE_ENTITY, true);

    config.getProperties().put(PROPERTY_CONTAINER_REQUEST_FILTERS,
        ImmutableList.of(LoggingFilter.class.getName(),
            DownloadFilter.class.getName(),
            CachingFilter.class.getName()));
    config.getProperties().put(PROPERTY_CONTAINER_RESPONSE_FILTERS,
        ImmutableList.of(LoggingFilter.class.getName(),
            VersionFilter.class.getName(),
            CrossOriginFilter.class.getName(),
            CachingFilter.class.getName()));

    return config;
  }

  @SneakyThrows
  private static String getResourceLocation() {
    val url = ServerConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    if (url.toString().contains("jar")) {
      return "classpath:/app/";
    } else {
      return "file:" + new File("../dcc-portal-ui/app").getCanonicalPath() + "/";
    }
  }

}
