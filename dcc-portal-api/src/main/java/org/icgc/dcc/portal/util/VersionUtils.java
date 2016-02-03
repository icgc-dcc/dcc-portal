/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.util;

import static com.google.common.base.Objects.firstNonNull;
import static lombok.AccessLevel.PRIVATE;

import java.util.Properties;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class VersionUtils {

  private static final String LOCAL_VERSION = "?";

  private static final String VERSION = firstNonNull( //
      VersionUtils.class.getPackage().getImplementationVersion(), //
      LOCAL_VERSION);

  private static final Properties SCM_INFO = loadScmInfo();

  public static Properties getScmInfo() {
    return SCM_INFO;
  }

  public static String getApplicationVersion() {
    return VERSION;
  }

  public static String getApiVersion() {
    return "v" + 1;
  }

  public static String getCommitId() {
    return SCM_INFO.getProperty("git.commit.id.abbrev", "unknown");
  }

  private static Properties loadScmInfo() {
    Properties properties = new Properties();
    try {
      properties.load(VersionUtils.class.getClassLoader().getResourceAsStream("git.properties"));
    } catch (Exception e) {
      // Local build
    }

    return properties;
  }

}
