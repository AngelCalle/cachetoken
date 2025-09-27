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
import java.time.Instant;
import java.util.Map;

class UsersClientTest {

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
		props.setUsername("u");
		props.setPassword("p");
		props.setExpiresInMins(30);
		props.setTokenJsonKey("accessToken");
		cache = Caffeine.newBuilder().maximumSize(1).build();
	}

	@Test
	void postRetriesOnceOn401WithRefresh() {
		// 1) login -> token AAA
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).setBody("{\"accessToken\":\"AAA\"}"));

		// 2) primer POST -> 401
		server.enqueue(new MockResponse().setResponseCode(401));

		// 3) re-login -> token BBB
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).setBody("{\"accessToken\":\"BBB\"}"));

		// 4) segundo POST -> 200 OK con cuerpo
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).setBody("{\"ok\":true}"));

		TokenService tokenService = new TokenService(webClient, props, cache);
		UsersClient client = new UsersClient(webClient, tokenService);

		StepVerifier.create(client.addUser(Map.of("firstName", "Ada")))
	    .expectNextMatches(body -> body != null && !body.isBlank())
	    .verifyComplete();

		Assertions.assertEquals(1, server.getRequestCount(), "login, 401, relogin, 200");
	}
}
