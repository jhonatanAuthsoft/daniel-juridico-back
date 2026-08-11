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
import com.laweact.dto.solicitacao.CriarSolicitacaoInputDTO;
import com.laweact.e2e.support.Fixtures;
import com.laweact.model.enums.ModalidadeSolicitacaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

@DisplayName("E2E — POST /solicitacoes/{id}/cancelar")
class SolicitacaoCancelarE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("cliente cancela solicitação AGUARDANDO_MATCHING")
    void shouldCancelAguardandoMatching() {
        cadastrarEAutenticarCliente("cancel.ok@laweact.com", "39053344705");
        String id = criarSolicitacao();

        ResponseEntity<JsonNode> response = api.post("/solicitacoes/" + id + "/cancelar", null);

        assertSuccess(response, HttpStatus.OK);
        assertThat(response.getBody().path("data").path("id").asText()).isEqualTo(id);
        assertThat(response.getBody().path("data").path("status").asText()).isEqualTo("CANCELADA");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM solicitacoes WHERE id = ? AND status = 'CANCELADA'",
                Integer.class,
                UUID.fromString(id)
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("não cancela solicitação já CANCELADA")
    void shouldRejectAlreadyCancelled() {
        cadastrarEAutenticarCliente("cancel.dup@laweact.com", "15350946056");
        String id = criarSolicitacao();
        assertSuccess(api.post("/solicitacoes/" + id + "/cancelar", null), HttpStatus.OK);

        ResponseEntity<JsonNode> second = api.post("/solicitacoes/" + id + "/cancelar", null);
        assertErrorCode(second, HttpStatus.CONFLICT, "INVALID_STATUS");
    }

    @Test
    @DisplayName("não cancela solicitação MATCH_REALIZADO")
    void shouldRejectMatchRealizado() {
        cadastrarEAutenticarCliente("cancel.match@laweact.com", "71428793860");
        String id = criarSolicitacao();
        jdbcTemplate.update(
                "UPDATE solicitacoes SET status = 'MATCH_REALIZADO' WHERE id = ?::uuid",
                id
        );

        ResponseEntity<JsonNode> response = api.post("/solicitacoes/" + id + "/cancelar", null);
        assertErrorCode(response, HttpStatus.CONFLICT, "INVALID_STATUS");
    }

    @Test
    @DisplayName("não cancela solicitação de outro cliente")
    void shouldForbidOtherCliente() {
        cadastrarEAutenticarCliente("cancel.owner@laweact.com", "39053344705");
        String id = criarSolicitacao();

        cadastrarEAutenticarCliente("cancel.other@laweact.com", "15350946056");
        ResponseEntity<JsonNode> response = api.post("/solicitacoes/" + id + "/cancelar", null);
        assertErrorCode(response, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    @Test
    @DisplayName("sem autenticação retorna 401")
    void shouldRequireAuth() {
        ResponseEntity<JsonNode> response = api.post(
                "/solicitacoes/" + UUID.randomUUID() + "/cancelar",
                null
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("advogado não pode cancelar")
    void shouldForbidAdvogado() {
        cadastrarEAutenticarCliente("cancel.cli@laweact.com", "39053344705");
        String id = criarSolicitacao();

        ResponseEntity<JsonNode> cadastro = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido("cancel.adv@laweact.com", "15350946056", "112233")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> response = api.post("/solicitacoes/" + id + "/cancelar", null);
        assertErrorCode(response, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    private void cadastrarEAutenticarCliente(String email, String documento) {
        ResponseEntity<JsonNode> cadastro = api.post("/clientes/cadastrar", Fixtures.clienteValido(email, documento));
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());
    }

    private String criarSolicitacao() {
        ResponseEntity<JsonNode> response = api.post("/solicitacoes", CriarSolicitacaoInputDTO.builder()
                .titulo("Caso para cancelar")
                .modalidade(ModalidadeSolicitacaoEnum.CONSULTORIA)
                .especialidadeCodigo("CIVIL")
                .uf("SP")
                .cidade("São Paulo")
                .urgencia(UrgenciaSolicitacaoEnum.MEDIO)
                .descricao("Preciso de orientação sobre o caso descrito no título.")
                .build());
        assertSuccess(response, HttpStatus.CREATED);
        return response.getBody().path("data").path("id").asText();
    }
}
