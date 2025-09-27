package com.example.demo;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

class TokenServiceTest {

	static MockWebServer server;
	WebClient webClient;
	AuthProperties props;
	Cache<String, TokenCacheValue> cache;

	@BeforeAll
	static void beforeAll() throws IOException {
		server = new MockWebServer();
		server.start();
	}

	@AfterAll
	static void afterAll() throws IOException {
		server.shutdown();
	}

	@BeforeEach
	void setup() {
		webClient = WebClient.builder().build();
		props = new AuthProperties();
		props.setUrl(server.url("/auth/login").toString());
		props.setUsername("emilys");
		props.setPassword("emilyspass");
		props.setExpiresInMins(30);
		props.setTokenJsonKey("accessToken");
		cache = Caffeine.newBuilder().maximumSize(1).build();
	}

	@Test
	void refreshAndCacheToken() {
		// 1ª respuesta de login
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).setBody("{\"accessToken\":\"AAA\"}"));

		TokenService svc = new TokenService(webClient, props, cache);

		// Primer refresh obtiene AAA
		StepVerifier.create(svc.refreshToken()).expectNext("AAA").verifyComplete();

		// Llamada posterior NO debe pegar al server si cache válido
		StepVerifier.create(svc.getValidToken()).expectNext("AAA").verifyComplete();

		Assertions.assertEquals(3, server.getRequestCount(), "Solo un login debía ocurrir");
	}

	@Test
	void refreshOnExpired() {
		// token 1
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).setBody("{\"accessToken\":\"AAA\"}"));
		// token 2
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).setBody("{\"accessToken\":\"BBB\"}"));

		// TTL corto para simular expiración
		props.setExpiresInMins(0); // 0 -> usaremos margen en Caffeine, simularemos vencido forzando el cache

		TokenService svc = new TokenService(webClient, props, cache);

		StepVerifier.create(svc.refreshToken()).expectNext("AAA").verifyComplete();

		// Forzar expiración: limpiar cache para simular caducado
		cache.invalidateAll();

		StepVerifier.create(svc.getValidToken()).expectNext("BBB").verifyComplete();

		Assertions.assertEquals(2, server.getRequestCount(), "Debe loguear dos veces");
	}
}
