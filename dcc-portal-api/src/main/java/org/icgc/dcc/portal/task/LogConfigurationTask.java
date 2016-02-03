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
package org.icgc.dcc.portal.task;



import java.io.PrintWriter;
import java.util.List;

import lombok.val;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

import com.google.common.collect.ImmutableMultimap;
import com.yammer.dropwizard.tasks.Task;

/**
 * Task that changes log level of the portal.<br>
 * <br>
 * Usage: <br>
 * <li>Configure a user-defined log level for a single Logger</li>
 * {@code curl -XPOST -d "logger=org.icgc.dcc&level=INFO" http://localhost:8081/tasks/log-level}<br>
 * <br>
 * <li>Configure a user-defined log level for multiple Loggers</li>
 * {@code curl -XPOST -d "logger=org.icgc.dcc&logger=io.icgc.core&level=INFO" http://localhost:8081/tasks/log-level}<br>
 * <br>
 * <li>Configure the default log level for a single Logger</li>
 * {@code curl -XPOST -d "logger=org.icgc.dcc" http://localhost:8081/tasks/log-level}
 */
@Component
public class LogConfigurationTask extends Task {

  private final LoggerContext loggerContext;

  protected LogConfigurationTask() {
    super("log-level");
    this.loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
  }

  @Override
  public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
    val loggerNames = getLoggerNames(parameters);
    val loggerLevel = getLoggerLevel(parameters);

    for (val loggerName : loggerNames) {
      val logger = loggerContext.getLogger(loggerName);
      logger.setLevel(loggerLevel);
      output.println(String.format("Configured logging level for %s to %s", loggerName, loggerLevel));
      output.flush();
    }
  }

  private List<String> getLoggerNames(ImmutableMultimap<String, String> parameters) {
    return parameters.get("logger").asList();
  }

  private Level getLoggerLevel(ImmutableMultimap<String, String> parameters) {
    val loggerLevels = parameters.get("level").asList();

    return loggerLevels.isEmpty() ? null : Level.valueOf(loggerLevels.get(0));
  }

}
