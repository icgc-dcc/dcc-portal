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
package org.icgc.dcc.portal.server.endpoint;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.util.List;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import lombok.val;

@Component
@ConfigurationProperties(prefix = "endpoints." + LoggerEndpoint.ENDPOINT_ID, ignoreUnknownFields = false)
public class LoggerEndpoint implements MvcEndpoint {

  /**
   * Constants.
   */
  protected static final String ENDPOINT_ID = "logger";

  @RequestMapping(method = POST)
  public @ResponseBody String updateLogger(@RequestParam("logger") List<String> loggerNames, @Valid @NotEmpty Level loggerLevel) {
    val loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    for (val loggerName : loggerNames) {
      val logger = loggerContext.getLogger(loggerName);
      logger.setLevel(loggerLevel);
      return String.format("Configured logging level for %s to %s", loggerName, loggerLevel);
    }

    return "Logger(s) not found: " + loggerNames;
  }

  @Override
  public String getPath() {
    return ENDPOINT_ID;
  }

  @Override
  public boolean isSensitive() {
    return true;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Class<? extends Endpoint> getEndpointType() {
    return null;
  }

}
