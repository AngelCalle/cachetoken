package com.example.demo;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class TokenCacheValue {
	TokenCacheValue( String token,Instant expiresAt) {
		this.token = token;
		this.expiresAt = expiresAt;
	}
	TokenCacheValue( Instant expiresAt) {
		this.token = "";
		this.expiresAt = expiresAt;
	}
	private final String token;
	private final Instant expiresAt;

}
