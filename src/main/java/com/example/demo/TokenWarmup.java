package com.example.demo;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TokenWarmup {

	private final TokenService tokenService;

	public TokenWarmup(TokenService tokenService) {
		this.tokenService = tokenService;
	}

	/**
	 * Se ejecuta cuando la app está lista: precarga el token sin bloquear el hilo.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		tokenService.refreshToken().doOnSuccess(t -> log.info("Token precargado correctamente")).doOnError(
				e -> log.warn("No se pudo precargar el token (se intentará cuando haga falta): {}", e.toString()))
				.subscribe(); // importante: NO block(), suscríbete
	}

}
