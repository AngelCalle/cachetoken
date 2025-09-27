package com.example.demo;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;

import lombok.extern.slf4j.Slf4j;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Configuration
public class CaffeineConfig {

  /**
   * Cache con expiración por-entrada: cada token expira exactamente en su "expiresAt".
   * Se fuerza un mínimo de 1 segundo y un pequeño margen de seguridad.
   */
  @Bean
  Cache<String, TokenCacheValue> tokenCache() {
	  log.info("1");
    Expiry<String, TokenCacheValue> expiry = new Expiry<>() {
      private static final Duration SAFETY = Duration.ofSeconds(5);
      

      @Override
      public long expireAfterCreate(String key, TokenCacheValue value, long currentTime) {
        return nanosUntil(value);
      }

      @Override
      public long expireAfterUpdate(String key, TokenCacheValue value, long currentTime, long currentDuration) {
        return nanosUntil(value);
      }

      @Override
      public long expireAfterRead(String key, TokenCacheValue value, long currentTime, long currentDuration) {
        return currentDuration;
      }

      private long nanosUntil(TokenCacheValue value) {
    	  log.info("2");
        Instant now = Instant.now();
        Instant target = value.getExpiresAt().minus(SAFETY);
        long nanos = Duration.between(now, target).toNanos();
        if (nanos < Duration.ofSeconds(1).toNanos()) {
          return Duration.ofSeconds(1).toNanos();
        }
        return nanos;
      }
    };

    return Caffeine.newBuilder()
        .expireAfter(expiry)
        .maximumSize(1) // solo necesitamos 1 entrada
        .build();
  }
  
}
