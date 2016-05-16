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

package org.icgc.dcc.portal.swagger;

import org.icgc.dcc.portal.resource.Resource;
import org.icgc.dcc.portal.util.VersionUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Environment;

import io.swagger.converter.ModelConverters;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import lombok.val;

/**
 * Makes swagger docs available at {@code api/swagger.json}.
 */
public class SwaggerBundle extends AssetsBundle {

  /**
   * Constants.
   */
  private static final String API_TITLE = "ICGC Data Portal API";
  private static final String API_BASE_PATH = "/api";

  private static final String RESOURCE_PATH = "/swagger-ui";
  private static final String URL_PATH = "/docs";
  private static final String INDEX_FILE = "index.html";

  public SwaggerBundle() {
    super(RESOURCE_PATH, URL_PATH, INDEX_FILE);
  }

  @Override
  public void run(Environment environment) {
    super.run(environment);

    configureEnvironment(environment);
    configureSwagger();
  }

  private void configureEnvironment(Environment environment) {
    // Add resource
    environment.addResource(new ApiListingResource());

    // Swagger needs this for compliance with their spec
    environment.getObjectMapperFactory().setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  private void configureSwagger() {
    // Add converters
    ModelConverters.getInstance().addConverter(new PrimitiveModelResolver());

    // Configure and scan
    val config = new BeanConfig();
    config.setTitle(API_TITLE);
    config.setVersion(VersionUtils.getApiVersion());
    config.setResourcePackage(getResourcePackages());
    config.setBasePath(API_BASE_PATH);
    config.setScan(true);
  }

  private String getResourcePackages() {
    val swaggerPackage = getClass().getPackage().getName();
    val resourcePackage = Resource.class.getPackage().getName();

    return resourcePackage + "," + swaggerPackage;
  }

}
