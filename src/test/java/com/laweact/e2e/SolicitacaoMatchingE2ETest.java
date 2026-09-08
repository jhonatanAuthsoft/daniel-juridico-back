package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorCode;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

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

@DisplayName("E2E — matching da solicitação")
class SolicitacaoMatchingE2ETest extends BaseE2ETest {

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

    private void cadastrarAdvogado(
            String nome,
            String email,
            String cpf,
            String oab,
            String uf,
            String cidade,
            List<String> modalidades
    ) {
        ResponseEntity<JsonNode> response = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoParaMatching(nome, email, cpf, oab, uf, cidade, modalidades)
        );
        assertSuccess(response, HttpStatus.CREATED);
        api.logout();
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

    @Test
    @DisplayName("ranking salva compatíveis por score e ignora advogado fora do estado")
    void shouldRankCompatibleLawyersAndSkipOutOfState() {
        cadastrarAdvogado("Bruna Capital", "bruna.sp@laweact.com", "11144477735", "100001",
                "SP", "São Paulo", List.of("GENERALISTA"));
        cadastrarAdvogado("Carlos Interior", "carlos.sp@laweact.com", "39053344705", "100002",
                "SP", "Campinas", List.of("GENERALISTA"));
        cadastrarAdvogado("Diego Litoral", "diego.rj@laweact.com", "15350946056", "100003",
                "RJ", "Rio de Janeiro", List.of("GENERALISTA"));

        autenticarCliente("cliente.matching@laweact.com", "52998224725");

        ResponseEntity<JsonNode> criacao = api.post("/solicitacoes", demandaConsultoriaSaoPaulo());
        assertSuccess(criacao, HttpStatus.CREATED);

        JsonNode data = criacao.getBody().path("data");
        assertThat(data.path("totalMatches").asInt()).isEqualTo(2);

        String solicitacaoId = data.path("id").asText();
        ResponseEntity<JsonNode> matches = api.get("/solicitacoes/" + solicitacaoId + "/matches");
        assertSuccess(matches, HttpStatus.OK);

        JsonNode lista = matches.getBody().path("data");
        assertThat(lista.size()).isEqualTo(2);

        JsonNode primeiro = lista.get(0);
        assertThat(primeiro.path("nome").asText()).isEqualTo("Bruna Capital");
        assertThat(primeiro.path("posicao").asInt()).isEqualTo(1);
        assertThat(primeiro.path("compatibilidade").asInt()).isEqualTo(100);
        assertThat(primeiro.path("nivelLocalidade").asText()).isEqualTo("MESMA_CIDADE");
        assertThat(primeiro.path("bairro").asText()).isEqualTo("Bela Vista");
        assertThat(primeiro.path("cidade").asText()).isEqualTo("São Paulo");
        assertThat(primeiro.path("modalidadeAtuacao").path("codigo").asText()).isEqualTo("GENERALISTA");
        assertThat(primeiro.path("modalidadeAtuacao").path("nome").asText()).isEqualTo("Generalista");
        assertThat(primeiro.path("pontuacao").path("localidade").asInt()).isEqualTo(20);

        JsonNode segundo = lista.get(1);
        assertThat(segundo.path("nome").asText()).isEqualTo("Carlos Interior");
        assertThat(segundo.path("posicao").asInt()).isEqualTo(2);
        assertThat(segundo.path("compatibilidade").asInt()).isEqualTo(90);
        assertThat(segundo.path("nivelLocalidade").asText()).isEqualTo("MESMO_ESTADO");

        Integer persistidos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM solicitacao_matches WHERE solicitacao_id = ?::uuid",
                Integer.class,
                solicitacaoId
        );
        assertThat(persistidos).isEqualTo(2);
    }

    @Test
    @DisplayName("modalidade incompatível deixa a solicitação sem matches")
    void shouldReturnNoMatchesWhenModalidadeIncompatible() {
        cadastrarAdvogado("Elisa Pautista", "elisa.sp@laweact.com", "11144477735", "200001",
                "SP", "São Paulo", List.of("PAUTISTA"));

        autenticarCliente("cliente.sem.match@laweact.com", "52998224725");

        ResponseEntity<JsonNode> criacao = api.post("/solicitacoes", demandaConsultoriaSaoPaulo());
        assertSuccess(criacao, HttpStatus.CREATED);
        assertThat(criacao.getBody().path("data").path("totalMatches").asInt()).isZero();
    }

    @Test
    @DisplayName("'nenhuma das anteriores' entra pela especialidade com 80 pontos")
    void shouldRankNenhumaDasAnterioresBySpecialty() {
        cadastrarAdvogado("Fabio Especialista", "fabio.sp@laweact.com", "39053344705", "300001",
                "SP", "São Paulo", List.of("NENHUMA_DAS_ANTERIORES"));

        autenticarCliente("cliente.nenhuma@laweact.com", "52998224725");

        ResponseEntity<JsonNode> criacao = api.post("/solicitacoes", demandaConsultoriaSaoPaulo());
        String solicitacaoId = criacao.getBody().path("data").path("id").asText();

        ResponseEntity<JsonNode> matches = api.get("/solicitacoes/" + solicitacaoId + "/matches");
        assertSuccess(matches, HttpStatus.OK);

        JsonNode item = matches.getBody().path("data").get(0);
        assertThat(item.path("compatibilidade").asInt()).isEqualTo(80);
        assertThat(item.path("pontuacao").path("modalidade").asInt()).isZero();
        assertThat(item.path("pontuacao").path("especialidade").asInt()).isEqualTo(20);
    }

    @Test
    @DisplayName("cliente não acessa matches de solicitação de outro cliente")
    void shouldForbidMatchesFromAnotherClient() {
        cadastrarAdvogado("Gustavo Geral", "gustavo.sp@laweact.com", "11144477735", "400001",
                "SP", "São Paulo", List.of("GENERALISTA"));

        autenticarCliente("cliente.dono@laweact.com", "52998224725");
        ResponseEntity<JsonNode> criacao = api.post("/solicitacoes", demandaConsultoriaSaoPaulo());
        String solicitacaoId = criacao.getBody().path("data").path("id").asText();
        api.logout();

        autenticarCliente("cliente.intruso@laweact.com", "15350946056");
        ResponseEntity<JsonNode> matches = api.get("/solicitacoes/" + solicitacaoId + "/matches");

        assertErrorCode(matches, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    @Test
    @DisplayName("advogado indisponível não entra no ranking de matching")
    void shouldExcludeUnavailableLawyerFromMatching() {
        cadastrarAdvogado("Helena Disponível", "helena.disp@laweact.com", "11144477735", "500001",
                "SP", "São Paulo", List.of("GENERALISTA"));

        ResponseEntity<JsonNode> cadastro = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoParaMatching(
                        "Igor Indisponível", "igor.indisp@laweact.com", "39053344705", "500002",
                        "SP", "São Paulo", List.of("GENERALISTA")
                )
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());
        garantirAssinaturaSeAdvogado();
        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/disponibilidade",
                Map.of("disponibilidade", "INDISPONIVEL")
        );
        assertSuccess(patch, HttpStatus.OK);
        api.logout();

        autenticarCliente("cliente.indisp.match@laweact.com", "52998224725");
        ResponseEntity<JsonNode> criacao = api.post("/solicitacoes", demandaConsultoriaSaoPaulo());
        assertSuccess(criacao, HttpStatus.CREATED);
        assertThat(criacao.getBody().path("data").path("totalMatches").asInt()).isEqualTo(1);

        String solicitacaoId = criacao.getBody().path("data").path("id").asText();
        ResponseEntity<JsonNode> matches = api.get("/solicitacoes/" + solicitacaoId + "/matches");
        assertSuccess(matches, HttpStatus.OK);

        JsonNode lista = matches.getBody().path("data");
        assertThat(lista.size()).isEqualTo(1);
        assertThat(lista.get(0).path("nome").asText()).isEqualTo("Helena Disponível");
        assertThat(lista.get(0).path("disponibilidade").asText()).isEqualTo("DISPONIVEL");
    }

    @Test
    @DisplayName("GET matches expõe a disponibilidade atual mesmo após o ranking")
    void shouldExposeCurrentAvailabilityInMatches() {
        cadastrarAdvogado("Helena Disponível", "helena.atual@laweact.com", "11144477735", "500003",
                "SP", "São Paulo", List.of("GENERALISTA"));

        autenticarCliente("cliente.disp.atual@laweact.com", "52998224725");
        ResponseEntity<JsonNode> criacao = api.post("/solicitacoes", demandaConsultoriaSaoPaulo());
        assertSuccess(criacao, HttpStatus.CREATED);
        String solicitacaoId = criacao.getBody().path("data").path("id").asText();

        ResponseEntity<JsonNode> matchesAntes = api.get("/solicitacoes/" + solicitacaoId + "/matches");
        assertSuccess(matchesAntes, HttpStatus.OK);
        JsonNode antes = matchesAntes.getBody().path("data");
        assertThat(antes.size()).isEqualTo(1);
        assertThat(antes.get(0).path("disponibilidade").asText()).isEqualTo("DISPONIVEL");

        autenticarComo("helena.atual@laweact.com");
        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/disponibilidade",
                Map.of("disponibilidade", "INDISPONIVEL")
        );
        assertSuccess(patch, HttpStatus.OK);

        autenticarComo("cliente.disp.atual@laweact.com");
        ResponseEntity<JsonNode> matchesDepois = api.get("/solicitacoes/" + solicitacaoId + "/matches");
        assertSuccess(matchesDepois, HttpStatus.OK);
        JsonNode depois = matchesDepois.getBody().path("data");
        assertThat(depois.size()).isEqualTo(1);
        assertThat(depois.get(0).path("nome").asText()).isEqualTo("Helena Disponível");
        assertThat(depois.get(0).path("disponibilidade").asText()).isEqualTo("INDISPONIVEL");
    }
}
