package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorCode;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.dto.solicitacao.CriarSolicitacaoInputDTO;
import com.laweact.e2e.support.Fixtures;
import com.laweact.model.enums.FormaCobrancaSolicitacaoEnum;
import com.laweact.model.enums.ModalidadeSolicitacaoEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

@DisplayName("E2E — POST /solicitacoes")
class SolicitacaoCriarE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("cliente autenticado cria solicitação com campos obrigatórios e opcionais")
    void shouldCreateSolicitacaoAsCliente() {
        String email = "solicitacao.ok@laweact.com";
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido(email, "39053344705")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        CriarSolicitacaoInputDTO input = CriarSolicitacaoInputDTO.builder()
                .titulo("Rescisão trabalhista")
                .modalidade(ModalidadeSolicitacaoEnum.CONSULTORIA)
                .especialidadeCodigo("CIVIL")
                .subespecialidadeCodigo("CONTRATOS")
                .uf("SP")
                .cidade("São Paulo")
                .urgencia(UrgenciaSolicitacaoEnum.URGENTE)
                .descricao("Preciso de orientação sobre rescisão sem justa causa.")
                .formaCobranca(FormaCobrancaSolicitacaoEnum.VALOR_FIXO)
                .experienciaMinimaMeses(24)
                .build();

        ResponseEntity<JsonNode> response = api.post("/solicitacoes", input);

        assertSuccess(response, HttpStatus.CREATED);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("id").asText()).isNotBlank();
        assertThat(UUID.fromString(data.path("id").asText())).isNotNull();
        assertThat(data.path("status").asText()).isEqualTo("AGUARDANDO_MATCHING");
        assertThat(data.path("titulo").asText()).isEqualTo("Rescisão trabalhista");
        assertThat(data.path("modalidade").asText()).isEqualTo("CONSULTORIA");
        assertThat(data.path("especialidadeCodigo").asText()).isEqualTo("CIVIL");
        assertThat(data.path("subespecialidadeCodigo").asText()).isEqualTo("CONTRATOS");
        assertThat(data.path("uf").asText()).isEqualTo("SP");
        assertThat(data.path("cidade").asText()).isEqualTo("São Paulo");
        assertThat(data.path("urgencia").asText()).isEqualTo("URGENTE");
        assertThat(data.path("formaCobranca").asText()).isEqualTo("VALOR_FIXO");
        assertThat(data.path("experienciaMinimaMeses").asInt()).isEqualTo(24);
        assertThat(data.path("criadoEm").asText()).isNotBlank();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM solicitacoes WHERE id = ?",
                Integer.class,
                UUID.fromString(data.path("id").asText())
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("sem autenticação retorna 401")
    void shouldRequireAuth() {
        ResponseEntity<JsonNode> response = api.post("/solicitacoes", Map.of(
                "titulo", "Teste",
                "modalidade", "CONSULTORIA",
                "especialidadeCodigo", "CIVIL",
                "uf", "SP",
                "cidade", "São Paulo",
                "urgencia", "MEDIO",
                "descricao", "Descrição válida com conteúdo suficiente."
        ));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("advogado autenticado não pode criar solicitação")
    void shouldForbidAdvogado() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido("adv.solicitacao@laweact.com", "39053344705", "998877")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());
        garantirAssinaturaSeAdvogado();

        ResponseEntity<JsonNode> response = api.post("/solicitacoes", CriarSolicitacaoInputDTO.builder()
                .titulo("Não deveria")
                .modalidade(ModalidadeSolicitacaoEnum.PROCESSO)
                .especialidadeCodigo("CIVIL")
                .uf("RJ")
                .cidade("Rio de Janeiro")
                .urgencia(UrgenciaSolicitacaoEnum.MEDIO)
                .descricao("Tentativa de criação por advogado.")
                .build());

        assertErrorCode(response, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    @Test
    @DisplayName("especialidade inexistente retorna 400")
    void shouldRejectInvalidSpecialty() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("solicitacao.esp@laweact.com", "15350946056")
        );
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> response = api.post("/solicitacoes", CriarSolicitacaoInputDTO.builder()
                .titulo("Esp inválida")
                .modalidade(ModalidadeSolicitacaoEnum.MEDIACAO)
                .especialidadeCodigo("NAO_EXISTE")
                .uf("BA")
                .cidade("Salvador")
                .urgencia(UrgenciaSolicitacaoEnum.TENHO_TEMPO)
                .descricao("Especialidade inventada.")
                .build());

        assertErrorCode(response, HttpStatus.BAD_REQUEST, "INVALID_SPECIALTY");
    }
}
