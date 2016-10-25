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
package org.icgc.dcc.portal.server.jersey.mapper;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.serverError;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.icgc.dcc.portal.server.util.HttpServletRequests.getHeadersFromRequest;
import static org.icgc.dcc.portal.server.util.HttpServletRequests.getHttpRequestCallerInfo;

import java.util.Date;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.icgc.dcc.common.core.mail.Mailer;
import org.icgc.dcc.common.core.report.BufferedReport;
import org.icgc.dcc.common.core.report.ReportEmail;
import org.icgc.dcc.portal.server.model.Error;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Provider
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

  private static final Random RANDOM = new Random();

  @NonNull
  private final Mailer mailer;

  @Context
  private HttpHeaders headers;
  @Context
  private HttpServletRequest request;

  private static Response buildErrorResponse(@NonNull final ResponseBuilder builder, @NonNull final Error error) {
    return builder.type(APPLICATION_JSON_TYPE)
        .entity(error)
        .build();
  }

  private static Error webErrorResponse(Throwable t, final long id, final int statusCode) {
    return new Error(statusCode, t.getMessage());
  }

  private static Error errorResponse(Throwable t, final long id) {
    return new Error(INTERNAL_SERVER_ERROR, formatResponseEntity(id, t));
  }

  protected static void logException(long id, Throwable t) {
    log.error(formatLogMessage(id, t), t);
  }

  protected static String formatResponseEntity(long id, Throwable t) {
    val message =
        "There was an error processing your request, with the message of '%s'. It has been logged (ID %016x).%n";
    return String.format(message, t.getMessage(), id);
  }

  protected static String formatLogMessage(long id, Throwable t) {
    val message = "Error handling a request: %016x, with the message of '%s'.";
    return String.format(message, id, t.getMessage());
  }

  protected static long randomId() {
    return RANDOM.nextLong();
  }

  @Override
  @SneakyThrows
  public Response toResponse(Throwable t) {
    val id = randomId();

    if (t instanceof WebApplicationException) {
      val response = ((WebApplicationException) t).getResponse();
      val responseBuilder = Response.fromResponse(response);
      val statusCode = response.getStatus();

      val ok = statusCode < 400;
      if (ok) {
        return responseBuilder.build();
      } else {
        logException(id, t);

        if (statusCode >= 500) {
          sendEmail(id, t);
        }

        return buildErrorResponse(responseBuilder, webErrorResponse(t, id, statusCode));
      }
    }

    logException(id, t);
    sendEmail(id, t);

    return buildErrorResponse(serverError(), errorResponse(t, id));
  }

  protected void sendEmail(long id, Throwable t) {
    try {
      val report = new BufferedReport();
      report.addException((Exception) t);
      report.addInfo("Info = %s", getHttpRequestCallerInfo(request));
      report.addInfo("Request URL = %s", request.getRequestURI());
      report.addInfo("Request Method = %s", request.getMethod());
      report.addInfo("Request Query String = %s", request.getQueryString());
      report.addInfo("Request Headers= %s", getHeadersFromRequest(request));
      report.addInfo("Date = %s", new Date());

      val email = new ReportEmail("DCC Portal", report);
      mailer.sendMail(email);
    } catch (Exception e) {
      log.error("Exception mailing:", e);
    }
  }

}
