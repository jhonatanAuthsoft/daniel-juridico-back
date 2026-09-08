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

@DisplayName("E2E — assinaturas")
class AssinaturaE2ETest extends BaseE2ETest {

    private void cadastrarEAutenticarAdvogado(String email, String cpf, String oab) {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido(email, cpf, oab)
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());
    }

    @Test
    @DisplayName("advogado nasce no paywall (PENDENTE) e é bloqueado nas rotas do app")
    void shouldBlockNewLawyerUntilSubscription() {
        cadastrarEAutenticarAdvogado("assinatura@laweact.com", "39053344705", "880101");

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        JsonNode assinatura = me.getBody().path("data").path("assinatura");
        assertThat(assinatura.path("status").asText()).isEqualTo("PENDENTE");
        assertThat(assinatura.path("acessoLiberado").asBoolean()).isFalse();

        ResponseEntity<JsonNode> bloqueado = api.get("/notificacoes");
        assertThat(bloqueado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(bloqueado.getBody().path("errors").path(0).path("code").asText())
                .isEqualTo("SUBSCRIPTION_REQUIRED");
    }

    @Test
    @DisplayName("assinou pela loja → acesso liberado; bloquear volta ao paywall")
    void shouldManageSubscriptionLifecycle() {
        cadastrarEAutenticarAdvogado("assinatura.ciclo@laweact.com", "26153377050", "880102");

        String fakeToken = "fake:e2e-" + System.currentTimeMillis();
        ResponseEntity<JsonNode> validar = api.post(
                "/assinaturas/validar",
                Map.of(
                        "plataforma", "FAKE",
                        "productId", "laweact_basic_mensal",
                        "purchaseToken", fakeToken
                )
        );
        assertSuccess(validar, HttpStatus.OK);
        assertThat(validar.getBody().path("data").path("status").asText()).isEqualTo("ATIVA");
        assertThat(validar.getBody().path("data").path("acessoLiberado").asBoolean()).isTrue();

        ResponseEntity<JsonNode> liberado = api.get("/notificacoes");
        assertSuccess(liberado, HttpStatus.OK);

        ResponseEntity<JsonNode> bloquear = api.post("/dev/assinaturas/bloquear", Map.of());
        assertSuccess(bloquear, HttpStatus.OK);
        assertThat(bloquear.getBody().path("data").path("status").asText()).isEqualTo("PENDENTE");
        assertThat(bloquear.getBody().path("data").path("acessoLiberado").asBoolean()).isFalse();

        ResponseEntity<JsonNode> meBloqueado = api.get("/usuarios/me");
        assertSuccess(meBloqueado, HttpStatus.OK);
        assertThat(meBloqueado.getBody().path("data").path("assinatura").path("acessoLiberado").asBoolean())
                .isFalse();
    }

    @Test
    @DisplayName("cancelou dentro do período vigente: mantém acesso até o fim")
    void shouldKeepAccessAfterCancellingInsidePeriod() {
        cadastrarEAutenticarAdvogado("assinatura.cancelada@laweact.com", "71428793860", "880103");
        assinarPlanoDoAdvogadoAutenticado();

        jdbcTemplate.update("UPDATE assinaturas SET status = 'CANCELADA', auto_renovacao = FALSE");

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        JsonNode assinatura = me.getBody().path("data").path("assinatura");
        assertThat(assinatura.path("status").asText()).isEqualTo("CANCELADA");
        assertThat(assinatura.path("acessoLiberado").asBoolean()).isTrue();
        assertThat(assinatura.path("autoRenovacao").asBoolean()).isFalse();

        ResponseEntity<JsonNode> liberado = api.get("/notificacoes");
        assertSuccess(liberado, HttpStatus.OK);
    }

    @Test
    @DisplayName("cancelou e o período venceu: bloqueia")
    void shouldBlockAfterCancelledPeriodEnds() {
        cadastrarEAutenticarAdvogado("assinatura.vencida@laweact.com", "15350946056", "880104");
        assinarPlanoDoAdvogadoAutenticado();

        jdbcTemplate.update(
                "UPDATE assinaturas SET status = 'CANCELADA', periodo_fim_em = NOW() - INTERVAL '1 minute'"
        );

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        assertThat(me.getBody().path("data").path("assinatura").path("acessoLiberado").asBoolean())
                .isFalse();
    }

    @Test
    @DisplayName("cliente não precisa de assinatura")
    void clientShouldNotRequireSubscription() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("cliente.assinatura@laweact.com", "52998224725")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        assertThat(me.getBody().path("data").path("assinatura").path("acessoLiberado").asBoolean()).isTrue();
    }
}
