package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
 * Marca de "solicitação aberta pelo advogado" (`visualizadaEm`), que alimenta a
 * faixa lateral do card na home do advogado.
 */
@DisplayName("E2E — POST /conexoes/{id}/visualizar")
class ConexaoVisualizarE2ETest extends BaseE2ETest {

    private static final String ADVOGADO_EMAIL = "adv.visualizar@laweact.com";
    private static final String OUTRO_ADVOGADO_EMAIL = "outro.adv.visualizar@laweact.com";
    private static final String CLIENTE_EMAIL = "cliente.visualizar@laweact.com";

    private UUID advogadoId;

    @BeforeEach
    void cadastrarParticipantes() {
        advogadoId = cadastrarAdvogado("Bruna Visual", ADVOGADO_EMAIL, "11144477735", "730001");
        cadastrarAdvogado("Diego Visual", OUTRO_ADVOGADO_EMAIL, "39053344705", "730002");

        ResponseEntity<JsonNode> cliente = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido(CLIENTE_EMAIL, "52998224725")
        );
        assertSuccess(cliente, HttpStatus.CREATED);
        api.authenticate(cliente.getBody().path("data").path("token").asText());
    }

    @Test
    @DisplayName("conexão nova vem com visualizadaEm nulo")
    void shouldStartUnviewed() {
        pedirConexao();

        JsonNode item = primeiroItemDoAdvogado();

        assertThat(item.path("visualizadaEm").isNull()).isTrue();
    }

    @Test
    @DisplayName("advogado dono marca como visualizada e a listagem passa a devolver a data")
    void shouldMarkAsViewed() {
        String conexaoId = pedirConexao();

        autenticarComo(ADVOGADO_EMAIL);
        ResponseEntity<JsonNode> visualizar = api.post("/conexoes/" + conexaoId + "/visualizar", Map.of());
        assertSuccess(visualizar, HttpStatus.OK);
        assertThat(visualizar.getBody().path("data").path("visualizadaEm").asText()).isNotBlank();

        assertThat(primeiroItemDoAdvogado().path("visualizadaEm").asText()).isNotBlank();
    }

    @Test
    @DisplayName("chamar duas vezes preserva a data da primeira abertura")
    void shouldKeepFirstViewedAt() {
        String conexaoId = pedirConexao();

        autenticarComo(ADVOGADO_EMAIL);
        ResponseEntity<JsonNode> primeira = api.post("/conexoes/" + conexaoId + "/visualizar", Map.of());
        assertSuccess(primeira, HttpStatus.OK);
        String visualizadaEm = primeira.getBody().path("data").path("visualizadaEm").asText();

        ResponseEntity<JsonNode> segunda = api.post("/conexoes/" + conexaoId + "/visualizar", Map.of());
        assertSuccess(segunda, HttpStatus.OK);

        assertThat(segunda.getBody().path("data").path("visualizadaEm").asText()).isEqualTo(visualizadaEm);
    }

    @Test
    @DisplayName("advogado de outra conexão → 403")
    void shouldRejectOtherLawyer() {
        String conexaoId = pedirConexao();

        autenticarComo(OUTRO_ADVOGADO_EMAIL);
        ResponseEntity<JsonNode> response = api.post("/conexoes/" + conexaoId + "/visualizar", Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("cliente não pode marcar como visualizada → 403")
    void shouldRejectCliente() {
        String conexaoId = pedirConexao();

        ResponseEntity<JsonNode> response = api.post("/conexoes/" + conexaoId + "/visualizar", Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private UUID cadastrarAdvogado(String nome, String email, String cpf, String oab) {
        ResponseEntity<JsonNode> response = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoParaMatching(nome, email, cpf, oab, "SP", "São Paulo", List.of("GENERALISTA"))
        );
        assertSuccess(response, HttpStatus.CREATED);
        UUID id = UUID.fromString(response.getBody().path("data").path("usuario").path("id").asText());
        api.logout();
        return id;
    }

    /** Cliente autenticado cria solicitação e pede conexão com {@link #advogadoId}. */
    private String pedirConexao() {
        ResponseEntity<JsonNode> criacao = api.post("/solicitacoes", CriarSolicitacaoInputDTO.builder()
                .titulo("Rescisão de contrato")
                .modalidade(ModalidadeSolicitacaoEnum.CONSULTORIA)
                .especialidadeCodigo("CIVIL")
                .subespecialidadeCodigo("CONTRATOS")
                .uf("SP")
                .cidade("São Paulo")
                .urgencia(UrgenciaSolicitacaoEnum.URGENTE)
                .descricao("Preciso de orientação sobre rescisão de contrato.")
                .build());
        assertSuccess(criacao, HttpStatus.CREATED);
        String solicitacaoId = criacao.getBody().path("data").path("id").asText();

        ResponseEntity<JsonNode> conexao = api.post(
                "/conexoes",
                Map.of("solicitacaoId", solicitacaoId, "advogadoId", advogadoId.toString())
        );
        assertSuccess(conexao, HttpStatus.CREATED);
        return conexao.getBody().path("data").path("id").asText();
    }

    private JsonNode primeiroItemDoAdvogado() {
        autenticarComo(ADVOGADO_EMAIL);
        ResponseEntity<JsonNode> response = api.get("/conexoes?status=PENDENTE");
        assertSuccess(response, HttpStatus.OK);
        return response.getBody().path("data").path("items").get(0);
    }

    private void autenticarComo(String email) {
        api.logout();
        ResponseEntity<JsonNode> login = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder()
                        .email(email)
                        .senha(Fixtures.VALID_PASSWORD)
                        .build()
        );
        assertSuccess(login, HttpStatus.OK);
        api.authenticate(login.getBody().path("data").path("token").asText());
    }
}
