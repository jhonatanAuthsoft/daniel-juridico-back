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

@DisplayName("E2E — DELETE /advogados/{advogadoId}/avaliacoes/{avaliacaoId}")
class AdvogadoAvaliacaoExcluirE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("cliente exclui a própria avaliação")
    void shouldDeleteOwnAvaliacao() {
        UUID advogadoId = cadastrarAdvogado("del.adv@laweact.com", "39053344705", "155242");
        ResponseEntity<JsonNode> cli = cadastrarCliente("del.cli@laweact.com", "15350946056");
        UUID clienteId = UUID.fromString(cli.getBody().path("data").path("usuario").path("id").asText());
        UUID avaliacaoId = inserirAvaliacao(advogadoId, clienteId, "4.5", "Bom atendimento.");

        api.authenticate(cli.getBody().path("data").path("token").asText());
        ResponseEntity<JsonNode> response = api.delete(
                "/advogados/" + advogadoId + "/avaliacoes/" + avaliacaoId
        );

        assertSuccess(response, HttpStatus.OK);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM avaliacoes_advogado WHERE id = ?",
                Integer.class,
                avaliacaoId
        );
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("outro cliente não pode excluir avaliação alheia")
    void shouldForbidOtherCliente() {
        UUID advogadoId = cadastrarAdvogado("del.adv2@laweact.com", "39053344705", "155243");
        ResponseEntity<JsonNode> autor = cadastrarCliente("del.autor@laweact.com", "15350946056");
        UUID autorId = UUID.fromString(autor.getBody().path("data").path("usuario").path("id").asText());
        UUID avaliacaoId = inserirAvaliacao(advogadoId, autorId, "5.0", "Excelente.");

        ResponseEntity<JsonNode> outro = cadastrarCliente("del.outro@laweact.com", "71428793860");
        api.authenticate(outro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> response = api.delete(
                "/advogados/" + advogadoId + "/avaliacoes/" + avaliacaoId
        );
        assertErrorCode(response, HttpStatus.FORBIDDEN, "FORBIDDEN");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM avaliacoes_advogado WHERE id = ?",
                Integer.class,
                avaliacaoId
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("avaliação inexistente retorna 404")
    void shouldReturn404WhenMissing() {
        UUID advogadoId = cadastrarAdvogado("del.adv3@laweact.com", "39053344705", "155244");
        ResponseEntity<JsonNode> cli = cadastrarCliente("del.404@laweact.com", "15350946056");
        api.authenticate(cli.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> response = api.delete(
                "/advogados/" + advogadoId + "/avaliacoes/" + UUID.randomUUID()
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("sem autenticação retorna 401")
    void shouldRequireAuth() {
        ResponseEntity<JsonNode> response = api.delete(
                "/advogados/" + UUID.randomUUID() + "/avaliacoes/" + UUID.randomUUID()
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private UUID cadastrarAdvogado(String email, String cpf, String oab) {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido(email, cpf, oab)
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        return UUID.fromString(cadastro.getBody().path("data").path("usuario").path("id").asText());
    }

    private ResponseEntity<JsonNode> cadastrarCliente(String email, String documento) {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido(email, documento)
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        return cadastro;
    }

    private UUID inserirAvaliacao(UUID advogadoId, UUID clienteId, String nota, String comentario) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO avaliacoes_advogado (id, advogado_id, cliente_id, nota, comentario, criado_em)
                VALUES (?::uuid, ?::uuid, ?::uuid, ?::numeric, ?, CURRENT_TIMESTAMP)
                """,
                id,
                advogadoId,
                clienteId,
                nota,
                comentario
        );
        return id;
    }
}
