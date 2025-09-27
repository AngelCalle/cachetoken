package com.example.demo;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * Modelo flexible que permite leer el token desde cualquier clave (p. ej. "accessToken" o "token")
 * sin acoplar el POJO a un nombre de campo específico.
 */
public class AuthResponse {

  @JsonIgnore
  private Map<String, Object> raw = new HashMap<>();

  @JsonAnySetter
  public void put(String key, Object value) {
    raw.put(key, value);
  }

  @JsonAnyGetter
  public Map<String, Object> any() {
    return raw;
  }

  public Object get(String key) {
    return raw.get(key);
  }
  
}
