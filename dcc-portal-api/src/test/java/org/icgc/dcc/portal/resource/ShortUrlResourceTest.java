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
package org.icgc.dcc.portal.resource;

import static org.icgc.dcc.portal.resource.ResourceTestUtils.assertOK;
import static org.icgc.dcc.portal.resource.ShortUrlResource.SHORT_URL_PARAM;
import static org.mockito.Mockito.when;
import lombok.val;

import org.icgc.dcc.common.client.api.ICGCUnknownException;
import org.icgc.dcc.common.client.api.shorturl.ShortURLClient;
import org.icgc.dcc.common.client.api.shorturl.ShortURLResponse;
import org.icgc.dcc.portal.config.PortalProperties.WebProperties;
import org.icgc.dcc.portal.mapper.NotFoundExceptionMapper;
import org.icgc.dcc.portal.service.BadRequestException;
import org.icgc.dcc.portal.service.ServiceUnavailableException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sun.jersey.api.client.ClientResponse;
import com.yammer.dropwizard.testing.ResourceTest;

@RunWith(MockitoJUnitRunner.class)
public class ShortUrlResourceTest extends ResourceTest {

  private static final String RESOURCE = "/v1/short";

  private static final String FAILED_RESPONSE_URL = "http://example.org/blah";
  private static final String BASE_URL = "https://dcc.icgc.org";
  private static final String REQUEST_URL = BASE_URL + "/genes/123";

  @Mock
  ShortURLClient shortURLClient;

  ShortUrlResource shortUrlResource;

  @Override
  protected void setUpResources() throws Exception {
    val webConfig = new WebProperties();
    webConfig.setBaseUrl(BASE_URL);

    shortUrlResource = new ShortUrlResource(shortURLClient, webConfig);
    addResource(shortUrlResource);
    addProvider(NotFoundExceptionMapper.class);
  }

  @Test
  public void successfulTest() {
    when(shortURLClient.shorten(REQUEST_URL)).thenReturn(getSuccessfulResponse());

    val response = getResourceResponse(REQUEST_URL);
    assertOK(response);
  }

  @Test(expected = BadRequestException.class)
  public void failedTest() {
    when(shortURLClient.shorten(FAILED_RESPONSE_URL)).thenReturn(getFailedResponse());

    getResourceResponse(FAILED_RESPONSE_URL);
  }

  @Test(expected = BadRequestException.class)
  public void parameterTest_empty() {
    getResourceResponse("");
  }

  @Test(expected = BadRequestException.class)
  public void parameterTest_wrong_domain() {
    getResourceResponse("http://google.ca/test");
  }

  @Test(expected = BadRequestException.class)
  public void parameterTest_no_domain() {
    getResourceResponse("/test");
  }

  @Test(expected = ServiceUnavailableException.class)
  public void icgcExceptionTest() {
    when(shortURLClient.shorten(REQUEST_URL)).thenThrow(
        new ICGCUnknownException(new Exception()));

    getResourceResponse(REQUEST_URL);
  }

  private ClientResponse getResourceResponse(String url) {
    return client().resource(RESOURCE).queryParam(SHORT_URL_PARAM, url).get(ClientResponse.class);
  }

  private static ShortURLResponse getSuccessfulResponse() {
    return new ShortURLResponse(
        true, // success
        "", // error
        REQUEST_URL, // longUrl
        "http://beta-tra.icgc.org/FoO", // shortUrl
        1); // user
  }

  private static ShortURLResponse getFailedResponse() {
    return new ShortURLResponse(
        false, // success
        "Invalid URL.", // error
        FAILED_RESPONSE_URL, // longUrl
        "", // shortUrl
        1); // user
  }

}
