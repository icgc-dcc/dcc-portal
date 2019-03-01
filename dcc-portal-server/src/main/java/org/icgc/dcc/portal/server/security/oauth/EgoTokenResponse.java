package org.icgc.dcc.portal.server.security.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Set;

@EqualsAndHashCode
@Getter
public class EgoTokenResponse {
    String accessToken;
    private Set<String> scope;
    private Long exp;
    private String description;

    public EgoTokenResponse(
    @JsonProperty("accessToken") String accessToken,
    @JsonProperty("scope") Set<String> scope,
    @JsonProperty("exp") Long exp,
    @JsonProperty("description") String description){
        this.accessToken = accessToken;
        this.scope = scope;
        this.exp = exp;
        this.description = description;
    };
}
