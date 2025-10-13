package com.example.demo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.auth")
@Validated
public class AuthProperties {

  @NotBlank
  private String url;

  @NotBlank
  private String clientId;

  @NotBlank
  private String clientSecret;

  @NotBlank
  private String scope;
  
  @Min(1)
  private int expiresInMins = 30;

  @NotBlank
  private String tokenJsonKey = "accessToken";

}
