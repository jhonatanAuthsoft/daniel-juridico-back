package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorDetailContains;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.e2e.support.Fixtures;

@DisplayName("E2E — GET /usuarios/email-disponivel")
class UsuarioEmailDisponivelE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("deve indicar disponível quando o e-mail não está cadastrado")
    void shouldReturnAvailableWhenEmailIsFree() {
        ResponseEntity<JsonNode> response = api.get(
                "/usuarios/email-disponivel?email=novo.usuario@laweact.com"
        );

        assertSuccess(response, HttpStatus.OK);
        assertThat(response.getBody().path("data").path("disponivel").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("deve indicar indisponível quando o e-mail já está cadastrado")
    void shouldReturnUnavailableWhenEmailExists() {
        String email = "email.ocupado@laweact.com";
        api.post("/clientes/cadastrar", Fixtures.clienteValido(email, "15350946056"));

        ResponseEntity<JsonNode> response = api.get(
                "/usuarios/email-disponivel?email=" + email
        );

        assertSuccess(response, HttpStatus.OK);
        assertThat(response.getBody().path("data").path("disponivel").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("deve normalizar e-mail (case) na verificação")
    void shouldNormalizeEmailCase() {
        String email = "Case.Check@laweact.com";
        api.post("/clientes/cadastrar", Fixtures.clienteValido(email.toLowerCase(), "11144477735"));

        ResponseEntity<JsonNode> response = api.get(
                "/usuarios/email-disponivel?email=" + email
        );

        assertSuccess(response, HttpStatus.OK);
        assertThat(response.getBody().path("data").path("disponivel").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("deve rejeitar e-mail vazio")
    void shouldRejectBlankEmail() {
        ResponseEntity<JsonNode> response = api.get("/usuarios/email-disponivel?email=");

        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "E-mail");
    }
}
