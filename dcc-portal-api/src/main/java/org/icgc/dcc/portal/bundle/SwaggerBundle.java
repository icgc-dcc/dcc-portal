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

package org.icgc.dcc.portal.bundle;

import java.util.List;
import java.util.Map;

import org.icgc.dcc.portal.util.VersionUtils;

import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.FilterFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.config.SwaggerConfig;
import com.wordnik.swagger.core.filter.SwaggerSpecFilter;
import com.wordnik.swagger.jaxrs.config.DefaultJaxrsScanner;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;
import com.wordnik.swagger.jaxrs.reader.DefaultJaxrsApiReader;
import com.wordnik.swagger.model.ApiDescription;
import com.wordnik.swagger.model.Operation;
import com.wordnik.swagger.model.Parameter;
import com.wordnik.swagger.reader.ClassReaders;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Environment;

public class SwaggerBundle extends AssetsBundle {

  private static final String RESOURCE_PATH = "/swagger-ui";

  private static final String URI_PATH = "/docs";

  public SwaggerBundle() {
    super(RESOURCE_PATH, URI_PATH, "index.html");
  }

  @Override
  public void run(Environment environment) {
    super.run(environment);

    // Add swagger resource
    environment.addResource(new ApiListingResourceJSON());

    // Add swagger providers
    environment.addProvider(new ApiDeclarationProvider());
    environment.addProvider(new ResourceListingProvider());

    // Add a class scanner. The DefaultJaxrsScanner will look for an Application context and getSingletons() and
    // getClasses()
    ScannerFactory.setScanner(new DefaultJaxrsScanner());

    // Add a custom filter
    FilterFactory.setFilter(new InternalFilter());

    // Add a reader, the DefaultJaxrsApiReader will scan @Api annotations and create the swagger spec from them
    ClassReaders.setReader(new DefaultJaxrsApiReader());

    // ConfigFactory.setConfig(new SwaggerConfig());
    SwaggerConfig config = ConfigFactory.config();
    config.setApiVersion(VersionUtils.getApiVersion());
    config.setBasePath("/");
  }

  private static class InternalFilter implements SwaggerSpecFilter {

    @Override
    public boolean isOperationAllowed(Operation arg0, ApiDescription arg1, Map<String, List<String>> arg2,
        Map<String, String> arg3, Map<String, List<String>> arg4) {
      return true;
    }

    @Override
    public boolean isParamAllowed(Parameter parameter, Operation operation, ApiDescription api,
        Map<String, List<String>> params, Map<String, String> cookies, Map<String, List<String>> headers) {
      if (!parameter.paramAccess().isEmpty() && parameter.paramAccess().get().equals("internal")) {
        return false;
      }
      return true;
    }

  }

}
