package com.example.demo;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TokenServiceTest_NoExtraLibs {

  private ExchangeFunction exchange; // mock del canal HTTP
  private WebClient webClient;
  private Cache<String, TokenCacheValue> cache;
  private TokenService service;

  @BeforeEach
  void setUp() throws Exception {
    exchange = mock(ExchangeFunction.class);
    webClient = WebClient.builder().exchangeFunction(exchange).build();
    cache = Caffeine.newBuilder().maximumSize(2).build();
    service = new TokenService(webClient, cache);

    // Inyectamos @Value privados (según tu clase)
    setField(service, "clientSecret", "s3cr3t");
    setField(service, "clientId", "client-123");
    setField(service, "ibpUrl", "https://unused");
    setField(service, "ibpPath", "/unused");
    setField(service, "tokenUrl", "/oauth/token");
  }

  @Test
  void devuelveTokenDelCacheSiVigente() {
    cache.put("authToken", new TokenCacheValue(Instant.now().plusSeconds(60), "CACHED"));

    String token = service.getValidToken().block();
    assertThat(token).isEqualTo("CACHED");

    // No se llamó a la red
    verify(exchange, never()).exchange(any());
  }

  @Test
  void cuandoNoHayTokenPosteaAlEndpointYCachea() {
    // simulamos respuesta 200 con JSON válido
    String json = "{\"access_token\":\"NEW\",\"expires_in\":90}";
    when(exchange.exchange(any())).thenReturn(
        reactor.core.publisher.Mono.just(ok(json))
    );

    String token = service.getValidToken().block();
    assertThat(token).isEqualTo("NEW");

    // Verificamos que se hizo POST y a la URI esperada
    ArgumentCaptor<ClientRequest> captor = ArgumentCaptor.forClass(ClientRequest.class);
    verify(exchange).exchange(captor.capture());
    ClientRequest req = captor.getValue();
    assertThat(req.method().name()).isEqualTo("POST");
    assertThat(req.url().getPath()).isEqualTo("/oauth/token");
    assertThat(req.headers().getFirst(HttpHeaders.ACCEPT)).contains("application/json");

    // Verificamos caché con margen de seguridad (30s en tu servicio)
    TokenCacheValue cached = cache.getIfPresent("authToken");
    assertThat(cached).isNotNull();
    long safety = 30L;
    Duration diff = Duration.between(Instant.now().plusSeconds(90 - safety), cached.getExpiresAt());
    assertThat(Math.abs(diff.getSeconds())).isLessThanOrEqualTo(5);
  }

  @Test
  void siCacheVencidoRefrescaYReemplaza() {
    cache.put("authToken", new TokenCacheValue(Instant.now().minusSeconds(1), "OLD"));

    when(exchange.exchange(any())).thenReturn(
        reactor.core.publisher.Mono.just(ok("{\"access_token\":\"REFRESH\",\"expires_in\":40}"))
    );

    String token = service.getValidToken().block();
    assertThat(token).isEqualTo("REFRESH");
    assertThat(cache.getIfPresent("authToken").getToken()).isEqualTo("REFRESH");
  }

  @Test
  void errorAnteHttp5xx() {
    when(exchange.exchange(any())).thenReturn(
        reactor.core.publisher.Mono.just(
            ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).body("boom").build())
    );

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> service.getValidToken().block());
    assertThat(ex).hasMessageContaining("Fallo autenticando");
  }

  @Test
  void errorSiRespuestaSinAccessToken() {
    when(exchange.exchange(any())).thenReturn(
        reactor.core.publisher.Mono.just(ok("{\"expires_in\":60}"))
    );

    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> service.getValidToken().block());
    assertThat(ex).hasMessageContaining("sin access_token");
  }

  // ==== helpers ====

  /** ClientResponse 200 application/json */
  private static ClientResponse ok(String body) {
    return ClientResponse.create(HttpStatus.OK)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(body.getBytes(StandardCharsets.UTF_8))
        .build();
  }

  private static void setField(Object target, String field, Object value) throws Exception {
    Field f = target.getClass().getDeclaredField(field);
    f.setAccessible(true);
    f.set(target, value);
  }
}
