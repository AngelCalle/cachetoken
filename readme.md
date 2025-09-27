// src/test/java/tu/paquete/CallIberpayTest.java
package tu.paquete;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentMatchers;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CallIberpayTest {

    @Mock
    private TokenIBPService tokenService;

    @Mock
    private VerificationOfPayeeRequest entrada;

    private CallIberpay sut; // system under test (spy)
    private CallIberpay spySut;

    @BeforeEach
    void setUp() {
        sut = new CallIberpay(tokenService);
        // Espiamos para stubear/verificar la invocación interna a callEndpoint(...)
        spySut = spy(sut);
    }

    @Test
    void validTokenCallEndpoint_usaTokenYLlamaCallEndpointConRetryTrue() {
        String token = "TOK-123";
        String bicPartyAgent = "BPAGENT";
        String bicRequestingAgent = "BRAGENT";
        String uuid = "uuid-1";

        when(tokenService.getValidToken()).thenReturn(Mono.just(token));
        // Stub de la llamada interna
        when(spySut.callEndpoint(
                eq(entrada),
                eq(bicPartyAgent),
                eq(bicRequestingAgent),
                eq(uuid),
                eq(token),
                eq(true)
        )).thenReturn(Mono.just("OK"));

        StepVerifier.create(spySut.validTokenCallEndpoint(entrada, bicPartyAgent, bicRequestingAgent, uuid))
                .expectNext("OK")
                .verifyComplete();

        // Verifica que se usó el token entregado y retry=true
        verify(spySut, times(1)).callEndpoint(
                eq(entrada),
                eq(bicPartyAgent),
                eq(bicRequestingAgent),
                eq(uuid),
                eq(token),
                eq(true)
        );
        verify(tokenService, times(1)).getValidToken();
    }

    @Test
    void validTokenCallEndpoint_noLlamaCallEndpointSiFallaGetValidToken() {
        when(tokenService.getValidToken()).thenReturn(Mono.error(new IllegalStateException("sin token")));

        StepVerifier.create(spySut.validTokenCallEndpoint(entrada, "A", "B", "C"))
                .expectError(IllegalStateException.class)
                .verify();

        verify(spySut, never()).callEndpoint(any(), anyString(), anyString(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void validTokenCallEndpoint_propagaErrorDeCallEndpoint() {
        when(tokenService.getValidToken()).thenReturn(Mono.just("TOK"));
        when(spySut.callEndpoint(any(), anyString(), anyString(), anyString(), anyString(), eq(true)))
                .thenReturn(Mono.error(new RuntimeException("fallo http")));

        StepVerifier.create(spySut.validTokenCallEndpoint(entrada, "A", "B", "C"))
                .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().contains("fallo http"))
                .verify();

        verify(spySut, times(1)).callEndpoint(any(), anyString(), anyString(), anyString(), anyString(), eq(true));
    }
}
