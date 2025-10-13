package com.example.demo;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsersClient {

    private final @NonNull WebClient webClient;
    private final @NonNull AuthProperties authProperties; // elimínalo si no lo usas
    private final @NonNull TokenService tokenService;

	public Mono<String> addUser(Object payload) {
		// Obtiene token válido de cache (o refresca si hizo falta), hace POST y
		// reintenta 1 vez si 401
		// 1) Usa token válido
		return tokenService
				.getValidToken()
				.flatMap(tok -> sendPost(authProperties.getUrl(), payload, tok))
				// 2) Si 401, refresca y reintenta una sola vez
				.onErrorResume(WebClientResponseException.Unauthorized.class,
						e -> tokenService.refreshToken().flatMap(tok -> sendPost(authProperties.getUrl(), payload, tok)));
	}

	private Mono<String> sendPost(String url, Object body, String authHeaderValue) {
		return webClient
				.post()
				.uri(url)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, authHeaderValue) // ya viene "Bearer …"
				.retrieve()
				.onStatus(s -> s.isError(), resp -> resp.bodyToMono(String.class).defaultIfEmpty("")
						.flatMap(b -> Mono.error(
								new IllegalStateException("POST " + url + " -> " + resp.statusCode() + " : " + b))))
				.bodyToMono(String.class);
	}

}
