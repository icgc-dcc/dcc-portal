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
package org.icgc.dcc.portal.util;

import static lombok.AccessLevel.PRIVATE;

import java.util.Iterator;

import javax.ws.rs.ext.ExceptionMapper;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.yammer.dropwizard.config.Environment;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class DropwizardUtils {

  /**
   * See http://thoughtspark.org/2013/02/25/dropwizard-and-jersey-exceptionmappers/
   */
  public static void removeDwExceptionMappers(Environment environment) {
    Iterator<Object> iterator = environment.getJerseyResourceConfig().getSingletons().iterator();

    // Remove all of Dropwizard's custom ExceptionMappers
    while (iterator.hasNext()) {
      Object singleton = iterator.next();
      if (isDwExceptionMapper(singleton)) {
        log.info("Removing Dropwizard exception mapper: {}", singleton.getClass());
        iterator.remove();
      }
    }
  }

  public static void removeDwExceptionMapper(Environment environment, Class<?> mapper) {
    Iterator<Object> iterator = environment.getJerseyResourceConfig().getSingletons().iterator();

    // Remove single Dropwizard custom ExceptionMapper
    while (iterator.hasNext()) {
      Object singleton = iterator.next();
      if (mapper.isAssignableFrom(singleton.getClass())) {
        log.info("Removing Dropwizard exception mapper: {}, ({})", singleton, mapper);
        iterator.remove();
      }
    }
  }

  public static boolean isDwExceptionMapper(Object s) {
    return s instanceof ExceptionMapper && s.getClass().getName().startsWith("com.yammer.dropwizard.jersey.");
  }

}
