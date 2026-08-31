package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorDetailContains;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.e2e.support.Fixtures;

@DisplayName("E2E — alterar senha do usuário autenticado")
class UsuarioAtualizarSenhaE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("PATCH senha troca a senha e o login passa a exigir a nova")
    void shouldUpdatePasswordAndLoginWithNewOne() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("senha.ok@laweact.com", "52998224725")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> patch = api.patch(
                "/usuarios/me/senha",
                Map.of("senhaAtual", Fixtures.VALID_PASSWORD, "novaSenha", "NovaSenha1")
        );
        assertSuccess(patch, HttpStatus.OK);
        assertThat(patch.getBody().path("data").path("mensagem").asText())
                .isEqualTo("Senha alterada com sucesso");

        api.logout();

        ResponseEntity<JsonNode> loginAntigo = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder().email("senha.ok@laweact.com").senha(Fixtures.VALID_PASSWORD).build()
        );
        assertErrorDetailContains(loginAntigo, HttpStatus.BAD_REQUEST, "Usuário ou senha inválidos");

        ResponseEntity<JsonNode> loginNovo = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder().email("senha.ok@laweact.com").senha("NovaSenha1").build()
        );
        assertSuccess(loginNovo, HttpStatus.OK);
        assertThat(loginNovo.getBody().path("data").path("token").asText()).isNotBlank();
    }

    @Test
    @DisplayName("rejeita senha atual incorreta")
    void shouldRejectWrongCurrentPassword() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("senha.wrong@laweact.com", "11144477735")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> patch = api.patch(
                "/usuarios/me/senha",
                Map.of("senhaAtual", "WrongPass1", "novaSenha", "NovaSenha1")
        );
        assertErrorDetailContains(patch, HttpStatus.BAD_REQUEST, "senha atual está incorreta");
    }

    @Test
    @DisplayName("rejeita senha fraca")
    void shouldRejectWeakPassword() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("senha.weak@laweact.com", "15350946056")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> patch = api.patch(
                "/usuarios/me/senha",
                Map.of("senhaAtual", Fixtures.VALID_PASSWORD, "novaSenha", "fraca")
        );
        assertErrorDetailContains(patch, HttpStatus.UNPROCESSABLE_ENTITY, "mínimo 8 caracteres");
    }

    @Test
    @DisplayName("rejeita chamada sem autenticação")
    void shouldRejectUnauthenticated() {
        ResponseEntity<JsonNode> patch = api.patch(
                "/usuarios/me/senha",
                Map.of("senhaAtual", Fixtures.VALID_PASSWORD, "novaSenha", "NovaSenha1")
        );
        assertThat(patch.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
