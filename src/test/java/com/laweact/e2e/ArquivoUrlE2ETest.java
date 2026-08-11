package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorCode;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

@DisplayName("E2E — POST /arquivos")
class ArquivoUrlE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("url-upload sem body válido retorna 422")
    void shouldValidateUploadInput() {
        ResponseEntity<JsonNode> response = api.post("/arquivos/url-upload", Map.of());
        assertErrorCode(response, HttpStatus.UNPROCESSABLE_ENTITY, "REQUIRED_FIELD");
    }

    @Test
    @DisplayName("url-upload com S3 desabilitado retorna 503")
    void shouldReturn503WhenS3Disabled() {
        ResponseEntity<JsonNode> response = api.post("/arquivos/url-upload", Map.of(
                "finalidade", "ADVOGADO_PERFIL",
                "contentType", "image/jpeg"
        ));

        assertErrorCode(response, HttpStatus.SERVICE_UNAVAILABLE, "S3_DISABLED");
    }

    @Test
    @DisplayName("url-leitura com key inválida retorna 400")
    void shouldRejectInvalidKey() {
        ResponseEntity<JsonNode> response = api.post("/arquivos/url-leitura", Map.of(
                "key", "../secret"
        ));

        assertErrorCode(response, HttpStatus.BAD_REQUEST, "INVALID_OBJECT_KEY");
    }
}
