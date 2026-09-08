package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorCode;
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

@DisplayName("E2E — conexões cliente ↔ advogado")
class ConexaoE2ETest extends BaseE2ETest {

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

    private UUID cadastrarAdvogadoMatching(
            String nome,
            String email,
            String cpf,
            String oab
    ) {
        ResponseEntity<JsonNode> response = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoParaMatching(nome, email, cpf, oab, "SP", "São Paulo", List.of("GENERALISTA"))
        );
        assertSuccess(response, HttpStatus.CREATED);
        UUID id = UUID.fromString(response.getBody().path("data").path("usuario").path("id").asText());
        api.logout();
        return id;
    }

    private String autenticarCliente(String email, String documento) {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido(email, documento)
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        String token = cadastro.getBody().path("data").path("token").asText();
        api.authenticate(token);
        return token;
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

    private UUID criarSolicitacaoComMatch() {
        cadastrarAdvogadoMatching("Bruna Capital", "bruna.conexao@laweact.com", "11144477735", "510001");
        autenticarCliente("cliente.conexao@laweact.com", "52998224725");
        ResponseEntity<JsonNode> criacao = api.post("/solicitacoes", demandaConsultoriaSaoPaulo());
        assertSuccess(criacao, HttpStatus.CREATED);
        return UUID.fromString(criacao.getBody().path("data").path("id").asText());
    }

    @Test
    @DisplayName("cliente solicita conexão → PENDENTE sem contato")
    void shouldCreatePendingWithoutContact() {
        UUID solicitacaoId = criarSolicitacaoComMatch();
        UUID advogadoId = UUID.fromString(jdbcTemplate.queryForObject(
                """
                        SELECT advogado_id::text FROM solicitacao_matches
                        WHERE solicitacao_id = ?::uuid LIMIT 1
                        """,
                String.class,
                solicitacaoId
        ));

        ResponseEntity<JsonNode> response = api.post(
                "/conexoes",
                Map.of("solicitacaoId", solicitacaoId.toString(), "advogadoId", advogadoId.toString())
        );

        assertSuccess(response, HttpStatus.CREATED);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("status").asText()).isEqualTo("PENDENTE");
        assertThat(data.path("solicitacaoId").asText()).isEqualTo(solicitacaoId.toString());
        assertThat(data.path("advogadoId").asText()).isEqualTo(advogadoId.toString());
        assertThat(data.path("telefone").isMissingNode() || data.path("telefone").isNull()).isTrue();
        assertThat(data.path("email").isMissingNode() || data.path("email").isNull()).isTrue();
    }

    @Test
    @DisplayName("advogado aceita → ACEITA com contato e MATCH_REALIZADO na solicitação")
    void shouldAcceptAndExposeContact() {
        UUID solicitacaoId = criarSolicitacaoComMatch();
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

        autenticarComo("bruna.conexao@laweact.com");
        ResponseEntity<JsonNode> aceita = api.post("/conexoes/" + conexaoId + "/aceitar", Map.of());
        assertSuccess(aceita, HttpStatus.OK);

        JsonNode data = aceita.getBody().path("data");
        assertThat(data.path("status").asText()).isEqualTo("ACEITA");
        assertThat(data.path("telefone").asText()).isEqualTo("11988887777");
        assertThat(data.path("email").asText()).isEqualTo("bruna.conexao@laweact.com");

        String statusSolicitacao = jdbcTemplate.queryForObject(
                "SELECT status FROM solicitacoes WHERE id = ?::uuid",
                String.class,
                solicitacaoId
        );
        assertThat(statusSolicitacao).isEqualTo("MATCH_REALIZADO");
    }

    @Test
    @DisplayName("advogado recusa → RECUSADA e cliente não pode reenviar")
    void shouldRejectAndBlockResend() {
        UUID solicitacaoId = criarSolicitacaoComMatch();
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

        autenticarComo("bruna.conexao@laweact.com");
        ResponseEntity<JsonNode> recusada = api.post("/conexoes/" + conexaoId + "/recusar", Map.of());
        assertSuccess(recusada, HttpStatus.OK);
        assertThat(recusada.getBody().path("data").path("status").asText()).isEqualTo("RECUSADA");

        autenticarComo("cliente.conexao@laweact.com");
        ResponseEntity<JsonNode> reenvio = api.post(
                "/conexoes",
                Map.of("solicitacaoId", solicitacaoId.toString(), "advogadoId", advogadoId.toString())
        );
        assertErrorCode(reenvio, HttpStatus.CONFLICT, "INVALID_STATUS");
    }

    @Test
    @DisplayName("cliente cancela → CANCELADA e pode solicitar de novo")
    void shouldCancelAndAllowResend() {
        UUID solicitacaoId = criarSolicitacaoComMatch();
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

        ResponseEntity<JsonNode> cancelada = api.post("/conexoes/" + conexaoId + "/cancelar", Map.of());
        assertSuccess(cancelada, HttpStatus.OK);
        assertThat(cancelada.getBody().path("data").path("status").asText()).isEqualTo("CANCELADA");

        ResponseEntity<JsonNode> reenvio = api.post(
                "/conexoes",
                Map.of("solicitacaoId", solicitacaoId.toString(), "advogadoId", advogadoId.toString())
        );
        assertSuccess(reenvio, HttpStatus.CREATED);
        assertThat(reenvio.getBody().path("data").path("status").asText()).isEqualTo("PENDENTE");
        assertThat(reenvio.getBody().path("data").path("id").asText()).isEqualTo(conexaoId);
    }

    @Test
    @DisplayName("cliente pode conectar com vários advogados na mesma solicitação")
    void shouldAllowMultipleConnectionsOnSameSolicitation() {
        cadastrarAdvogadoMatching("Bruna Capital", "bruna.multi@laweact.com", "11144477735", "520001");
        cadastrarAdvogadoMatching("Carlos Interior", "carlos.multi@laweact.com", "39053344705", "520002");
        autenticarCliente("cliente.multi@laweact.com", "52998224725");

        ResponseEntity<JsonNode> criacao = api.post("/solicitacoes", demandaConsultoriaSaoPaulo());
        UUID solicitacaoId = UUID.fromString(criacao.getBody().path("data").path("id").asText());

        List<String> advogados = jdbcTemplate.queryForList(
                "SELECT advogado_id::text FROM solicitacao_matches WHERE solicitacao_id = ?::uuid ORDER BY posicao",
                String.class,
                solicitacaoId
        );
        assertThat(advogados).hasSize(2);

        ResponseEntity<JsonNode> c1 = api.post(
                "/conexoes",
                Map.of("solicitacaoId", solicitacaoId.toString(), "advogadoId", advogados.get(0))
        );
        ResponseEntity<JsonNode> c2 = api.post(
                "/conexoes",
                Map.of("solicitacaoId", solicitacaoId.toString(), "advogadoId", advogados.get(1))
        );
        assertSuccess(c1, HttpStatus.CREATED);
        assertSuccess(c2, HttpStatus.CREATED);
        assertThat(c1.getBody().path("data").path("id").asText())
                .isNotEqualTo(c2.getBody().path("data").path("id").asText());
    }

    @Test
    @DisplayName("advogado lista conexões pendentes no inbox")
    void shouldListPendingForLawyer() {
        UUID solicitacaoId = criarSolicitacaoComMatch();
        UUID advogadoId = UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT advogado_id::text FROM solicitacao_matches WHERE solicitacao_id = ?::uuid LIMIT 1",
                String.class,
                solicitacaoId
        ));
        api.post(
                "/conexoes",
                Map.of("solicitacaoId", solicitacaoId.toString(), "advogadoId", advogadoId.toString())
        );

        autenticarComo("bruna.conexao@laweact.com");
        ResponseEntity<JsonNode> lista = api.get("/conexoes?status=PENDENTE");
        assertSuccess(lista, HttpStatus.OK);
        JsonNode items = lista.getBody().path("data").path("items");
        assertThat(items.isArray()).isTrue();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).path("status").asText()).isEqualTo("PENDENTE");
    }

    @Test
    @DisplayName("GET status da conexão no perfil do advogado")
    void shouldGetConnectionStatusForLawyerProfile() {
        UUID solicitacaoId = criarSolicitacaoComMatch();
        UUID advogadoId = UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT advogado_id::text FROM solicitacao_matches WHERE solicitacao_id = ?::uuid LIMIT 1",
                String.class,
                solicitacaoId
        ));
        api.post(
                "/conexoes",
                Map.of("solicitacaoId", solicitacaoId.toString(), "advogadoId", advogadoId.toString())
        );

        ResponseEntity<JsonNode> response = api.get(
                "/advogados/" + advogadoId + "/conexao?solicitacaoId=" + solicitacaoId
        );
        assertSuccess(response, HttpStatus.OK);
        assertThat(response.getBody().path("data").path("status").asText()).isEqualTo("PENDENTE");
    }

    @Test
    @DisplayName("sem match prévio não cria conexão")
    void shouldRequireMatch() {
        cadastrarAdvogadoMatching("Bruna Capital", "bruna.match@laweact.com", "11144477735", "530001");
        ResponseEntity<JsonNode> rj = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoParaMatching(
                        "Diego RJ", "diego.rj.conexao@laweact.com", "39053344705", "530002",
                        "RJ", "Rio de Janeiro", List.of("GENERALISTA")
                )
        );
        assertSuccess(rj, HttpStatus.CREATED);
        UUID rjId = UUID.fromString(rj.getBody().path("data").path("usuario").path("id").asText());
        api.logout();

        autenticarCliente("cliente.semmatch@laweact.com", "52998224725");
        ResponseEntity<JsonNode> criacao = api.post("/solicitacoes", demandaConsultoriaSaoPaulo());
        UUID solicitacaoId = UUID.fromString(criacao.getBody().path("data").path("id").asText());

        ResponseEntity<JsonNode> response = api.post(
                "/conexoes",
                Map.of("solicitacaoId", solicitacaoId.toString(), "advogadoId", rjId.toString())
        );
        assertErrorCode(response, HttpStatus.BAD_REQUEST, "NO_MATCH");
    }

    @Test
    @DisplayName("advogado indisponível não recebe nova conexão")
    void shouldRejectConnectionWhenLawyerUnavailable() {
        UUID solicitacaoId = criarSolicitacaoComMatch();
        UUID advogadoId = UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT advogado_id::text FROM solicitacao_matches WHERE solicitacao_id = ?::uuid LIMIT 1",
                String.class,
                solicitacaoId
        ));

        autenticarComo("bruna.conexao@laweact.com");
        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/disponibilidade",
                Map.of("disponibilidade", "INDISPONIVEL")
        );
        assertSuccess(patch, HttpStatus.OK);

        autenticarComo("cliente.conexao@laweact.com");
        ResponseEntity<JsonNode> response = api.post(
                "/conexoes",
                Map.of("solicitacaoId", solicitacaoId.toString(), "advogadoId", advogadoId.toString())
        );
        assertErrorCode(response, HttpStatus.CONFLICT, "LAWYER_UNAVAILABLE");
    }
}
