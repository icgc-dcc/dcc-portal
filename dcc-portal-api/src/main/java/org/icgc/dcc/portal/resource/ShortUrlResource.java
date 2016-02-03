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

import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.validator.routines.UrlValidator.ALLOW_LOCAL_URLS;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.validator.routines.UrlValidator;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import org.icgc.dcc.common.client.api.ICGCException;
import org.icgc.dcc.common.client.api.shorturl.ShortURLClient;
import org.icgc.dcc.common.client.api.shorturl.ShortURLResponse;
import org.icgc.dcc.portal.config.PortalProperties.WebProperties;
import org.icgc.dcc.portal.model.ShortUrl;
import org.icgc.dcc.portal.service.BadRequestException;
import org.icgc.dcc.portal.service.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Component
@Slf4j
@Path("/v1/short")
@Produces(APPLICATION_JSON)
@Api(value = "/short", description = "URL shortening service")
public class ShortUrlResource {

  public static final String SHORT_URL_PARAM = "url";

  private static final String LONG_DESCRIPTION =
      "The valid ICGC Data Portal URL to be shortened.";
  private static final UrlValidator URL_VALIDATOR = new UrlValidator(ALLOW_LOCAL_URLS);

  @NonNull
  private final ShortURLClient apiClient;

  @URL
  @NotEmpty
  private final String baseUrl;

  @Autowired
  public ShortUrlResource(@NonNull ShortURLClient apiClient, @NonNull WebProperties config) {
    this.apiClient = apiClient;
    // Ensure baseUrl does not end with '/'
    this.baseUrl = config.getBaseUrl().replaceFirst("/$", "");
  }

  @GET
  @ApiOperation(value = "Shorten URL", httpMethod = "GET", response = ShortUrl.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Given URL has been shortened"),
      @ApiResponse(code = 400, message = "Given URL has incorrect format"),
      @ApiResponse(code = 503, message = "Failed to shorten given URL") })
  public ShortUrl getShortUrl(
      @ApiParam(value = LONG_DESCRIPTION, required = true) @QueryParam(SHORT_URL_PARAM) String url) {

    checkQueryParameter(baseUrl, url);
    ShortURLResponse response = null;
    try {
      response = apiClient.shorten(url);
    } catch (ICGCException e) {
      log.error("{}", e.getMessage());
      throw new ServiceUnavailableException("Failed to shorten given URL");
    }

    checkState(response.isSuccess(), "Failed to get a short URL for '%s'", url);

    return new ShortUrl(response.getShortUrl(), response.getLongUrl());
  }

  private static void checkQueryParameter(String baseUrl, String url) {
    checkState(!isNullOrEmpty(url), "The url must not be empty");
    checkState(url.startsWith(baseUrl), "The url must be from %s", baseUrl);
    validateRequestUrl(url);
  }

  private static void validateRequestUrl(String url) {
    checkState(URL_VALIDATOR.isValid(url), "Request URL '%s' is not a valid URL", url);
  }

  private static void checkState(boolean expression, String message) {
    if (!expression) throw new BadRequestException(message);
  }

  private static void checkState(boolean expression, String errorMessageTemplate, Object... errorMessageArgs) {
    if (!expression) throw new BadRequestException(String.format(errorMessageTemplate, errorMessageArgs));
  }

}
