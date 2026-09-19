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
                Map.of(
                        "nomeCompleto", "João Advogado Lima",
                        "telefone", "(11) 97777-6666",
                        "dataNascimento", "1988-03-12"
                )
        );
        assertSuccess(patch, HttpStatus.OK);
        assertThat(patch.getBody().path("data").path("perfil").path("nomeCompleto").asText())
                .isEqualTo("João Advogado Lima");
        assertThat(patch.getBody().path("data").path("perfil").path("cpf").asText())
                .isEqualTo("39053344705");
        assertThat(patch.getBody().path("data").path("perfil").path("dataNascimento").asText())
                .isEqualTo("1988-03-12");

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        JsonNode data = me.getBody().path("data");
        assertThat(data.path("usuario").path("nomeCompleto").asText()).isEqualTo("João Advogado Lima");
        assertThat(data.path("usuario").path("telefone").asText()).isEqualTo("11977776666");
        assertThat(data.path("advogado").path("perfil").path("nomeCompleto").asText())
                .isEqualTo("João Advogado Lima");
        assertThat(data.path("advogado").path("perfil").path("dataNascimento").asText())
                .isEqualTo("1988-03-12");

        UsuarioEntity usuario = usuarioRepository.findByEmail("edit.adv.nome@laweact.com").orElseThrow();
        assertThat(usuario.getNomeCompleto()).isEqualTo("João Advogado Lima");
        assertThat(usuario.getTelefone()).isEqualTo("11977776666");
        AdvogadoEntity advogado = advogadoRepository.findByUsuarioId(usuario.getId()).orElseThrow();
        assertThat(advogado.getNomeCompleto()).isEqualTo("João Advogado Lima");
        assertThat(advogado.getCpf()).isEqualTo("39053344705");
        assertThat(advogado.getDataNascimento()).isEqualTo(java.time.LocalDate.of(1988, 3, 12));
    }

    @Test
    @DisplayName("PATCH dados gerais rejeita nome em branco")
    void shouldRejectBlankName() {
        authenticateAdvogado("edit.adv.blank@laweact.com", "52998224725", "810002");

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/dados-gerais",
                Map.of(
                        "nomeCompleto", "   ",
                        "telefone", "11988887777"
                )
        );
        assertErrorDetailContains(patch, HttpStatus.UNPROCESSABLE_ENTITY, "nome");
    }

    @Test
    @DisplayName("PATCH dados gerais rejeita telefone em branco")
    void shouldRejectBlankPhone() {
        authenticateAdvogado("edit.adv.blank.phone@laweact.com", "12345678909", "810017");

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/dados-gerais",
                Map.of(
                        "nomeCompleto", "João Advogado",
                        "telefone", "   "
                )
        );
        assertErrorDetailContains(patch, HttpStatus.UNPROCESSABLE_ENTITY, "telefone");
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
    @DisplayName("PATCH áreas de atuação substitui a lista e GET /me reflete")
    void shouldUpdateServiceAreas() {
        authenticateAdvogado("edit.adv.areas@laweact.com", "28001238938", "810015");

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/areas-atuacao",
                Map.of("areasAtuacao", List.of(
                        Map.of("estado", "SP", "cidade", "Adamantina"),
                        Map.of("estado", "SP", "cidade", "Avaré"),
                        Map.of("estado", "BA", "cidade", "Salvador")
                ))
        );
        assertSuccess(patch, HttpStatus.OK);
        JsonNode areas = patch.getBody().path("data").path("areasAtuacao");
        assertThat(areas).hasSize(3);
        assertThat(areas.findValuesAsText("cidade"))
                .containsExactlyInAnyOrder("Adamantina", "Avaré", "Salvador");

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        assertThat(me.getBody().path("data").path("advogado").path("areasAtuacao").findValuesAsText("cidade"))
                .containsExactlyInAnyOrder("Adamantina", "Avaré", "Salvador");
    }

    @Test
    @DisplayName("PATCH áreas de atuação aceita todo o estado sem cidade")
    void shouldUpdateEntireStateServiceArea() {
        authenticateAdvogado("edit.adv.areas.estado@laweact.com", "28001238938", "810043");

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/areas-atuacao",
                Map.of("areasAtuacao", List.of(
                        Map.of("estado", "SP", "todoEstado", true)
                ))
        );
        assertSuccess(patch, HttpStatus.OK);
        JsonNode areas = patch.getBody().path("data").path("areasAtuacao");
        assertThat(areas).hasSize(1);
        assertThat(areas.get(0).path("estado").asText()).isEqualTo("SP");
        assertThat(areas.get(0).path("todoEstado").asBoolean()).isTrue();
        assertThat(areas.get(0).path("cidade").asText("")).isEmpty();
    }

    @Test
    @DisplayName("PATCH áreas de atuação rejeita lista vazia")
    void shouldRejectEmptyServiceAreas() {
        authenticateAdvogado("edit.adv.areas.empty@laweact.com", "26153377050", "810016");

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/areas-atuacao",
                Map.of("areasAtuacao", List.of())
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
        body.put("oabPrincipal", oabBody("810010", "SP", "2016-03-15", List.of("tmp/oab/frente.jpg", "tmp/oab/verso.jpg")));
        body.put("oabsSuplementares", List.of(oabBody("910010", "RJ", "2018-01-20", List.of("tmp/oab/frente2.jpg", "tmp/oab/verso2.jpg"))));

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
        assertThat(patch.getBody().path("data").path("perfil").path("atuacaoDesde").asText())
                .isEqualTo("2016-03-15");
    }

    @Test
    @DisplayName("PATCH documentação rejeita OAB já cadastrada por outro advogado")
    void shouldRejectDuplicateOabFromAnotherLawyer() {
        api.post("/advogados/cadastrar", Fixtures.advogadoValido("edit.adv.oab.dono@laweact.com", "26153377050", "810011"));
        authenticateAdvogado("edit.adv.oab.outro@laweact.com", "71428793860", "810012");

        Map<String, Object> body = new HashMap<>();
        body.put("oabPrincipal", oabBody("810011", "SP", "2016-03-15", List.of("tmp/oab/frente.jpg", "tmp/oab/verso.jpg")));

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
    @DisplayName("PATCH modalidades substitui a lista e GET /me reflete")
    void shouldUpdateModalidades() {
        authenticateAdvogado("edit.adv.mod@laweact.com", "39053344705", "810018");

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/modalidades",
                Map.of("modalidades", List.of("CONSULTOR", "PAUTISTA"))
        );
        assertSuccess(patch, HttpStatus.OK);
        assertThat(extractCodigos(patch.getBody().path("data").path("modalidades")))
                .containsExactlyInAnyOrder("CONSULTOR", "PAUTISTA");

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        assertThat(extractCodigos(me.getBody().path("data").path("advogado").path("modalidades")))
                .containsExactlyInAnyOrder("CONSULTOR", "PAUTISTA");
    }

    @Test
    @DisplayName("PATCH modalidades rejeita lista vazia")
    void shouldRejectEmptyModalidades() {
        authenticateAdvogado("edit.adv.mod.empty@laweact.com", "52998224725", "810019");

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/modalidades",
                Map.of("modalidades", List.of())
        );
        assertThat(patch.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    @DisplayName("PATCH modalidades rejeita NENHUMA_DAS_ANTERIORES misturada com outras")
    void shouldRejectMixedNenhumaModalidade() {
        authenticateAdvogado("edit.adv.mod.mix@laweact.com", "15350946056", "810020");

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/modalidades",
                Map.of("modalidades", List.of("GENERALISTA", "NENHUMA_DAS_ANTERIORES"))
        );
        assertErrorDetailContains(patch, HttpStatus.BAD_REQUEST, "Nenhuma das anteriores");
    }

    @Test
    @DisplayName("PATCH especialidades substitui a lista e GET /me reflete")
    void shouldUpdateEspecialidades() {
        authenticateAdvogado("edit.adv.esp@laweact.com", "11144477735", "810021");

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/especialidades",
                Map.of("especialidades", List.of(
                        Map.of(
                                "especialidadeCodigo", "CIVIL",
                                "subespecialidadeCodigo", "CONTRATOS"
                        ),
                        Map.of("especialidadeCodigo", "TRABALHISTA")
                ))
        );
        assertSuccess(patch, HttpStatus.OK);
        JsonNode especialidades = patch.getBody().path("data").path("especialidades");
        assertThat(especialidades).hasSize(2);
        assertThat(especialidades.findValuesAsText("especialidadeCodigo"))
                .containsExactlyInAnyOrder("CIVIL", "TRABALHISTA");
        assertThat(especialidades.findValuesAsText("subespecialidadeCodigo"))
                .contains("CONTRATOS");

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        assertThat(me.getBody().path("data").path("advogado").path("especialidades")
                .findValuesAsText("especialidadeCodigo"))
                .containsExactlyInAnyOrder("CIVIL", "TRABALHISTA");
    }

    @Test
    @DisplayName("PATCH especialidades rejeita lista vazia quando a modalidade é NENHUMA_DAS_ANTERIORES")
    void shouldRejectEmptyEspecialidadesWhenNenhuma() {
        authenticateAdvogado("edit.adv.esp.none.setup@laweact.com", "26153377050", "810022");
        ResponseEntity<JsonNode> toNenhuma = api.patch(
                "/advogados/me/modalidades",
                Map.of("modalidades", List.of("NENHUMA_DAS_ANTERIORES"))
        );
        assertSuccess(toNenhuma, HttpStatus.OK);

        ResponseEntity<JsonNode> patch = api.patch(
                "/advogados/me/especialidades",
                Map.of("especialidades", List.of())
        );
        assertErrorDetailContains(patch, HttpStatus.BAD_REQUEST, "especialidade");
    }

    @Test
    @DisplayName("PATCH graduação atualiza universidade, curso, ano e pós-graduações")
    void shouldUpdateGraduation() {
        authenticateAdvogado("edit.adv.grad@laweact.com", "39053344705", "810013");

        ResponseEntity<JsonNode> patch = api.patch("/advogados/me/graduacao", Map.of(
                "universidade", "PUC-SP",
                "curso", "Direito",
                "anoFormacao", 2018,
                "posGraduacoes", List.of(Map.of(
                        "nomeCurso", "LLM Direito Digital",
                        "instituicao", "FGV",
                        "anoFormacao", 2020
                ))
        ));
        assertSuccess(patch, HttpStatus.OK);
        JsonNode perfil = patch.getBody().path("data").path("perfil");
        assertThat(perfil.path("universidade").asText()).isEqualTo("PUC-SP");
        assertThat(perfil.path("curso").asText()).isEqualTo("Direito");
        assertThat(perfil.path("anoFormacao").asInt()).isEqualTo(2018);
        JsonNode pos = patch.getBody().path("data").path("posGraduacoes");
        assertThat(pos).hasSize(1);
        assertThat(pos.get(0).path("nomeCurso").asText()).isEqualTo("LLM Direito Digital");
        assertThat(pos.get(0).path("instituicao").asText()).isEqualTo("FGV");
        assertThat(pos.get(0).path("anoFormacao").asInt()).isEqualTo(2020);

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        JsonNode mePerfil = me.getBody().path("data").path("advogado").path("perfil");
        assertThat(mePerfil.path("universidade").asText()).isEqualTo("PUC-SP");
        assertThat(mePerfil.path("anoFormacao").asInt()).isEqualTo(2018);
        assertThat(me.getBody().path("data").path("advogado").path("posGraduacoes").get(0).path("instituicao").asText())
                .isEqualTo("FGV");
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
                Map.of(
                        "nomeCompleto", "Não deveria",
                        "telefone", "11999999999"
                )
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
        garantirAssinaturaSeAdvogado();
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
