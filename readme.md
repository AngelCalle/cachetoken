// src/test/java/tu/paquete/WebClientServiceImplTest.java
package tu.paquete;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class WebClientServiceImplTest {

    // Mocks de la cadena de WebClient
    @Mock private WebClient.Builder builder;
    @Mock private WebClient client;
    @Mock private WebClient.RequestBodyUriSpec uriSpec;
    @Mock private WebClient.RequestBodySpec bodySpec;
    @Mock private WebClient.RequestHeadersSpec<?> headersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    @Mock private Cache<String, TokenCacheValue> cache;

    private WebClientServiceImpl service;

    // Datos fijos
    private final String tokenUrl = "https://auth.example.com/token";
    private final String clientId = "cid";
    private final String clientSecret = "csec";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Stubs comunes de la cadena de llamadas
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.defaultHeader(eq(HttpHeaders.CONTENT_TYPE), eq(MediaType.APPLICATION_FORM_URLENCODED_VALUE))).thenReturn(builder);
        when(builder.build()).thenReturn(client);

        when(client.post()).thenReturn(uriSpec);
        when(uriSpec.body(any())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        // .onStatus(...) devuelve el mismo responseSpec en nuestros tests
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        // Instancia real a probar
        service = new WebClientServiceImpl(builder, clientId, clientSecret, tokenUrl, cache);
    }

    @Test
    void refreshToken_devuelveToken_yGuardaEnCache() {
        // Respuesta válida
        AuthResponse ar = new AuthResponse();
        ar.setAccessToken("abc123");
        ar.setTokenType("Bearer");
        ar.setExpiresIn(60L);

        when(responseSpec.bodyToMono(AuthResponse.class)).thenReturn(Mono.just(ar));

        StepVerifier.create(service.refreshToken())
                .expectNext("abc123")
                .verifyComplete();

        // Verifica que se usó el builder como esperamos (sanity check)
        verify(builder).baseUrl(tokenUrl);
        verify(builder).defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        verify(client).post();
        verify(bodySpec).retrieve();
        verify(responseSpec).onStatus(any(), any());

        // Verifica que se guardó algo en cache con la clave
        verify(cache, times(1)).put(eq("authToken"), any(TokenCacheValue.class));
    }

    @Test
    void refreshToken_errorCuandoAccessTokenVacioONulo() {
        AuthResponse ar = new AuthResponse();
        ar.setAccessToken("");            // vacío
        ar.setTokenType("Bearer");
        ar.setExpiresIn(30L);

        when(responseSpec.bodyToMono(AuthResponse.class)).thenReturn(Mono.just(ar));

        StepVerifier.create(service.refreshToken())
                .expectErrorMatches(e -> e instanceof IllegalStateException &&
                        e.getMessage().contains("sin access_token"))
                .verify();

        verify(cache, never()).put(any(), any());
    }

    @Test
    void refreshToken_errorHttpPropagadoDesdeOnStatus() {
        // Simulamos que tras .onStatus(...) el ResponseSpec termina en error
        when(responseSpec.bodyToMono(AuthResponse.class))
                .thenReturn(Mono.error(new IllegalStateException("Falló autenticando (401)")));

        StepVerifier.create(service.refreshToken())
                .expectErrorMatches(e -> e instanceof IllegalStateException &&
                        e.getMessage().contains("Falló autenticando"))
                .verify();

        verify(cache, never()).put(any(), any());
    }
}
