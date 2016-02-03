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
package org.icgc.dcc.portal.test;

import static lombok.AccessLevel.PRIVATE;

import java.net.URI;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.icgc.dcc.portal.config.PortalProperties.CacheProperties;

import com.google.common.collect.ImmutableList;
import com.sun.jersey.api.Responses;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.WebApplication;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
public final class Tests {

  /**
   * Creates a stub of {@link ContainerRequest} that is used for testing.
   * 
   * @param webApplication - a mock of {@link WebApplication}
   */
  @SneakyThrows
  public static ContainerRequest createContainerRequest(WebApplication webApplication) {
    val method = "GET";
    val baseUri = new URI("http://localhost");
    val requestUri = new URI("http://localhost/test");
    val inHeaders = new InBoundHeaders();
    inHeaders.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

    return new ContainerRequest(webApplication, method, baseUri, requestUri, inHeaders, null);
  }

  /**
   * Creates a stub of {@link ContainerResponse} that is used for testing.
   * 
   * @param webApplication - a mock of {@link WebApplication}
   * @param entity of the response
   */
  public static ContainerResponse createContainerResponse(ContainerRequest cr, Object entity) {
    val result = new ContainerResponse(null, cr, null);
    result.setResponse(Responses.noContent().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build());
    result.setEntity(entity);

    return result;
  }

  /**
   * Creates a {@link CacheProperties} with enabled generation of <tt>Last-Modified</tt> and <tt>ETag</tt> HTTP headers
   * and empty exclude resources lists.
   * @return
   */
  public static CacheProperties createCacheConfigucation() {
    val config = new CacheProperties();
    val excludeList = new ImmutableList.Builder<String>().build();

    config.setEnableETag(true);
    config.setExcludeETag(excludeList);
    config.setEnableLastModified(true);
    config.setExcludeLastModified(excludeList);

    return config;
  }

  public static Object getFirstValueByHeader(String header, MultivaluedMap<String, Object> headersMap) {
    for (val key : headersMap.keySet()) {
      if (header.equals(key)) {
        return headersMap.getFirst(key);
      }
    }

    return null;
  }

}
