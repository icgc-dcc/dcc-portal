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
package org.icgc.dcc.portal.task;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.yammer.dropwizard.tasks.Task;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Task that works in concert with the {@code install} script to install the application.
 * 
 * @see {@link src/main/bin/install}
 */
@Slf4j
@Component
public class InstallTask extends Task {

  /**
   * Relative path to the install script. CWD is set by the service wrapper to be {@code lib/}.
   */
  private static final String INSTALL_SCRIPT_PATH = "../bin/install";

  protected InstallTask() {
    super("install");
  }

  @Override
  public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
    val message =
        format("Requesting application install via external install script with parameters '%s'...", parameters);
    log.info(message);
    output.println(message);
    output.flush();

    String mode = value(parameters, "mode");
    String version = value(parameters, "version");
    install(mode, version);
  }

  private static void install(String mode, String version) {
    log.info("Building install command for mode '{}' and version '{}'...", mode, version);
    List<String> command = command(mode, version);

    log.info("Executing install command: '{}'...", command);
    execute(command);
  }

  private static List<String> command(String mode, String version) {
    checkArgument(mode != null, "Mode is required");
    List<String> command = newArrayList();
    command.add(INSTALL_SCRIPT_PATH);
    if (mode.equals("release")) {
      checkArgument(version != null, "Version is required when using -r");
      command.add("-r");
      command.add(version);
    } else if (mode.equals("latest")) {
      command.add("-l");
    } else if (mode.equals("snapshot")) {
      command.add("-s");
    } else {
      checkArgument(false, "Invalid mode '%s'", mode);
    }

    return command;
  }

  @SneakyThrows
  private static void execute(List<String> command) {
    Process process = new ProcessBuilder(command)
        .redirectErrorStream(true)
        .start();

    @Cleanup
    val reader = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8));
    String line = null;
    while ((line = reader.readLine()) != null) {
      log.info("'install': {}", line);
    }

    int exitCode = process.waitFor();

    // If the script was successful, we wouldn't be here...
    checkState(false, "Install operation failed with exit code '%s'", exitCode);
  }

  private static String value(Multimap<String, String> parameters, String name) {
    val values = parameters.get(name);
    return values.isEmpty() ? null : values.iterator().next();
  }

}
