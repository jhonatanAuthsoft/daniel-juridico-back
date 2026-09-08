package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.dto.solicitacao.CriarSolicitacaoInputDTO;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.e2e.support.Fixtures;
import com.laweact.model.enums.ModalidadeSolicitacaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

/**
 * Inbox do advogado: ordenação por grau de urgência, paginação, filtro por
 * urgência, busca livre e contagem por urgência dos filtros da listagem.
 */
@DisplayName("E2E — GET /conexoes (inbox do advogado)")
class ConexaoListarAdvogadoE2ETest extends BaseE2ETest {

    private static final String ADVOGADO_EMAIL = "inbox.adv@laweact.com";
    private static final String CLIENTE_EMAIL = "inbox.cli@laweact.com";

    private String advogadoId;

    @BeforeEach
    void cadastrarParticipantes() {
        ResponseEntity<JsonNode> advogado = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoParaMatching(
                        "Bruna Inbox",
                        ADVOGADO_EMAIL,
                        "11144477735",
                        "710001",
                        "SP",
                        "São Paulo",
                        List.of("GENERALISTA")
                )
        );
        assertSuccess(advogado, HttpStatus.CREATED);
        advogadoId = advogado.getBody().path("data").path("usuario").path("id").asText();
        api.logout();

        ResponseEntity<JsonNode> cliente = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido(CLIENTE_EMAIL, "52998224725")
        );
        assertSuccess(cliente, HttpStatus.CREATED);
        api.authenticate(cliente.getBody().path("data").path("token").asText());
    }

    @Test
    @DisplayName("ordena emergências no topo e, dentro do grau, as mais recentes na frente")
    void shouldOrderEmergenciesFirst() {
        pedirConexao("Tenho tempo", UrgenciaSolicitacaoEnum.TENHO_TEMPO);
        pedirConexao("Média", UrgenciaSolicitacaoEnum.MEDIO);
        pedirConexao("Emergência antiga", UrgenciaSolicitacaoEnum.EMERGENCIA);
        pedirConexao("Urgente", UrgenciaSolicitacaoEnum.URGENTE);
        pedirConexao("Emergência nova", UrgenciaSolicitacaoEnum.EMERGENCIA);

        JsonNode items = listarComoAdvogado("/conexoes?status=PENDENTE").path("data").path("items");

        assertThat(titulos(items)).containsExactly(
                "Emergência nova",
                "Emergência antiga",
                "Urgente",
                "Média",
                "Tenho tempo"
        );
        assertThat(urgencias(items)).containsExactly(
                "EMERGENCIA",
                "EMERGENCIA",
                "URGENTE",
                "MEDIO",
                "TENHO_TEMPO"
        );
    }

    @Test
    @DisplayName("devolve contagem por urgência para os filtros da listagem")
    void shouldReturnUrgencyCounts() {
        pedirConexao("Emergência 1", UrgenciaSolicitacaoEnum.EMERGENCIA);
        pedirConexao("Emergência 2", UrgenciaSolicitacaoEnum.EMERGENCIA);
        pedirConexao("Urgente 1", UrgenciaSolicitacaoEnum.URGENTE);

        JsonNode contagem = listarComoAdvogado("/conexoes?status=PENDENTE")
                .path("data")
                .path("contagemPorUrgencia");

        assertThat(contagem.path("EMERGENCIA").asInt()).isEqualTo(2);
        assertThat(contagem.path("URGENTE").asInt()).isEqualTo(1);
        assertThat(contagem.path("MEDIO").asInt()).isZero();
        assertThat(contagem.path("TENHO_TEMPO").asInt()).isZero();
    }

    @Test
    @DisplayName("filtra por urgência mantendo a contagem global")
    void shouldFilterByUrgenciaKeepingGlobalCounts() {
        pedirConexao("Emergência 1", UrgenciaSolicitacaoEnum.EMERGENCIA);
        pedirConexao("Urgente 1", UrgenciaSolicitacaoEnum.URGENTE);
        pedirConexao("Média 1", UrgenciaSolicitacaoEnum.MEDIO);

        ResponseEntity<JsonNode> response = autenticarAdvogadoEBuscar(
                "/conexoes?status=PENDENTE&urgencia=EMERGENCIA"
        );
        JsonNode data = response.getBody().path("data");

        assertThat(titulos(data.path("items"))).containsExactly("Emergência 1");
        assertThat(response.getBody().path("pagination").path("totalElements").asInt()).isEqualTo(1);

        JsonNode contagem = data.path("contagemPorUrgencia");
        assertThat(contagem.path("EMERGENCIA").asInt()).isEqualTo(1);
        assertThat(contagem.path("URGENTE").asInt()).isEqualTo(1);
        assertThat(contagem.path("MEDIO").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("pagina de 10 em 10 com offset")
    void shouldPaginateTenByTen() {
        for (int i = 1; i <= 11; i++) {
            pedirConexao("Demanda " + i, UrgenciaSolicitacaoEnum.MEDIO);
        }

        ResponseEntity<JsonNode> pagina1 = autenticarAdvogadoEBuscar(
                "/conexoes?status=PENDENTE&limit=10&offset=0"
        );
        assertThat(pagina1.getBody().path("data").path("items")).hasSize(10);
        assertThat(pagina1.getBody().path("pagination").path("page").asInt()).isEqualTo(1);
        assertThat(pagina1.getBody().path("pagination").path("size").asInt()).isEqualTo(10);
        assertThat(pagina1.getBody().path("pagination").path("totalElements").asInt()).isEqualTo(11);
        assertThat(pagina1.getBody().path("pagination").path("totalPages").asInt()).isEqualTo(2);

        ResponseEntity<JsonNode> pagina2 = api.get("/conexoes?status=PENDENTE&limit=10&offset=10");
        assertSuccess(pagina2, HttpStatus.OK);
        assertThat(pagina2.getBody().path("data").path("items")).hasSize(1);
        assertThat(pagina2.getBody().path("pagination").path("page").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("sem limit devolve a lista inteira, sem paginar")
    void shouldReturnFullListWithoutLimit() {
        for (int i = 1; i <= 11; i++) {
            pedirConexao("Demanda " + i, UrgenciaSolicitacaoEnum.MEDIO);
        }

        ResponseEntity<JsonNode> response = autenticarAdvogadoEBuscar("/conexoes?status=PENDENTE");

        assertThat(response.getBody().path("data").path("items")).hasSize(11);
        assertThat(response.getBody().path("pagination").path("totalPages").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("busca livre por título, descrição e cidade da solicitação")
    void shouldFilterByBusca() {
        pedirConexao("Rescisão trabalhista", UrgenciaSolicitacaoEnum.URGENTE);
        pedirConexao("Contrato de aluguel", UrgenciaSolicitacaoEnum.MEDIO);

        ResponseEntity<JsonNode> porTitulo = autenticarAdvogadoEBuscar(
                "/conexoes?status=PENDENTE&busca=rescis"
        );
        assertThat(titulos(porTitulo.getBody().path("data").path("items")))
                .containsExactly("Rescisão trabalhista");
        assertThat(porTitulo.getBody().path("pagination").path("totalElements").asInt()).isEqualTo(1);

        ResponseEntity<JsonNode> porCidade = api.get("/conexoes?status=PENDENTE&busca=paulo");
        assertSuccess(porCidade, HttpStatus.OK);
        assertThat(porCidade.getBody().path("data").path("items")).hasSize(2);

        ResponseEntity<JsonNode> porNomeDoCliente = api.get("/conexoes?status=PENDENTE&busca=maria");
        assertSuccess(porNomeDoCliente, HttpStatus.OK);
        assertThat(porNomeDoCliente.getBody().path("data").path("items")).hasSize(2);
    }

    @Test
    @DisplayName("busca sem correspondência devolve lista vazia mantendo a contagem")
    void shouldReturnEmptyListForUnmatchedBusca() {
        pedirConexao("Rescisão trabalhista", UrgenciaSolicitacaoEnum.URGENTE);

        ResponseEntity<JsonNode> response = autenticarAdvogadoEBuscar(
                "/conexoes?status=PENDENTE&busca=inexistente"
        );

        assertThat(response.getBody().path("data").path("items")).isEmpty();
        assertThat(response.getBody().path("pagination").path("totalElements").asInt()).isZero();
        assertThat(response.getBody().path("data").path("contagemPorUrgencia").path("URGENTE").asInt())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("histórico aceita vários status e exclui pendentes")
    void shouldFilterHistoryByMultipleStatuses() {
        String aceitaId = criarConexao("Aceita", UrgenciaSolicitacaoEnum.URGENTE);
        String recusadaId = criarConexao("Recusada", UrgenciaSolicitacaoEnum.MEDIO);
        criarConexao("Ainda pendente", UrgenciaSolicitacaoEnum.EMERGENCIA);

        decidirComoAdvogado(aceitaId, "aceitar");
        decidirComoAdvogado(recusadaId, "recusar");

        JsonNode items = listarComoAdvogado("/conexoes?status=ACEITA&status=RECUSADA")
                .path("data")
                .path("items");

        assertThat(titulos(items)).containsExactly("Aceita", "Recusada");
        assertThat(statuses(items)).containsExactly("ACEITA", "RECUSADA");
    }

    @Test
    @DisplayName("devolve contagem global por status para os filtros do histórico")
    void shouldReturnStatusCounts() {
        String aceita1 = criarConexao("Aceita 1", UrgenciaSolicitacaoEnum.URGENTE);
        String aceita2 = criarConexao("Aceita 2", UrgenciaSolicitacaoEnum.MEDIO);
        String recusada = criarConexao("Recusada", UrgenciaSolicitacaoEnum.EMERGENCIA);
        criarConexao("Pendente", UrgenciaSolicitacaoEnum.TENHO_TEMPO);

        decidirComoAdvogado(aceita1, "aceitar");
        decidirComoAdvogado(aceita2, "aceitar");
        decidirComoAdvogado(recusada, "recusar");

        JsonNode contagem = listarComoAdvogado("/conexoes?status=ACEITA&status=RECUSADA")
                .path("data")
                .path("contagemPorStatus");

        assertThat(contagem.path("ACEITA").asInt()).isEqualTo(2);
        assertThat(contagem.path("RECUSADA").asInt()).isEqualTo(1);
        assertThat(contagem.path("PENDENTE").asInt()).isEqualTo(1);
        assertThat(contagem.path("CANCELADA").asInt()).isZero();
    }

    @Test
    @DisplayName("filtra um status do histórico mantendo a contagem global")
    void shouldFilterHistoryBySingleStatusKeepingGlobalCounts() {
        String aceitaId = criarConexao("Aceita", UrgenciaSolicitacaoEnum.URGENTE);
        String recusadaId = criarConexao("Recusada", UrgenciaSolicitacaoEnum.MEDIO);

        decidirComoAdvogado(aceitaId, "aceitar");
        decidirComoAdvogado(recusadaId, "recusar");

        ResponseEntity<JsonNode> response = autenticarAdvogadoEBuscar("/conexoes?status=ACEITA");
        JsonNode data = response.getBody().path("data");

        assertThat(titulos(data.path("items"))).containsExactly("Aceita");
        assertThat(response.getBody().path("pagination").path("totalElements").asInt()).isEqualTo(1);

        JsonNode contagem = data.path("contagemPorStatus");
        assertThat(contagem.path("ACEITA").asInt()).isEqualTo(1);
        assertThat(contagem.path("RECUSADA").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("busca no histórico devolve lista vazia mantendo a contagem por status")
    void shouldKeepStatusCountsWhenHistorySearchIsEmpty() {
        String aceitaId = criarConexao("Rescisão trabalhista", UrgenciaSolicitacaoEnum.URGENTE);
        decidirComoAdvogado(aceitaId, "aceitar");

        ResponseEntity<JsonNode> response = autenticarAdvogadoEBuscar(
                "/conexoes?status=ACEITA&status=RECUSADA&busca=inexistente"
        );

        assertThat(response.getBody().path("data").path("items")).isEmpty();
        assertThat(response.getBody().path("pagination").path("totalElements").asInt()).isZero();
        assertThat(response.getBody().path("data").path("contagemPorStatus").path("ACEITA").asInt())
                .isEqualTo(1);
    }

    private void pedirConexao(String titulo, UrgenciaSolicitacaoEnum urgencia) {
        criarConexao(titulo, urgencia);
    }

    private String criarConexao(String titulo, UrgenciaSolicitacaoEnum urgencia) {
        ResponseEntity<JsonNode> criacao = api.post("/solicitacoes", CriarSolicitacaoInputDTO.builder()
                .titulo(titulo)
                .modalidade(ModalidadeSolicitacaoEnum.CONSULTORIA)
                .especialidadeCodigo("CIVIL")
                .subespecialidadeCodigo("CONTRATOS")
                .uf("SP")
                .cidade("São Paulo")
                .urgencia(urgencia)
                .descricao("Preciso de orientação sobre " + titulo + ".")
                .build());
        assertSuccess(criacao, HttpStatus.CREATED);
        String solicitacaoId = criacao.getBody().path("data").path("id").asText();

        ResponseEntity<JsonNode> conexao = api.post(
                "/conexoes",
                Map.of("solicitacaoId", solicitacaoId, "advogadoId", advogadoId)
        );
        assertSuccess(conexao, HttpStatus.CREATED);
        return conexao.getBody().path("data").path("id").asText();
    }

    private void decidirComoAdvogado(String conexaoId, String acao) {
        autenticarAdvogado();
        ResponseEntity<JsonNode> decisao = api.post("/conexoes/" + conexaoId + "/" + acao, Map.of());
        assertSuccess(decisao, HttpStatus.OK);
        autenticarCliente();
    }

    private void autenticarCliente() {
        api.logout();
        ResponseEntity<JsonNode> login = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder()
                        .email(CLIENTE_EMAIL)
                        .senha(Fixtures.VALID_PASSWORD)
                        .build()
        );
        assertSuccess(login, HttpStatus.OK);
        api.authenticate(login.getBody().path("data").path("token").asText());
    }

    private void autenticarAdvogado() {
        api.logout();
        ResponseEntity<JsonNode> login = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder()
                        .email(ADVOGADO_EMAIL)
                        .senha(Fixtures.VALID_PASSWORD)
                        .build()
        );
        assertSuccess(login, HttpStatus.OK);
        api.authenticate(login.getBody().path("data").path("token").asText());
        garantirAssinaturaSeAdvogado();
    }

    private JsonNode listarComoAdvogado(String path) {
        return autenticarAdvogadoEBuscar(path).getBody();
    }

    private ResponseEntity<JsonNode> autenticarAdvogadoEBuscar(String path) {
        autenticarAdvogado();
        ResponseEntity<JsonNode> response = api.get(path);
        assertSuccess(response, HttpStatus.OK);
        return response;
    }

    private List<String> titulos(JsonNode items) {
        List<String> valores = new ArrayList<>();
        items.forEach(item -> valores.add(item.path("tituloSolicitacao").asText()));
        return valores;
    }

    private List<String> urgencias(JsonNode items) {
        List<String> valores = new ArrayList<>();
        items.forEach(item -> valores.add(item.path("urgencia").asText()));
        return valores;
    }

    private List<String> statuses(JsonNode items) {
        List<String> valores = new ArrayList<>();
        items.forEach(item -> valores.add(item.path("status").asText()));
        return valores;
    }
}
