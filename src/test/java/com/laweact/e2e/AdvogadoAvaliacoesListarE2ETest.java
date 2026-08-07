package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.e2e.support.Fixtures;

@DisplayName("E2E — GET /advogados/{id}/avaliacoes")
class AdvogadoAvaliacoesListarE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("lista avaliações paginadas com agregados e flag propria")
    void shouldListAvaliacoes() {
        ResponseEntity<JsonNode> cadastroAdv = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido("aval.adv@laweact.com", "39053344705", "155242")
        );
        assertSuccess(cadastroAdv, HttpStatus.CREATED);
        UUID advogadoId = UUID.fromString(
                cadastroAdv.getBody().path("data").path("usuario").path("id").asText()
        );

        ResponseEntity<JsonNode> cli1 = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("aval.cli1@laweact.com", "15350946056")
        );
        assertSuccess(cli1, HttpStatus.CREATED);
        UUID cliente1Id = UUID.fromString(cli1.getBody().path("data").path("usuario").path("id").asText());
        String tokenCli1 = cli1.getBody().path("data").path("token").asText();

        ResponseEntity<JsonNode> cli2 = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("aval.cli2@laweact.com", "71428793860")
        );
        assertSuccess(cli2, HttpStatus.CREATED);
        UUID cliente2Id = UUID.fromString(cli2.getBody().path("data").path("usuario").path("id").asText());

        inserirAvaliacao(advogadoId, cliente1Id, "4.0", "Atendimento claro e profissional.");
        inserirAvaliacao(advogadoId, cliente2Id, "5.0", "Excelente orientação jurídica.");

        api.authenticate(tokenCli1);
        ResponseEntity<JsonNode> response = api.get("/advogados/" + advogadoId + "/avaliacoes");

        assertSuccess(response, HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        JsonNode items = data.path("items");
        assertThat(items.isArray()).isTrue();
        assertThat(items).hasSize(2);

        assertThat(data.path("totalAvaliacoes").asInt()).isEqualTo(2);
        assertThat(data.path("mediaAvaliacoes").decimalValue())
                .isEqualByComparingTo(new BigDecimal("4.5"));

        JsonNode propria = null;
        for (JsonNode item : items) {
            assertThat(item.path("id").asText()).isNotBlank();
            assertThat(item.path("nota").isNumber()).isTrue();
            assertThat(item.path("comentario").asText()).isNotBlank();
            assertThat(item.path("nomeAvaliador").asText()).isNotBlank();
            assertThat(item.path("criadoEm").asText()).isNotBlank();
            if (item.path("propria").asBoolean()) {
                propria = item;
            }
        }
        assertThat(propria).isNotNull();
        assertThat(propria.path("comentario").asText()).contains("Atendimento claro");

        JsonNode pagination = response.getBody().path("pagination");
        assertThat(pagination.path("size").asInt()).isEqualTo(10);
        assertThat(pagination.path("totalElements").asInt()).isEqualTo(2);
        assertThat(pagination.path("page").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("advogado sem avaliações retorna lista vazia e agregados zerados")
    void shouldReturnEmptyList() {
        ResponseEntity<JsonNode> cadastroAdv = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido("aval.empty@laweact.com", "39053344705", "998877")
        );
        assertSuccess(cadastroAdv, HttpStatus.CREATED);
        String advogadoId = cadastroAdv.getBody().path("data").path("usuario").path("id").asText();

        ResponseEntity<JsonNode> cli = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("aval.empty.cli@laweact.com", "15350946056")
        );
        assertSuccess(cli, HttpStatus.CREATED);
        api.authenticate(cli.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> response = api.get("/advogados/" + advogadoId + "/avaliacoes");
        assertSuccess(response, HttpStatus.OK);
        assertThat(response.getBody().path("data").path("items")).isEmpty();
        assertThat(response.getBody().path("data").path("totalAvaliacoes").asInt()).isEqualTo(0);
        assertThat(response.getBody().path("data").path("mediaAvaliacoes").decimalValue())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("sem autenticação retorna 401")
    void shouldRequireAuth() {
        ResponseEntity<JsonNode> response = api.get("/advogados/" + UUID.randomUUID() + "/avaliacoes");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("advogado inexistente retorna 404")
    void shouldReturn404WhenAdvogadoMissing() {
        ResponseEntity<JsonNode> cli = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("aval.404@laweact.com", "15350946056")
        );
        assertSuccess(cli, HttpStatus.CREATED);
        api.authenticate(cli.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> response = api.get("/advogados/" + UUID.randomUUID() + "/avaliacoes");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void inserirAvaliacao(UUID advogadoId, UUID clienteId, String nota, String comentario) {
        jdbcTemplate.update(
                """
                INSERT INTO avaliacoes_advogado (id, advogado_id, cliente_id, nota, comentario, criado_em)
                VALUES (?::uuid, ?::uuid, ?::uuid, ?::numeric, ?, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(),
                advogadoId,
                clienteId,
                nota,
                comentario
        );
    }
}
