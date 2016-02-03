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
package org.icgc.dcc.portal.spring;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.github.nhuray.dropwizard.spring.SpringBundle;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;

/**
 * Base class for Dropwizard Spring integration.
 * 
 * @param <T>
 */
@Slf4j
public abstract class SpringService<T extends Configuration> extends Service<T> {

  private final AnnotationConfigApplicationContext context;

  public SpringService() {
    this.context = new AnnotationConfigApplicationContext();

    context.scan(this.getClass().getPackage().getName());
  }

  @Override
  public void initialize(Bootstrap<T> bootstrap) {
    bootstrap.addBundle(new SpringBundle<T>(context, true, true) {

      @Override
      public void run(T configuration, Environment environment) throws Exception {
        // Order matters
        registerEnvironment(environment);
        super.run(configuration, environment);

        registerJerseyProvider(environment);
      }

      private void registerEnvironment(Environment environment) {
        // This is needed to work with Dropwizard components
        log.info("Registering Dropwizard Environment under name : dwEnv");
        val beanFactory = context.getBeanFactory();
        beanFactory.registerSingleton("dwEnv", environment);
      }

      private void registerJerseyProvider(Environment environment) {
        // This is required for ContainerRequestFilter, ContainerResponseFilter, etc injection
        log.info("Registering SpringComponentProviderFactory");
        val springProvider = new SpringComponentProviderFactory(environment.getJerseyResourceConfig(), context);
        environment.addProvider(springProvider);
      }

    });
  }

}
