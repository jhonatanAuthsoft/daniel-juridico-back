package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorCode;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.e2e.support.Fixtures;

@DisplayName("E2E — GET /advogados/{id}")
class AdvogadoPerfilPublicoE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("cliente autenticado visualiza perfil público do advogado com campos da UI")
    void shouldReturnPublicProfileForCliente() {
        ResponseEntity<JsonNode> cadastroAdv = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido("perfil.adv@laweact.com", "39053344705", "155242")
        );
        assertSuccess(cadastroAdv, HttpStatus.CREATED);
        String advogadoId = cadastroAdv.getBody().path("data").path("usuario").path("id").asText();

        ResponseEntity<JsonNode> cadastroCli = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("perfil.cli@laweact.com", "15350946056")
        );
        assertSuccess(cadastroCli, HttpStatus.CREATED);
        api.authenticate(cadastroCli.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> response = api.get("/advogados/" + advogadoId);

        assertSuccess(response, HttpStatus.OK);
        JsonNode data = response.getBody().path("data");

        assertThat(data.path("id").asText()).isEqualTo(advogadoId);
        assertThat(data.path("nome").asText()).isEqualTo("João Advogado");
        assertThat(data.path("pronomeTratamento").asText()).isEqualTo("DOUTOR");
        assertThat(data.path("biografia").isMissingNode() || data.path("biografia").isNull()
                || data.path("biografia").isTextual()).isTrue();
        assertThat(data.path("universidade").asText()).isEqualTo("USP");
        assertThat(data.path("curso").asText()).isEqualTo("Direito");
        assertThat(data.path("anoFormacao").asInt()).isEqualTo(2015);
        assertThat(data.path("atuacaoDesde").asText()).isEqualTo("2016-03-15");
        assertThat(data.path("anosExperiencia").asInt()).isGreaterThanOrEqualTo(0);
        assertThat(data.path("mediaAvaliacoes").isNumber()).isTrue();
        assertThat(data.path("totalAvaliacoes").isNumber()).isTrue();
        assertThat(data.path("disponibilidade").asText()).isNotBlank();

        assertThat(data.path("endereco").path("bairro").asText()).isEqualTo("Bela Vista");
        assertThat(data.path("endereco").path("cidade").asText()).isEqualTo("São Paulo");
        assertThat(data.path("endereco").path("estado").asText()).isEqualTo("SP");
        assertThat(data.path("endereco").has("cep")).isFalse();
        assertThat(data.path("endereco").has("numero")).isFalse();

        assertThat(data.path("oabPrincipal").path("numero").asText()).isEqualTo("155242");
        assertThat(data.path("oabPrincipal").path("uf").asText()).isEqualTo("SP");
        assertThat(data.path("oabPrincipal").has("fotosUrls")).isFalse();

        assertThat(data.path("oabsSuplementares").isArray()).isTrue();
        assertThat(data.path("modalidades").isArray()).isTrue();
        assertThat(data.path("modalidades").get(0).path("codigo").asText()).isEqualTo("GENERALISTA");
        assertThat(data.path("modalidades").get(0).path("nome").asText()).isNotBlank();

        assertThat(data.path("especialidades").isArray()).isTrue();
        assertThat(data.path("especialidades").get(0).path("codigo").asText()).isEqualTo("CIVIL");
        assertThat(data.path("especialidades").get(0).path("nome").asText()).isNotBlank();

        assertThat(data.path("subespecialidades").isArray()).isTrue();
        assertThat(data.path("formasCobranca").isArray()).isTrue();
        assertThat(data.path("formasCobranca").get(0).path("codigo").asText())
                .isEqualTo("HONORARIOS_CONTRATUAIS");
        assertThat(data.path("areasAtuacao").isArray()).isTrue();
        assertThat(data.path("areasAtuacao").get(0).path("cidade").asText()).isEqualTo("São Paulo");

        assertThat(data.has("cpf")).isFalse();
        assertThat(data.has("rg")).isFalse();
        assertThat(data.has("telefone")).isFalse();
        assertThat(data.has("email")).isFalse();
    }

    @Test
    @DisplayName("advogado inexistente retorna 404")
    void shouldReturn404WhenNotFound() {
        ResponseEntity<JsonNode> cadastroCli = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("perfil.404@laweact.com", "39053344705")
        );
        assertSuccess(cadastroCli, HttpStatus.CREATED);
        api.authenticate(cadastroCli.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> response = api.get("/advogados/" + UUID.randomUUID());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("sem autenticação retorna 401")
    void shouldRequireAuth() {
        ResponseEntity<JsonNode> response = api.get("/advogados/" + UUID.randomUUID());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("advogado autenticado não pode consultar perfil público de outro")
    void shouldForbidAdvogado() {
        ResponseEntity<JsonNode> cadastroAdv = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido("perfil.target@laweact.com", "39053344705", "155242")
        );
        assertSuccess(cadastroAdv, HttpStatus.CREATED);
        String targetId = cadastroAdv.getBody().path("data").path("usuario").path("id").asText();

        ResponseEntity<JsonNode> outroAdv = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido("perfil.viewer@laweact.com", "15350946056", "998877")
        );
        assertSuccess(outroAdv, HttpStatus.CREATED);
        api.authenticate(outroAdv.getBody().path("data").path("token").asText());
        garantirAssinaturaSeAdvogado();

        ResponseEntity<JsonNode> response = api.get("/advogados/" + targetId);
        assertErrorCode(response, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }
}
