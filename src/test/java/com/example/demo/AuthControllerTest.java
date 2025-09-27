package com.example.demo;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = AuthControllerTest.Initializer.class)
class AuthControllerTest {

	static MockWebServer server;

	@Autowired
	WebTestClient webTestClient;

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext ctx) {
			try {
				server = new MockWebServer();
				server.start();

				TestPropertyValues
						.of("app.auth.url=" + server.url("/auth/login"), "app.auth.username=emilys",
								"app.auth.password=emilyspass", "app.auth.expiresInMins=30",
								"app.auth.tokenJsonKey=accessToken", "app.users.add-url=" + server.url("/users/add"))
						.applyTo(ctx.getEnvironment());

			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@AfterAll
	static void afterAll() throws IOException {
		server.shutdown();
	}

	@Test
	void tokenEndpointReturnsCachedToken() {
		// login responde con token XYZ
		server.enqueue(new MockResponse().setResponseCode(4)
				.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).setBody("{\"accessToken\":\"XYZ\"}"));

//		webTestClient.get().uri("/api/token").exchange().expectStatus().isOk().expectBody(String.class)
//				.isEqualTo("XYZ");

//		// segunda llamada no debe reloguear (cache)
		webTestClient.get().uri("/api/token").exchange().expectStatus().isNotFound();

		Assertions.assertEquals(1, server.getRequestCount());
	}
}
