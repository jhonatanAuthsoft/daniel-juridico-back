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

    @Test
    @DisplayName("cadastro advogado cria TRIAL → /me libera → expirar trial bloqueia → validar fake ativa")
    void shouldManageSubscriptionLifecycle() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido("assinatura@laweact.com", "39053344705", "880101")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> meTrial = api.get("/usuarios/me");
        assertSuccess(meTrial, HttpStatus.OK);
        JsonNode assinaturaTrial = meTrial.getBody().path("data").path("assinatura");
        assertThat(assinaturaTrial.path("status").asText()).isEqualTo("TRIAL");
        assertThat(assinaturaTrial.path("acessoLiberado").asBoolean()).isTrue();
        assertThat(assinaturaTrial.path("emTrial").asBoolean()).isTrue();

        ResponseEntity<JsonNode> expirarTrial = api.post("/dev/assinaturas/expirar-trial", Map.of());
        assertSuccess(expirarTrial, HttpStatus.OK);
        assertThat(expirarTrial.getBody().path("data").path("acessoLiberado").asBoolean()).isFalse();

        ResponseEntity<JsonNode> meBloqueado = api.get("/usuarios/me");
        assertSuccess(meBloqueado, HttpStatus.OK);
        assertThat(meBloqueado.getBody().path("data").path("assinatura").path("acessoLiberado").asBoolean())
                .isFalse();

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

        ResponseEntity<JsonNode> meAtivo = api.get("/usuarios/me");
        assertSuccess(meAtivo, HttpStatus.OK);
        assertThat(meAtivo.getBody().path("data").path("assinatura").path("acessoLiberado").asBoolean())
                .isTrue();
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
