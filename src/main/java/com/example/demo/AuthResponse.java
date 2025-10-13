package com.example.demo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {

    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("token_type")
    private String tokenType; // e.g., "Bearer"
    
    @JsonProperty("expires_in")
    private Long expiresIn;   // en segundos
    
    @JsonProperty("scope")
    private String scope;
  
}
