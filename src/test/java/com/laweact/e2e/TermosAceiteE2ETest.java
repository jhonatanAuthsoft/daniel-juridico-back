package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.dto.usuario.AceitarTermosInputDTO;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.e2e.support.Fixtures;

@DisplayName("E2E — POST /usuarios/aceitar-termos")
class TermosAceiteE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("cadastro e login devem retornar termosAceitos=false até o aceite")
    void shouldExposeTermosAceitosFalseUntilAccepted() {
        String email = "termos.false@laweact.com";
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido(email, "52998224725")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        assertThat(cadastro.getBody().path("data").path("usuario").path("termosAceitos").asBoolean()).isFalse();

        ResponseEntity<JsonNode> login = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder().email(email).senha(Fixtures.VALID_PASSWORD).build()
        );
        assertSuccess(login, HttpStatus.OK);
        assertThat(login.getBody().path("data").path("usuario").path("termosAceitos").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("deve registrar aceite com usuário, versão e data")
    void shouldAcceptTermsSuccessfully() {
        String email = "termos.aceite@laweact.com";
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido(email, "11144477735")
        );
        String token = cadastro.getBody().path("data").path("token").asText();
        String usuarioId = cadastro.getBody().path("data").path("usuario").path("id").asText();
        api.authenticate(token);

        ResponseEntity<JsonNode> response = api.post(
                "/usuarios/aceitar-termos",
                AceitarTermosInputDTO.builder()
                        .checkboxConfirmado(true)
                        .scrollConfirmado(true)
                        .build()
        );

        assertSuccess(response, HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("termosAceitos").asBoolean()).isTrue();
        assertThat(data.path("usuarioId").asText()).isEqualTo(usuarioId);
        assertThat(data.path("versao").asText()).isEqualTo("v1");
        assertThat(data.path("aceitoEm").asText()).isNotBlank();
        assertThat(data.path("checkboxConfirmado").asBoolean()).isTrue();
        assertThat(data.path("scrollConfirmado").asBoolean()).isTrue();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM termos_aceite WHERE usuario_id = ?",
                Integer.class,
                java.util.UUID.fromString(usuarioId)
        );
        assertThat(count).isEqualTo(1);

        ResponseEntity<JsonNode> login = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder().email(email).senha(Fixtures.VALID_PASSWORD).build()
        );
        assertThat(login.getBody().path("data").path("usuario").path("termosAceitos").asBoolean()).isTrue();
        assertThat(login.getBody().path("data").path("usuario").path("termosVersao").asText()).isEqualTo("v1");
    }

    @Test
    @DisplayName("deve rejeitar aceite sem checkbox")
    void shouldFailWithoutCheckbox() {
        String email = "termos.semcheck@laweact.com";
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido(email, "39053344705")
        );
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> response = api.post(
                "/usuarios/aceitar-termos",
                Map.of("checkboxConfirmado", false, "scrollConfirmado", true)
        );

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody().path("success").asBoolean()).isFalse();
    }
}
