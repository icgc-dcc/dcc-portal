package org.icgc.dcc.portal.server.resource.ui;

import io.swagger.annotations.Api;
import lombok.NonNull;
import org.icgc.dcc.portal.server.config.ServerProperties;
import org.icgc.dcc.portal.server.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Api(hidden = true)
@Path("/v1/ui/message")
@Produces(APPLICATION_JSON)
public class UIMessageResource extends Resource {

  @NonNull
  private final ServerProperties properties;

  @Autowired
  public UIMessageResource(ServerProperties properties) {
    this.properties = properties;
  }

  @GET
  public Map<String, String> getMessage() {
    return properties.getBanner().getJsonMessage();
  }

}
