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
package org.dcc.portal.pql.meta.field;

import static java.util.Collections.singleton;
import static org.dcc.portal.pql.meta.field.FieldModel.FieldType.OBJECT;

import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;

import org.dcc.portal.pql.meta.visitor.FieldVisitor;

import com.google.common.collect.ImmutableList;

@Getter
public class ObjectFieldModel extends FieldModel {

  private final List<? extends FieldModel> fields;

  private ObjectFieldModel(String name, List<? extends FieldModel> fields) {
    this(name, EMPTY_UI_ALIAS, fields);
  }

  private ObjectFieldModel(String name, String alias, List<? extends FieldModel> fields) {
    this(name, alias, NOT_NESTED, fields);
  }

  private ObjectFieldModel(String name, Set<String> uiAlias, List<? extends FieldModel> fields) {
    this(name, uiAlias, NOT_NESTED, fields);
  }

  private ObjectFieldModel(String name, boolean nested, List<? extends FieldModel> fields) {
    this(name, EMPTY_UI_ALIAS, nested, fields);
  }

  private ObjectFieldModel(String name, String uiAlias, boolean nested, List<? extends FieldModel> fields) {
    super(name, singleton(uiAlias), OBJECT, nested);
    this.fields = fields;
  }

  private ObjectFieldModel(String name, Set<String> uiAlias, boolean nested, List<? extends FieldModel> fields) {
    super(name, uiAlias, OBJECT, nested);
    this.fields = fields;
  }

  @SafeVarargs
  public static <T extends FieldModel> ObjectFieldModel object(@NonNull String name, @NonNull T... fields) {
    val fieldsList = new ImmutableList.Builder<T>();
    fieldsList.add(fields);

    return new ObjectFieldModel(name, fieldsList.build());
  }

  @SafeVarargs
  public static <T extends FieldModel> ObjectFieldModel object(@NonNull String name, @NonNull String alias,
      @NonNull T... fields) {
    val fieldsList = new ImmutableList.Builder<T>();
    fieldsList.add(fields);

    return new ObjectFieldModel(name, alias, fieldsList.build());
  }

  @SafeVarargs
  public static <T extends FieldModel> ObjectFieldModel object(@NonNull T... fields) {
    val fieldsList = new ImmutableList.Builder<T>();
    fieldsList.add(fields);

    return new ObjectFieldModel(NO_NAME, fieldsList.build());
  }

  @SafeVarargs
  public static <T extends FieldModel> ObjectFieldModel nestedObject(@NonNull String name, @NonNull T... fields) {
    val fieldsList = new ImmutableList.Builder<T>();
    fieldsList.add(fields);

    return new ObjectFieldModel(name, EMPTY_UI_ALIAS, NESTED, fieldsList.build());
  }

  @Override
  public <T> T accept(@NonNull FieldVisitor<T> visitor) {
    return visitor.visitObjectField(this);
  }

}
