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

@DisplayName("E2E — log de acesso a telas")
class LogAcessoTelaE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("POST /usuarios/me/acessos-tela grava usuário, tela e data/hora")
    void shouldRegisterScreenAccessLog() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("acesso.termos@laweact.com", "52998224725")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        String usuarioId = cadastro.getBody().path("data").path("usuario").path("id").asText();
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> primeiro = api.post(
                "/usuarios/me/acessos-tela",
                Map.of("tela", "TERMOS")
        );
        assertSuccess(primeiro, HttpStatus.OK);
        JsonNode data = primeiro.getBody().path("data");
        assertThat(data.path("usuarioId").asText()).isEqualTo(usuarioId);
        assertThat(data.path("tela").asText()).isEqualTo("TERMOS");
        assertThat(data.path("acessadoEm").asText()).isNotBlank();

        ResponseEntity<JsonNode> segundo = api.post(
                "/usuarios/me/acessos-tela",
                Map.of("tela", "TERMOS")
        );
        assertSuccess(segundo, HttpStatus.OK);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM logs_acesso_tela WHERE usuario_id = ?",
                Integer.class,
                UUID.fromString(usuarioId)
        );
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("rejeita acesso a tela sem autenticação")
    void shouldRejectUnauthenticated() {
        ResponseEntity<JsonNode> response = api.post(
                "/usuarios/me/acessos-tela",
                Map.of("tela", "TERMOS")
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
