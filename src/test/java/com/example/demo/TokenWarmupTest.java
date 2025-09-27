package com.example.demo;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.time.Duration;

@SpringBootTest
@ContextConfiguration(initializers = TokenWarmupTest.Initializer.class)
class TokenWarmupTest {

	static MockWebServer server;

	static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext ctx) {
			try {
				server = new MockWebServer();
				server.start();

				// Respuesta de login para el warm-up
				server.enqueue(new MockResponse().setResponseCode(200)
						.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.setBody("{\"accessToken\":\"WARM\"}"));

				TestPropertyValues
						.of("app.auth.url=" + server.url("/auth/login"), "app.auth.username=u", "app.auth.password=p",
								"app.auth.expiresInMins=30", "app.auth.tokenJsonKey=accessToken")
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
	void tokenIsLoadedAtStartup() {
		// Espera a que el warm-up dispare una petición
		Awaitility.await().atMost(Duration.ofSeconds(3)).until(() -> server.getRequestCount() >= 1);
	}
}
