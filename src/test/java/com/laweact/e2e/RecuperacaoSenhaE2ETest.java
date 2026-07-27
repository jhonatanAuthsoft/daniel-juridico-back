package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorDetailContains;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.dto.usuario.RedefinirSenhaInputDTO;
import com.laweact.dto.usuario.SolicitarRecuperacaoSenhaInputDTO;
import com.laweact.dto.usuario.ValidarCodigoRecuperacaoInputDTO;
import com.laweact.e2e.support.Fixtures;
import com.laweact.service.imp.EmailServiceImp;
import com.laweact.service.imp.RecuperacaoSenhaServiceImp;

@DisplayName("E2E — Recuperação de senha")
class RecuperacaoSenhaE2ETest extends BaseE2ETest {

    @Autowired
    private EmailServiceImp emailServiceImp;

    @Test
    @DisplayName("deve solicitar código com resposta genérica mesmo se e-mail não existir")
    void shouldReturnGenericMessageForUnknownEmail() {
        ResponseEntity<JsonNode> response = api.post(
                "/usuarios/recuperar-senha",
                SolicitarRecuperacaoSenhaInputDTO.builder().email("naoexiste@laweact.com").build()
        );

        assertSuccess(response, HttpStatus.OK);
        assertThat(response.getBody().path("data").path("mensagem").asText())
                .isEqualTo(RecuperacaoSenhaServiceImp.MENSAGEM_GENERICA);
    }

    @Test
    @DisplayName("fluxo completo: solicitar → validar → redefinir → login com nova senha")
    void shouldResetPasswordSuccessfully() {
        String email = "reset.ok@laweact.com";
        api.post("/clientes/cadastrar", Fixtures.clienteValido(email, "52998224725"));

        ResponseEntity<JsonNode> solicitar = api.post(
                "/usuarios/recuperar-senha",
                SolicitarRecuperacaoSenhaInputDTO.builder().email(email).build()
        );
        assertSuccess(solicitar, HttpStatus.OK);

        String codigo = emailServiceImp.obterCodigoCapturado(email);
        assertThat(codigo).isNotBlank().hasSize(4);

        ResponseEntity<JsonNode> validar = api.post(
                "/usuarios/validar-codigo-recuperacao",
                ValidarCodigoRecuperacaoInputDTO.builder().email(email).codigo(codigo).build()
        );
        assertSuccess(validar, HttpStatus.OK);
        assertThat(validar.getBody().path("data").path("valido").asBoolean()).isTrue();

        String novaSenha = "NovaSenha1";
        ResponseEntity<JsonNode> redefinir = api.post(
                "/usuarios/redefinir-senha",
                RedefinirSenhaInputDTO.builder()
                        .email(email)
                        .codigo(codigo)
                        .novaSenha(novaSenha)
                        .confirmarSenha(novaSenha)
                        .build()
        );
        assertSuccess(redefinir, HttpStatus.OK);
        assertThat(redefinir.getBody().path("data").path("mensagem").asText())
                .contains("Senha alterada com sucesso");

        ResponseEntity<JsonNode> loginAntigo = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder().email(email).senha(Fixtures.VALID_PASSWORD).build()
        );
        assertErrorDetailContains(loginAntigo, HttpStatus.BAD_REQUEST, "E-mail ou senha inválidos");

        ResponseEntity<JsonNode> loginNovo = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder().email(email).senha(novaSenha).build()
        );
        assertSuccess(loginNovo, HttpStatus.OK);
        assertThat(loginNovo.getBody().path("data").path("token").asText()).isNotBlank();

        Integer auditorias = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auditoria_eventos WHERE evento = ?",
                Integer.class,
                RecuperacaoSenhaServiceImp.EVENTO_RECUPERACAO_SENHA
        );
        assertThat(auditorias).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("deve rejeitar código inválido/expirado na redefinição")
    void shouldFailResetWithWrongCode() {
        String email = "reset.codigoerrado@laweact.com";
        api.post("/clientes/cadastrar", Fixtures.clienteValido(email, "11144477735"));
        api.post("/usuarios/recuperar-senha", SolicitarRecuperacaoSenhaInputDTO.builder().email(email).build());

        ResponseEntity<JsonNode> response = api.post(
                "/usuarios/redefinir-senha",
                RedefinirSenhaInputDTO.builder()
                        .email(email)
                        .codigo("0000")
                        .novaSenha("NovaSenha1")
                        .confirmarSenha("NovaSenha1")
                        .build()
        );

        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "Código inválido ou expirado");
    }

    @Test
    @DisplayName("novo código deve invalidar o anterior")
    void shouldInvalidatePreviousCodeWhenRequestingNewOne() throws InterruptedException {
        String email = "reset.invalidate@laweact.com";
        api.post("/clientes/cadastrar", Fixtures.clienteValido(email, "39053344705"));

        api.post("/usuarios/recuperar-senha", SolicitarRecuperacaoSenhaInputDTO.builder().email(email).build());
        String primeiroCodigo = emailServiceImp.obterCodigoCapturado(email);
        assertThat(primeiroCodigo).isNotBlank();

        Thread.sleep(5100);

        api.post("/usuarios/recuperar-senha", SolicitarRecuperacaoSenhaInputDTO.builder().email(email).build());
        String segundoCodigo = emailServiceImp.obterCodigoCapturado(email);
        assertThat(segundoCodigo).isNotBlank().isNotEqualTo(primeiroCodigo);

        ResponseEntity<JsonNode> validarAntigo = api.post(
                "/usuarios/validar-codigo-recuperacao",
                ValidarCodigoRecuperacaoInputDTO.builder().email(email).codigo(primeiroCodigo).build()
        );
        assertThat(validarAntigo.getBody().path("data").path("valido").asBoolean()).isFalse();

        ResponseEntity<JsonNode> validarNovo = api.post(
                "/usuarios/validar-codigo-recuperacao",
                ValidarCodigoRecuperacaoInputDTO.builder().email(email).codigo(segundoCodigo).build()
        );
        assertThat(validarNovo.getBody().path("data").path("valido").asBoolean()).isTrue();
    }
}
