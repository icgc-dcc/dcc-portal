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
package org.icgc.dcc.portal.server.util;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.net.HttpHeaders.X_FORWARDED_FOR;
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

import java.net.InetAddress;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Joiner;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

/**
 * HttpServletRequest-related helpers
 */

@NoArgsConstructor(access = PRIVATE)
public final class HttpServletRequests {

  private static final String UNKNOWN_HOST_NAME = "Unknown";
  private static final char SPACE = ' ';
  private static final Joiner JOINER = Joiner.on(SPACE).skipNulls();

  /*
   * A helper to return a string containing hostname and IP-related info for a web request. The format is "Web request
   * received on x.x.x.x (web-server-hostname) from y.y.y.y via z.z.z.z (proxy-hostname)
   */
  @NonNull
  public static String getHttpRequestCallerInfo(final HttpServletRequest request) {
    val intro = "Web request received";

    return JOINER.join(intro, getLocalNetworkInfo(), getRemoteUserNetworkInfo(request),
        getProxyNetworkInfo(request));
  }

  public static String getHeadersFromRequest(@NonNull final HttpServletRequest request) {
    val headers = new StringBuilder();

    val headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      val name = headerNames.nextElement();
      val values = request.getHeaders(name);
      while (values.hasMoreElements()) {
        val value = values.nextElement();
        headers.append(format("%s : %s ;\n", name, value));
      }
    }

    return headers.toString();
  }

  private static String formatNetworkInfo(final String hostName, final String ipAddress) {
    val info = isNullOrEmpty(hostName) ? UNKNOWN_HOST_NAME : hostName.trim();

    return isNullOrEmpty(ipAddress) ? info : info + " (" + ipAddress.trim() + ")";
  }

  public static String getLocalNetworkInfo() {
    String localHostName = null;
    String localHostIp = null;

    try {
      val host = InetAddress.getLocalHost();

      localHostName = host.getHostName();
      localHostIp = host.getHostAddress();
    } catch (Exception e) {
      // Simply ignore this.
    }

    return "on " + formatNetworkInfo(localHostName, localHostIp);
  }

  private static String getProxyNetworkInfo(final HttpServletRequest request) {
    val proxyHostName = request.getRemoteHost();
    val proxyHostIp = request.getRemoteAddr();

    return "via " + formatNetworkInfo(proxyHostName, proxyHostIp);
  }

  private static String getRemoteUserNetworkInfo(final HttpServletRequest request) {
    val userIp = request.getHeader(X_FORWARDED_FOR);

    val result = isNullOrEmpty(userIp) ? null : userIp.trim();
    return (null == result) ? null : "from " + result;
  }

}
