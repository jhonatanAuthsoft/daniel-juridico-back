package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorDetailContains;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.e2e.support.Fixtures;

@DisplayName("E2E — POST /usuarios/login")
class UsuarioLoginE2ETest extends BaseE2ETest {

    @Test
    @DisplayName("deve autenticar cliente com e-mail e senha válidos e retornar JWT")
    void shouldLoginClienteSuccessfully() {
        // Arrange
        String email = "login.cliente@laweact.com";
        api.post("/clientes/cadastrar", Fixtures.clienteValido(email, "52998224725"));
        LoginUsuarioInputDTO login = LoginUsuarioInputDTO.builder()
                .email(email)
                .senha(Fixtures.VALID_PASSWORD)
                .build();

        // Act
        ResponseEntity<JsonNode> response = api.post("/usuarios/login", login);

        // Assert
        assertSuccess(response, HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("token").asText()).isNotBlank();
        assertThat(data.path("usuario").path("email").asText()).isEqualTo(email);
        assertThat(data.path("usuario").path("perfil").asText()).isEqualTo("CLIENTE");
        assertThat(data.path("usuario").path("termosAceitos").asBoolean()).isFalse();
        assertThat(data.path("cliente").path("perfil").path("tipoDocumento").asText()).isEqualTo("CPF");
        assertThat(data.path("advogado").isMissingNode() || data.path("advogado").isNull()).isTrue();
    }

    @Test
    @DisplayName("deve autenticar advogado com e-mail e senha válidos e retornar JWT")
    void shouldLoginAdvogadoSuccessfully() {
        // Arrange
        String email = "login.advogado@laweact.com";
        api.post("/advogados/cadastrar", Fixtures.advogadoValido(email, "39053344705", "555555"));
        LoginUsuarioInputDTO login = LoginUsuarioInputDTO.builder()
                .email(email)
                .senha(Fixtures.VALID_PASSWORD)
                .build();

        // Act
        ResponseEntity<JsonNode> response = api.post("/usuarios/login", login);

        // Assert
        assertSuccess(response, HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("token").asText()).isNotBlank();
        assertThat(data.path("usuario").path("perfil").asText()).isEqualTo("ADVOGADO");
        assertThat(data.path("advogado").path("perfil").path("cpf").asText()).isEqualTo("39053344705");
        assertThat(data.path("advogado").path("oabs").get(0).path("dataExpedicao").asText()).isEqualTo("2016-03-15");
        assertThat(data.path("cliente").isMissingNode() || data.path("cliente").isNull()).isTrue();
    }

    @Test
    @DisplayName("deve autenticar cliente CNPJ e retornar detalhe do perfil PJ")
    void shouldLoginClienteCnpjWithPerfil() {
        String email = "login.empresa@laweact.com";
        api.post("/clientes/cadastrar", Fixtures.clienteCnpjValido(email, "11222333000181"));

        ResponseEntity<JsonNode> response = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder().email(email).senha(Fixtures.VALID_PASSWORD).build()
        );

        assertSuccess(response, HttpStatus.OK);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("usuario").path("perfil").asText()).isEqualTo("CLIENTE");
        assertThat(data.path("cliente").path("perfil").path("tipoDocumento").asText()).isEqualTo("CNPJ");
        assertThat(data.path("cliente").path("perfil").path("razaoSocial").asText())
                .isEqualTo("Empresa Exemplo LTDA");
        assertThat(data.path("cliente").path("endereco").path("complemento").asText()).isEqualTo("Sala 200");
    }

    @Test
    @DisplayName("deve permitir usar o JWT do login em GET /usuarios/me")
    void shouldAccessMeWithLoginToken() {
        // Arrange
        String email = "login.me@laweact.com";
        api.post("/clientes/cadastrar", Fixtures.clienteValido(email, "11144477735"));
        ResponseEntity<JsonNode> loginResponse = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder().email(email).senha(Fixtures.VALID_PASSWORD).build()
        );
        String token = loginResponse.getBody().path("data").path("token").asText();
        api.authenticate(token);

        // Act
        ResponseEntity<JsonNode> meResponse = api.get("/usuarios/me");

        // Assert
        assertSuccess(meResponse, HttpStatus.OK);
        assertThat(meResponse.getBody().path("data").path("usuario").path("email").asText()).isEqualTo(email);
        assertThat(meResponse.getBody().path("data").path("cliente").path("perfil").path("nomeCompleto").asText())
                .isEqualTo("Maria Silva");
    }

    @Test
    @DisplayName("deve retornar detalhe do advogado em GET /usuarios/me")
    void shouldAccessMeAsAdvogadoWithPerfilDetalhe() {
        String email = "login.me.adv@laweact.com";
        api.post("/advogados/cadastrar", Fixtures.advogadoValido(email, "39053344705", "777777"));
        ResponseEntity<JsonNode> loginResponse = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder().email(email).senha(Fixtures.VALID_PASSWORD).build()
        );
        api.authenticate(loginResponse.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> meResponse = api.get("/usuarios/me");

        assertSuccess(meResponse, HttpStatus.OK);
        JsonNode data = meResponse.getBody().path("data");
        assertThat(data.path("usuario").path("perfil").asText()).isEqualTo("ADVOGADO");
        assertThat(data.path("advogado").path("perfil").path("cpf").asText()).isEqualTo("39053344705");
        assertThat(data.path("advogado").path("oabs").get(0).path("dataExpedicao").asText()).isEqualTo("2016-03-15");
        assertThat(data.path("advogado").path("modalidades")).isNotEmpty();
        assertThat(data.path("cliente").isMissingNode() || data.path("cliente").isNull()).isTrue();
    }

    @Test
    @DisplayName("deve retornar erro genérico se a senha estiver incorreta")
    void shouldFailWhenPasswordIsWrong() {
        // Arrange
        String email = "login.senhaerrada@laweact.com";
        api.post("/clientes/cadastrar", Fixtures.clienteValido(email, "52998224725"));

        // Act
        ResponseEntity<JsonNode> response = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder().email(email).senha("WrongPass1").build()
        );

        // Assert
        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "E-mail ou senha inválidos");
    }

    @Test
    @DisplayName("deve retornar erro genérico se o e-mail não existir")
    void shouldFailWhenEmailDoesNotExist() {
        // Arrange
        LoginUsuarioInputDTO login = LoginUsuarioInputDTO.builder()
                .email("naoexiste@laweact.com")
                .senha(Fixtures.VALID_PASSWORD)
                .build();

        // Act
        ResponseEntity<JsonNode> response = api.post("/usuarios/login", login);

        // Assert
        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "E-mail ou senha inválidos");
    }

    @Test
    @DisplayName("deve retornar erro de validação se e-mail estiver em branco")
    void shouldFailWhenEmailIsBlank() {
        // Arrange
        Map<String, String> body = Map.of("email", "", "senha", Fixtures.VALID_PASSWORD);

        // Act
        ResponseEntity<JsonNode> response = api.post("/usuarios/login", body);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().path("success").asBoolean()).isFalse();
    }
}
