// src/test/java/tu/paquete/AuthResponseTest.java
package tu.paquete;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthResponseTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        // No es necesario configurar FAIL_ON_UNKNOWN_PROPERTIES: la clase tiene @JsonIgnoreProperties(ignoreUnknown = true)
    }

    @Test
    void deserializaSnakeCase_yCamposDesconocidos() throws Exception {
        String json = """
          {
            "access_token": "abc123",
            "token_type": "Bearer",
            "expires_in": 3600,
            "some_unknown_field": "ignored"
          }
        """;

        AuthResponse dto = mapper.readValue(json, AuthResponse.class);

        assertEquals("abc123", dto.getAccessToken());
        assertEquals("Bearer", dto.getTokenType());
        assertEquals(3600L, dto.getExpiresIn());
    }

    @Test
    void deserializaUsandoAlias() throws Exception {
        // Aunque @JsonNaming ya mapea snake_case, probamos explícitamente los alias
        String jsonConAlias = """
          {
            "access_token": "token-x",
            "token_type": "bearer",
            "expires_in": 1200
          }
        """;

        AuthResponse dto = mapper.readValue(jsonConAlias, AuthResponse.class);

        assertAll(
            () -> assertEquals("token-x", dto.getAccessToken()),
            () -> assertEquals("bearer", dto.getTokenType()),
            () -> assertEquals(1200L, dto.getExpiresIn())
        );
    }

    @Test
    void serializaEnSnakeCase() throws JsonProcessingException {
        AuthResponse dto = new AuthResponse();
        dto.setAccessToken("tok-999");
        dto.setTokenType("Bearer");
        dto.setExpiresIn(86400L);

        String json = mapper.writeValueAsString(dto);

        // Comprueba nombres snake_case y valores
        assertTrue(json.contains("\"access_token\":\"tok-999\""), json);
        assertTrue(json.contains("\"token_type\":\"Bearer\""), json);
        assertTrue(json.contains("\"expires_in\":86400"), json);
        // No debería contener nombres camelCase
        assertFalse(json.contains("accessToken"), json);
    }

    @Test
    void gettersYSettersFuncionan() {
        AuthResponse dto = new AuthResponse();
        dto.setAccessToken("A");
        dto.setTokenType("B");
        dto.setExpiresIn(1L);

        assertEquals("A", dto.getAccessToken());
        assertEquals("B", dto.getTokenType());
        assertEquals(1L, dto.getExpiresIn());
    }
}
