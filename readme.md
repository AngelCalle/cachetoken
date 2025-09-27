// src/test/java/tu/paquete/TokenIBPServiceTest.java
package tu.paquete;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.github.benmanes.caffeine.cache.Cache; // si usas otra interfaz, mantén solo java.util.function.* y Mockito
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TokenIBPServiceTest {

    private static final String CACHE_KEY = "authToken";

    @Mock
    private WebClientServiceImpl webClientServiceImpl;

    @Mock
    private Cache<String, TokenCacheValue> cache;

    @Mock
    private TokenCacheValue tokenCacheValue;

    private TokenIBPService service;

    @BeforeEach
    void setUp() {
        service = new TokenIBPService(webClientServiceImpl, cache);
    }

    @Test
    void getValidToken_devuelveCacheCuandoNoExpira() {
        when(tokenCacheValue.getToken()).thenReturn("cached-token");
        when(tokenCacheValue.getExpiresAt()).thenReturn(Instant.now().plusSeconds(60));
        when(cache.getIfPresent(CACHE_KEY)).thenReturn(tokenCacheValue);

        StepVerifier.create(service.getValidToken())
                .expectNext("cached-token")
                .verifyComplete();

        verify(webClientServiceImpl, never()).refreshToken();
    }

    @Test
    void getValidToken_refrescaCuandoNoHayEnCache() {
        when(cache.getIfPresent(CACHE_KEY)).thenReturn(null); // primera lectura en getValidToken()
        when(webClientServiceImpl.refreshToken()).thenReturn(Mono.just("new-token"));

        StepVerifier.create(service.getValidToken())
                .expectNext("new-token")
                .verifyComplete();

        verify(webClientServiceImpl, times(1)).refreshToken();
    }

    @Test
    void refreshToken_noLlamaClienteSiAlEntrarYaHayTokenValido() {
        // 1ª llamada desde getValidToken(): miss
        // 2ª llamada (dentro de refreshToken()): ya existe y no expira
        when(cache.getIfPresent(CACHE_KEY)).thenReturn(null, tokenCacheValue);
        when(tokenCacheValue.getToken()).thenReturn("now-valid");
        when(tokenCacheValue.getExpiresAt()).thenReturn(Instant.now().plusSeconds(120));

        StepVerifier.create(service.getValidToken())
                .expectNext("now-valid")
                .verifyComplete();

        verify(webClientServiceImpl, never()).refreshToken();
    }

    @Test
    void getValidToken_refrescaCuandoTokenExpirado() {
        when(tokenCacheValue.getToken()).thenReturn("old-token");
        when(tokenCacheValue.getExpiresAt()).thenReturn(Instant.now().minusSeconds(5)); // expirado
        when(cache.getIfPresent(CACHE_KEY)).thenReturn(tokenCacheValue);
        when(webClientServiceImpl.refreshToken()).thenReturn(Mono.just("fresh-token"));

        StepVerifier.create(service.getValidToken())
                .expectNext("fresh-token")
                .verifyComplete();

        verify(webClientServiceImpl, times(1)).refreshToken();
    }
}
