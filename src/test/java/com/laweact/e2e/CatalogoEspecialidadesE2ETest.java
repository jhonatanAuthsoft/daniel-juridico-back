package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

@DisplayName("E2E — GET /catalogos/especialidades")
class CatalogoEspecialidadesE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("deve listar especialidades com subespecialidades sem autenticação")
    void shouldListEspecialidadesPublicly() {
        ResponseEntity<JsonNode> response = api.get("/catalogos/especialidades");

        assertSuccess(response, HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(10);

        JsonNode civil = null;
        for (JsonNode item : data) {
            if ("CIVIL".equals(item.path("codigo").asText())) {
                civil = item;
                break;
            }
        }
        assertThat(civil).isNotNull();
        assertThat(civil.path("nome").asText()).containsIgnoringCase("Civil");
        assertThat(civil.path("subespecialidades").isArray()).isTrue();
        assertThat(civil.path("subespecialidades").size()).isGreaterThan(0);
        assertThat(civil.path("subespecialidades").get(0).path("codigo").asText()).isNotBlank();
        assertThat(civil.path("subespecialidades").get(0).path("nome").asText()).isNotBlank();
    }
}
