package com.example.demo;

import com.github.benmanes.caffeine.cache.Caffeine;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Map;

class UsersClientRetryFailTest {
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
	void retryOnceThenFailIfStill401() {
		var webClient = WebClient.builder().build();

		var props = new AuthProperties();
		props.setUrl(server.url("/auth/login").toString());
		props.setUsername("u");
		props.setPassword("p");
		props.setExpiresInMins(30);
		props.setTokenJsonKey("accessToken");

		// login -> AAA
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).setBody("{\"accessToken\":\"AAA\"}"));
		// POST -> 401
		server.enqueue(new MockResponse().setResponseCode(401));
		// relogin -> BBB
		server.enqueue(new MockResponse().setResponseCode(200)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).setBody("{\"accessToken\":\"BBB\"}"));
		// segundo POST -> 401 otra vez => error
		server.enqueue(new MockResponse().setResponseCode(401));

		var tokenSvc = new TokenService(webClient, props, Caffeine.newBuilder().maximumSize(1).build());
		var client = new UsersClient(webClient, tokenSvc);


		StepVerifier.create(client.addUser(Map.of("firstName", "Ada")))
	    .expectNextMatches(body -> body != null && !body.isBlank())
	    .verifyComplete();


		Assertions.assertEquals(1, server.getRequestCount());
	}
}
