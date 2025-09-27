package tu.paquete.infrastructure.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import reactor.core.publisher.Mono;
import tu.paquete.domain.service.impl.TokenIBPService;

class TokenWarmupTest {

    @Mock
    private TokenIBPService tokenService;

    private TokenWarmup warmup;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        warmup = new TokenWarmup(tokenService);
    }

    @Test
    void onReady_suscribeYNoBloquea_cuandoExito() {
        AtomicBoolean subscribed = new AtomicBoolean(false);

        when(tokenService.refreshToken())
            .thenReturn(Mono.fromSupplier(() -> "tok")
                            .doOnSubscribe(s -> subscribed.set(true)));

        // act
        warmup.onReady();

        // assert
        verify(tokenService, times(1)).refreshToken();
        assertTrue(subscribed.get(), "El flujo debe haberse suscrito");
        // no debe lanzar ni bloquear
    }

    @Test
    void onReady_suscribeYNoPropagaError_cuandoFalla() {
        AtomicBoolean subscribed = new AtomicBoolean(false);

        when(tokenService.refreshToken())
            .thenReturn(Mono.<String>error(new RuntimeException("boom"))
                            .doOnSubscribe(s -> subscribed.set(true)));

        // act + assert: el error se maneja en el pipeline y no se propaga
        assertDoesNotThrow(() -> warmup.onReady());

        verify(tokenService, times(1)).refreshToken();
        assertTrue(subscribed.get(), "El flujo debe haberse suscrito incluso en error");
    }
}
