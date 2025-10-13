package com.example.demo;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/users")
public class UsersController {

	private final UsersClient client;
	private final AtomicInteger idx = new AtomicInteger(0);


	public UsersController(UsersClient client) {
		this.client = client;
	}
	
	private final List<Map<String, Object>> PAYLOADS = List.of(
			Map.of("firstName", "Muhammad", "lastName", "Ovi", "age", 25),
			Map.of("firstName", "Ada", "lastName", "Lovelace", "age", 36),
			Map.of("firstName", "Linus", "lastName", "Torvalds", "age", 54));


	@PostMapping(value = "/add-rotating", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<String>> addRotating() {
		int i = Math.floorMod(idx.getAndIncrement(), PAYLOADS.size());
		Map<String, Object> payload = PAYLOADS.get(i);
		return client.addUser(payload).map(ResponseEntity::ok);
	}
	
	

}
