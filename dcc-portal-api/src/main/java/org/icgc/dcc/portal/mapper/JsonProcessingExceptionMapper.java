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
package org.icgc.dcc.portal.mapper;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.icgc.dcc.portal.model.Error;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {

  /**
   * Constants.
   */
  private static final Status STATUS = BAD_REQUEST;

  /**
   * Dependencies
   */
  @Context
  private HttpServletRequest request;

  @Override
  public Response toResponse(JsonProcessingException exception) {
    // If the error is in the JSON generation, it's a server error.
    if (exception instanceof JsonGenerationException) {
      log.warn("Error generating JSON", exception);
      return Response.serverError().build();
    }

    // If we can't deserialize the JSON because someone forgot a no-arg constructor, it's a server error and we should
    // inform the developer.
    String message = exception.getMessage();
    if (message.startsWith("No suitable constructor found")) {
      log.error("Unable to deserialize the specific type", exception);
      return Response.serverError().build();
    }

    if (exception instanceof UnrecognizedPropertyException) {
      val unrecognized = (UnrecognizedPropertyException) exception;
      message = "Unrecognized field '" + unrecognized.getPropertyName() + "' in "
          + unrecognized.getReferringClass().getSimpleName().toLowerCase() + ". Valid fields are: "
          + unrecognized.getKnownPropertyIds();
    }

    // Otherwise, it's those pesky users.
    log.debug("Unable to process JSON", exception);
    return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(new Error(STATUS, message))
        .build();
  }

}
