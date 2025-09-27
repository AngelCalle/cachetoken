// src/test/java/tu/paquete/CacheConfigManagerTest.java
package tu.paquete;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

// ⬇️ Cambia a tu paquete real:
import tu.paquete.config.CacheConfig;

import com.github.benmanes.caffeine.cache.Policy;

public class CacheConfigManagerTest {

  private final CacheConfig config = new CacheConfig();

  @Test
  void registra_bic_y_sepa_con_politicas_correctas() {
    CacheManager cm = config.cacheManager();

    // ----- bicCache -----
    Cache bic = cm.getCache("bicCache");
    assertThat(bic).isNotNull();

    com.github.benmanes.caffeine.cache.Cache<?, ?> nativeBic =
        ((CaffeineCache) bic).getNativeCache();

    Optional<Policy.FixedExpiration<?, ?>> expWriteBic =
        nativeBic.policy().expireAfterWrite();
    assertThat(expWriteBic).isPresent();
    long minsBic = expWriteBic.get().getExpiresAfter(TimeUnit.MINUTES);
    assertThat(minsBic).isEqualTo(15);

    Optional<Policy.Eviction<?, ?>> evictionBic = nativeBic.policy().eviction();
    assertThat(evictionBic).isPresent();
    assertThat(evictionBic.get().getMaximum()).isEqualTo(1000);

    // ----- sepaCache -----
    Cache sepa = cm.getCache("sepaCache");
    assertThat(sepa).isNotNull();

    com.github.benmanes.caffeine.cache.Cache<?, ?> nativeSepa =
        ((CaffeineCache) sepa).getNativeCache();

    Optional<Policy.FixedExpiration<?, ?>> expWriteSepa =
        nativeSepa.policy().expireAfterWrite();
    assertThat(expWriteSepa).isPresent();
    long minsSepa = expWriteSepa.get().getExpiresAfter(TimeUnit.MINUTES);
    assertThat(minsSepa).isEqualTo(60);

    Optional<Policy.Eviction<?, ?>> evictionSepa = nativeSepa.policy().eviction();
    assertThat(evictionSepa).isPresent();
    assertThat(evictionSepa.get().getMaximum()).isEqualTo(500);
  }
}
