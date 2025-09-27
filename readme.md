  @Test
  void minimo_un_segundo_si_safety_sobrepasa() throws Exception {
    Cache<String, TokenCacheValue> cache = config.tokenCache();

    cache.put("k1", new TokenCacheValue(Instant.now().plusSeconds(2))); // efectivo < 0 -> 1s
    assertThat(cache.getIfPresent("k1")).isNotNull();

    Thread.sleep(1200); // > 1s
    assertThat(cache.getIfPresent("k1")).isNull();
  }

  @Test
  void expira_a_expiresAt_menos_cinco_segundos() throws Exception {
    Cache<String, TokenCacheValue> cache = config.tokenCache();

    cache.put("k2", new TokenCacheValue(Instant.now().plusSeconds(7))); // efectivo ~2s
    assertThat(cache.getIfPresent("k2")).isNotNull();

    Thread.sleep(1200);
    assertThat(cache.getIfPresent("k2")).isNotNull(); // aún vivo

    Thread.sleep(1100); // total ~2.3s
    assertThat(cache.getIfPresent("k2")).isNull();
  }

  @Test
  void leer_no_renueva_la_expiracion() throws Exception {
    Cache<String, TokenCacheValue> cache = config.tokenCache();

    cache.put("k3", new TokenCacheValue(Instant.now().plusSeconds(7))); // efectivo ~2s

    Thread.sleep(1000);
    assertThat(cache.getIfPresent("k3")).isNotNull(); // read

    Thread.sleep(1500); // total ~2.5s
    assertThat(cache.getIfPresent("k3")).isNull();
  }

  @Test
  void actualizar_reinicia_el_reloj() throws Exception {
    Cache<String, TokenCacheValue> cache = config.tokenCache();

    cache.put("k4", new TokenCacheValue(Instant.now().plusSeconds(7))); // ~2s
    Thread.sleep(1200);
    cache.put("k4", new TokenCacheValue(Instant.now().plusSeconds(7))); // reinicia (~2s)

    Thread.sleep(1200);
    assertThat(cache.getIfPresent("k4")).isNotNull();

    Thread.sleep(1100);
    assertThat(cache.getIfPresent("k4")).isNull();
  }
