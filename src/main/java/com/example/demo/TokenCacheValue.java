package com.example.demo;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TokenCacheValue {
	
	private final String token;
	private final Instant expiresAt;

}
