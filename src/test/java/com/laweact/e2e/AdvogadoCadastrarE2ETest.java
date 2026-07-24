package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorDetailContains;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.dto.advogado.CadastrarAdvogadoInputDTO;
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
        // Arrange
        String email = "joao.sucesso@laweact.com";
        String cpf = "39053344705";
        String oab = "123456";
        CadastrarAdvogadoInputDTO input = Fixtures.advogadoValido(email, cpf, oab);

        // Act
        ResponseEntity<JsonNode> response = api.post("/advogados/cadastrar", input);

        // Assert — HTTP
        assertSuccess(response, HttpStatus.CREATED);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("token").asText()).isNotBlank();
        assertThat(data.path("usuario").path("perfil").asText()).isEqualTo("ADVOGADO");
        assertThat(data.path("advogado").path("cpf").asText()).isEqualTo(cpf);
        assertThat(data.path("advogado").path("statusVerificacao").asText()).isEqualTo("PENDENTE");
        assertThat(data.path("oabs")).hasSize(1);
        assertThat(data.path("oabs").get(0).path("principal").asBoolean()).isTrue();
        assertThat(data.path("oabs").get(0).path("dataExpedicao").asText()).isEqualTo("2016-03-15");
        assertThat(data.path("areasAtuacao")).hasSize(1);
        assertThat(data.path("modalidades").get(0).path("codigo").asText()).isEqualTo("GENERALISTA");
        assertThat(data.path("formasCobranca")).hasSize(1);
        assertThat(data.path("endereco").path("complemento").asText()).isEqualTo("Conjunto 41");

        // Assert — DB
        UsuarioEntity usuarioDb = usuarioRepository.findByEmail(email).orElseThrow();
        assertThat(usuarioDb.getPerfil()).isEqualTo(PerfilUsuarioEnum.ADVOGADO);

        AdvogadoEntity advogadoDb = advogadoRepository.findByUsuarioId(usuarioDb.getId()).orElseThrow();
        assertThat(advogadoDb.getCpf()).isEqualTo(cpf);
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
    @DisplayName("deve retornar erro se e-mail já estiver cadastrado")
    void shouldFailWhenEmailAlreadyTaken() {
        // Arrange
        String email = "joao.duplicado@laweact.com";
        api.post("/advogados/cadastrar", Fixtures.advogadoValido(email, "39053344705", "111111"));

        // Act
        ResponseEntity<JsonNode> response = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido(email, "52998224725", "222222")
        );

        // Assert
        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "E-mail já cadastrado");
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("deve retornar erro se CPF já estiver cadastrado")
    void shouldFailWhenCpfAlreadyTaken() {
        // Arrange
        String cpf = "39053344705";
        api.post("/advogados/cadastrar", Fixtures.advogadoValido("a@laweact.com", cpf, "111111"));

        // Act
        ResponseEntity<JsonNode> response = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido("b@laweact.com", cpf, "222222")
        );

        // Assert
        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "CPF já cadastrado");
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("deve retornar erro se OAB já estiver cadastrada")
    void shouldFailWhenOabAlreadyTaken() {
        // Arrange
        String oab = "123456";
        api.post("/advogados/cadastrar", Fixtures.advogadoValido("a@laweact.com", "39053344705", oab));

        // Act
        ResponseEntity<JsonNode> response = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido("b@laweact.com", "52998224725", oab)
        );

        // Assert
        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "OAB já cadastrada");
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("deve retornar erro se termos não forem aceitos")
    void shouldFailWhenTermsNotAccepted() {
        // Arrange
        CadastrarAdvogadoInputDTO input = Fixtures.advogadoValido(
                "semtermos.adv@laweact.com",
                "39053344705",
                "999999"
        );
        input = CadastrarAdvogadoInputDTO.builder()
                .nomeCompleto(input.nomeCompleto())
                .email(input.email())
                .senha(input.senha())
                .rg(input.rg())
                .rgOrgaoEmissor(input.rgOrgaoEmissor())
                .rgUf(input.rgUf())
                .cpf(input.cpf())
                .nomePai(input.nomePai())
                .nomeMae(input.nomeMae())
                .pronomeTratamento(input.pronomeTratamento())
                .telefone(input.telefone())
                .universidade(input.universidade())
                .curso(input.curso())
                .anoFormacao(input.anoFormacao())
                .atuacaoDesde(input.atuacaoDesde())
                .cep(input.cep())
                .logradouro(input.logradouro())
                .numero(input.numero())
                .bairro(input.bairro())
                .cidade(input.cidade())
                .estado(input.estado())
                .oabPrincipal(input.oabPrincipal())
                .areasAtuacao(input.areasAtuacao())
                .modalidades(input.modalidades())
                .especialidades(input.especialidades())
                .formasCobranca(input.formasCobranca())
                .aceiteTermos(false)
                .build();

        // Act
        ResponseEntity<JsonNode> response = api.post("/advogados/cadastrar", input);

        // Assert
        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "aceitar os termos");
        assertThat(usuarioRepository.count()).isZero();
    }
}
