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
package org.icgc.dcc.portal.server.controller;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;

import java.io.InputStreamReader;

import org.icgc.dcc.portal.server.model.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.io.CharStreams;

import lombok.SneakyThrows;
import lombok.val;

/**
 * Dynamic {@literal index.html} page that injects {@link Settings} into a {@code <script>} tag at
 * {@code <portal-settings></portal-settings>} if present.
 * 
 * @see https://jira.oicr.on.ca/browse/DCC-5331
 */
@Controller
public class IndexController {

  /**
   * Constants.
   */
  private static final String VARIABLE_NAME = "ICGC_SETTINGS";
  private static final String SETTINGS_TAG = "<portal-settings></portal-settings>";
  private static final String GA_TAG = "<ga-account>";

  /**
   * Dependencies
   */
  @Value("classpath:/app/index.html")
  private Resource index;
  @Value("${web.gaAccount}")
  private String gaAccount;
  @Autowired
  private Settings settings;

  @Cacheable("index.html")
  @RequestMapping("/index.html")
  public ResponseEntity<String> index() {
    // Inject script into existing HTML content
    val script = createScript();

    String html = readIndex();
    html = injectScript(html, script);
    html = injectGAAccount(html, gaAccount);

    return ResponseEntity.ok(html);
  }

  @SneakyThrows
  private String readIndex() {
    return CharStreams.toString(new InputStreamReader(index.getInputStream(), UTF_8));
  }

  @SneakyThrows
  private String createScript() {
    val json = DEFAULT.writeValueAsString(settings);
    return "<script type=\"text/javascript\"> window." + VARIABLE_NAME + " = " + json + ";</script>";
  }

  private String injectGAAccount(String html, String gaAccount) {
    return html.replace(GA_TAG, gaAccount);
  }

  private String injectScript(String html, String script) {
    return html.replace(SETTINGS_TAG, script);
  }

}
