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

@DisplayName("E2E — job de notificações insistentes")
class NotificacaoInsistenteJobE2ETest extends BaseE2ETest {

    private static final String JOBS_API_KEY = "test-jobs-key";

    private CriarSolicitacaoInputDTO demanda(UrgenciaSolicitacaoEnum urgencia) {
        return CriarSolicitacaoInputDTO.builder()
                .titulo("Revisão urgente")
                .modalidade(ModalidadeSolicitacaoEnum.CONSULTORIA)
                .especialidadeCodigo("CIVIL")
                .subespecialidadeCodigo("CONTRATOS")
                .uf("SP")
                .cidade("São Paulo")
                .urgencia(urgencia)
                .descricao("Preciso de atendimento com prioridade.")
                .formaCobranca(FormaCobrancaSolicitacaoEnum.VALOR_FIXO)
                .experienciaMinimaMeses(24)
                .build();
    }

    private void cadastrarAdvogado(String nome, String email, String cpf, String oab) {
        ResponseEntity<JsonNode> response = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoParaMatching(nome, email, cpf, oab, "SP", "São Paulo", List.of("GENERALISTA"))
        );
        assertSuccess(response, HttpStatus.CREATED);
        api.logout();
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
    }

    @Test
    @DisplayName("sem X-Api-Key → 401")
    void shouldRejectWithoutApiKey() {
        api.logout();
        ResponseEntity<JsonNode> response = api.post("/jobs/notificacoes-insistentes", Map.of());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("X-Api-Key inválida → 401")
    void shouldRejectInvalidApiKey() {
        api.logout();
        ResponseEntity<JsonNode> response = api.postWithHeader(
                "/jobs/notificacoes-insistentes",
                Map.of(),
                "X-Api-Key",
                "wrong-key"
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("EMERGENCIA pendente + interval vencido → reusa linha, zera lida_em")
    void shouldReinsistUnreadOnSameNotification() {
        cadastrarAdvogado("Bruna Insist", "bruna.insist@laweact.com", "11144477735", "620001");
        autenticarCliente("cliente.insist@laweact.com", "52998224725");

        ResponseEntity<JsonNode> criacao = api.post(
                "/solicitacoes",
                demanda(UrgenciaSolicitacaoEnum.EMERGENCIA)
        );
        assertSuccess(criacao, HttpStatus.CREATED);
        UUID solicitacaoId = UUID.fromString(criacao.getBody().path("data").path("id").asText());
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

        autenticarComo("bruna.insist@laweact.com");
        ResponseEntity<JsonNode> listaAntes = api.get("/notificacoes");
        assertSuccess(listaAntes, HttpStatus.OK);
        assertThat(listaAntes.getBody().path("data").size()).isEqualTo(1);
        String notifId = listaAntes.getBody().path("data").get(0).path("id").asText();

        ResponseEntity<JsonNode> lida = api.post("/notificacoes/" + notifId + "/ler", Map.of());
        assertSuccess(lida, HttpStatus.OK);
        assertThat(lida.getBody().path("data").path("lidaEm").asText()).isNotBlank();

        jdbcTemplate.update(
                "UPDATE conexoes SET criado_em = NOW() - INTERVAL '25 hours' WHERE id = ?::uuid",
                conexaoId
        );

        api.logout();
        ResponseEntity<JsonNode> job = api.postWithHeader(
                "/jobs/notificacoes-insistentes",
                Map.of(),
                "X-Api-Key",
                JOBS_API_KEY
        );
        assertSuccess(job, HttpStatus.OK);
        assertThat(job.getBody().path("data").path("reenviadas").asInt()).isGreaterThanOrEqualTo(1);

        autenticarComo("bruna.insist@laweact.com");
        ResponseEntity<JsonNode> listaDepois = api.get("/notificacoes");
        assertSuccess(listaDepois, HttpStatus.OK);
        assertThat(listaDepois.getBody().path("data").size()).isEqualTo(1);
        JsonNode notif = listaDepois.getBody().path("data").get(0);
        assertThat(notif.path("id").asText()).isEqualTo(notifId);
        assertThat(notif.path("tipo").asText()).isEqualTo("CONEXAO_SOLICITADA");
        assertThat(notif.path("titulo").asText()).startsWith("Lembrete:");
        assertThat(notif.path("lidaEm").isNull() || notif.path("lidaEm").isMissingNode()).isTrue();

        Integer lembreteCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM conexoes WHERE id = ?::uuid AND ultimo_lembrete_insistente_em IS NOT NULL",
                Integer.class,
                conexaoId
        );
        assertThat(lembreteCount).isEqualTo(1);
    }

    @Test
    @DisplayName("EMERGENCIA pendente com menos de 24h → não reinsiste")
    void shouldNotReinsistBeforeDailyInterval() {
        cadastrarAdvogado("Bruna Daily", "bruna.daily.insist@laweact.com", "11144477735", "620010");
        autenticarCliente("cliente.daily.insist@laweact.com", "52998224725");

        ResponseEntity<JsonNode> criacao = api.post(
                "/solicitacoes",
                demanda(UrgenciaSolicitacaoEnum.EMERGENCIA)
        );
        assertSuccess(criacao, HttpStatus.CREATED);
        UUID solicitacaoId = UUID.fromString(criacao.getBody().path("data").path("id").asText());
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

        jdbcTemplate.update(
                "UPDATE conexoes SET criado_em = NOW() - INTERVAL '2 hours' WHERE id = ?::uuid",
                conexaoId
        );

        api.logout();
        ResponseEntity<JsonNode> job = api.postWithHeader(
                "/jobs/notificacoes-insistentes",
                Map.of(),
                "X-Api-Key",
                JOBS_API_KEY
        );
        assertSuccess(job, HttpStatus.OK);
        assertThat(job.getBody().path("data").path("reenviadas").asInt()).isEqualTo(0);
    }

    @Test
    @DisplayName("após aceitar → job não reinsiste a conexão")
    void shouldNotReinsistAfterAccept() {
        cadastrarAdvogado("Bruna Done", "bruna.done.insist@laweact.com", "11144477735", "620002");
        autenticarCliente("cliente.done.insist@laweact.com", "52998224725");

        ResponseEntity<JsonNode> criacao = api.post(
                "/solicitacoes",
                demanda(UrgenciaSolicitacaoEnum.URGENTE)
        );
        UUID solicitacaoId = UUID.fromString(criacao.getBody().path("data").path("id").asText());
        UUID advogadoId = UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT advogado_id::text FROM solicitacao_matches WHERE solicitacao_id = ?::uuid LIMIT 1",
                String.class,
                solicitacaoId
        ));

        ResponseEntity<JsonNode> criada = api.post(
                "/conexoes",
                Map.of("solicitacaoId", solicitacaoId.toString(), "advogadoId", advogadoId.toString())
        );
        String conexaoId = criada.getBody().path("data").path("id").asText();

        autenticarComo("bruna.done.insist@laweact.com");
        assertSuccess(api.post("/conexoes/" + conexaoId + "/aceitar", Map.of()), HttpStatus.OK);

        jdbcTemplate.update(
                "UPDATE conexoes SET criado_em = NOW() - INTERVAL '25 hours' WHERE id = ?::uuid",
                conexaoId
        );

        api.logout();
        ResponseEntity<JsonNode> job = api.postWithHeader(
                "/jobs/notificacoes-insistentes",
                Map.of(),
                "X-Api-Key",
                JOBS_API_KEY
        );
        assertSuccess(job, HttpStatus.OK);
        assertThat(job.getBody().path("data").path("reenviadas").asInt()).isEqualTo(0);
    }
}
