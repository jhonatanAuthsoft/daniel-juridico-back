package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
import com.laweact.model.enums.StatusUsuarioEnum;
import com.laweact.model.enums.UrgenciaSolicitacaoEnum;

@DisplayName("E2E — excluir conta do usuário autenticado")
class UsuarioExcluirContaE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("DELETE /usuarios/me marca a conta como excluída e o login deixa de funcionar")
    void shouldSoftDeleteAuthenticatedAccount() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("apagar.conta@laweact.com", "52998224725")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> deleted = api.delete("/usuarios/me");
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleted.getBody().path("success").asBoolean()).isTrue();
        assertThat(deleted.getBody().path("message").asText()).contains("excluída");

        var usuario = usuarioRepository.findByEmail("apagar.conta@laweact.com");
        assertThat(usuario).isPresent();
        assertThat(usuario.get().getStatus()).isEqualTo(StatusUsuarioEnum.EXCLUIDO);
        assertThat(usuario.get().getExcluidoEm()).isNotNull();

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        api.logout();
        ResponseEntity<JsonNode> login = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder()
                        .email("apagar.conta@laweact.com")
                        .senha(Fixtures.VALID_PASSWORD)
                        .build()
        );
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("advogado excluído some do matching e do perfil público")
    void shouldHideDeletedLawyerFromApp() {
        ResponseEntity<JsonNode> cadastroAdv = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoParaMatching(
                        "Helena Excluída",
                        "helena.excluir@laweact.com",
                        "11144477735",
                        "600001",
                        "SP",
                        "São Paulo",
                        List.of("GENERALISTA")
                )
        );
        assertSuccess(cadastroAdv, HttpStatus.CREATED);
        String advogadoId = cadastroAdv.getBody().path("data").path("usuario").path("id").asText();
        String advogadoToken = cadastroAdv.getBody().path("data").path("token").asText();

        ResponseEntity<JsonNode> cadastroCli = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("cliente.excluir.adv@laweact.com", "52998224725")
        );
        assertSuccess(cadastroCli, HttpStatus.CREATED);
        api.authenticate(cadastroCli.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> criacao = api.post(
                "/solicitacoes",
                CriarSolicitacaoInputDTO.builder()
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
                        .build()
        );
        assertSuccess(criacao, HttpStatus.CREATED);
        String solicitacaoId = criacao.getBody().path("data").path("id").asText();

        ResponseEntity<JsonNode> matchesAntes = api.get("/solicitacoes/" + solicitacaoId + "/matches");
        assertSuccess(matchesAntes, HttpStatus.OK);
        assertThat(matchesAntes.getBody().path("data").size()).isEqualTo(1);

        ResponseEntity<JsonNode> perfilAntes = api.get("/advogados/" + advogadoId);
        assertSuccess(perfilAntes, HttpStatus.OK);

        api.authenticate(advogadoToken);
        ResponseEntity<JsonNode> deleted = api.delete("/usuarios/me");
        assertSuccess(deleted, HttpStatus.OK);

        api.authenticate(cadastroCli.getBody().path("data").path("token").asText());
        ResponseEntity<JsonNode> matchesDepois = api.get("/solicitacoes/" + solicitacaoId + "/matches");
        assertSuccess(matchesDepois, HttpStatus.OK);
        assertThat(matchesDepois.getBody().path("data").size()).isEqualTo(0);

        ResponseEntity<JsonNode> perfilDepois = api.get("/advogados/" + advogadoId);
        assertThat(perfilDepois.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("rejeita DELETE /usuarios/me sem autenticação")
    void shouldRejectUnauthenticated() {
        ResponseEntity<JsonNode> deleted = api.delete("/usuarios/me");
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("DELETE /usuarios/excluir/{id} não existe e não apaga outro usuário")
    void shouldNotExposeDeleteById() {
        ResponseEntity<JsonNode> atacante = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("atacante.excluir@laweact.com", "11144477735")
        );
        assertSuccess(atacante, HttpStatus.CREATED);
        api.authenticate(atacante.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> vitima = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("vitima.excluir@laweact.com", "15350946056")
        );
        assertSuccess(vitima, HttpStatus.CREATED);
        UUID vitimaId = UUID.fromString(vitima.getBody().path("data").path("usuario").path("id").asText());

        ResponseEntity<JsonNode> deleted = api.delete("/usuarios/excluir/" + vitimaId);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(usuarioRepository.findByEmail("vitima.excluir@laweact.com")).isPresent();
    }
}
