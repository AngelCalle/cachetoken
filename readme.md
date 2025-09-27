package tu.paquete.infrastructure.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicBoolean;

import nl.altindag.log.LogCaptor;
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
    void onReady_suscribeYLogueaInfoEnExito_sinLogback() {
        AtomicBoolean subscribed = new AtomicBoolean(false);
        when(tokenService.refreshToken())
                .thenReturn(Mono.fromSupplier(() -> "tok").doOnSubscribe(s -> subscribed.set(true)));

        // Captura logs SLF4J de la clase
        LogCaptor captor = LogCaptor.forClass(TokenWarmup.class);

        warmup.onReady();

        verify(tokenService, times(1)).refreshToken();
        assertTrue(subscribed.get(), "Debe haberse suscrito al Mono");

        assertTrue(
            captor.getInfoLogs().stream()
                  .anyMatch(m -> m.contains("Token precargado correctamente")),
            "Debe loguear INFO de precarga correcta"
        );
    }

    @Test
    void onReady_suscribeYLogueaWarnEnError_sinLogback() {
        AtomicBoolean subscribed = new AtomicBoolean(false);
        when(tokenService.refreshToken())
                .thenReturn(Mono.<String>error(new RuntimeException("boom"))
                        .doOnSubscribe(s -> subscribed.set(true)));

        LogCaptor captor = LogCaptor.forClass(TokenWarmup.class);

        assertDoesNotThrow(() -> warmup.onReady());

        verify(tokenService, times(1)).refreshToken();
        assertTrue(subscribed.get(), "Debe haberse suscrito al Mono");

        assertTrue(
            captor.getWarnLogs().stream()
                  .anyMatch(m -> m.contains("No se pudo precargar el token")),
            "Debe loguear WARN cuando falla la precarga"
        );
    }
}
