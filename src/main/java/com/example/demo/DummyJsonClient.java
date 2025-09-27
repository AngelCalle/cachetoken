package com.example.demo;


import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class DummyJsonClient {
	private final WebClient webClient;
	private final TokenService tokenService;

	public DummyJsonClient(WebClient webClient, TokenService tokenService) {
		this.webClient = webClient;
		this.tokenService = tokenService;
	}

	public Mono<String> getMe() {
		return tokenService.getValidToken().flatMap(tok -> doGetWithToken("https://dummyjson.com/auth/me", tok, true));
	}

	private Mono<String> doGetWithToken(String url, String token, boolean allowRetryOn401) {
		return webClient.get().uri(url).accept(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchangeToMono(resp -> {
					int code = resp.statusCode().value();

					if (code == 401 && allowRetryOn401) {
						return tokenService.refreshToken()
								.flatMap(fresh -> webClient.get().uri(url)
										.accept(MediaType.APPLICATION_JSON)
										.header(HttpHeaders.AUTHORIZATION, "Bearer " + fresh)
										.retrieve().bodyToMono(String.class));
					}

					if (resp.statusCode().isError()) {
						return resp.bodyToMono(String.class).defaultIfEmpty("").flatMap(b -> Mono
								.error(new IllegalStateException("Error " + resp.statusCode() + " : " + b)));
					}

					return resp.bodyToMono(String.class);
				});
	}
}
