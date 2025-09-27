// src/test/java/tu/paquete/WebClientServiceImplTest.java
package tu.paquete;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.*;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

// Ajusta estos imports a tus clases reales:
import tu.paquete.domain.service.impl.WebClientServiceImpl;
import tu.paquete.domain.service.impl.TokenCacheValue;
import com.github.benmanes.caffeine.cache.Cache; // si no usas Caffeine, cambia el tipo por tu interfaz de cache

class WebClientServiceImplTest {

    private AutoCloseable closeable;

    // Simulamos el “backend HTTP” de WebClient
    private ExchangeFunction exchange;

    // Builder de WebClient que usará el servicio (inyectado)
    private WebClient.Builder builder;

    @Mock
    private Cache<String, TokenCacheValue> cache;

    private WebClientServiceImpl service;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        exchange = mock(ExchangeFunction.class);
        builder = WebClient.builder().exchangeFunction(exchange);

        // Construye tu servicio con el builder inyectado
        service = new WebClientServiceImpl(
                builder,
                "clientId",
                "clientSecret",
                "http://fake/token",   // tokenUrl
                cache
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    // ---------- Helpers ----------

    private static ClientResponse okJson(String json) {
        return ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build();
    }

    private static ClientResponse httpError(int code, String body) {
        return ClientResponse.create(HttpStatus.valueOf(code))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .body(body)
                .build();
    }

    // ---------- Tests ----------

    @Test
    void refreshToken_devuelveTokenYGuardaEnCache() {
        String json = """
          {"access_token":"abc123","token_type":"Bearer","expires_in":60}
        """;
        when(exchange.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(okJson(json)));

        StepVerifier.create(service.refreshToken())
                .expectNext("abc123")
                .verifyComplete();

        // Se debe guardar algo en cache con la clave "authToken"
        ArgumentCaptor<TokenCacheValue> captor = ArgumentCaptor.forClass(TokenCacheValue.class);
        verify(cache, times(1)).put(eq("authToken"), captor.capture());

        TokenCacheValue saved = captor.getValue();
        assertNotNull(saved);

        // Si tu TokenCacheValue expone expiresAt/token, puedes descomentar:
        // assertEquals("abc123", saved.getToken());
        // assertTrue(saved.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void refreshToken_errorSiNoVieneAccessToken() {
        String jsonSinToken = """
          {"token_type":"Bearer","expires_in":100}
        """;
        when(exchange.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(okJson(jsonSinToken)));

        StepVerifier.create(service.refreshToken())
                .expectErrorSatisfies(e -> {
                    assertTrue(e instanceof IllegalStateException);
                    assertTrue(e.getMessage().contains("sin access_token"));
                })
                .verify();

        verify(cache, never()).put(anyString(), any());
    }

    @Test
    void refreshToken_errorCuandoHttp4xxO5xx() {
        when(exchange.exchange(any(ClientRequest.class)))
                .thenReturn(Mono.just(httpError(401, "Unauthorized")));

        StepVerifier.create(service.refreshToken())
                .expectErrorSatisfies(e -> {
                    assertTrue(e instanceof IllegalStateException);
                    assertTrue(e.getMessage().contains("Falló autenticando"));
                    assertTrue(e.getMessage().contains("401"));
                })
                .verify();

        verify(cache, never()).put(anyString(), any());
    }
}
