package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorDetailContains;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    private static final String CPF = "52998224725";

    @Autowired
    private EmailServiceImp emailServiceImp;

    private void cadastrarCliente(String email) {
        assertSuccess(api.post("/clientes/cadastrar", Fixtures.clienteValido(email, CPF)), HttpStatus.CREATED);
    }

    private String solicitarEObterCodigo(String email) {
        ResponseEntity<JsonNode> response = api.post(
                "/usuarios/recuperar-senha",
                SolicitarRecuperacaoSenhaInputDTO.builder().email(email).build()
        );
        assertSuccess(response, HttpStatus.OK);
        String codigo = emailServiceImp.obterCodigoCapturado(email);
        assertThat(codigo).isNotBlank().hasSize(4);
        return codigo;
    }

    @Nested
    @DisplayName("POST /usuarios/recuperar-senha")
    class Solicitar {

        @Test
        @DisplayName("deve retornar mensagem genérica para e-mail inexistente")
        void shouldReturnGenericMessageForUnknownEmail() {
            ResponseEntity<JsonNode> response = api.post(
                    "/usuarios/recuperar-senha",
                    SolicitarRecuperacaoSenhaInputDTO.builder().email("naoexiste@laweact.com").build()
            );

            assertSuccess(response, HttpStatus.OK);
            assertThat(response.getBody().path("data").path("mensagem").asText())
                    .isEqualTo(RecuperacaoSenhaServiceImp.MENSAGEM_GENERICA);
            assertThat(emailServiceImp.obterCodigoCapturado("naoexiste@laweact.com")).isNull();
        }

        @Test
        @DisplayName("deve retornar mensagem genérica para usuário inativo (sem enviar código)")
        void shouldReturnGenericMessageForInactiveUser() {
            String email = "reset.inativo@laweact.com";
            cadastrarCliente(email);
            jdbcTemplate.update("UPDATE usuarios SET status = 'INATIVO' WHERE email = ?", email);

            ResponseEntity<JsonNode> response = api.post(
                    "/usuarios/recuperar-senha",
                    SolicitarRecuperacaoSenhaInputDTO.builder().email(email).build()
            );

            assertSuccess(response, HttpStatus.OK);
            assertThat(response.getBody().path("data").path("mensagem").asText())
                    .isEqualTo(RecuperacaoSenhaServiceImp.MENSAGEM_GENERICA);
            assertThat(emailServiceImp.obterCodigoCapturado(email)).isNull();
        }

        @Test
        @DisplayName("deve enviar código e registrar auditoria para usuário ativo")
        void shouldSendCodeForActiveUser() {
            String email = "reset.ativo@laweact.com";
            cadastrarCliente(email);

            ResponseEntity<JsonNode> response = api.post(
                    "/usuarios/recuperar-senha",
                    SolicitarRecuperacaoSenhaInputDTO.builder().email(email).build()
            );

            assertSuccess(response, HttpStatus.OK);
            assertThat(response.getBody().path("data").path("mensagem").asText())
                    .isEqualTo(RecuperacaoSenhaServiceImp.MENSAGEM_GENERICA);
            assertThat(response.getBody().path("data").path("aguardarSegundos").asInt()).isEqualTo(5);
            assertThat(emailServiceImp.obterCodigoCapturado(email)).isNotBlank().hasSize(4);

            Integer tokens = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tokens_recuperacao_senha t JOIN usuarios u ON u.id = t.usuario_id WHERE u.email = ?",
                    Integer.class,
                    email
            );
            assertThat(tokens).isEqualTo(1);

            Integer auditorias = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auditoria_eventos WHERE evento = ?",
                    Integer.class,
                    RecuperacaoSenhaServiceImp.EVENTO_RECUPERACAO_SENHA
            );
            assertThat(auditorias).isEqualTo(1);
        }

        @Test
        @DisplayName("deve normalizar e-mail (maiúsculas) ao solicitar")
        void shouldNormalizeEmailCase() {
            String email = "reset.case@laweact.com";
            cadastrarCliente(email);

            ResponseEntity<JsonNode> response = api.post(
                    "/usuarios/recuperar-senha",
                    SolicitarRecuperacaoSenhaInputDTO.builder().email("Reset.Case@Laweact.com").build()
            );

            assertSuccess(response, HttpStatus.OK);
            assertThat(emailServiceImp.obterCodigoCapturado(email)).isNotBlank();
        }

        @Test
        @DisplayName("deve aplicar cooldown sem gerar novo código")
        void shouldApplyCooldownWithoutNewCode() {
            String email = "reset.cooldown@laweact.com";
            cadastrarCliente(email);

            String primeiro = solicitarEObterCodigo(email);

            ResponseEntity<JsonNode> segundo = api.post(
                    "/usuarios/recuperar-senha",
                    SolicitarRecuperacaoSenhaInputDTO.builder().email(email).build()
            );

            assertSuccess(segundo, HttpStatus.OK);
            assertThat(segundo.getBody().path("data").path("aguardarSegundos").asInt()).isPositive();
            assertThat(emailServiceImp.obterCodigoCapturado(email)).isEqualTo(primeiro);

            Integer tokens = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM tokens_recuperacao_senha t JOIN usuarios u ON u.id = t.usuario_id WHERE u.email = ?",
                    Integer.class,
                    email
            );
            assertThat(tokens).isEqualTo(1);
        }

        @Test
        @DisplayName("novo código deve invalidar o anterior após cooldown")
        void shouldInvalidatePreviousCodeWhenRequestingNewOne() throws InterruptedException {
            String email = "reset.invalidate@laweact.com";
            cadastrarCliente(email);

            String primeiroCodigo = solicitarEObterCodigo(email);
            Thread.sleep(5100);
            String segundoCodigo = solicitarEObterCodigo(email);
            assertThat(segundoCodigo).isNotEqualTo(primeiroCodigo);

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

        @Test
        @DisplayName("deve rejeitar e-mail inválido")
        void shouldRejectInvalidEmail() {
            ResponseEntity<JsonNode> response = api.post(
                    "/usuarios/recuperar-senha",
                    SolicitarRecuperacaoSenhaInputDTO.builder().email("nao-e-email").build()
            );
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }
    }

    @Nested
    @DisplayName("POST /usuarios/validar-codigo-recuperacao")
    class Validar {

        @Test
        @DisplayName("deve validar código correto sem consumir")
        void shouldValidateWithoutConsuming() {
            String email = "reset.validar.ok@laweact.com";
            cadastrarCliente(email);
            String codigo = solicitarEObterCodigo(email);

            ResponseEntity<JsonNode> primeira = api.post(
                    "/usuarios/validar-codigo-recuperacao",
                    ValidarCodigoRecuperacaoInputDTO.builder().email(email).codigo(codigo).build()
            );
            assertSuccess(primeira, HttpStatus.OK);
            assertThat(primeira.getBody().path("data").path("valido").asBoolean()).isTrue();

            ResponseEntity<JsonNode> segunda = api.post(
                    "/usuarios/validar-codigo-recuperacao",
                    ValidarCodigoRecuperacaoInputDTO.builder().email(email).codigo(codigo).build()
            );
            assertSuccess(segunda, HttpStatus.OK);
            assertThat(segunda.getBody().path("data").path("valido").asBoolean()).isTrue();
        }

        @Test
        @DisplayName("deve retornar inválido para e-mail inexistente")
        void shouldFailForUnknownEmail() {
            ResponseEntity<JsonNode> response = api.post(
                    "/usuarios/validar-codigo-recuperacao",
                    ValidarCodigoRecuperacaoInputDTO.builder()
                            .email("naoexiste@laweact.com")
                            .codigo("1234")
                            .build()
            );

            assertSuccess(response, HttpStatus.OK);
            assertThat(response.getBody().path("data").path("valido").asBoolean()).isFalse();
            assertThat(response.getBody().path("data").path("mensagem").asText())
                    .contains("Código inválido ou expirado");
        }

        @Test
        @DisplayName("deve retornar inválido para código errado")
        void shouldFailForWrongCode() {
            String email = "reset.validar.errado@laweact.com";
            cadastrarCliente(email);
            solicitarEObterCodigo(email);

            ResponseEntity<JsonNode> response = api.post(
                    "/usuarios/validar-codigo-recuperacao",
                    ValidarCodigoRecuperacaoInputDTO.builder().email(email).codigo("0000").build()
            );

            assertSuccess(response, HttpStatus.OK);
            assertThat(response.getBody().path("data").path("valido").asBoolean()).isFalse();
        }

        @Test
        @DisplayName("deve retornar inválido para código expirado")
        void shouldFailForExpiredCode() {
            String email = "reset.validar.expirado@laweact.com";
            cadastrarCliente(email);
            String codigo = solicitarEObterCodigo(email);

            jdbcTemplate.update(
                    """
                    UPDATE tokens_recuperacao_senha t
                    SET expira_em = NOW() - INTERVAL '1 minute'
                    FROM usuarios u
                    WHERE t.usuario_id = u.id AND u.email = ?
                    """,
                    email
            );

            ResponseEntity<JsonNode> response = api.post(
                    "/usuarios/validar-codigo-recuperacao",
                    ValidarCodigoRecuperacaoInputDTO.builder().email(email).codigo(codigo).build()
            );

            assertSuccess(response, HttpStatus.OK);
            assertThat(response.getBody().path("data").path("valido").asBoolean()).isFalse();
        }

        @Test
        @DisplayName("deve rejeitar código com formato inválido")
        void shouldRejectInvalidCodeFormat() {
            ResponseEntity<JsonNode> response = api.post(
                    "/usuarios/validar-codigo-recuperacao",
                    ValidarCodigoRecuperacaoInputDTO.builder()
                            .email("a@laweact.com")
                            .codigo("12")
                            .build()
            );
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }
    }

    @Nested
    @DisplayName("POST /usuarios/redefinir-senha")
    class Redefinir {

        @Test
        @DisplayName("fluxo completo: redefinir → login nova senha → JWT antigo inválido")
        void shouldResetPasswordAndInvalidateSessions() {
            String email = "reset.ok@laweact.com";
            ResponseEntity<JsonNode> cadastro = api.post("/clientes/cadastrar", Fixtures.clienteValido(email, CPF));
            assertSuccess(cadastro, HttpStatus.CREATED);
            String tokenAntigo = cadastro.getBody().path("data").path("token").asText();
            assertThat(tokenAntigo).isNotBlank();

            String codigo = solicitarEObterCodigo(email);

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

            api.authenticate(tokenAntigo);
            ResponseEntity<JsonNode> meComTokenAntigo = api.get("/usuarios/me");
            assertThat(meComTokenAntigo.getStatusCode().value()).isIn(401, 403);

            Integer auditorias = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM auditoria_eventos WHERE evento = ?",
                    Integer.class,
                    RecuperacaoSenhaServiceImp.EVENTO_RECUPERACAO_SENHA
            );
            assertThat(auditorias).isGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("deve rejeitar código inválido")
        void shouldFailWithWrongCode() {
            String email = "reset.codigoerrado@laweact.com";
            cadastrarCliente(email);
            solicitarEObterCodigo(email);

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
        @DisplayName("deve rejeitar código já usado (uso único)")
        void shouldFailWhenCodeAlreadyUsed() {
            String email = "reset.usado@laweact.com";
            cadastrarCliente(email);
            String codigo = solicitarEObterCodigo(email);

            assertSuccess(
                    api.post(
                            "/usuarios/redefinir-senha",
                            RedefinirSenhaInputDTO.builder()
                                    .email(email)
                                    .codigo(codigo)
                                    .novaSenha("NovaSenha1")
                                    .confirmarSenha("NovaSenha1")
                                    .build()
                    ),
                    HttpStatus.OK
            );

            ResponseEntity<JsonNode> segunda = api.post(
                    "/usuarios/redefinir-senha",
                    RedefinirSenhaInputDTO.builder()
                            .email(email)
                            .codigo(codigo)
                            .novaSenha("OutraSenha1")
                            .confirmarSenha("OutraSenha1")
                            .build()
            );
            assertErrorDetailContains(segunda, HttpStatus.BAD_REQUEST, "Código inválido ou expirado");
        }

        @Test
        @DisplayName("deve rejeitar código expirado")
        void shouldFailWithExpiredCode() {
            String email = "reset.redefinir.expirado@laweact.com";
            cadastrarCliente(email);
            String codigo = solicitarEObterCodigo(email);

            jdbcTemplate.update(
                    """
                    UPDATE tokens_recuperacao_senha t
                    SET expira_em = NOW() - INTERVAL '1 minute'
                    FROM usuarios u
                    WHERE t.usuario_id = u.id AND u.email = ?
                    """,
                    email
            );

            ResponseEntity<JsonNode> response = api.post(
                    "/usuarios/redefinir-senha",
                    RedefinirSenhaInputDTO.builder()
                            .email(email)
                            .codigo(codigo)
                            .novaSenha("NovaSenha1")
                            .confirmarSenha("NovaSenha1")
                            .build()
            );
            assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "Código inválido ou expirado");
        }

        @Test
        @DisplayName("deve rejeitar confirmação de senha divergente")
        void shouldFailWhenPasswordConfirmationDiffers() {
            String email = "reset.confirm@laweact.com";
            cadastrarCliente(email);
            String codigo = solicitarEObterCodigo(email);

            ResponseEntity<JsonNode> response = api.post(
                    "/usuarios/redefinir-senha",
                    RedefinirSenhaInputDTO.builder()
                            .email(email)
                            .codigo(codigo)
                            .novaSenha("NovaSenha1")
                            .confirmarSenha("OutraSenha1")
                            .build()
            );
            assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "confirmação de senha não confere");
        }

        @Test
        @DisplayName("deve rejeitar senha fraca")
        void shouldRejectWeakPassword() {
            String email = "reset.fraca@laweact.com";
            cadastrarCliente(email);
            String codigo = solicitarEObterCodigo(email);

            ResponseEntity<JsonNode> response = api.post(
                    "/usuarios/redefinir-senha",
                    RedefinirSenhaInputDTO.builder()
                            .email(email)
                            .codigo(codigo)
                            .novaSenha("fraca")
                            .confirmarSenha("fraca")
                            .build()
            );
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }

        @Test
        @DisplayName("deve rejeitar e-mail inexistente com mensagem genérica de código")
        void shouldFailForUnknownEmail() {
            ResponseEntity<JsonNode> response = api.post(
                    "/usuarios/redefinir-senha",
                    RedefinirSenhaInputDTO.builder()
                            .email("naoexiste@laweact.com")
                            .codigo("1234")
                            .novaSenha("NovaSenha1")
                            .confirmarSenha("NovaSenha1")
                            .build()
            );
            assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "Código inválido ou expirado");
        }
    }
}
