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
import static org.dcc.portal.pql.meta.field.FieldModel.FieldType.LONG;

import java.util.Set;

import lombok.NonNull;

import org.dcc.portal.pql.meta.visitor.FieldVisitor;

public class LongFieldModel extends FieldModel {

  private LongFieldModel(String name) {
    this(name, EMPTY_UI_ALIAS);
  }

  private LongFieldModel(String name, String alias) {
    this(name, alias, NOT_NESTED);
  }

  private LongFieldModel(String name, Set<String> aliases) {
    this(name, aliases, NOT_NESTED);
  }

  private LongFieldModel(String name, boolean nested) {
    this(name, EMPTY_UI_ALIAS, nested);
  }

  private LongFieldModel(String name, String alias, boolean nested) {
    this(name, singleton(alias), nested);
  }

  private LongFieldModel(String name, Set<String> aliases, boolean nested) {
    super(name, aliases, LONG, nested);
  }

  public static LongFieldModel long_(@NonNull String name) {
    return new LongFieldModel(name);
  }

  public static LongFieldModel long_(@NonNull String name, @NonNull String alias) {
    return new LongFieldModel(name, alias);
  }

  public static LongFieldModel long_(@NonNull String name, @NonNull Set<String> alias) {
    return new LongFieldModel(name, alias);
  }

  @Override
  public <T> T accept(@NonNull FieldVisitor<T> visitor) {
    return visitor.visitLongField(this);
  }

}
