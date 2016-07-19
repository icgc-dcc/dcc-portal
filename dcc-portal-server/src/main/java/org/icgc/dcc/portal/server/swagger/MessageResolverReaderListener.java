/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.portal.server.swagger;

import java.util.Collection;
import java.util.List;

import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;
import lombok.SneakyThrows;
import lombok.val;

/**
 * Replaces message keys with message values in select {@link Swagger} fields.
 */
@SwaggerDefinition // Needed for classpath scanning
public class MessageResolverReaderListener implements ReaderListener {

  /**
   * Dependencies
   */
  private final MessageResolver resolver = new MessageResolver();

  @Override
  public void beforeScan(Reader reader, Swagger swagger) {
    // No-op
  }

  @Override
  @SneakyThrows
  public void afterScan(Reader reader, Swagger swagger) {
    resolveTags(swagger.getTags());
    resolvePaths(swagger.getPaths().values());
    resolveModels(swagger.getDefinitions().values());
  }

  private void resolveTags(List<Tag> tags) {
    if (tags == null) return;

    for (val tag : tags) {
      tag.setDescription(resolve(tag.getDescription()));
    }
  }

  private void resolvePaths(Collection<Path> paths) {
    if (paths == null) return;

    for (val path : paths) {
      resolveOperations(path.getOperations());
    }
  }

  private void resolveOperations(Collection<Operation> operations) {
    if (operations == null) return;

    for (val operation : operations) {
      val summary = operation.getSummary();
      if (summary != null) {
        operation.setSummary(resolve(summary));
      }

      resolveParameters(operation.getParameters());
    }
  }

  private void resolveParameters(List<Parameter> parameters) {
    if (parameters == null) return;

    for (val parameter : parameters) {
      parameter.setDescription(resolve(parameter.getDescription()));
    }
  }

  private void resolveModels(Collection<Model> models) {
    if (models == null) return;

    for (val model : models) {
      val properties = model.getProperties();
      if (properties == null) return;

      resolveProperties(properties.values());
    }
  }

  private void resolveProperties(Collection<Property> properties) {
    if (properties == null) return;

    for (val property : properties) {
      property.setDescription(resolve(property.getDescription()));
    }
  }

  private String resolve(String key) {
    return resolver.resolve(key);
  }

}