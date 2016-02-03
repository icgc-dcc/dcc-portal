/*
 * Copyright (type) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static org.springframework.aop.support.AopUtils.isAopProxy;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.BeanFactory;

import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.core.spi.component.ioc.IoCManagedComponentProvider;

@Slf4j
@RequiredArgsConstructor
public class SpringManagedComponentProvider implements IoCManagedComponentProvider {

  private final BeanFactory factory;
  private final ComponentScope scope;
  private final String beanName;
  private final Class<?> type;

  @Override
  public ComponentScope getScope() {
    return scope;
  }

  @Override
  public Object getInjectableInstance(Object o) {
    if (isAopProxy(o)) {
      val aopResource = (Advised) o;
      try {
        return aopResource.getTargetSource().getTarget();
      } catch (Exception e) {
        log.error("Could not get target object from proxy.", e);
        throw new RuntimeException("Could not get target object from proxy.", e);
      }
    } else {
      return o;
    }
  }

  @Override
  public Object getInstance() {
    return factory.getBean(beanName, type);
  }

}