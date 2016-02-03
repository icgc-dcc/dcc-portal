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
package org.icgc.dcc.portal.spring;

import java.lang.annotation.Annotation;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ClassUtils;

import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.core.spi.component.ComponentContext;

@RequiredArgsConstructor
public class SpringBeanNameResolver {

  @NonNull
  private final ConfigurableApplicationContext context;

  public String resolveBeanName(ComponentContext componentContext, Class<?> type) {
    boolean annotatedWithInject = false;
    if (componentContext != null) {

      val injectParam = getAnnotation(componentContext.getAnnotations(), InjectParam.class);
      if (injectParam != null) {
        annotatedWithInject = true;
        if (injectParam.value() != null && !injectParam.value().equals("")) {
          return injectParam.value();
        }
      }
    }

    val names = context.getBeanNamesForType(type);
    if (names.length == 0) {
      return null;
    } else if (names.length == 1) {
      return names[0];
    } else {
      val result = resolveSubTypeBeanNames(names, type);
      if (result != null) {
        return result;
      }

      val message = new StringBuilder()
          .append("There are multiple beans configured in spring for the type ")
          .append(type.getName())
          .append(".");

      if (annotatedWithInject) {
        message
            .append("\nYou should specify the name of the preferred bean with @InjectParam(\"name\") or @Inject(\"name\").");
      } else {
        message
            .append("\nAnnotation information was not available, the reason might be because you're not using ")
            .append("@InjectParam. You should use @InjectParam and specifiy the bean name via InjectParam(\"name\").");
      }

      throw new RuntimeException(message.toString());
    }
  }

  private String resolveSubTypeBeanNames(String[] names, Class<?> type) {
    // Check if types of the beans names are assignable
    // Spring auto-registration for a type A will include the bean names for classes that extend A
    boolean inheritedNames = false;
    String beanName = null;
    for (val name : names) {
      val beanType = ClassUtils.getUserClass(context.getType(name));
      inheritedNames = type.isAssignableFrom(beanType);

      if (type == beanType) {
        beanName = name;
      }
    }

    return inheritedNames && beanName != null ? beanName : null;
  }

  private static <T extends Annotation> T getAnnotation(Annotation[] annotations, Class<T> type) {
    if (annotations != null) {
      for (val annotation : annotations) {
        if (annotation.annotationType().equals(type)) {
          return type.cast(annotation);
        }
      }
    }

    return null;
  }

}
