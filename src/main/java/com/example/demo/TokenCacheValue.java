package com.example.demo;

import java.time.Instant;

import io.micrometer.common.lang.NonNull;
import lombok.Builder;
import lombok.Value;
import lombok.With;

//Si la vas a deserializar con Jackson vía builder, añade:
//import lombok.extern.jackson.Jacksonized;

@Value			// genera: privados + final, getters, equals/hashCode, toString
@Builder		// genera builder
@With		// genera "withToken(...)", "withExpiresAt(...)" (útil para copiar con cambios)
//@Jacksonized  // <-- descomenta si usas Jackson con el builder
public class TokenCacheValue {

	@Builder.Default
	String token = ""; // valor por defecto si no lo pasas al builder

	@NonNull
	Instant expiresAt; // requerido por el builder

}