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

import static java.util.Collections.singletonMap;
import static org.dcc.portal.pql.meta.field.FieldModel.FieldType.OBJECT;

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

public class CreateFullyQualifiedNameVisitor implements FieldVisitor<Map<String, FieldModel>> {

  @Override
  public Map<String, FieldModel> visitArrayField(ArrayFieldModel field) {
    if (field.getElement().getType() != OBJECT) {
      return singletonMap(field.getName(), (FieldModel) field);
    }

    val result = new ImmutableMap.Builder<String, FieldModel>();
    result.put(field.getName(), field);
    result.putAll(prefix(field.getName(), field.getElement().accept(this)));

    return result.build();
  }

  @Override
  public Map<String, FieldModel> visitBooleanField(BooleanFieldModel field) {
    return singletonMap(field.getName(), (FieldModel) field);
  }

  @Override
  public Map<String, FieldModel> visitDoubleField(DoubleFieldModel field) {
    return singletonMap(field.getName(), (FieldModel) field);
  }

  @Override
  public Map<String, FieldModel> visitLongField(LongFieldModel field) {
    return singletonMap(field.getName(), (FieldModel) field);
  }

  @Override
  public Map<String, FieldModel> visitObjectField(ObjectFieldModel field) {
    val result = new ImmutableMap.Builder<String, FieldModel>();
    result.put(field.getName(), field);

    for (val child : field.getFields()) {
      result.putAll(prefix(field.getName(), child.accept(this)));
    }

    return result.build();
  }

  private static Map<String, FieldModel> prefix(String name, Map<String, FieldModel> map) {
    val result = new ImmutableMap.Builder<String, FieldModel>();
    for (val entry : map.entrySet()) {
      if (name.equals(FieldModel.NO_NAME)) {
        result.put(entry.getKey(), entry.getValue());
      } else {
        result.put(name + FieldModel.FIELD_SEPARATOR + entry.getKey(), entry.getValue());
      }
    }

    return result.build();
  }

  @Override
  public Map<String, FieldModel> visitStringField(StringFieldModel field) {
    return singletonMap(field.getName(), (FieldModel) field);
  }

}
