/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
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

package org.icgc.dcc.portal;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Strings.padEnd;
import static com.sun.jersey.api.container.filter.LoggingFilter.FEATURE_LOGGING_DISABLE_ENTITY;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS;
import static com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.eclipse.jetty.util.resource.JarResource.newJarResource;
import static org.eclipse.jetty.util.resource.Resource.newClassPathResource;
import static org.eclipse.jetty.util.resource.Resource.newResource;
import static org.icgc.dcc.portal.util.DropwizardUtils.removeDwExceptionMapper;
import static org.icgc.dcc.portal.util.ListUtils.list;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.jetty.util.resource.Resource;
import org.icgc.dcc.portal.bundle.SwaggerBundle;
import org.icgc.dcc.portal.config.PortalProperties;
import org.icgc.dcc.portal.filter.CachingFilter;
import org.icgc.dcc.portal.filter.CrossOriginFilter;
import org.icgc.dcc.portal.filter.DownloadFilter;
import org.icgc.dcc.portal.filter.VersionFilter;
import org.icgc.dcc.portal.spring.SpringService;
import org.icgc.dcc.portal.util.VersionUtils;
import org.icgc.dcc.portal.writer.ErrorMessageBodyWriter;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import com.sun.jersey.api.container.filter.LoggingFilter;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.jersey.LoggingExceptionMapper;

@Slf4j
public class PortalMain extends SpringService<PortalProperties> {

  /**
   * Constants.
   */
  private static final String APPLICATION_NAME = "dcc-portal-api";

  public static void main(String... args) throws Exception {
    new PortalMain().run(args);
  }

  @Override
  public void initialize(Bootstrap<PortalProperties> bootstrap) {
    super.initialize(bootstrap);

    bootstrap.setName(APPLICATION_NAME);
    bootstrap.addBundle(new SwaggerBundle());
    bootstrap.addBundle(new AssetsBundle("/app/", "/", "index.html"));
  }

  @Override
  public void run(PortalProperties config, Environment environment) throws Exception {
    environment.setBaseResource(getBaseResource());

    environment.addFilter(UrlRewriteFilter.class, "/*")
        .setInitParam("confPath", "urlrewrite.xml")
        .setInitParam("statusEnabled", "false");

    environment.addProvider(new ErrorMessageBodyWriter());

    environment.enableJerseyFeature(FEATURE_LOGGING_DISABLE_ENTITY);

    environment.setJerseyProperty(PROPERTY_CONTAINER_REQUEST_FILTERS,
        list(LoggingFilter.class.getName(),
            DownloadFilter.class.getName(),
            CachingFilter.class.getName()));
    environment.setJerseyProperty(PROPERTY_CONTAINER_RESPONSE_FILTERS,
        list(LoggingFilter.class.getName(),
            VersionFilter.class.getName(),
            CrossOriginFilter.class.getName(),
            CachingFilter.class.getName()));

    removeDwExceptionMapper(environment, LoggingExceptionMapper.class);

    logInfo(config);
  }

  /**
   * Determines the base resource subject to the current physical file packaging.
   * 
   * @return the base resource
   */
  @SneakyThrows
  private Resource getBaseResource() {
    return firstNonNull(
        newClassPathResource("."), // File system resource
        newJarResource(newResource(getJarUri()))); // Jar resource
  }

  private void logInfo(PortalProperties config) {
    log.info("{}", repeat("-", 100));
    log.info("Version: {}", getVersion());
    log.info("Built:   {}", getBuildTimestamp());
    log.info("SCM:");
    Properties scmInfo = VersionUtils.getScmInfo();
    for (String property : scmInfo.stringPropertyNames()) {
      String value = scmInfo.getProperty(property, "").replaceAll("\n", " ");
      log.info("         {}: {}", padEnd(property, 24, ' '), value);
    }
    log.info("Config: {}", config);
    log.info("Working Directory: {}", System.getProperty("user.dir"));
    log.info("{}", repeat("-", 100));
  }

  private String getVersion() {
    String version = getClass().getPackage().getImplementationVersion();
    return version == null ? "[unknown version]" : version;
  }

  private String getBuildTimestamp() {
    String buildTimestamp = getClass().getPackage().getSpecificationVersion();
    return buildTimestamp == null ? "[unknown build timestamp]" : buildTimestamp;
  }

  private URI getJarUri() throws URISyntaxException {
    return getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
  }

}
