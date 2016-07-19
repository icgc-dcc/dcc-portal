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

import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import org.icgc.dcc.portal.server.model.param.AlleleParam;
import org.icgc.dcc.portal.server.model.param.FieldsParam;
import org.icgc.dcc.portal.server.model.param.FiltersParam;
import org.icgc.dcc.portal.server.model.param.IntParam;
import org.icgc.dcc.portal.server.model.param.JacksonParam;
import org.icgc.dcc.portal.server.model.param.ListParam;
import org.icgc.dcc.portal.server.model.param.LongParam;
import org.icgc.dcc.portal.server.model.param.UUIDSetParam;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.jackson.ModelResolver;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;
import lombok.val;

/**
 * Aliases logically primitive types from complex Java types.
 */
public class PrimitiveModelResolver extends ModelResolver {

  public PrimitiveModelResolver() {
    super(DEFAULT);
  }

  @Override
  public Model resolve(JavaType type, ModelConverterContext context, Iterator<ModelConverter> next) {
    val primitive = resolvePrimitive(type);
    if (primitive != null) return primitive;

    return super.resolve(type, context, next);
  }

  @Override
  public Property resolveProperty(JavaType propType, ModelConverterContext context, Annotation[] annotations,
      Iterator<ModelConverter> next) {
    val property = resolveProperty(propType);
    if (property != null) return property;

    return super.resolveProperty(propType, context, annotations, next);
  }

  private static Model resolvePrimitive(JavaType type) {
    val property = resolveProperty(type);
    if (property == null) return null;

    return new ModelImpl().type(property.getType()).format(property.getFormat());
  }

  private static Property resolveProperty(JavaType propType) {
    if (isString(propType)) {
      return new StringProperty();
    }
    if (isInteger(propType)) {
      return new IntegerProperty();
    }
    if (isLong(propType)) {
      return new LongProperty();
    }

    return null;
  }

  private static boolean isString(JavaType type) {
    return isA(type, AlleleParam.class)
        || isA(type, FieldsParam.class)
        || isA(type, FiltersParam.class)
        || isA(type, JacksonParam.class)
        || isA(type, JsonNode.class)
        || isA(type, ListParam.class)
        || isA(type, UUIDSetParam.class);
  }

  private static boolean isInteger(JavaType type) {
    return isA(type, IntParam.class);
  }

  private static boolean isLong(JavaType type) {
    return isA(type, LongParam.class);
  }

  private static boolean isA(JavaType type, Class<?> clazz) {
    return clazz.isAssignableFrom(type.getRawClass());
  }

}