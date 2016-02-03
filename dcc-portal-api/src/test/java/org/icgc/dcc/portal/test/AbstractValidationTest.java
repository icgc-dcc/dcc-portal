/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.test;

import java.lang.reflect.ParameterizedType;
import java.util.Locale;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import lombok.val;

import org.junit.BeforeClass;

/**
 * Base class for testing model class JSR 303 validation.
 * 
 * @param <T>
 */
public abstract class AbstractValidationTest<T> {

  private static Validator validator;

  private final Class<T> modelClass;

  @SuppressWarnings("unchecked")
  public AbstractValidationTest() {
    this.modelClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
  }

  /**
   * Creates the {@code Validator} instance.
   * 
   * @throws Exception
   */
  @BeforeClass
  public static void createValidator() throws Exception {
    Locale.setDefault(Locale.ENGLISH);
    val config = Validation.buildDefaultValidatorFactory();
    validator = config.getValidator();
  }

  /**
   * Validates the supplied {@code Object} instance.
   * 
   * @param obj - the object to validate
   * @return constraint violations
   */
  protected Set<ConstraintViolation<T>> validate(T obj) {
    return validator.validate(obj);
  }

  /**
   * Validates the supplied {@code Object} property instance.
   * 
   * @param obj - the object to validate
   * @return constraint violations
   */
  protected Set<ConstraintViolation<T>> validateProperty(T value, String propertyName) {
    return validator.validateProperty(value, propertyName);
  }

  /**
   * Validates the supplied {@code Object} property value.
   * 
   * @param obj - the object to validate
   * @return constraint violations
   */
  protected Set<ConstraintViolation<T>> validateValue(String propertyName, String value) {
    return validator.validateValue(modelClass, propertyName, value);
  }

}
