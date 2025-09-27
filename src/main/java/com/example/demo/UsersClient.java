package com.example.demo;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

@Component
public class UsersClient {

	private final WebClient webClient;
	private final TokenService tokenService;

	public UsersClient(WebClient webClient, TokenService tokenService) {
		this.webClient = webClient;
		this.tokenService = tokenService;
	}

	private static final String URL = "https://dummyjson.com/users/add";

	public Mono<String> addUser(Object payload) {
		// Obtiene token válido de cache (o refresca si hizo falta), hace POST y
		// reintenta 1 vez si 401
		return tokenService.getValidToken().flatMap(tok -> doPostWithToken(URL, payload, tok, true));
	}

	private Mono<String> doPostWithToken(String url, Object body, String token, boolean allowRetryOn401) {
		return webClient.post().uri(url).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).bodyValue(body).exchangeToMono(resp -> {
					int code = resp.statusCode().value();

					if (code == 401 && allowRetryOn401) {
						// refrescar y reintentar una sola vez
						return tokenService.refreshToken()
								.flatMap(fresh -> webClient.post().uri(url).contentType(MediaType.APPLICATION_JSON)
										.accept(MediaType.APPLICATION_JSON)
										.header(HttpHeaders.AUTHORIZATION, "Bearer " + fresh).bodyValue(body).retrieve()
										.bodyToMono(String.class));
					}

					if (resp.statusCode().isError()) {
						return resp.bodyToMono(String.class).defaultIfEmpty("").flatMap(b -> Mono.error(
								new IllegalStateException("POST " + url + " -> " + resp.statusCode() + " : " + b)));
					}

					return resp.bodyToMono(String.class);
				});
	}
}
