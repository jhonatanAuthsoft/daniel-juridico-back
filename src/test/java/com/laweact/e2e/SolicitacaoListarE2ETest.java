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

@DisplayName("E2E — GET /solicitacoes")
class SolicitacaoListarE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("cliente lista as próprias solicitações com campos da listagem e contagem por status")
    void shouldListOwnSolicitacoes() {
        cadastrarEAutenticarCliente("lista.ok@laweact.com", "39053344705");
        criarSolicitacao("Rescisão trabalhista", UrgenciaSolicitacaoEnum.URGENTE, "CIVIL");

        ResponseEntity<JsonNode> response = api.get("/solicitacoes");

        assertSuccess(response, HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        JsonNode items = data.path("items");
        assertThat(items.isArray()).isTrue();
        assertThat(items).hasSize(1);

        JsonNode item = items.get(0);
        assertThat(item.path("id").asText()).isNotBlank();
        assertThat(item.path("status").asText()).isEqualTo("ABERTA");
        assertThat(item.path("urgencia").asText()).isEqualTo("URGENTE");
        assertThat(item.path("titulo").asText()).isEqualTo("Rescisão trabalhista");
        assertThat(item.path("descricao").asText()).contains("orientação");
        assertThat(item.path("dataAbertura").asText()).isNotBlank();
        assertThat(item.path("especialidadeCodigo").asText()).isEqualTo("CIVIL");
        assertThat(item.path("especialidade").asText()).isNotBlank();
        assertThat(item.path("totalMatches").isNumber()).isTrue();
        assertThat(item.path("totalMatches").asInt()).isGreaterThanOrEqualTo(0);

        JsonNode contagem = data.path("contagemPorStatus");
        assertThat(contagem.path("ABERTA").asInt()).isEqualTo(1);
        assertThat(contagem.path("AGUARDANDO_MATCHING").asInt()).isEqualTo(0);
        assertThat(contagem.path("MATCH_REALIZADO").asInt()).isEqualTo(0);
        assertThat(contagem.path("CANCELADA").asInt()).isEqualTo(0);
        assertThat(contagem.path("ENCERRADA").asInt()).isEqualTo(0);

        JsonNode pagination = response.getBody().path("pagination");
        assertThat(pagination.path("size").asInt()).isEqualTo(10);
        assertThat(pagination.path("page").asInt()).isEqualTo(1);
        assertThat(pagination.path("totalElements").asInt()).isEqualTo(1);
        assertThat(pagination.path("totalPages").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("filtra por status e mantém contagem global por status")
    void shouldFilterByStatusAndKeepGlobalCounts() {
        cadastrarEAutenticarCliente("lista.status@laweact.com", "15350946056");
        String idAberta = criarSolicitacao("Aberta 1", UrgenciaSolicitacaoEnum.MEDIO, "CIVIL");
        criarSolicitacao("Aberta 2", UrgenciaSolicitacaoEnum.URGENTE, "CIVIL");

        ResponseEntity<JsonNode> cancel = api.post("/solicitacoes/" + idAberta + "/cancelar", null);
        assertSuccess(cancel, HttpStatus.OK);

        ResponseEntity<JsonNode> filtrada = api.get("/solicitacoes?status=ABERTA");
        assertSuccess(filtrada, HttpStatus.OK);
        JsonNode data = filtrada.getBody().path("data");
        assertThat(data.path("items")).hasSize(1);
        assertThat(data.path("items").get(0).path("titulo").asText()).isEqualTo("Aberta 2");
        assertThat(data.path("items").get(0).path("status").asText()).isEqualTo("ABERTA");
        assertThat(filtrada.getBody().path("pagination").path("totalElements").asInt()).isEqualTo(1);

        JsonNode contagem = data.path("contagemPorStatus");
        assertThat(contagem.path("ABERTA").asInt()).isEqualTo(1);
        assertThat(contagem.path("CANCELADA").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("paginação de 10 em 10 com offset")
    void shouldPaginateTenByTen() {
        cadastrarEAutenticarCliente("lista.page@laweact.com", "39053344705");
        for (int i = 1; i <= 11; i++) {
            criarSolicitacao("Solicitação " + i, UrgenciaSolicitacaoEnum.MEDIO, "CIVIL");
        }

        ResponseEntity<JsonNode> page1 = api.get("/solicitacoes?limit=10&offset=0");
        assertSuccess(page1, HttpStatus.OK);
        assertThat(page1.getBody().path("data").path("items")).hasSize(10);
        assertThat(page1.getBody().path("pagination").path("totalElements").asInt()).isEqualTo(11);
        assertThat(page1.getBody().path("pagination").path("totalPages").asInt()).isEqualTo(2);
        assertThat(page1.getBody().path("pagination").path("page").asInt()).isEqualTo(1);
        assertThat(page1.getBody().path("pagination").path("size").asInt()).isEqualTo(10);

        ResponseEntity<JsonNode> page2 = api.get("/solicitacoes?limit=10&offset=10");
        assertSuccess(page2, HttpStatus.OK);
        assertThat(page2.getBody().path("data").path("items")).hasSize(1);
        assertThat(page2.getBody().path("pagination").path("page").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("lista só as solicitações do cliente autenticado")
    void shouldNotListOtherClienteSolicitacoes() {
        cadastrarEAutenticarCliente("lista.a@laweact.com", "39053344705");
        criarSolicitacao("Do cliente A", UrgenciaSolicitacaoEnum.MEDIO, "CIVIL");

        cadastrarEAutenticarCliente("lista.b@laweact.com", "15350946056");
        criarSolicitacao("Do cliente B", UrgenciaSolicitacaoEnum.EMERGENCIA, "TRABALHISTA");

        ResponseEntity<JsonNode> response = api.get("/solicitacoes");
        assertSuccess(response, HttpStatus.OK);
        JsonNode items = response.getBody().path("data").path("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).path("titulo").asText()).isEqualTo("Do cliente B");
        assertThat(items.get(0).path("urgencia").asText()).isEqualTo("EMERGENCIA");
    }

    @Test
    @DisplayName("sem autenticação retorna 401")
    void shouldRequireAuth() {
        ResponseEntity<JsonNode> response = api.get("/solicitacoes");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("advogado autenticado não pode listar solicitações")
    void shouldForbidAdvogado() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido("lista.adv@laweact.com", "39053344705", "998877")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> response = api.get("/solicitacoes");
        assertErrorCode(response, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    private void cadastrarEAutenticarCliente(String email, String documento) {
        ResponseEntity<JsonNode> cadastro = api.post("/clientes/cadastrar", Fixtures.clienteValido(email, documento));
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());
    }

    private String criarSolicitacao(String titulo, UrgenciaSolicitacaoEnum urgencia, String especialidadeCodigo) {
        ResponseEntity<JsonNode> response = api.post("/solicitacoes", CriarSolicitacaoInputDTO.builder()
                .titulo(titulo)
                .modalidade(ModalidadeSolicitacaoEnum.CONSULTORIA)
                .especialidadeCodigo(especialidadeCodigo)
                .uf("SP")
                .cidade("São Paulo")
                .urgencia(urgencia)
                .descricao("Preciso de orientação sobre o caso descrito no título.")
                .build());
        assertSuccess(response, HttpStatus.CREATED);
        return response.getBody().path("data").path("id").asText();
    }
}
