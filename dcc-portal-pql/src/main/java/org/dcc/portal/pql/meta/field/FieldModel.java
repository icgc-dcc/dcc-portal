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

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.dcc.portal.pql.meta.visitor.FieldVisitor;

import com.google.common.collect.ImmutableSet;

@Getter
@ToString
@AllArgsConstructor
public abstract class FieldModel {

  public static final String FIELD_SEPARATOR = ".";
  public static final Set<String> EMPTY_UI_ALIAS = ImmutableSet.of();
  public static final String NO_NAME = "";
  public static final boolean NOT_NESTED = false;
  public static final boolean NESTED = true;

  private String name;
  private Set<String> alias;
  private FieldType type;
  @Setter
  private boolean nested;

  public static enum FieldType {
    LONG,
    DOUBLE,
    STRING,
    ARRAY,
    BOOLEAN,
    OBJECT
  }

  public boolean isLong() {
    return type == FieldType.LONG;
  }

  public boolean isDouble() {
    return type == FieldType.DOUBLE;
  }

  public boolean isString() {
    return type == FieldType.STRING;
  }

  public boolean isArray() {
    return type == FieldType.ARRAY;
  }

  public boolean isBoolean() {
    return type == FieldType.BOOLEAN;
  }

  public boolean isObject() {
    return type == FieldType.OBJECT;
  }

  public abstract <T> T accept(FieldVisitor<T> visitor);

}
