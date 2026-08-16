package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.e2e.support.Fixtures;

@DisplayName("E2E — dispositivos push")
class DispositivoPushE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("registrar → re-registrar (upsert) → DELETE desativa")
    void shouldRegisterUpsertAndDeactivate() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("device.push@laweact.com", "52998224725")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        String token = "ExponentPushToken[device-e2e-001]";

        ResponseEntity<JsonNode> created = api.post(
                "/dispositivos-push",
                Map.of("expoPushToken", token, "plataforma", "IOS")
        );
        assertSuccess(created, HttpStatus.CREATED);
        UUID id = UUID.fromString(created.getBody().path("data").path("id").asText());
        assertThat(created.getBody().path("data").path("ativo").asBoolean()).isTrue();
        assertThat(created.getBody().path("data").path("plataforma").asText()).isEqualTo("IOS");

        ResponseEntity<JsonNode> upsert = api.post(
                "/dispositivos-push",
                Map.of("expoPushToken", token, "plataforma", "ANDROID")
        );
        assertSuccess(upsert, HttpStatus.CREATED);
        assertThat(upsert.getBody().path("data").path("id").asText()).isEqualTo(id.toString());
        assertThat(upsert.getBody().path("data").path("plataforma").asText()).isEqualTo("ANDROID");
        assertThat(upsert.getBody().path("data").path("ativo").asBoolean()).isTrue();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dispositivos_push WHERE expo_push_token = ?",
                Integer.class,
                token
        );
        assertThat(count).isEqualTo(1);

        ResponseEntity<JsonNode> deleted = api.delete(
                "/dispositivos-push",
                Map.of("expoPushToken", token)
        );
        assertSuccess(deleted, HttpStatus.OK);
        assertThat(deleted.getBody().path("data").path("ativo").asBoolean()).isFalse();

        Boolean ativo = jdbcTemplate.queryForObject(
                "SELECT ativo FROM dispositivos_push WHERE expo_push_token = ?",
                Boolean.class,
                token
        );
        assertThat(ativo).isFalse();
    }
}
