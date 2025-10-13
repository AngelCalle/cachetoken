public class AuthResponse {
  private String access_token;
  private long expires_in;

  public AuthResponse() {}
  public AuthResponse(String token, long ttl) {
    this.access_token = token;
    this.expires_in = ttl;
  }
  public String getAccessToken() { return access_token; }
  public long getExpiresIn() { return expires_in; }
}

// Simula el valor guardado en caché
public record TokenCacheValue(String token, java.time.Instant expiresAt) {}


package com.example.auth;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthTokenServiceTest {

    // SUT fields (ajusta a tus nombres reales)
    private AuthTokenService service;

    // Mocks de toda la cadena WebClient
    @Mock private WebClient.Builder builder;
    @Mock private WebClient webClient;
    @Mock private WebClient.RequestBodyUriSpec postSpec;
    @Mock private WebClient.RequestHeadersSpec<?> headersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private final String tokenUrl = "https://auth.local/token";
    private final String clientId = "cid";
    private final String clientSecret = "sec";
    private Map<String, TokenCacheValue> cache;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Stubs “neutros” de la cadena fluida
        when(builder.baseUrl(tokenUrl)).thenReturn(builder);
        when(builder.defaultHeader(eq(HttpHeaders.CONTENT_TYPE), eq(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                .thenReturn(builder);
        when(builder.build()).thenReturn(webClient);

        when(webClient.post()).thenReturn(postSpec);
        // .body(...) devuelve RequestHeadersSpec
        when(postSpec.body(any(BodyInserters.FormInserter.class))).thenReturn(headersSpec);
        // .retrieve() devuelve ResponseSpec
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        // .onStatus(...) devuelve la misma ResponseSpec (comportamiento “pass-through”)
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);

        cache = new HashMap<>();

        // Crea el servicio bajo prueba. Ajusta el ctor a tu implementación.
        service = new AuthTokenService(builder, tokenUrl, clientId, clientSecret, cache);
    }

    @Test
    void refreshToken_ok_devuelveTokenYCachea() {
        // given
        AuthResponse ok = new AuthResponse("TOK_123", 120L);
        when(responseSpec.bodyToMono(eq(AuthResponse.class))).thenReturn(Mono.just(ok));

        // when / then
        StepVerifier.create(service.refreshToken())
                .expectNext("TOK_123")
                .verifyComplete();

        // cacheado con TTL aplicado (con margen de seguridad dentro del método)
        Assertions.assertTrue(cache.containsKey("authTokenIBP"));
        TokenCacheValue cached = cache.get("authTokenIBP");
        Assertions.assertEquals("TOK_123", cached.token());
        Assertions.assertTrue(cached.expiresAt().isAfter(Instant.now()));
    }

    @Test
    void refreshToken_respuestaVacia_lanzaError() {
        // bodyToMono devuelve Mono.empty()
        when(responseSpec.bodyToMono(eq(AuthResponse.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.refreshToken())
                .expectErrorSatisfies(err ->
                        Assertions.assertTrue(err.getMessage().toLowerCase().contains("oauth vacía")
                                || err.getMessage().toLowerCase().contains("oauth vacia")))
                .verify();

        Assertions.assertFalse(cache.containsKey("authTokenIBP"));
    }

    @Test
    void refreshToken_sinAccessToken_lanzaError() {
        AuthResponse sinToken = new AuthResponse(null, 60L);
        when(responseSpec.bodyToMono(eq(AuthResponse.class))).thenReturn(Mono.just(sinToken));

        StepVerifier.create(service.refreshToken())
                .expectErrorSatisfies(err ->
                        Assertions.assertTrue(err.getMessage().toLowerCase().contains("sin access_token")))
                .verify();

        Assertions.assertFalse(cache.containsKey("authTokenIBP"));
    }

    @Test
    void refreshToken_http4xx5xx_provocaErrorPorOnStatus() {
        // simulamos que después de onStatus, bodyToMono termina con error
        when(responseSpec.bodyToMono(eq(String.class)))
                .thenReturn(Mono.just("mensaje de error remoto"));
        when(responseSpec.bodyToMono(eq(AuthResponse.class)))
                .thenReturn(Mono.error(new IllegalStateException("fallo autenticando (401): mensaje de error remoto")));

        StepVerifier.create(service.refreshToken())
                .expectErrorSatisfies(err ->
                        Assertions.assertTrue(err.getMessage().toLowerCase().contains("fallo autenticando")))
                .verify();

        Assertions.assertFalse(cache.containsKey("authTokenIBP"));
    }
}


package com.example.auth;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthTokenServiceTest {

    // SUT fields (ajusta a tus nombres reales)
    private AuthTokenService service;

    // Mocks de toda la cadena WebClient
    @Mock private WebClient.Builder builder;
    @Mock private WebClient webClient;
    @Mock private WebClient.RequestBodyUriSpec postSpec;
    @Mock private WebClient.RequestHeadersSpec<?> headersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private final String tokenUrl = "https://auth.local/token";
    private final String clientId = "cid";
    private final String clientSecret = "sec";
    private Map<String, TokenCacheValue> cache;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Stubs “neutros” de la cadena fluida
        when(builder.baseUrl(tokenUrl)).thenReturn(builder);
        when(builder.defaultHeader(eq(HttpHeaders.CONTENT_TYPE), eq(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                .thenReturn(builder);
        when(builder.build()).thenReturn(webClient);

        when(webClient.post()).thenReturn(postSpec);
        // .body(...) devuelve RequestHeadersSpec
        when(postSpec.body(any(BodyInserters.FormInserter.class))).thenReturn(headersSpec);
        // .retrieve() devuelve ResponseSpec
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        // .onStatus(...) devuelve la misma ResponseSpec (comportamiento “pass-through”)
        when(responseSpec.onStatus(any(Predicate.class), any(Function.class))).thenReturn(responseSpec);

        cache = new HashMap<>();

        // Crea el servicio bajo prueba. Ajusta el ctor a tu implementación.
        service = new AuthTokenService(builder, tokenUrl, clientId, clientSecret, cache);
    }

    @Test
    void refreshToken_ok_devuelveTokenYCachea() {
        // given
        AuthResponse ok = new AuthResponse("TOK_123", 120L);
        when(responseSpec.bodyToMono(eq(AuthResponse.class))).thenReturn(Mono.just(ok));

        // when / then
        StepVerifier.create(service.refreshToken())
                .expectNext("TOK_123")
                .verifyComplete();

        // cacheado con TTL aplicado (con margen de seguridad dentro del método)
        Assertions.assertTrue(cache.containsKey("authTokenIBP"));
        TokenCacheValue cached = cache.get("authTokenIBP");
        Assertions.assertEquals("TOK_123", cached.token());
        Assertions.assertTrue(cached.expiresAt().isAfter(Instant.now()));
    }

    @Test
    void refreshToken_respuestaVacia_lanzaError() {
        // bodyToMono devuelve Mono.empty()
        when(responseSpec.bodyToMono(eq(AuthResponse.class))).thenReturn(Mono.empty());

        StepVerifier.create(service.refreshToken())
                .expectErrorSatisfies(err ->
                        Assertions.assertTrue(err.getMessage().toLowerCase().contains("oauth vacía")
                                || err.getMessage().toLowerCase().contains("oauth vacia")))
                .verify();

        Assertions.assertFalse(cache.containsKey("authTokenIBP"));
    }

    @Test
    void refreshToken_sinAccessToken_lanzaError() {
        AuthResponse sinToken = new AuthResponse(null, 60L);
        when(responseSpec.bodyToMono(eq(AuthResponse.class))).thenReturn(Mono.just(sinToken));

        StepVerifier.create(service.refreshToken())
                .expectErrorSatisfies(err ->
                        Assertions.assertTrue(err.getMessage().toLowerCase().contains("sin access_token")))
                .verify();

        Assertions.assertFalse(cache.containsKey("authTokenIBP"));
    }

    @Test
    void refreshToken_http4xx5xx_provocaErrorPorOnStatus() {
        // simulamos que después de onStatus, bodyToMono termina con error
        when(responseSpec.bodyToMono(eq(String.class)))
                .thenReturn(Mono.just("mensaje de error remoto"));
        when(responseSpec.bodyToMono(eq(AuthResponse.class)))
                .thenReturn(Mono.error(new IllegalStateException("fallo autenticando (401): mensaje de error remoto")));

        StepVerifier.create(service.refreshToken())
                .expectErrorSatisfies(err ->
                        Assertions.assertTrue(err.getMessage().toLowerCase().contains("fallo autenticando")))
                .verify();

        Assertions.assertFalse(cache.containsKey("authTokenIBP"));
    }
}
