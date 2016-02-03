/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.util;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import lombok.val;

import org.icgc.dcc.portal.util.FloatRange.FloatRangeValidator;

@Target({ METHOD, FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = FloatRangeValidator.class)
@Documented
public @interface FloatRange {

  public String message() default "{java.lang.Float.range.error}";

  public Class<?>[] groups() default {};

  public Class<? extends Payload>[] payload() default {};

  float min() default Float.MIN_VALUE;

  float max() default Float.MAX_VALUE;

  public static class FloatRangeValidator implements ConstraintValidator<FloatRange, Object> {

    /**
     * Configuration.
     */
    private float min;
    private float max;

    @Override
    public void initialize(FloatRange range) {
      this.min = range.min();
      this.max = range.max();
    }

    @Override
    public boolean isValid(final Object object, final ConstraintValidatorContext context) {
      boolean isValid = false;

      if (object == null) {
        isValid = true;
      } else if (object instanceof Float) {
        val value = (Float) object;
        isValid = value >= min && value <= max;

        if (!isValid) {
          context.disableDefaultConstraintViolation();
          context.buildConstraintViolationWithTemplate(
              "must be in range [" + String.format("%f", min) + ", " + String.format("%f", max) + "] inclusive")
              .addConstraintViolation();
        }
      }

      return isValid;
    }

  }
}