package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.e2e.support.Fixtures;

@DisplayName("E2E — preferências de push do usuário")
class PreferenciasUsuarioE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("cadastro retorna notificacoesPushHabilitadas=true por padrão")
    void shouldDefaultPushPreferenceToTrue() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("pref.default@laweact.com", "52998224725")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        assertThat(cadastro.getBody().path("data").path("usuario").path("notificacoesPushHabilitadas").asBoolean())
                .isTrue();
    }

    @Test
    @DisplayName("PATCH preferencias false → GET /me reflete o valor")
    void shouldUpdatePushPreference() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("pref.update@laweact.com", "11144477735")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> patch = api.patch(
                "/usuarios/me/preferencias",
                Map.of("notificacoesPushHabilitadas", false)
        );
        assertSuccess(patch, HttpStatus.OK);
        assertThat(patch.getBody().path("data").path("notificacoesPushHabilitadas").asBoolean()).isFalse();

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        assertThat(me.getBody().path("data").path("usuario").path("notificacoesPushHabilitadas").asBoolean())
                .isFalse();
    }
}
