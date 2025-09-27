package your.pkg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import com.github.benmanes.caffeine.cache.Cache as CaffeineNative;
import com.github.benmanes.caffeine.cache.Policy;

public class CacheConfigManagerTest {

  private final CacheConfig config = new CacheConfig();

  @Test
  void bicCache_y_sepaCache_se_registran_con_politicas_correctas() {
    CacheManager cm = config.cacheManager();

    // --- bicCache ---
    Cache bic = cm.getCache("bicCache");
    assertThat(bic).as("bicCache debe existir").isNotNull();

    CaffeineNative<?, ?> nativeBic = (CaffeineNative<?, ?>) ((CaffeineCache) bic).getNativeCache();

    Optional<Policy.FixedExpiration<?, ?>> expWriteBic = nativeBic.policy().expireAfterWrite();
    assertThat(expWriteBic).isPresent();
    long minsBic = expWriteBic.get().getExpiresAfter(TimeUnit.MINUTES);
    assertThat(minsBic).isEqualTo(15);

    Optional<Policy.Eviction<?, ?>> evictionBic = nativeBic.policy().eviction();
    assertThat(evictionBic).isPresent();
    assertThat(evictionBic.get().getMaximum()).isEqualTo(1000);

    // --- sepaCache ---
    Cache sepa = cm.getCache("sepaCache");
    assertThat(sepa).as("sepaCache debe existir").isNotNull();

    CaffeineNative<?, ?> nativeSepa = (CaffeineNative<?, ?>) ((CaffeineCache) sepa).getNativeCache();

    Optional<Policy.FixedExpiration<?, ?>> expWriteSepa = nativeSepa.policy().expireAfterWrite();
    assertThat(expWriteSepa).isPresent();
    long minsSepa = expWriteSepa.get().getExpiresAfter(TimeUnit.MINUTES);
    assertThat(minsSepa).isEqualTo(60);

    Optional<Policy.Eviction<?, ?>> evictionSepa = nativeSepa.policy().eviction();
    assertThat(evictionSepa).isPresent();
    assertThat(evictionSepa.get().getMaximum()).isEqualTo(500);
  }

  

  private final CacheConfig config = new CacheConfig();

  @Test
  void expira_en_minimo_1s_cuando_safety_sobrepasa() throws Exception {
    Cache<String, TokenCacheValue> cache = config.tokenCache();

    // expiresAt ~ ahora + 2s; menos 5s de safety => negativo -> debe forzar 1s
    String key = "k1";
    cache.put(key, new TokenCacheValue(Instant.now().plusSeconds(2)));

    assertThat(cache.getIfPresent(key)).isNotNull();
    // dormir 1200ms para pasar el mínimo de 1s
    Thread.sleep(1200);
    assertThat(cache.getIfPresent(key)).isNull();
  }
   @Test
  void expira_a_expiresAt_menos_safety() throws Exception {
    Cache<String, TokenCacheValue> cache = config.tokenCache();

    // expiresAt = ahora + 7s -> efectivo = 2s
    String key = "k2";
    cache.put(key, new TokenCacheValue(Instant.now().plusSeconds(7)));

    assertThat(cache.getIfPresent(key)).isNotNull();
    Thread.sleep(1200);
    // aún debe existir (~2s total)
    assertThat(cache.getIfPresent(key)).isNotNull();
    Thread.sleep(1100);
    // ya debe haber expirado (~2.3s transcurridos)
    assertThat(cache.getIfPresent(key)).isNull();
  }
   @Test
  void leer_no_renueva_la_expiracion() throws Exception {
    Cache<String, TokenCacheValue> cache = config.tokenCache();

    // efectivo ~2s (7s - 5s)
    String key = "k3";
    cache.put(key, new TokenCacheValue(Instant.now().plusSeconds(7)));

    // a ~1s, leemos
    Thread.sleep(1000);
    assertThat(cache.getIfPresent(key)).isNotNull(); // read
    // Si la lectura renovara, aún existiría pasado 1.5s más; no debería.
    Thread.sleep(1500);
    assertThat(cache.getIfPresent(key)).isNull();
  }

 @Test
  void leer_no_renueva_la_expiracion() throws Exception {
    Cache<String, TokenCacheValue> cache = config.tokenCache();

    // efectivo ~2s (7s - 5s)
    String key = "k3";
    cache.put(key, new TokenCacheValue(Instant.now().plusSeconds(7)));

    // a ~1s, leemos
    Thread.sleep(1000);
    assertThat(cache.getIfPresent(key)).isNotNull(); // read
    // Si la lectura renovara, aún existiría pasado 1.5s más; no debería.
    Thread.sleep(1500);
    assertThat(cache.getIfPresent(key)).isNull();
  }
  }
