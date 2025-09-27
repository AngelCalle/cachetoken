package com.example.demo;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

  MockWebServer server;
  Cache<String, TokenCacheValue> cache;
  TokenService service;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();

    WebClient webClient = WebClient.builder().baseUrl(server.url("/").toString()).build();
    cache = Caffeine.newBuilder().maximumSize(2).build();

    service = new TokenService(webClient, cache);

    // Inyectamos propiedades privadas (@Value) por reflexión
    setField(service, "clientSecret", "s3cr3t");
    setField(service, "clientId", "client-123");
    setField(service, "ibpUrl", "https://ignored.com"); // no usado aquí
    setField(service, "ibpPath", "/ignored");           // no usado aquí
    setField(service, "tokenUrl", "/oauth/token");
  }

  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
    cache.invalidateAll();
  }

  // ======== CASOS ========

  @Test
  void devuelveTokenDelCacheSiVigente() {
    Instant future = Instant.now().plusSeconds(120);
    cache.put("authToken", new TokenCacheValue(future, "CACHED-TOKEN"));

    StepVerifier.create(service.getValidToken())
        .expectNext("CACHED-TOKEN")
        .verifyComplete();

    // No se llamó al servidor (cola vacía)
    assertThat(server.getRequestCount()).isZero();
  }

  @Test
  void cuandoNoHayTokenLlamaEndpointYCachea() throws Exception {
    // Respuesta “válida”
    String body = "{\"access_token\":\"NEW-TOKEN\",\"expires_in\":90}";
    server.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body));

    StepVerifier.create(service.getValidToken())
        .expectNext("NEW-TOKEN")
        .verifyComplete();

    // Verificamos request
    RecordedRequest req = server.takeRequest();
    assertThat(req.getMethod()).isEqualTo("POST");
    assertThat(req.getPath()).isEqualTo("/oauth/token");
    String form = req.getBody().readString(StandardCharsets.UTF_8);
    assertThat(form)
        .contains("grant_type=client_credentials")
        .contains("client_id=client-123")
        .contains("client_secret=s3cr3t");

    // Verificamos que cacheó con expiración (~ now + (90 - safety))
    TokenCacheValue cached = cache.getIfPresent("authToken");
    assertThat(cached).isNotNull();
    assertThat(cached.getToken()).isEqualTo("NEW-TOKEN");

    // safety en tu código es 30 (seg)
    long safety = 30L;
    Duration diff = Duration.between(Instant.now().plusSeconds(90 - safety), cached.getExpiresAt());
    // Permitimos ~5s de tolerancia por tiempos de ejecución
    assertThat(Math.abs(diff.getSeconds())).isLessThanOrEqualTo(5);
  }

  @Test
  void siCacheVencidoRefrescaYReemplaza() {
    // Poblamos cache con vencido
    cache.put("authToken",
        new TokenCacheValue(Instant.now().minusSeconds(1), "OLD"));

    server.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("{\"access_token\":\"REFRESHED\",\"expires_in\":40}"));

    StepVerifier.create(service.getValidToken())
        .expectNext("REFRESHED")
        .verifyComplete();

    assertThat(cache.getIfPresent("authToken").getToken()).isEqualTo("REFRESHED");
  }

  @Test
  void respondeErrorAnteHttp5xx() {
    server.enqueue(new MockResponse()
        .setResponseCode(500)
        .setBody("boom"));

    StepVerifier.create(service.getValidToken())
        .expectErrorSatisfies(err ->
            assertThat(err)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fallo autenticando")
        )
        .verify();
  }

  @Test
  void errorSiRespuestaSinAccessToken() {
    server.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("{\"expires_in\":60}"));

    StepVerifier.create(service.getValidToken())
        .expectErrorSatisfies(err ->
            assertThat(err)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sin access_token"))
        .verify();
  }

  // ======== helpers ========

  private static void setField(Object target, String field, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(field);
    f.setAccessible(true);
    f.set(target, value);
  }
}
