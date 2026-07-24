package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorCode;
import static com.laweact.e2e.support.ApiAssertions.assertErrorDetailContains;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.dto.cliente.CadastrarClienteInputDTO;
import com.laweact.e2e.support.Fixtures;
import com.laweact.model.entity.ClienteEntity;
import com.laweact.model.entity.UsuarioEntity;
import com.laweact.model.enums.PerfilUsuarioEnum;
import com.laweact.model.enums.StatusUsuarioEnum;

@DisplayName("E2E — POST /clientes/cadastrar")
class ClienteCadastrarE2ETest extends BaseE2ETest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("deve criar cliente com sucesso (usuario + cliente + endereco) e retornar JWT")
    void shouldCreateClienteSuccessfully() {
        // Arrange
        String email = "maria.sucesso@laweact.com";
        String documento = "52998224725";
        CadastrarClienteInputDTO input = Fixtures.clienteValido(email, documento);

        // Act
        ResponseEntity<JsonNode> response = api.post("/clientes/cadastrar", input);

        // Assert — HTTP + envelope
        assertSuccess(response, HttpStatus.CREATED);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("token").asText()).isNotBlank();
        assertThat(data.path("usuario").path("email").asText()).isEqualTo(email);
        assertThat(data.path("usuario").path("perfil").asText()).isEqualTo("CLIENTE");
        assertThat(data.path("cliente").path("numeroDocumento").asText()).isEqualTo(documento);
        assertThat(data.path("endereco").path("cidade").asText()).isEqualTo("São Paulo");
        assertThat(data.path("endereco").path("complemento").asText()).isEqualTo("Apto 12");

        // Assert — persistência
        UsuarioEntity usuarioDb = usuarioRepository.findByEmail(email).orElseThrow();
        assertThat(usuarioDb.getId()).isNotNull();
        assertThat(usuarioDb.getNomeCompleto()).isEqualTo(input.nomeCompleto());
        assertThat(usuarioDb.getPerfil()).isEqualTo(PerfilUsuarioEnum.CLIENTE);
        assertThat(usuarioDb.getStatus()).isEqualTo(StatusUsuarioEnum.ATIVO);
        assertThat(passwordEncoder.matches(Fixtures.VALID_PASSWORD, usuarioDb.getSenha())).isTrue();

        ClienteEntity clienteDb = clienteRepository.findByUsuarioId(usuarioDb.getId()).orElseThrow();
        assertThat(clienteDb.getNumeroDocumento()).isEqualTo(documento);
        assertThat(clienteDb.getProfissao()).isEqualTo("Analista");

        Integer enderecos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM enderecos WHERE usuario_id = ?",
                Integer.class,
                usuarioDb.getId()
        );
        assertThat(enderecos).isEqualTo(1);
    }

    @Test
    @DisplayName("deve criar cliente PJ com CNPJ (razão social + área de atuação)")
    void shouldCreateClienteCnpjSuccessfully() {
        String email = "empresa.sucesso@laweact.com";
        String cnpj = "11222333000181";
        CadastrarClienteInputDTO input = Fixtures.clienteCnpjValido(email, cnpj);

        ResponseEntity<JsonNode> response = api.post("/clientes/cadastrar", input);

        assertSuccess(response, HttpStatus.CREATED);
        JsonNode data = response.getBody().path("data");
        assertThat(data.path("cliente").path("tipoDocumento").asText()).isEqualTo("CNPJ");
        assertThat(data.path("cliente").path("razaoSocial").asText()).isEqualTo("Empresa Exemplo LTDA");
        assertThat(data.path("cliente").path("areaAtuacao").asText()).isEqualTo("Tecnologia");
        assertThat(data.path("cliente").path("numeroDocumento").asText()).isEqualTo(cnpj);
        assertThat(data.path("endereco").path("complemento").asText()).isEqualTo("Sala 200");

        UsuarioEntity usuarioDb = usuarioRepository.findByEmail(email).orElseThrow();
        assertThat(usuarioDb.getNomeCompleto()).isEqualTo("Empresa Exemplo LTDA");
        ClienteEntity clienteDb = clienteRepository.findByUsuarioId(usuarioDb.getId()).orElseThrow();
        assertThat(clienteDb.getRazaoSocial()).isEqualTo("Empresa Exemplo LTDA");
        assertThat(clienteDb.getRg()).isNull();
    }

    @Test
    @DisplayName("deve exigir razão social e área de atuação para CNPJ")
    void shouldFailCnpjWithoutRazaoSocial() {
        CadastrarClienteInputDTO input = CadastrarClienteInputDTO.builder()
                .email("empresa.invalida@laweact.com")
                .senha(Fixtures.VALID_PASSWORD)
                .tipoDocumento(com.laweact.model.enums.TipoDocumentoEnum.CNPJ)
                .numeroDocumento("11222333000181")
                .pronomes(com.laweact.model.enums.PronomesEnum.NEUTRO)
                .telefone("1133334444")
                .cep("01310-100")
                .logradouro("Av. Paulista")
                .numero("1000")
                .bairro("Bela Vista")
                .cidade("São Paulo")
                .estado("SP")
                .aceiteTermos(true)
                .build();

        ResponseEntity<JsonNode> response = api.post("/clientes/cadastrar", input);

        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "razão social");
    }

    @Test
    @DisplayName("deve exigir área de atuação para CNPJ")
    void shouldFailCnpjWithoutAreaAtuacao() {
        CadastrarClienteInputDTO input = CadastrarClienteInputDTO.builder()
                .razaoSocial("Empresa Sem Area LTDA")
                .email("empresa.semarea@laweact.com")
                .senha(Fixtures.VALID_PASSWORD)
                .tipoDocumento(com.laweact.model.enums.TipoDocumentoEnum.CNPJ)
                .numeroDocumento("11222333000181")
                .pronomes(com.laweact.model.enums.PronomesEnum.NEUTRO)
                .telefone("1133334444")
                .cep("01310-100")
                .logradouro("Av. Paulista")
                .numero("1000")
                .bairro("Bela Vista")
                .cidade("São Paulo")
                .estado("SP")
                .aceiteTermos(true)
                .build();

        ResponseEntity<JsonNode> response = api.post("/clientes/cadastrar", input);

        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "área de atuação");
        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    @DisplayName("deve exigir campos de CPF (nome, RG, nascimento, profissão)")
    void shouldFailCpfWithoutRequiredFields() {
        CadastrarClienteInputDTO input = CadastrarClienteInputDTO.builder()
                .email("cpf.incompleto@laweact.com")
                .senha(Fixtures.VALID_PASSWORD)
                .tipoDocumento(com.laweact.model.enums.TipoDocumentoEnum.CPF)
                .numeroDocumento("52998224725")
                .pronomes(com.laweact.model.enums.PronomesEnum.ELA)
                .telefone("11999999999")
                .cep("01310-100")
                .logradouro("Av. Paulista")
                .numero("1000")
                .bairro("Bela Vista")
                .cidade("São Paulo")
                .estado("SP")
                .aceiteTermos(true)
                .build();

        ResponseEntity<JsonNode> response = api.post("/clientes/cadastrar", input);

        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "nome completo");
        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    @DisplayName("deve rejeitar CNPJ com tamanho inválido")
    void shouldFailWhenCnpjHasInvalidLength() {
        CadastrarClienteInputDTO input = Fixtures.clienteCnpjValido("empresa.cnpjinvalido@laweact.com", "123");

        ResponseEntity<JsonNode> response = api.post("/clientes/cadastrar", input);

        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "CNPJ deve conter 14 dígitos");
        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    @DisplayName("deve retornar erro se e-mail já estiver cadastrado")
    void shouldFailWhenEmailAlreadyTaken() {
        // Arrange
        String email = "maria.duplicada@laweact.com";
        api.post("/clientes/cadastrar", Fixtures.clienteValido(email, "11144477735"));

        // Act
        ResponseEntity<JsonNode> response = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido(email, "39053344705")
        );

        // Assert
        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "E-mail já cadastrado");
        assertThat(usuarioRepository.findByEmail(email)).isPresent();
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("deve retornar erro se documento já estiver cadastrado")
    void shouldFailWhenDocumentoAlreadyTaken() {
        // Arrange
        String documento = "52998224725";
        api.post("/clientes/cadastrar", Fixtures.clienteValido("primeira@laweact.com", documento));

        // Act
        ResponseEntity<JsonNode> response = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("segunda@laweact.com", documento)
        );

        // Assert
        assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "Documento já cadastrado");
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Nested
    @DisplayName("validações de entrada")
    class Validacoes {

        @Test
        @DisplayName("deve retornar erro se e-mail for inválido")
        void shouldFailWhenEmailIsInvalid() {
            // Arrange
            CadastrarClienteInputDTO input = Fixtures.clienteCom(
                    "email-invalido.com",
                    Fixtures.VALID_PASSWORD,
                    "52998224725"
            );

            // Act
            ResponseEntity<JsonNode> response = api.post("/clientes/cadastrar", input);

            // Assert
            assertErrorCode(response, HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_EMAIL");
            assertThat(usuarioRepository.count()).isZero();
        }

        @Test
        @DisplayName("deve retornar erro se senha tiver menos de 8 caracteres")
        void shouldFailWhenPasswordIsTooShort() {
            // Arrange
            CadastrarClienteInputDTO input = Fixtures.clienteCom(
                    "curta@laweact.com",
                    "Ab1",
                    "52998224725"
            );

            // Act
            ResponseEntity<JsonNode> response = api.post("/clientes/cadastrar", input);

            // Assert
            assertErrorDetailContains(response, HttpStatus.UNPROCESSABLE_ENTITY, "mínimo 8 caracteres");
            assertThat(usuarioRepository.count()).isZero();
        }

        @Test
        @DisplayName("deve retornar erro se senha não tiver número")
        void shouldFailWhenPasswordHasNoDigit() {
            // Arrange
            CadastrarClienteInputDTO input = Fixtures.clienteCom(
                    "semnumero@laweact.com",
                    "Password",
                    "52998224725"
            );

            // Act
            ResponseEntity<JsonNode> response = api.post("/clientes/cadastrar", input);

            // Assert
            assertErrorDetailContains(
                    response,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "1 letra maiúscula, 1 minúscula e 1 número"
            );
            assertThat(usuarioRepository.count()).isZero();
        }

        @Test
        @DisplayName("deve retornar erro se senha não tiver letra maiúscula")
        void shouldFailWhenPasswordHasNoUppercase() {
            // Arrange
            CadastrarClienteInputDTO input = Fixtures.clienteCom(
                    "semmaiuscula@laweact.com",
                    "secret12",
                    "52998224725"
            );

            // Act
            ResponseEntity<JsonNode> response = api.post("/clientes/cadastrar", input);

            // Assert
            assertErrorDetailContains(
                    response,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "1 letra maiúscula, 1 minúscula e 1 número"
            );
            assertThat(usuarioRepository.count()).isZero();
        }

        @Test
        @DisplayName("deve retornar erro se termos não forem aceitos")
        void shouldFailWhenTermsNotAccepted() {
            // Arrange
            CadastrarClienteInputDTO input = CadastrarClienteInputDTO.builder()
                    .nomeCompleto("Maria Silva")
                    .email("semtermos@laweact.com")
                    .senha(Fixtures.VALID_PASSWORD)
                    .profissao("Analista")
                    .tipoDocumento(com.laweact.model.enums.TipoDocumentoEnum.CPF)
                    .numeroDocumento("52998224725")
                    .rg("1234567")
                    .dataNascimento(java.time.LocalDate.of(1990, 5, 20))
                    .pronomes(com.laweact.model.enums.PronomesEnum.ELA)
                    .telefone("11999999999")
                    .cep("01310-100")
                    .logradouro("Av. Paulista")
                    .numero("1000")
                    .bairro("Bela Vista")
                    .cidade("São Paulo")
                    .estado("SP")
                    .aceiteTermos(false)
                    .build();

            // Act
            ResponseEntity<JsonNode> response = api.post("/clientes/cadastrar", input);

            // Assert
            assertErrorDetailContains(response, HttpStatus.BAD_REQUEST, "aceitar os termos");
            assertThat(usuarioRepository.count()).isZero();
        }
    }
}
