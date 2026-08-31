package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorCode;
import static com.laweact.e2e.support.ApiAssertions.assertErrorDetailContains;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.e2e.support.Fixtures;
import com.laweact.model.entity.AdvogadoEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.DisponibilidadeAdvogadoEnum;
import com.laweact.model.enums.PronomeTratamentoEnum;

@DisplayName("E2E — edição de dados cadastrais do advogado autenticado")
class AdvogadoEditarPerfilE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("PATCH dados gerais atualiza o nome e GET /me reflete")
    void shouldUpdateGeneralDataName() {
        authenticateAdvogado("edit.adv.nome@laweact.com", "39053344705", "810001");

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/dados-gerais",
                Map.of("nomeCompleto", "João Advogado Lima")
        );
        assertSuccess(patch, HttpStatus.OK);
        assertThat(patch.getBody().path("data").path("perfil").path("nomeCompleto").asText())
                .isEqualTo("João Advogado Lima");
        assertThat(patch.getBody().path("data").path("perfil").path("cpf").asText())
                .isEqualTo("39053344705");

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        JsonNode data = me.getBody().path("data");
        assertThat(data.path("usuario").path("nomeCompleto").asText()).isEqualTo("João Advogado Lima");
        assertThat(data.path("advogado").path("perfil").path("nomeCompleto").asText())
                .isEqualTo("João Advogado Lima");

        UsuarioEntity usuario = usuarioRepository.findByEmail("edit.adv.nome@laweact.com").orElseThrow();
        assertThat(usuario.getNomeCompleto()).isEqualTo("João Advogado Lima");
        AdvogadoEntity advogado = advogadoRepository.findByUsuarioId(usuario.getId()).orElseThrow();
        assertThat(advogado.getNomeCompleto()).isEqualTo("João Advogado Lima");
        assertThat(advogado.getCpf()).isEqualTo("39053344705");
    }

    @Test
    @DisplayName("PATCH dados gerais rejeita nome em branco")
    void shouldRejectBlankName() {
        authenticateAdvogado("edit.adv.blank@laweact.com", "52998224725", "810002");

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/dados-gerais",
                Map.of("nomeCompleto", "   ")
        );
        assertErrorDetailContains(patch, HttpStatus.UNPROCESSABLE_ENTITY, "nome");
    }

    @Test
    @DisplayName("PATCH endereço atualiza o cadastro e GET /me reflete")
    void shouldUpdateAddress() {
        authenticateAdvogado("edit.adv.end@laweact.com", "15350946056", "810003");

        ResponseEntity<JsonNode> patch = api.patch("/advogados/me/endereco", addressBody(
                "01311-100",
                "Rua Augusta",
                "200",
                "Cj 10",
                "Consolação",
                "São Paulo",
                "SP"
        ));
        assertSuccess(patch, HttpStatus.OK);
        JsonNode endereco = patch.getBody().path("data").path("endereco");
        assertThat(endereco.path("cep").asText()).isEqualTo("01311-100");
        assertThat(endereco.path("logradouro").asText()).isEqualTo("Rua Augusta");
        assertThat(endereco.path("numero").asText()).isEqualTo("200");
        assertThat(endereco.path("complemento").asText()).isEqualTo("Cj 10");
        assertThat(endereco.path("bairro").asText()).isEqualTo("Consolação");
        assertThat(endereco.path("cidade").asText()).isEqualTo("São Paulo");
        assertThat(endereco.path("estado").asText()).isEqualTo("SP");

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        assertThat(me.getBody().path("data").path("advogado").path("endereco").path("logradouro").asText())
                .isEqualTo("Rua Augusta");
    }

    @Test
    @DisplayName("PATCH endereço aceita CEP sem hífen e persiste formatado")
    void shouldNormalizeCepOnAddressUpdate() {
        authenticateAdvogado("edit.adv.cep@laweact.com", "11144477735", "810004");

        ResponseEntity<JsonNode> patch = api.patch("/advogados/me/endereco", addressBody(
                "01311100",
                "Rua Augusta",
                "200",
                null,
                "Consolação",
                "São Paulo",
                "sp"
        ));
        assertSuccess(patch, HttpStatus.OK);
        JsonNode endereco = patch.getBody().path("data").path("endereco");
        assertThat(endereco.path("cep").asText()).isEqualTo("01311-100");
        assertThat(endereco.path("estado").asText()).isEqualTo("SP");
        assertThat(endereco.path("complemento").isNull()).isTrue();
    }

    @Test
    @DisplayName("PATCH endereço rejeita CEP inválido")
    void shouldRejectInvalidCep() {
        authenticateAdvogado("edit.adv.cep.bad@laweact.com", "26153377050", "810005");

        ResponseEntity<JsonNode> patch = api.patch("/advogados/me/endereco", addressBody(
                "123",
                "Rua Augusta",
                "200",
                null,
                "Consolação",
                "São Paulo",
                "SP"
        ));
        assertThat(patch.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @DisplayName("PATCH formas de cobrança substitui a lista e GET /me reflete")
    void shouldUpdateBillingMethods() {
        authenticateAdvogado("edit.adv.cobranca@laweact.com", "71428793860", "810006");

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/formas-cobranca",
                Map.of("formasCobranca", List.of("HONORARIOS_PERCENTUAIS", "HONORARIOS_ARBITRADOS"))
        );
        assertSuccess(patch, HttpStatus.OK);
        List<String> codigos = extractCodigos(patch.getBody().path("data").path("formasCobranca"));
        assertThat(codigos).containsExactlyInAnyOrder("HONORARIOS_PERCENTUAIS", "HONORARIOS_ARBITRADOS");

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        assertThat(extractCodigos(me.getBody().path("data").path("advogado").path("formasCobranca")))
                .containsExactlyInAnyOrder("HONORARIOS_PERCENTUAIS", "HONORARIOS_ARBITRADOS");
    }

    @Test
    @DisplayName("PATCH formas de cobrança rejeita lista vazia")
    void shouldRejectEmptyBillingMethods() {
        authenticateAdvogado("edit.adv.cobranca.empty@laweact.com", "39053344705", "810007");

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/formas-cobranca",
                Map.of("formasCobranca", List.of())
        );
        assertThat(patch.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @DisplayName("PATCH biografia atualiza pronome e texto")
    void shouldUpdateBiography() {
        authenticateAdvogado("edit.adv.bio@laweact.com", "52998224725", "810008");

        Map<String, Object> body = new HashMap<>();
        body.put("pronomeTratamento", "DOUTORA");
        body.put("biografia", "Advogada com atuação em direito civil.");

        ResponseEntity<JsonNode> patch = api.patch("/advogados/me/biografia", body);
        assertSuccess(patch, HttpStatus.OK);
        JsonNode perfil = patch.getBody().path("data").path("perfil");
        assertThat(perfil.path("pronomeTratamento").asText()).isEqualTo("DOUTORA");
        assertThat(perfil.path("biografia").asText()).isEqualTo("Advogada com atuação em direito civil.");

        UsuarioEntity usuario = usuarioRepository.findByEmail("edit.adv.bio@laweact.com").orElseThrow();
        AdvogadoEntity advogado = advogadoRepository.findByUsuarioId(usuario.getId()).orElseThrow();
        assertThat(advogado.getPronomeTratamento()).isEqualTo(PronomeTratamentoEnum.DOUTORA);
        assertThat(advogado.getBiografia()).isEqualTo("Advogada com atuação em direito civil.");
    }

    @Test
    @DisplayName("PATCH biografia limpa texto em branco")
    void shouldClearBlankBiography() {
        authenticateAdvogado("edit.adv.bio.clear@laweact.com", "15350946056", "810009");

        Map<String, Object> body = new HashMap<>();
        body.put("pronomeTratamento", "NEUTRO");
        body.put("biografia", "   ");

        ResponseEntity<JsonNode> patch = api.patch("/advogados/me/biografia", body);
        assertSuccess(patch, HttpStatus.OK);
        assertThat(patch.getBody().path("data").path("perfil").path("pronomeTratamento").asText())
                .isEqualTo("NEUTRO");
        assertThat(patch.getBody().path("data").path("perfil").path("biografia").isNull()).isTrue();
    }

    @Test
    @DisplayName("PATCH documentação substitui OABs e GET /me reflete")
    void shouldUpdateDocumentation() {
        authenticateAdvogado("edit.adv.oab@laweact.com", "11144477735", "810010");

        Map<String, Object> body = new HashMap<>();
        body.put("oabPrincipal", oabBody("810010", "SP", "2016-03-15", List.of("tmp/oab/frente.jpg")));
        body.put("oabsSuplementares", List.of(oabBody("910010", "RJ", "2018-01-20", List.of())));

        ResponseEntity<JsonNode> patch = api.patch("/advogados/me/documentacao", body);
        assertSuccess(patch, HttpStatus.OK);
        JsonNode oabs = patch.getBody().path("data").path("oabs");
        assertThat(oabs.size()).isEqualTo(2);
        JsonNode principal = findOab(oabs, true);
        assertThat(principal.path("numero").asText()).isEqualTo("810010");
        assertThat(principal.path("uf").asText()).isEqualTo("SP");
        assertThat(principal.path("fotosUrls").get(0).asText()).isEqualTo("tmp/oab/frente.jpg");
        JsonNode suplementar = findOab(oabs, false);
        assertThat(suplementar.path("numero").asText()).isEqualTo("910010");
        assertThat(suplementar.path("uf").asText()).isEqualTo("RJ");

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        assertThat(me.getBody().path("data").path("advogado").path("oabs").size()).isEqualTo(2);
    }

    @Test
    @DisplayName("PATCH documentação rejeita OAB já cadastrada por outro advogado")
    void shouldRejectDuplicateOabFromAnotherLawyer() {
        api.post("/advogados/cadastrar", Fixtures.advogadoValido("edit.adv.oab.dono@laweact.com", "26153377050", "810011"));
        authenticateAdvogado("edit.adv.oab.outro@laweact.com", "71428793860", "810012");

        Map<String, Object> body = new HashMap<>();
        body.put("oabPrincipal", oabBody("810011", "SP", "2016-03-15", List.of()));

        ResponseEntity<JsonNode> patch = api.patch("/advogados/me/documentacao", body);
        assertErrorDetailContains(patch, HttpStatus.BAD_REQUEST, "OAB");
    }

    @Test
    @DisplayName("PATCH disponibilidade persiste e GET /me reflete")
    void shouldUpdateAvailability() {
        authenticateAdvogado("edit.adv.disp@laweact.com", "28001238938", "810014");

        ResponseEntity<JsonNode> meBefore = api.get("/usuarios/me");
        assertSuccess(meBefore, HttpStatus.OK);
        assertThat(meBefore.getBody().path("data").path("advogado").path("perfil").path("disponibilidade").asText())
                .isEqualTo("DISPONIVEL");

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/disponibilidade",
                Map.of("disponibilidade", "INDISPONIVEL")
        );
        assertSuccess(patch, HttpStatus.OK);
        assertThat(patch.getBody().path("data").path("perfil").path("disponibilidade").asText())
                .isEqualTo("INDISPONIVEL");

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        assertThat(me.getBody().path("data").path("advogado").path("perfil").path("disponibilidade").asText())
                .isEqualTo("INDISPONIVEL");

        UsuarioEntity usuario = usuarioRepository.findByEmail("edit.adv.disp@laweact.com").orElseThrow();
        AdvogadoEntity advogado = advogadoRepository.findByUsuarioId(usuario.getId()).orElseThrow();
        assertThat(advogado.getDisponibilidade()).isEqualTo(DisponibilidadeAdvogadoEnum.INDISPONIVEL);
    }

    @Test
    @DisplayName("PATCH graduação atualiza universidade, curso e ano")
    void shouldUpdateGraduation() {
        authenticateAdvogado("edit.adv.grad@laweact.com", "39053344705", "810013");

        ResponseEntity<JsonNode> patch = api.patch("/advogados/me/graduacao", Map.of(
                "universidade", "PUC-SP",
                "curso", "Direito",
                "anoFormacao", 2018
        ));
        assertSuccess(patch, HttpStatus.OK);
        JsonNode perfil = patch.getBody().path("data").path("perfil");
        assertThat(perfil.path("universidade").asText()).isEqualTo("PUC-SP");
        assertThat(perfil.path("curso").asText()).isEqualTo("Direito");
        assertThat(perfil.path("anoFormacao").asInt()).isEqualTo(2018);

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        JsonNode mePerfil = me.getBody().path("data").path("advogado").path("perfil");
        assertThat(mePerfil.path("universidade").asText()).isEqualTo("PUC-SP");
        assertThat(mePerfil.path("anoFormacao").asInt()).isEqualTo(2018);
    }

    @Test
    @DisplayName("cliente autenticado não pode editar dados de advogado")
    void shouldForbidCliente() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("edit.cli.forbid@laweact.com", "52998224725")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/dados-gerais",
                Map.of("nomeCompleto", "Não deveria")
        );
        assertErrorCode(patch, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    private void authenticateAdvogado(String email, String cpf, String oabNumero) {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido(email, cpf, oabNumero)
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());
    }

    private static Map<String, Object> addressBody(
            String cep,
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String estado
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("cep", cep);
        body.put("logradouro", logradouro);
        body.put("numero", numero);
        body.put("complemento", complemento);
        body.put("bairro", bairro);
        body.put("cidade", cidade);
        body.put("estado", estado);
        return body;
    }

    private static Map<String, Object> oabBody(String numero, String uf, String dataExpedicao, List<String> fotosUrls) {
        Map<String, Object> body = new HashMap<>();
        body.put("numero", numero);
        body.put("uf", uf);
        body.put("dataExpedicao", dataExpedicao);
        body.put("fotosUrls", fotosUrls);
        return body;
    }

    private static JsonNode findOab(JsonNode oabs, boolean principal) {
        for (JsonNode oab : oabs) {
            if (oab.path("principal").asBoolean() == principal) {
                return oab;
            }
        }
        throw new AssertionError("OAB principal=" + principal + " não encontrada em " + oabs);
    }

    private static List<String> extractCodigos(JsonNode itens) {
        return itens.findValuesAsText("codigo");
    }
}
