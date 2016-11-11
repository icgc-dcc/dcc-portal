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
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.Family.SERVER_ERROR;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.icgc.dcc.portal.server.model.Error;
import org.icgc.dcc.portal.server.service.BadGatewayException;
import org.springframework.stereotype.Component;

import lombok.NoArgsConstructor;
import lombok.Value;

@Component
@Provider
public class BadGatewayExceptionMapper implements ExceptionMapper<BadGatewayException> {

  private final static StatusType STATUS = new BadGatewayStatus();

  @Override
  public Response toResponse(BadGatewayException e) {
    return status(STATUS)
        .type(APPLICATION_JSON_TYPE)
        .entity(errorResponse(e))
        .build();
  }

  private Error errorResponse(BadGatewayException e) {
    return new Error(STATUS, e.getMessage());
  }

  @Value
  @NoArgsConstructor
  private static class BadGatewayStatus implements StatusType {

    private final int statusCode = 502;
    private final String reasonPhrase = "Bad Gateway";
    private final Family family = SERVER_ERROR;

  }

}
