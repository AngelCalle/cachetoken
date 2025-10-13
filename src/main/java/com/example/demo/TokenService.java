package com.example.demo;

import com.github.benmanes.caffeine.cache.Cache;

import io.micrometer.common.lang.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.BodyInserters.FormInserter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor // genera el constructor con los campos final/@NonNull
public class TokenService {
	
	private static final String CACHE_KEY = "authToken";	
	private static final String BEARER = "Bearer ";

    private final @NonNull WebClient webClient;
    private final @NonNull AuthProperties props;
    private final @NonNull Cache<String, TokenCacheValue> cache;

    /** Devuelve SIEMPRE el valor completo del header Authorization (p. ej., "Bearer xxx") */
	public Mono<String> getValidToken() {
		TokenCacheValue cached = cache.getIfPresent(CACHE_KEY);
		if (cached != null && cached.getExpiresAt().isAfter(Instant.now())) {
			return Mono.just(cached.getToken()); // ya viene con "Bearer "
		}
		return refreshToken();
	}

	/** Refresca y devuelve SIEMPRE "Bearer xxx" */
	public synchronized Mono<String> refreshToken() {
		// doble check por si otro hilo refrescó
		TokenCacheValue again = cache.getIfPresent(CACHE_KEY);
		if (again != null && again.getExpiresAt().isAfter(Instant.now())) {
			return Mono.just(again.getToken());
		}
		
		FormInserter<String> body = BodyInserters
		.fromFormData("grant_type", "client_credentials")
                .with("client_id", props.getClientId())
                .with("client_secret", props.getClientSecret());
		
	     if (props.getScope() != null && !props.getScope().isBlank()) {
	            body = body.with("scope", props.getScope());
	       }

	    return webClient
	            .post()
	            .uri(props.getUrl())
	            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
	            .accept(MediaType.APPLICATION_JSON)
	            .body(body)
	            .retrieve()
	            .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
	                resp -> resp.bodyToMono(String.class)
	                    .defaultIfEmpty("")
	                    .flatMap(b -> Mono.error(new IllegalStateException(
	                        "Fallo autenticando (" + resp.statusCode() + "): " + b))))
	            .bodyToMono(AuthResponse.class)
	            .flatMap(ar -> {
	            	
	                String token = ar.getAccessToken();
	                if (token == null || token.isBlank()) {
	                    return Mono.error(new IllegalStateException("No se encontró 'access_token' en la respuesta OAuth"));
	                }

	                long skew = Math.max(0L, props.getExpiresInMins()); // p. ej., 60
	                long expiresInSec = (ar.getExpiresIn() != null && ar.getExpiresIn() > skew)
	                    ? ar.getExpiresIn() - skew
	                    : 60L; // fallback mínimo

	                Duration ttl = Duration.ofSeconds(expiresInSec);
	                Instant expiresAt = Instant.now().plus(ttl);

	                String bearer = BEARER + token;        // opcional
	                // Guarda solo el valor del token o "Bearer <token>" si te resulta más cómodo
	                cache.put(CACHE_KEY, TokenCacheValue.builder()
	                	    .token(bearer)
	                	    .expiresAt(expiresAt)
	                	    .build());
	                
	                // No expongas secretos en logs. Si necesitas log, usa nivel debug e imprime el TTL.
	                log.error("Nuevo access token; expira en {}s; token = {} ",
	                    Duration.between(Instant.now(), expiresAt).toSeconds(),
	                    mask(bearer));
	             
	                return Mono.just(token);
	            });
	    
		}
	
		//	Helper para enmascarar:
		private static String mask(String token) {
		    if (token == null || token.length() < 10) return "****";
		    int n = token.length();
		    return token.substring(0, 6) + " …" + token.substring(n - 4);
		}
		
}
