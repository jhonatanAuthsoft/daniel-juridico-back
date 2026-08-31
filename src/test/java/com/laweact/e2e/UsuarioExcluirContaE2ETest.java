package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.e2e.support.Fixtures;

@DisplayName("E2E — excluir conta do usuário autenticado")
class UsuarioExcluirContaE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("DELETE /usuarios/me remove a conta e o login deixa de funcionar")
    void shouldDeleteAuthenticatedAccount() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("apagar.conta@laweact.com", "52998224725")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> deleted = api.delete("/usuarios/me");
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleted.getBody().path("success").asBoolean()).isTrue();
        assertThat(deleted.getBody().path("message").asText()).contains("excluída");

        assertThat(usuarioRepository.findByEmail("apagar.conta@laweact.com")).isEmpty();

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        api.logout();
        ResponseEntity<JsonNode> login = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder()
                        .email("apagar.conta@laweact.com")
                        .senha(Fixtures.VALID_PASSWORD)
                        .build()
        );
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("rejeita DELETE /usuarios/me sem autenticação")
    void shouldRejectUnauthenticated() {
        ResponseEntity<JsonNode> deleted = api.delete("/usuarios/me");
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("DELETE /usuarios/excluir/{id} não existe e não apaga outro usuário")
    void shouldNotExposeDeleteById() {
        ResponseEntity<JsonNode> atacante = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("atacante.excluir@laweact.com", "11144477735")
        );
        assertSuccess(atacante, HttpStatus.CREATED);
        api.authenticate(atacante.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> vitima = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("vitima.excluir@laweact.com", "15350946056")
        );
        assertSuccess(vitima, HttpStatus.CREATED);
        UUID vitimaId = UUID.fromString(vitima.getBody().path("data").path("usuario").path("id").asText());

        ResponseEntity<JsonNode> deleted = api.delete("/usuarios/excluir/" + vitimaId);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(usuarioRepository.findByEmail("vitima.excluir@laweact.com")).isPresent();
    }
}
