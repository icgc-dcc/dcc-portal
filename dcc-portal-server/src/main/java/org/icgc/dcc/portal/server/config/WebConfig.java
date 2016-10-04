/*
 * Copyright 2016(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.portal.server.config;

import static com.sun.jersey.api.container.filter.LoggingFilter.FEATURE_LOGGING_DISABLE_ENTITY;
import static com.sun.jersey.api.core.ResourceConfig.FEATURE_DISABLE_WADL;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.icgc.dcc.portal.server.controller.IndexController;
import org.icgc.dcc.portal.server.jersey.filter.CachingFilter;
import org.icgc.dcc.portal.server.jersey.filter.CrossOriginFilter;
import org.icgc.dcc.portal.server.jersey.filter.DownloadFilter;
import org.icgc.dcc.portal.server.jersey.filter.VersionFilter;
import org.icgc.dcc.portal.server.resource.Resource;
import org.icgc.dcc.portal.server.spring.SpringComponentProviderFactory;
import org.icgc.dcc.portal.server.swagger.PrimitiveModelResolver;
import org.icgc.dcc.portal.server.util.VersionUtils;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.sun.jersey.api.container.filter.LoggingFilter;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.inject.InjectableProvider;
import com.yammer.metrics.jersey.InstrumentedResourceMethodDispatchAdapter;

import io.swagger.converter.ModelConverters;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

  /**
   * @see {@link IndexController} for dynamic {@code /index.html} generation.
   */
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/app/");
    registry.addResourceHandler("/docs/**").addResourceLocations("classpath:/swagger-ui/");
    registry.addResourceHandler("/**").addResourceLocations("classpath:/app/");
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/").setViewName("forward:/index.html");
    registry.addViewController("/docs").setViewName("forward:/docs/index.html");
    registry.addViewController("/**/{path:[^.]+}").setViewName("forward:/index.html");
  }

  @Bean
  public ServletRegistrationBean jersey(ResourceConfig resourceConfig) {
    val jersey = new ServletRegistrationBean();
    jersey.setOrder(1);
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

    val singletons = config.getSingletons();
    singletons.addAll(resources);
    singletons.addAll(mappers);
    singletons.addAll(injectables);
    singletons.addAll(readers);
    singletons.addAll(writers);
    singletons.add(new ApiListingResource());
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

  @PostConstruct
  public void init() {
    val watch = Stopwatch.createStarted();
    log.info("[init] Initializing Swagger...");

    // Add converters
    ModelConverters.getInstance().addConverter(new PrimitiveModelResolver());

    // Configure and scan
    val config = new BeanConfig();

    config.setTitle("ICGC Data Portal API");
    config.setVersion(VersionUtils.getApiVersion());
    config.setResourcePackage(
        Resource.class.getPackage().getName() + "," + PrimitiveModelResolver.class.getPackage().getName());
    config.setBasePath("/api");
    config.setScan(true);

    log.info("[init] Finished initializing Swagger in {}", watch);
  }

}
