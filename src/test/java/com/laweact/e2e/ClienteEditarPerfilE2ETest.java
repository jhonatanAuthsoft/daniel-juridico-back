package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorCode;
import static com.laweact.e2e.support.ApiAssertions.assertErrorDetailContains;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.e2e.support.Fixtures;
import com.laweact.model.entity.ClienteEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PronomesEnum;

@DisplayName("E2E — edição de dados cadastrais do cliente autenticado")
class ClienteEditarPerfilE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("PATCH dados gerais atualiza o nome e GET /me reflete")
    void shouldUpdateGeneralDataName() {
        authenticateCliente("edit.nome@laweact.com", "52998224725");

        ResponseEntity<JsonNode> patch = api.patch(
                "/clientes/me/dados-gerais",
                Map.of("nomeCompleto", "Maria Silva Lima")
        );
        assertSuccess(patch, HttpStatus.OK);
        assertThat(patch.getBody().path("data").path("perfil").path("nomeCompleto").asText())
                .isEqualTo("Maria Silva Lima");
        assertThat(patch.getBody().path("data").path("perfil").path("numeroDocumento").asText())
                .isEqualTo("52998224725");

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        JsonNode data = me.getBody().path("data");
        assertThat(data.path("usuario").path("nomeCompleto").asText()).isEqualTo("Maria Silva Lima");
        assertThat(data.path("cliente").path("perfil").path("nomeCompleto").asText())
                .isEqualTo("Maria Silva Lima");

        UsuarioEntity usuario = usuarioRepository.findByEmail("edit.nome@laweact.com").orElseThrow();
        assertThat(usuario.getNomeCompleto()).isEqualTo("Maria Silva Lima");
        ClienteEntity cliente = clienteRepository.findByUsuarioId(usuario.getId()).orElseThrow();
        assertThat(cliente.getNomeCompleto()).isEqualTo("Maria Silva Lima");
        assertThat(cliente.getNumeroDocumento()).isEqualTo("52998224725");
        assertThat(cliente.getRg()).isEqualTo("1234567");
    }

    @Test
    @DisplayName("PATCH dados gerais em cliente PJ atualiza razão social")
    void shouldUpdateCnpjDisplayName() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteCnpjValido("edit.pj@laweact.com", "11222333000181")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> patch = api.patch(
                "/clientes/me/dados-gerais",
                Map.of("nomeCompleto", "Empresa Exemplo Atualizada LTDA")
        );
        assertSuccess(patch, HttpStatus.OK);
        JsonNode perfil = patch.getBody().path("data").path("perfil");
        assertThat(perfil.path("nomeCompleto").asText()).isEqualTo("Empresa Exemplo Atualizada LTDA");
        assertThat(perfil.path("razaoSocial").asText()).isEqualTo("Empresa Exemplo Atualizada LTDA");
        assertThat(perfil.path("tipoDocumento").asText()).isEqualTo("CNPJ");
        assertThat(perfil.path("numeroDocumento").asText()).isEqualTo("11222333000181");
    }

    @Test
    @DisplayName("PATCH dados gerais rejeita nome em branco")
    void shouldRejectBlankName() {
        authenticateCliente("edit.blank@laweact.com", "11144477735");

        ResponseEntity<JsonNode> patch = api.patch(
                "/clientes/me/dados-gerais",
                Map.of("nomeCompleto", "   ")
        );
        assertErrorDetailContains(patch, HttpStatus.UNPROCESSABLE_ENTITY, "nome");
    }

    @Test
    @DisplayName("PATCH endereço atualiza o cadastro e GET /me reflete")
    void shouldUpdateAddress() {
        authenticateCliente("edit.end@laweact.com", "15350946056");

        ResponseEntity<JsonNode> patch = api.patch("/clientes/me/endereco", addressBody(
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
        assertThat(me.getBody().path("data").path("cliente").path("endereco").path("logradouro").asText())
                .isEqualTo("Rua Augusta");
    }

    @Test
    @DisplayName("PATCH endereço aceita CEP sem hífen e persiste formatado")
    void shouldNormalizeCepOnAddressUpdate() {
        authenticateCliente("edit.cep@laweact.com", "39053344705");

        ResponseEntity<JsonNode> patch = api.patch("/clientes/me/endereco", addressBody(
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
        authenticateCliente("edit.cep.bad@laweact.com", "26153377050");

        ResponseEntity<JsonNode> patch = api.patch("/clientes/me/endereco", addressBody(
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
    @DisplayName("PATCH perfil pessoal atualiza pronomes, profissão, estado civil e renda")
    void shouldUpdatePersonalProfile() {
        authenticateCliente("edit.pessoal@laweact.com", "71428793860");

        Map<String, Object> body = new HashMap<>();
        body.put("pronomes", "ELE");
        body.put("profissao", "Designer");
        body.put("estadoCivil", "solteiro");
        body.put("faixaRenda", "2500");

        ResponseEntity<JsonNode> patch = api.patch("/clientes/me/perfil-pessoal", body);
        assertSuccess(patch, HttpStatus.OK);
        JsonNode perfil = patch.getBody().path("data").path("perfil");
        assertThat(perfil.path("pronomes").asText()).isEqualTo("ELE");
        assertThat(perfil.path("profissao").asText()).isEqualTo("Designer");
        assertThat(perfil.path("estadoCivil").asText()).isEqualTo("solteiro");
        assertThat(perfil.path("faixaRenda").asText()).isEqualTo("2500");

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        JsonNode mePerfil = me.getBody().path("data").path("cliente").path("perfil");
        assertThat(mePerfil.path("pronomes").asText()).isEqualTo("ELE");
        assertThat(mePerfil.path("profissao").asText()).isEqualTo("Designer");

        UsuarioEntity usuario = usuarioRepository.findByEmail("edit.pessoal@laweact.com").orElseThrow();
        ClienteEntity cliente = clienteRepository.findByUsuarioId(usuario.getId()).orElseThrow();
        assertThat(cliente.getPronomes()).isEqualTo(PronomesEnum.ELE);
        assertThat(cliente.getProfissao()).isEqualTo("Designer");
        assertThat(cliente.getEstadoCivil()).isEqualTo("solteiro");
        assertThat(cliente.getFaixaRenda()).isEqualTo("2500");
    }

    @Test
    @DisplayName("PATCH perfil pessoal em PJ atualiza área de atuação")
    void shouldUpdateCnpjPersonalProfile() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteCnpjValido("edit.pj.pessoal@laweact.com", "04252011000110")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        Map<String, Object> body = new HashMap<>();
        body.put("pronomes", "NEUTRO");
        body.put("areaAtuacao", "Consultoria jurídica");
        body.put("estadoCivil", "");
        body.put("faixaRenda", "");

        ResponseEntity<JsonNode> patch = api.patch("/clientes/me/perfil-pessoal", body);
        assertSuccess(patch, HttpStatus.OK);
        JsonNode perfil = patch.getBody().path("data").path("perfil");
        assertThat(perfil.path("areaAtuacao").asText()).isEqualTo("Consultoria jurídica");
        assertThat(perfil.path("pronomes").asText()).isEqualTo("NEUTRO");
        assertThat(perfil.path("estadoCivil").isNull()).isTrue();
        assertThat(perfil.path("faixaRenda").isNull()).isTrue();
    }

    @Test
    @DisplayName("advogado autenticado não pode editar dados de cliente")
    void shouldForbidLawyer() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido("edit.adv@laweact.com", "39053344705", "112233")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());
        garantirAssinaturaSeAdvogado();

        ResponseEntity<JsonNode> patch = api.patch(
                "/clientes/me/dados-gerais",
                Map.of("nomeCompleto", "Não deveria")
        );
        assertErrorCode(patch, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    private void authenticateCliente(String email, String documento) {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido(email, documento)
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
}
