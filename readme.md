
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicBoolean;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;
import tu.paquete.domain.service.impl.TokenIBPService;

class TokenWarmupTest {

    @Mock
    private TokenIBPService tokenService;

    private TokenWarmup warmup;

    // logger (lombok @Slf4j usa el de la clase)
    private Logger warmupLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        warmup = new TokenWarmup(tokenService);

        warmupLogger = (Logger) LoggerFactory.getLogger(TokenWarmup.class);
        appender = new ListAppender<>();
        appender.start();
        warmupLogger.addAppender(appender);
    }

    @Test
    void onReady_suscribeYLogueaInfoCuandoExito() {
        AtomicBoolean subscribed = new AtomicBoolean(false);
        Mono<String> mono = Mono.fromSupplier(() -> "tok")
                .doOnSubscribe(s -> subscribed.set(true));
        when(tokenService.refreshToken()).thenReturn(mono);

        // act
        warmup.onReady();

        // assert
        verify(tokenService, times(1)).refreshToken();
        assertTrue(subscribed.get(), "El Mono debe haberse suscrito");

        // logs
        boolean sawInfo = appender.list.stream()
                .anyMatch(e -> e.getLevel() == Level.INFO &&
                        e.getFormattedMessage().contains("Token precargado correctamente"));
        assertTrue(sawInfo, "Debe loguear INFO de precarga correcta");
    }

    @Test
    void onReady_suscribeYLogueaWarnCuandoFalla() {
        AtomicBoolean subscribed = new AtomicBoolean(false);
        Mono<String> mono = Mono.<String>error(new RuntimeException("boom"))
                .doOnSubscribe(s -> subscribed.set(true));
        when(tokenService.refreshToken()).thenReturn(mono);

        // act: no debe lanzar excepción (el error se maneja en el flujo)
        assertDoesNotThrow(() -> warmup.onReady());

        // assert
        verify(tokenService, times(1)).refreshToken();
        assertTrue(subscribed.get(), "El Mono debe haberse suscrito incluso en error");

        boolean sawWarn = appender.list.stream()
                .anyMatch(e -> e.getLevel() == Level.WARN &&
                        e.getFormattedMessage().contains("No se pudo precargar el token"));
        assertTrue(sawWarn, "Debe loguear WARN cuando falla la precarga");
    }
}
