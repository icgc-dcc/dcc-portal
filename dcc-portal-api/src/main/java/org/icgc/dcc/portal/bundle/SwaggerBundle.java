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
import java.util.UUID;

import org.icgc.dcc.portal.manifest.model.ManifestFormat;
import org.icgc.dcc.portal.model.param.AlleleParam;
import org.icgc.dcc.portal.model.param.EnrichmentParamsParam;
import org.icgc.dcc.portal.model.param.FieldsParam;
import org.icgc.dcc.portal.model.param.FiltersParam;
import org.icgc.dcc.portal.model.param.IdsParam;
import org.icgc.dcc.portal.model.param.ListParam;
import org.icgc.dcc.portal.model.param.UUIDSetParam;
import org.icgc.dcc.portal.util.VersionUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.config.FilterFactory;
import com.wordnik.swagger.config.ScannerFactory;
import com.wordnik.swagger.converter.ModelConverters;
import com.wordnik.swagger.converter.TypeConverter;
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
import com.yammer.dropwizard.jersey.params.IntParam;

import lombok.val;

public class SwaggerBundle extends AssetsBundle {

  /**
   * Constants.
   */
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

    // Add a reader, the DefaultJaxrsApiReader will scan @Api annotations and create the swagger spec from them
    ClassReaders.setReader(new DefaultJaxrsApiReader());

    // Add a custom filter
    FilterFactory.setFilter(new InternalFilter());

    // Add custom description of type fields in models
    ModelConverters.addConverter(createTypeConverter(), true);

    // ConfigFactory.setConfig(new SwaggerConfig());
    val config = ConfigFactory.config();
    config.setApiVersion(VersionUtils.getApiVersion());
    config.setBasePath("/");
  }

  /**
   * This is only a partial solution. See linked resources for more information.
   * 
   * @see https://groups.google.com/d/topic/swagger-swaggersocket/YdQOP_rdKZU/discussion
   * @see https://github.com/swagger-api/swagger-core/issues/913
   */
  private TypeConverter createTypeConverter() {
    val typeConverter = new TypeConverter();
    typeConverter.add(AlleleParam.class.getSimpleName(), "string");
    typeConverter.add(UUID.class.getSimpleName(), "number");
    typeConverter.add(EnrichmentParamsParam.class.getSimpleName(), "string");
    typeConverter.add(FieldsParam.class.getSimpleName(), "number");
    typeConverter.add(FiltersParam.class.getSimpleName(), "string");
    typeConverter.add(ListParam.class.getSimpleName(), "string");
    typeConverter.add(IdsParam.class.getSimpleName(), "string");
    typeConverter.add(UUIDSetParam.class.getSimpleName(), "string");
    typeConverter.add(IntParam.class.getSimpleName(), "integer");
    typeConverter.add(ObjectNode.class.getSimpleName(), "string");
    typeConverter.add(ManifestFormat.class.getSimpleName(), "string");

    return typeConverter;
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
