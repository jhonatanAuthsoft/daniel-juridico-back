package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorDetailContains;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.dto.advogado.CadastrarAdvogadoInputDTO;
import com.laweact.dto.advogado.EspecialidadeInputDTO;
import com.laweact.dto.advogado.OabInputDTO;
import com.laweact.dto.advogado.PosGraduacaoInputDTO;
import com.laweact.e2e.support.Fixtures;
import com.laweact.model.entity.AdvogadoEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusVerificacaoEnum;

@DisplayName("E2E — POST /advogados/cadastrar")
class AdvogadoCadastrarE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("deve criar advogado com sucesso (usuario + advogado + endereco + oab + area) e retornar JWT")
    void shouldCreateAdvogadoSuccessfully() {
        String email = "joao.sucesso@laweact.com";
        String cpf = "39053344705";
        String oab = "123456";
        CadastrarAdvogadoInputDTO input = Fixtures.advogadoValido(email, cpf, oab);

        ResponseEntity<JsonNode> response = api.post("/advogados/cadastrar", input);

        assertSuccess(response, HttpStatus.CREATED);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("token").asText()).isNotBlank();
        assertThat(data.path("usuario").path("perfil").asText()).isEqualTo("ADVOGADO");
        assertThat(data.path("usuario").path("termosAceitos").asBoolean()).isFalse();
        assertThat(data.path("advogado").path("cpf").asText()).isEqualTo(cpf);
        assertThat(data.path("advogado").path("statusVerificacao").asText()).isEqualTo("PENDENTE");
        assertThat(data.path("oabs")).hasSize(1);
        assertThat(data.path("oabs").get(0).path("principal").asBoolean()).isTrue();
        assertThat(data.path("oabs").get(0).path("dataExpedicao").asText()).isEqualTo("2016-03-15");
        assertThat(data.path("areasAtuacao")).hasSize(1);
        assertThat(data.path("modalidades").get(0).path("codigo").asText()).isEqualTo("GENERALISTA");
        assertThat(data.path("especialidades").get(0).path("especialidadeCodigo").asText()).isEqualTo("CIVIL");
        assertThat(data.path("formasCobranca")).hasSize(1);
        assertThat(data.path("endereco").path("complemento").asText()).isEqualTo("Conjunto 41");

        UsuarioEntity usuarioDb = usuarioRepository.findByEmail(email).orElseThrow();
        assertThat(usuarioDb.getPerfil()).isEqualTo(PerfilUsuarioEnum.ADVOGADO);

        AdvogadoEntity advogadoDb = advogadoRepository.findByUsuarioId(usuarioDb.getId()).orElseThrow();
        assertThat(advogadoDb.getCpf()).isEqualTo(cpf);
        assertThat(advogadoDb.getFotoUrl()).isEqualTo(input.fotoUrl());
        assertThat(advogadoDb.getStatusVerificacao()).isEqualTo(StatusVerificacaoEnum.PENDENTE);

        Integer oabs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oabs WHERE advogado_id = ?",
                Integer.class,
                usuarioDb.getId()
        );
        assertThat(oabs).isEqualTo(1);

        Integer areas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM areas_atuacao_advogado WHERE advogado_id = ?",
                Integer.class,
                usuarioDb.getId()
        );
        assertThat(areas).isEqualTo(1);
    }

    @Test
    @DisplayName("deve cadastrar até 5 OABs suplementares, pós-graduação e especialidade")
    void shouldCreateAdvogadoWithSuplementaresAndPosGraduacao() {
        CadastrarAdvogadoInputDTO base = Fixtures.advogadoValido("joao.completo@laweact.com", "39053344705", "100001");
        List<OabInputDTO> suplementares = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> Fixtures.oab("20000" + i, "RJ", LocalDate.of(2018, i, 10)))
                .toList();

        CadastrarAdvogadoInputDTO input = copyAdvogado(base)
                .oabsSuplementares(suplementares)
                .modalidades(List.of("CONSULTOR"))
                .especialidades(List.of(EspecialidadeInputDTO.builder().especialidadeCodigo("TRABALHISTA").build()))
                .formasCobranca(List.of("HONORARIOS_PERCENTUAIS", "OUTROS_A_COMBINAR"))
                .posGraduacoes(List.of(PosGraduacaoInputDTO.builder()
                        .nomeCurso("LLM Direito Digital")
                        .instituicao("FGV")
                        .anoFormacao(2020)
                        .build()))
                .build();

        ResponseEntity<JsonNode> response = api.post("/advogados/cadastrar", input);

        assertSuccess(response, HttpStatus.CREATED);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("oabs")).hasSize(6);
        assertThat(data.path("posGraduacoes")).hasSize(1);
        assertThat(data.path("posGraduacoes").get(0).path("nomeCurso").asText()).isEqualTo("LLM Direito Digital");
        assertThat(data.path("formasCobranca")).hasSize(2);
        assertThat(data.path("especialidades").get(0).path("especialidadeCodigo").asText()).isEqualTo("TRABALHISTA");

        UsuarioEntity usuarioDb = usuarioRepository.findByEmail(base.email()).orElseThrow();
        Integer oabs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oabs WHERE advogado_id = ?",
                Integer.class,
                usuarioDb.getId()
        );
        assertThat(oabs).isEqualTo(6);
    }

    @Test
    @DisplayName("deve rejeitar mais de 5 OABs suplementares")
    void shouldFailWhenMoreThanFiveSuplementares() {
        CadastrarAdvogadoInputDTO base = Fixtures.advogadoValido("joao.limite@laweact.com", "39053344705", "300001");
        List<OabInputDTO> suplementares = IntStream.rangeClosed(1, 6)
                .mapToObj(i -> Fixtures.oab("30000" + i, "MG", LocalDate.of(2019, 1, i)))
                .toList();

        CadastrarAdvogadoInputDTO input = copyAdvogado(base)
                .oabsSuplementares(suplementares)
                .build();

        ResponseEntity<JsonNode> response = api.post("/advogados/cadastrar", input);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody().path("success").asBoolean()).isFalse();
        assertThat(usuarioRepository.count()).isZero();
    }

    @Nested
    @DisplayName("modalidades e especialidades")
    class Modalidades {

        @Test
        @DisplayName("deve aceitar NENHUMA_DAS_ANTERIORES com especialidade livre")
        void shouldAcceptNenhumaComEspecialidadeLivre() {
            CadastrarAdvogadoInputDTO base = Fixtures.advogadoValido(
                    "joao.nenhuma@laweact.com",
                    "39053344705",
                    "400001"
            );
            CadastrarAdvogadoInputDTO input = copyAdvogado(base)
                    .modalidades(List.of("NENHUMA_DAS_ANTERIORES"))
                    .especialidades(List.of(EspecialidadeInputDTO.builder()
                            .especialidadeLivre("Direito Canábico")
                            .build()))
                    .build();

            ResponseEntity<JsonNode> response = api.post("/advogados/cadastrar", input);

            assertSuccess(response, HttpStatus.CREATED);
            JsonNode data = response.getBody().path("data");
            assertThat(data.path("modalidades").get(0).path("codigo").asText())
                    .isEqualTo("NENHUMA_DAS_ANTERIORES");
            assertThat(data.path("especialidades").get(0).path("especialidadeLivre").asText())
                    .isEqualTo("Direito Canábico");
        }

        @Test
        @DisplayName("deve rejeitar NENHUMA_DAS_ANTERIORES misturada com outras modalidades")
        void shouldFailWhenNenhumaMixedWithOthers() {
            CadastrarAdvogadoInputDTO base = Fixtures.advogadoValido(
                    "joao.misto@laweact.com",
                    "39053344705",
                    "400002"
            );
            CadastrarAdvogadoInputDTO input = copyAdvogado(base)
                    .modalidades(List.of("GENERALISTA", "NENHUMA_DAS_ANTERIORES"))
                    .build();

            ResponseEntity<JsonNode> response = api.post("/advogados/cadastrar", input);

            assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "não pode ser combinada");
            assertThat(usuarioRepository.count()).isZero();
        }

        @Test
        @DisplayName("deve exigir especialidade quando modalidade for NENHUMA_DAS_ANTERIORES")
        void shouldFailNenhumaWithoutEspecialidade() {
            CadastrarAdvogadoInputDTO base = Fixtures.advogadoValido(
                    "joao.semesp@laweact.com",
                    "39053344705",
                    "400003"
            );
            CadastrarAdvogadoInputDTO input = copyAdvogado(base)
                    .modalidades(List.of("NENHUMA_DAS_ANTERIORES"))
                    .especialidades(List.of())
                    .build();

            ResponseEntity<JsonNode> response = api.post("/advogados/cadastrar", input);

            assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "especialidade");
            assertThat(usuarioRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("foto de perfil")
    class FotoPerfil {

        @Test
        @DisplayName("deve rejeitar cadastro sem foto de perfil")
        void shouldFailWhenFotoUrlMissing() {
            CadastrarAdvogadoInputDTO input = copyAdvogado(
                    Fixtures.advogadoValido("joao.semfoto@laweact.com", "39053344705", "600001")
            )
                    .fotoUrl(null)
                    .build();

            ResponseEntity<JsonNode> response = api.post("/advogados/cadastrar", input);

            assertErrorDetailContains(response, HttpStatus.UNPROCESSABLE_ENTITY, "foto de perfil");
            assertThat(usuarioRepository.count()).isZero();
        }

        @Test
        @DisplayName("deve rejeitar cadastro com foto de perfil em branco")
        void shouldFailWhenFotoUrlBlank() {
            CadastrarAdvogadoInputDTO input = copyAdvogado(
                    Fixtures.advogadoValido("joao.fotobranco@laweact.com", "39053344705", "600002")
            )
                    .fotoUrl("   ")
                    .build();

            ResponseEntity<JsonNode> response = api.post("/advogados/cadastrar", input);

            assertErrorDetailContains(response, HttpStatus.UNPROCESSABLE_ENTITY, "foto de perfil");
            assertThat(usuarioRepository.count()).isZero();
        }
    }

    @Test
    @DisplayName("deve aceitar cadastro sem nome do pai")
    void shouldCreateAdvogadoWithoutNomePai() {
        CadastrarAdvogadoInputDTO base = Fixtures.advogadoValido(
                "joao.sempai@laweact.com",
                "39053344705",
                "500001"
        );
        CadastrarAdvogadoInputDTO input = copyAdvogado(base)
                .nomePai(null)
                .build();

        ResponseEntity<JsonNode> response = api.post("/advogados/cadastrar", input);

        assertSuccess(response, HttpStatus.CREATED);
        assertThat(response.getBody().path("data").path("advogado").path("nomePai").isNull()).isTrue();
    }

    @Test
    @DisplayName("deve aceitar especialidade com subespecialidade do catálogo completo")
    void shouldCreateAdvogadoWithSubespecialidadeFromCatalog() {
        CadastrarAdvogadoInputDTO base = Fixtures.advogadoValido(
                "joao.subesp@laweact.com",
                "39053344705",
                "500002"
        );
        CadastrarAdvogadoInputDTO input = copyAdvogado(base)
                .especialidades(List.of(
                        EspecialidadeInputDTO.builder()
                                .especialidadeCodigo("CIVIL")
                                .subespecialidadeCodigo("CONTRATOS")
                                .build(),
                        EspecialidadeInputDTO.builder()
                                .especialidadeCodigo("IMOBILIARIO")
                                .subespecialidadeCodigo("DESPEJO")
                                .build()
                ))
                .build();

        ResponseEntity<JsonNode> response = api.post("/advogados/cadastrar", input);

        assertSuccess(response, HttpStatus.CREATED);
        JsonNode especialidades = response.getBody().path("data").path("especialidades");
        assertThat(especialidades).hasSize(2);
        assertThat(especialidades.get(0).path("subespecialidadeCodigo").asText()).isEqualTo("CONTRATOS");
        assertThat(especialidades.get(1).path("especialidadeCodigo").asText()).isEqualTo("IMOBILIARIO");
        assertThat(especialidades.get(1).path("subespecialidadeCodigo").asText()).isEqualTo("DESPEJO");
    }

    @Test
    @DisplayName("deve retornar erro se e-mail já estiver cadastrado")
    void shouldFailWhenEmailAlreadyTaken() {
        String email = "joao.duplicado@laweact.com";
        api.post("/advogados/cadastrar", Fixtures.advogadoValido(email, "39053344705", "111111"));

        ResponseEntity<JsonNode> response = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido(email, "52998224725", "222222")
        );

        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "E-mail já cadastrado");
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("deve retornar erro se CPF já estiver cadastrado")
    void shouldFailWhenCpfAlreadyTaken() {
        String cpf = "39053344705";
        api.post("/advogados/cadastrar", Fixtures.advogadoValido("a@laweact.com", cpf, "111111"));

        ResponseEntity<JsonNode> response = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido("b@laweact.com", cpf, "222222")
        );

        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "CPF já cadastrado");
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("deve retornar erro se OAB já estiver cadastrada")
    void shouldFailWhenOabAlreadyTaken() {
        String oab = "123456";
        api.post("/advogados/cadastrar", Fixtures.advogadoValido("a@laweact.com", "39053344705", oab));

        ResponseEntity<JsonNode> response = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido("b@laweact.com", "52998224725", oab)
        );

        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "OAB já cadastrada");
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    private static CadastrarAdvogadoInputDTO.CadastrarAdvogadoInputDTOBuilder copyAdvogado(
            CadastrarAdvogadoInputDTO base
    ) {
        return CadastrarAdvogadoInputDTO.builder()
                .nomeCompleto(base.nomeCompleto())
                .email(base.email())
                .senha(base.senha())
                .rg(base.rg())
                .rgOrgaoEmissor(base.rgOrgaoEmissor())
                .rgUf(base.rgUf())
                .cpf(base.cpf())
                .nomePai(base.nomePai())
                .nomeMae(base.nomeMae())
                .pronomeTratamento(base.pronomeTratamento())
                .telefone(base.telefone())
                .fotoUrl(base.fotoUrl())
                .universidade(base.universidade())
                .curso(base.curso())
                .anoFormacao(base.anoFormacao())
                .atuacaoDesde(base.atuacaoDesde())
                .cep(base.cep())
                .logradouro(base.logradouro())
                .numero(base.numero())
                .complemento(base.complemento())
                .bairro(base.bairro())
                .cidade(base.cidade())
                .estado(base.estado())
                .oabPrincipal(base.oabPrincipal())
                .oabsSuplementares(base.oabsSuplementares())
                .areasAtuacao(base.areasAtuacao())
                .modalidades(base.modalidades())
                .especialidades(base.especialidades())
                .formasCobranca(base.formasCobranca())
                .posGraduacoes(base.posGraduacoes());
    }
}
