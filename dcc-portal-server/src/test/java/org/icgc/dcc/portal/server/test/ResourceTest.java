/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.portal.server.test;

import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.LowLevelAppDescriptor;

import lombok.val;

/**
 * A base test class for testing resources.
 */
public abstract class ResourceTest {

  static {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  private final Set<Object> singletons = Sets.newHashSet();
  private final Set<Class<?>> providers = Sets.newHashSet();
  private final Map<String, Boolean> features = Maps.newHashMap();
  private final Map<String, Object> properties = Maps.newHashMap();

  private JerseyTest test;

  protected abstract void setUpResources() throws Exception;

  protected void addResource(Object resource) {
    singletons.add(resource);
  }

  public void addProvider(Class<?> klass) {
    providers.add(klass);
  }

  public void addProvider(Object provider) {
    singletons.add(provider);
  }

  protected void addFeature(String feature, Boolean value) {
    features.put(feature, value);
  }

  protected void addProperty(String property, Object value) {
    properties.put(property, value);
  }

  protected Client client() {
    return test.client();
  }

  protected JerseyTest getJerseyTest() {
    return test;
  }

  @Before
  public final void setUpJersey() throws Exception {
    setUpResources();
    this.test = new JerseyTest() {

      @Override
      protected AppDescriptor configure() {
        val config = new DefaultResourceConfig();
        for (Class<?> provider : providers) {
          config.getClasses().add(provider);
        }
        for (Map.Entry<String, Boolean> feature : features.entrySet()) {
          config.getFeatures().put(feature.getKey(), feature.getValue());
        }
        for (Map.Entry<String, Object> property : properties.entrySet()) {
          config.getProperties().put(property.getKey(), property.getValue());
        }
        config.getSingletons().add(new JacksonJaxbJsonProvider());
        config.getSingletons().addAll(singletons);
        return new LowLevelAppDescriptor.Builder(config).build();
      }
    };
    test.setUp();
  }

  @After
  public final void tearDownJersey() throws Exception {
    if (test != null) {
      test.tearDown();
    }
  }
}
