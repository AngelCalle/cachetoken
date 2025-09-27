package com.example.demo;




import static org.junit.jupiter.api.Assertions.*;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CaffeineConfigTest {

    private CaffeineConfig config;

    @BeforeEach
    void setUp() {
        config = new CaffeineConfig();
    }

    @Test
    void testBeanCreation() {
        Cache<String, TokenCacheValue> cache = config.tokenCache();
        assertNotNull(cache, "El cache no debería ser null");
    }

    @Test
    void testEntryIsStoredAndRetrieved() {
        Cache<String, TokenCacheValue> cache = config.tokenCache();

        Instant expiresAt = Instant.now().plusSeconds(10);
        TokenCacheValue value = new TokenCacheValue(null, expiresAt);

        cache.put("token1", value);

        assertEquals(value, cache.getIfPresent("token1"));
    }

    @Test
    void testEntryExpiresAccordingToExpiresAt() {
        Cache<String, TokenCacheValue> cache = config.tokenCache();

        // Expira en ~6 segundos (5 + 1 mínimo)
        Instant expiresAt = Instant.now().plusSeconds(6);
        TokenCacheValue value = new TokenCacheValue(expiresAt);

        cache.put("token2", value);

        // Al inicio debe existir
        assertNotNull(cache.getIfPresent("token2"));

        // Después de ~7 segundos ya debe expirar
        await()
            .atMost(8, TimeUnit.SECONDS)
            .until(() -> cache.getIfPresent("token2") == null);
    }

    @Test
    void testMinimumOneSecondExpiry() {
        Cache<String, TokenCacheValue> cache = config.tokenCache();

        // expiresAt ya, pero debería durar mínimo 1 segundo
        Instant expiresAt = Instant.now().plusMillis(10);
        TokenCacheValue value = new TokenCacheValue(expiresAt);

        cache.put("token3", value);

        // Justo después de meterlo debe estar presente
        assertNotNull(cache.getIfPresent("token3"));

        // Menos de un segundo después debería seguir ahí
        assertNotNull(cache.getIfPresent("token3"));
    }
}