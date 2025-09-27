package com.example.demo;

import com.github.benmanes.caffeine.cache.Caffeine;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

class TokenServiceMissingKeyTest {
	static MockWebServer server;

	@BeforeAll
	static void start() throws IOException {
		server = new MockWebServer();
		server.start();
	}

	@AfterAll
	static void stop() throws IOException {
		server.shutdown();
	}

	@Test
	void missingTokenKeyFails() {
		var webClient = WebClient.builder().build();
		var props = new AuthProperties();
		props.setUrl(server.url("/auth/login").toString());
		props.setUsername("u");
		props.setPassword("p");
		props.setExpiresInMins(30);
		props.setTokenJsonKey("accessToken");

		// Respuesta sin 'accessToken'
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).setBody("{\"token\":\"abc\"}"));

		var svc = new TokenService(webClient, props, Caffeine.newBuilder().maximumSize(1).build());

		StepVerifier.create(svc.refreshToken())
				.expectErrorMatches(
						e -> e instanceof IllegalStateException && e.getMessage().contains("No se encontró la clave"))
				.verify();
	}
}
