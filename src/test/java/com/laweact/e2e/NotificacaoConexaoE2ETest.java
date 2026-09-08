package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.dto.solicitacao.CriarSolicitacaoInputDTO;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.e2e.support.Fixtures;
import com.laweact.model.enums.FormaCobrancaSolicitacaoEnum;
import com.laweact.model.enums.ModalidadeSolicitacaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

@DisplayName("E2E — notificações de conexão")
class NotificacaoConexaoE2ETest extends BaseE2ETest {

    private CriarSolicitacaoInputDTO demandaConsultoriaSaoPaulo() {
        return CriarSolicitacaoInputDTO.builder()
                .titulo("Revisão de contrato")
                .modalidade(ModalidadeSolicitacaoEnum.CONSULTORIA)
                .especialidadeCodigo("CIVIL")
                .subespecialidadeCodigo("CONTRATOS")
                .uf("SP")
                .cidade("São Paulo")
                .urgencia(UrgenciaSolicitacaoEnum.EMERGENCIA)
                .descricao("Preciso revisar um contrato de prestação de serviços.")
                .formaCobranca(FormaCobrancaSolicitacaoEnum.VALOR_FIXO)
                .experienciaMinimaMeses(24)
                .build();
    }

    private UUID cadastrarAdvogadoMatching(String nome, String email, String cpf, String oab) {
        ResponseEntity<JsonNode> response = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoParaMatching(nome, email, cpf, oab, "SP", "São Paulo", List.of("GENERALISTA"))
        );
        assertSuccess(response, HttpStatus.CREATED);
        UUID id = UUID.fromString(response.getBody().path("data").path("usuario").path("id").asText());
        api.logout();
        return id;
    }

    private void autenticarCliente(String email, String documento) {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido(email, documento)
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());
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
        garantirAssinaturaSeAdvogado();
    }

    private UUID criarSolicitacaoComMatch(String emailAdvogado, String emailCliente) {
        cadastrarAdvogadoMatching("Bruna Notif", emailAdvogado, "11144477735", "610001");
        autenticarCliente(emailCliente, "52998224725");
        ResponseEntity<JsonNode> criacao = api.post("/solicitacoes", demandaConsultoriaSaoPaulo());
        assertSuccess(criacao, HttpStatus.CREATED);
        return UUID.fromString(criacao.getBody().path("data").path("id").asText());
    }

    @Test
    @DisplayName("criar conexão → advogado vê CONEXAO_SOLICITADA; aceitar → cliente vê CONEXAO_ACEITA")
    void shouldNotifyOnCreateAndAccept() {
        UUID solicitacaoId = criarSolicitacaoComMatch(
                "bruna.notif@laweact.com",
                "cliente.notif@laweact.com"
        );
        UUID advogadoId = UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT advogado_id::text FROM solicitacao_matches WHERE solicitacao_id = ?::uuid LIMIT 1",
                String.class,
                solicitacaoId
        ));

        ResponseEntity<JsonNode> criada = api.post(
                "/conexoes",
                Map.of("solicitacaoId", solicitacaoId.toString(), "advogadoId", advogadoId.toString())
        );
        assertSuccess(criada, HttpStatus.CREATED);
        String conexaoId = criada.getBody().path("data").path("id").asText();

        autenticarComo("bruna.notif@laweact.com");

        ResponseEntity<JsonNode> badge = api.get("/notificacoes/nao-lidas/existe");
        assertSuccess(badge, HttpStatus.OK);
        assertThat(badge.getBody().path("data").path("existe").asBoolean()).isTrue();

        ResponseEntity<JsonNode> lista = api.get("/notificacoes?limit=20&offset=0");
        assertSuccess(lista, HttpStatus.OK);
        assertThat(lista.getBody().path("data").isArray()).isTrue();
        assertThat(lista.getBody().path("data").size()).isEqualTo(1);
        JsonNode notif = lista.getBody().path("data").get(0);
        assertThat(notif.path("tipo").asText()).isEqualTo("CONEXAO_SOLICITADA");
        assertThat(notif.path("referenciaId").asText()).isEqualTo(conexaoId);
        assertThat(notif.path("statusEnvio").asText()).isEqualTo("SKIPPED");
        assertThat(notif.path("lidaEm").isNull() || notif.path("lidaEm").isMissingNode()).isTrue();

        String notifId = notif.path("id").asText();
        ResponseEntity<JsonNode> lida = api.post("/notificacoes/" + notifId + "/ler", Map.of());
        assertSuccess(lida, HttpStatus.OK);
        assertThat(lida.getBody().path("data").path("lidaEm").asText()).isNotBlank();

        ResponseEntity<JsonNode> aceita = api.post("/conexoes/" + conexaoId + "/aceitar", Map.of());
        assertSuccess(aceita, HttpStatus.OK);

        autenticarComo("cliente.notif@laweact.com");
        ResponseEntity<JsonNode> listaCliente = api.get("/notificacoes");
        assertSuccess(listaCliente, HttpStatus.OK);
        assertThat(listaCliente.getBody().path("data").size()).isEqualTo(1);
        assertThat(listaCliente.getBody().path("data").get(0).path("tipo").asText())
                .isEqualTo("CONEXAO_ACEITA");
        assertThat(listaCliente.getBody().path("data").get(0).path("referenciaId").asText())
                .isEqualTo(conexaoId);
    }

    @Test
    @DisplayName("preferência false → statusEnvio SKIPPED e conexão ok")
    void shouldSkipPushWhenPreferenceDisabled() {
        cadastrarAdvogadoMatching("Bruna Pref", "bruna.pref.notif@laweact.com", "11144477735", "610002");
        autenticarCliente("cliente.pref.notif@laweact.com", "52998224725");

        ResponseEntity<JsonNode> criacao = api.post("/solicitacoes", demandaConsultoriaSaoPaulo());
        UUID solicitacaoId = UUID.fromString(criacao.getBody().path("data").path("id").asText());
        UUID advogadoId = UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT advogado_id::text FROM solicitacao_matches WHERE solicitacao_id = ?::uuid LIMIT 1",
                String.class,
                solicitacaoId
        ));

        autenticarComo("bruna.pref.notif@laweact.com");
        ResponseEntity<JsonNode> pref = api.patch(
                "/usuarios/me/preferencias",
                Map.of("notificacoesPushHabilitadas", false)
        );
        assertSuccess(pref, HttpStatus.OK);

        autenticarComo("cliente.pref.notif@laweact.com");
        ResponseEntity<JsonNode> criada = api.post(
                "/conexoes",
                Map.of("solicitacaoId", solicitacaoId.toString(), "advogadoId", advogadoId.toString())
        );
        assertSuccess(criada, HttpStatus.CREATED);
        assertThat(criada.getBody().path("data").path("status").asText()).isEqualTo("PENDENTE");

        autenticarComo("bruna.pref.notif@laweact.com");
        ResponseEntity<JsonNode> lista = api.get("/notificacoes");
        assertSuccess(lista, HttpStatus.OK);
        assertThat(lista.getBody().path("data").size()).isEqualTo(1);
        assertThat(lista.getBody().path("data").get(0).path("tipo").asText()).isEqualTo("CONEXAO_SOLICITADA");
        assertThat(lista.getBody().path("data").get(0).path("statusEnvio").asText()).isEqualTo("SKIPPED");
    }

    @Test
    @DisplayName("abrir solicitação marca as notificações daquela solicitação como lidas")
    void shouldMarkNotificationsReadWhenOpeningSolicitation() {
        UUID solicitacaoId = criarSolicitacaoComMatch(
                "bruna.ler.sol@laweact.com",
                "cliente.ler.sol@laweact.com"
        );
        UUID advogadoId = UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT advogado_id::text FROM solicitacao_matches WHERE solicitacao_id = ?::uuid LIMIT 1",
                String.class,
                solicitacaoId
        ));

        ResponseEntity<JsonNode> criada = api.post(
                "/conexoes",
                Map.of("solicitacaoId", solicitacaoId.toString(), "advogadoId", advogadoId.toString())
        );
        assertSuccess(criada, HttpStatus.CREATED);
        String conexaoId = criada.getBody().path("data").path("id").asText();

        autenticarComo("bruna.ler.sol@laweact.com");
        ResponseEntity<JsonNode> lerAdvogado = api.post(
                "/notificacoes/ler-por-solicitacao/" + solicitacaoId,
                Map.of()
        );
        assertSuccess(lerAdvogado, HttpStatus.OK);

        ResponseEntity<JsonNode> listaAdvogado = api.get("/notificacoes");
        assertSuccess(listaAdvogado, HttpStatus.OK);
        JsonNode notifAdvogado = listaAdvogado.getBody().path("data").get(0);
        assertThat(notifAdvogado.path("tipo").asText()).isEqualTo("CONEXAO_SOLICITADA");
        assertThat(notifAdvogado.path("referenciaId").asText()).isEqualTo(conexaoId);
        assertThat(notifAdvogado.path("lidaEm").asText()).isNotBlank();

        ResponseEntity<JsonNode> badgeAdvogado = api.get("/notificacoes/nao-lidas/existe");
        assertSuccess(badgeAdvogado, HttpStatus.OK);
        assertThat(badgeAdvogado.getBody().path("data").path("existe").asBoolean()).isFalse();

        ResponseEntity<JsonNode> aceita = api.post("/conexoes/" + conexaoId + "/aceitar", Map.of());
        assertSuccess(aceita, HttpStatus.OK);

        autenticarComo("cliente.ler.sol@laweact.com");
        ResponseEntity<JsonNode> listaClienteAntes = api.get("/notificacoes");
        assertSuccess(listaClienteAntes, HttpStatus.OK);
        JsonNode notifClienteAntes = listaClienteAntes.getBody().path("data").get(0);
        assertThat(notifClienteAntes.path("lidaEm").isNull() || notifClienteAntes.path("lidaEm").isMissingNode())
                .isTrue();

        ResponseEntity<JsonNode> lerCliente = api.post(
                "/notificacoes/ler-por-solicitacao/" + solicitacaoId,
                Map.of()
        );
        assertSuccess(lerCliente, HttpStatus.OK);

        ResponseEntity<JsonNode> listaCliente = api.get("/notificacoes");
        assertSuccess(listaCliente, HttpStatus.OK);
        JsonNode notifCliente = listaCliente.getBody().path("data").get(0);
        assertThat(notifCliente.path("tipo").asText()).isEqualTo("CONEXAO_ACEITA");
        assertThat(notifCliente.path("referenciaId").asText()).isEqualTo(conexaoId);
        assertThat(notifCliente.path("lidaEm").asText()).isNotBlank();
    }
}
