package com.example.demo;

import com.github.benmanes.caffeine.cache.Cache;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Slf4j
@Service
public class TokenService {
	
	private static final String CACHE_KEY = "authToken";

	private final WebClient webClient;
	private final AuthProperties props;
	private final Cache<String, TokenCacheValue> cache;

	public TokenService(WebClient webClient, AuthProperties props, Cache<String, TokenCacheValue> cache) {
		this.webClient = webClient;
		this.props = props;
		this.cache = cache;
	}

	public Mono<String> getValidToken() {
		TokenCacheValue cached = cache.getIfPresent(CACHE_KEY);
		if (cached != null && cached.getExpiresAt().isAfter(Instant.now())) {
			return Mono.just(cached.getToken());
		}
		return refreshToken();
	}

	public synchronized Mono<String> refreshToken() {
		// doble check por si otro hilo refrescó
		TokenCacheValue again = cache.getIfPresent(CACHE_KEY);
		if (again != null && again.getExpiresAt().isAfter(Instant.now())) {
			return Mono.just(again.getToken());
		}

		AuthRequest body = new AuthRequest(props.getUsername(), props.getPassword(), props.getExpiresInMins());

		return webClient.post().uri(props.getUrl()).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).bodyValue(body).retrieve()
				.onStatus(s -> s.is4xxClientError() || s.is5xxServerError(), resp -> resp.bodyToMono(String.class)
						.defaultIfEmpty("")
						.flatMap(b -> Mono.error(
								new IllegalStateException("Fallo autenticando (" + resp.statusCode() + "): " + b))))
				.bodyToMono(AuthResponse.class).flatMap(ar -> {
					Object tokenRaw = ar.get(props.getTokenJsonKey());
					if (tokenRaw == null) {
						return Mono.error(
								new IllegalStateException("No se encontró la clave '" + props.getTokenJsonKey() + "'"));
					}
					String token = Objects.toString(tokenRaw);
					Duration ttl = Duration.ofMinutes(props.getExpiresInMins());
					Instant expiresAt = Instant.now().plus(ttl);
					cache.put(CACHE_KEY, new TokenCacheValue(token, expiresAt));
					return Mono.just(token);
				});
	}
}
