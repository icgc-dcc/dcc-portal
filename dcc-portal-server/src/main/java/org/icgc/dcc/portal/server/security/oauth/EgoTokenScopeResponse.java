package org.icgc.dcc.portal.server.security.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Set;

@EqualsAndHashCode
@Getter
public class EgoTokenScopeResponse {
  private String userName;
  private String clientId;
  private Long exp;
  private Set<String> scope;

  public EgoTokenScopeResponse(@JsonProperty("user_name") String userName,
                               @JsonProperty("client_id") String clientId,
                               @JsonProperty("exp") Long exp,
                               @JsonProperty("scope") Set<String> scope){
    this.userName = userName;
    this.clientId = clientId;
    this.exp = exp;
    this.scope = scope;

  }


}
