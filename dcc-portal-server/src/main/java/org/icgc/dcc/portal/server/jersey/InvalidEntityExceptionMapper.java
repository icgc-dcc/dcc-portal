package org.icgc.dcc.portal.server.jersey;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.status;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.icgc.dcc.portal.server.model.Error;
import org.icgc.dcc.portal.server.util.InvalidEntityException;
import org.springframework.stereotype.Component;

@Component
@Provider
public class InvalidEntityExceptionMapper implements ExceptionMapper<InvalidEntityException> {

  private static final int UNPROCESSABLE_ENTITY = 422;


  @Override
  public Response toResponse(InvalidEntityException e) {
    return status(UNPROCESSABLE_ENTITY)
        .type(APPLICATION_JSON_TYPE)
        .entity(errorResponse(e))
        .build();
  }

  private Error errorResponse(InvalidEntityException e) {
    return new Error(UNPROCESSABLE_ENTITY, e.getMessage());
  }

}
