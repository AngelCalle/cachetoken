package com.example.demo;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

class TokenServiceErrorsTest {
	static MockWebServer server;
	WebClient webClient;
	AuthProperties props;
	Cache<String, TokenCacheValue> cache;

	@BeforeAll
	static void start() throws IOException {
		server = new MockWebServer();
		server.start();
	}

	@AfterAll
	static void stop() throws IOException {
		server.shutdown();
	}

	@BeforeEach
	void setUp() {
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
	void auth401MapsToIllegalState() {
		server.enqueue(new MockResponse().setResponseCode(401).setBody("bad creds"));
		TokenService svc = new TokenService(webClient, props, cache);

		StepVerifier.create(svc.refreshToken())
				.expectErrorMatches(
						e -> e instanceof IllegalStateException && e.getMessage().contains("Fallo autenticando")
								&& e.getMessage().contains("401") && e.getMessage().contains("bad creds"))
				.verify();
	}

	@Test
	void auth500MapsToIllegalState() {
		server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));
		TokenService svc = new TokenService(webClient, props, cache);

		StepVerifier.create(svc.refreshToken())
				.expectErrorMatches(e -> e instanceof IllegalStateException && e.getMessage().contains("500")).verify();
	}
}
