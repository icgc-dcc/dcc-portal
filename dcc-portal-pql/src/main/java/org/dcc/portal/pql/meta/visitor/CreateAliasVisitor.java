/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.dcc.portal.pql.meta.visitor;

import static org.dcc.portal.pql.meta.field.FieldModel.FieldType.OBJECT;

import java.util.Collections;
import java.util.Map;

import lombok.val;

import org.dcc.portal.pql.meta.field.ArrayFieldModel;
import org.dcc.portal.pql.meta.field.BooleanFieldModel;
import org.dcc.portal.pql.meta.field.DoubleFieldModel;
import org.dcc.portal.pql.meta.field.FieldModel;
import org.dcc.portal.pql.meta.field.LongFieldModel;
import org.dcc.portal.pql.meta.field.ObjectFieldModel;
import org.dcc.portal.pql.meta.field.StringFieldModel;

import com.google.common.collect.ImmutableMap;

/**
 * Creates Alias -> FullyQualifiedName mapping.
 */
public class CreateAliasVisitor implements FieldVisitor<Map<String, String>> {

  @Override
  public Map<String, String> visitArrayField(ArrayFieldModel field) {
    if (field.getElement().getType() != OBJECT) {
      return getAlias(field);
    }

    val result = new ImmutableMap.Builder<String, String>();
    result.putAll(getAlias(field));
    result.putAll(prefixFieldName(field.getName(), field.getElement().accept(this)));

    return result.build();
  }

  @Override
  public Map<String, String> visitBooleanField(BooleanFieldModel field) {
    return getAlias(field);
  }

  @Override
  public Map<String, String> visitDoubleField(DoubleFieldModel field) {
    return getAlias(field);
  }

  @Override
  public Map<String, String> visitLongField(LongFieldModel field) {
    return getAlias(field);
  }

  @Override
  public Map<String, String> visitObjectField(ObjectFieldModel field) {
    val result = new ImmutableMap.Builder<String, String>();
    result.putAll(getAlias(field));
    for (val child : field.getFields()) {
      result.putAll(prefixFieldName(field.getName(), child.accept(this)));
    }

    return result.build();
  }

  @Override
  public Map<String, String> visitStringField(StringFieldModel field) {
    return getAlias(field);
  }

  private static Map<String, String> getAlias(FieldModel field) {
    if (field.getAlias().isEmpty()) {
      return Collections.<String, String> emptyMap();
    }

    val result = new ImmutableMap.Builder<String, String>();
    for (val alias : field.getAlias()) {
      result.put(alias, field.getName());
    }

    return result.build();
  }

  private static Map<String, String> prefixFieldName(String name, Map<String, String> fieldNameByAliasMap) {
    val result = new ImmutableMap.Builder<String, String>();
    for (val entry : fieldNameByAliasMap.entrySet()) {
      if (name.equals(FieldModel.NO_NAME)) {
        result.put(entry.getKey(), entry.getValue());
      } else {
        result.put(entry.getKey(), name + FieldModel.FIELD_SEPARATOR + entry.getValue());
      }
    }

    return result.build();
  }

}
