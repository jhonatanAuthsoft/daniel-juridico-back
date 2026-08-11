package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorCode;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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

@DisplayName("E2E — POST /advogados/{id}/avaliacoes")
class AdvogadoAvaliacaoCriarE2ETest extends BaseE2ETest {

    private CriarSolicitacaoInputDTO demandaSp() {
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

    private String cadastrarEAutenticarCliente(String email, String documento) {
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
    }

    /** Returns lawyer id after creating solicitation + ACEITA connection. Client stays authenticated. */
    private UUID prepararConexaoAceita(
            String advNome,
            String advEmail,
            String advCpf,
            String advOab,
            String cliEmail,
            String cliDoc
    ) {
        UUID advogadoId = cadastrarAdvogadoMatching(advNome, advEmail, advCpf, advOab);
        cadastrarEAutenticarCliente(cliEmail, cliDoc);

        ResponseEntity<JsonNode> criacao = api.post("/solicitacoes", demandaSp());
        assertSuccess(criacao, HttpStatus.CREATED);
        UUID solicitacaoId = UUID.fromString(criacao.getBody().path("data").path("id").asText());

        ResponseEntity<JsonNode> criada = api.post(
                "/conexoes",
                Map.of(
                        "solicitacaoId", solicitacaoId.toString(),
                        "advogadoId", advogadoId.toString()
                )
        );
        assertSuccess(criada, HttpStatus.CREATED);
        String conexaoId = criada.getBody().path("data").path("id").asText();

        autenticarComo(advEmail);
        assertSuccess(api.post("/conexoes/" + conexaoId + "/aceitar", Map.of()), HttpStatus.OK);

        autenticarComo(cliEmail);
        return advogadoId;
    }

    @Test
    @DisplayName("cliente com ACEITA cria avaliação e podeAvaliar fica false")
    void shouldCreateAvaliacaoWhenAceita() {
        UUID advogadoId = prepararConexaoAceita(
                "Ana Avaliar",
                "ana.criar.aval@laweact.com",
                "11144477735",
                "610001",
                "cli.criar.aval@laweact.com",
                "52998224725"
        );

        ResponseEntity<JsonNode> antes = api.get("/advogados/" + advogadoId + "/avaliacoes");
        assertSuccess(antes, HttpStatus.OK);
        assertThat(antes.getBody().path("data").path("podeAvaliar").asBoolean()).isTrue();

        ResponseEntity<JsonNode> criada = api.post(
                "/advogados/" + advogadoId + "/avaliacoes",
                Map.of("nota", "4.5", "comentario", "Ótimo atendimento na conexão.")
        );
        assertSuccess(criada, HttpStatus.CREATED);
        JsonNode item = criada.getBody().path("data");
        assertThat(item.path("id").asText()).isNotBlank();
        assertThat(item.path("nota").decimalValue()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(item.path("comentario").asText()).isEqualTo("Ótimo atendimento na conexão.");
        assertThat(item.path("propria").asBoolean()).isTrue();

        ResponseEntity<JsonNode> depois = api.get("/advogados/" + advogadoId + "/avaliacoes");
        assertSuccess(depois, HttpStatus.OK);
        JsonNode data = depois.getBody().path("data");
        assertThat(data.path("podeAvaliar").asBoolean()).isFalse();
        assertThat(data.path("totalAvaliacoes").asInt()).isEqualTo(1);
        assertThat(data.path("mediaAvaliacoes").decimalValue())
                .isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(data.path("items").get(0).path("propria").asBoolean()).isTrue();

        BigDecimal mediaDb = jdbcTemplate.queryForObject(
                "SELECT media_avaliacoes FROM advogados WHERE usuario_id = ?::uuid",
                BigDecimal.class,
                advogadoId
        );
        assertThat(mediaDb).isEqualByComparingTo(new BigDecimal("4.50"));
    }

    @Test
    @DisplayName("sem conexão ACEITA retorna 403")
    void shouldForbidWithoutAceita() {
        UUID advogadoId = cadastrarAdvogadoMatching(
                "Bruno Sem Aceite",
                "bruno.sem.aceite@laweact.com",
                "39053344705",
                "610002"
        );
        cadastrarEAutenticarCliente("cli.sem.aceite@laweact.com", "15350946056");

        ResponseEntity<JsonNode> listagem = api.get("/advogados/" + advogadoId + "/avaliacoes");
        assertSuccess(listagem, HttpStatus.OK);
        assertThat(listagem.getBody().path("data").path("podeAvaliar").asBoolean()).isFalse();

        ResponseEntity<JsonNode> response = api.post(
                "/advogados/" + advogadoId + "/avaliacoes",
                Map.of("nota", "5.0", "comentario", "Sem conexão aceita.")
        );
        assertErrorCode(response, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    @Test
    @DisplayName("segunda avaliação do mesmo cliente retorna 409")
    void shouldConflictOnDuplicate() {
        UUID advogadoId = prepararConexaoAceita(
                "Carla Dup",
                "carla.dup.aval@laweact.com",
                "11144477735",
                "610003",
                "cli.dup.aval@laweact.com",
                "71428793860"
        );

        assertSuccess(
                api.post(
                        "/advogados/" + advogadoId + "/avaliacoes",
                        Map.of("nota", "4.0", "comentario", "Primeira avaliação ok.")
                ),
                HttpStatus.CREATED
        );

        ResponseEntity<JsonNode> segunda = api.post(
                "/advogados/" + advogadoId + "/avaliacoes",
                Map.of("nota", "5.0", "comentario", "Tentativa duplicada.")
        );
        assertErrorCode(segunda, HttpStatus.CONFLICT, "CONFLICT");
    }

    @Test
    @DisplayName("nota inválida (passo) retorna 400")
    void shouldRejectInvalidNotaStep() {
        UUID advogadoId = prepararConexaoAceita(
                "Diego Nota",
                "diego.nota.aval@laweact.com",
                "39053344705",
                "610004",
                "cli.nota.aval@laweact.com",
                "52998224725"
        );

        ResponseEntity<JsonNode> response = api.post(
                "/advogados/" + advogadoId + "/avaliacoes",
                Map.of("nota", "4.3", "comentario", "Nota com passo inválido.")
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("sem autenticação retorna 401")
    void shouldRequireAuth() {
        UUID advogadoId = cadastrarAdvogadoMatching(
                "Eva Auth",
                "eva.auth.aval@laweact.com",
                "11144477735",
                "610005"
        );
        api.logout();

        ResponseEntity<JsonNode> response = api.post(
                "/advogados/" + advogadoId + "/avaliacoes",
                Map.of("nota", "5.0", "comentario", "Sem token.")
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
