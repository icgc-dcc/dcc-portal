package org.icgc.dcc.portal.server.security.oauth;

import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

public class HttpMultiStatus implements StatusType {
  private final int status = 207;
  private final Family family = SUCCESSFUL;
  private final String reasonPhrase = "Multi-Status";

  @Override
  public int getStatusCode() {
    return status;
  }

  @Override
  public Family getFamily() {
    return family;
  }

  @Override
  public String getReasonPhrase() {
    return reasonPhrase;
  }

}