package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorCode;
import static com.laweact.e2e.support.ApiAssertions.assertErrorDetailContains;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.e2e.support.Fixtures;

@DisplayName("E2E — foto de perfil do usuário autenticado")
class UsuarioFotoPerfilE2ETest extends BaseE2ETest {

    private static final String CLIENTE_KEY =
            "tmp/clientes/perfil/11111111-2222-3333-4444-555555555555.jpg";
    private static final String ADVOGADO_KEY =
            "tmp/advogados/perfil/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee.png";

    @Test
    @DisplayName("PATCH foto do cliente → GET /me reflete a nova key")
    void shouldUpdateClientProfilePhoto() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("foto.cli@laweact.com", "52998224725")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> patch = api.patch(
                "/usuarios/me/foto",
                Map.of("fotoUrl", CLIENTE_KEY)
        );
        assertSuccess(patch, HttpStatus.OK);
        assertThat(patch.getBody().path("data").path("fotoUrl").asText()).isEqualTo(CLIENTE_KEY);

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        assertThat(me.getBody().path("data").path("cliente").path("perfil").path("fotoUrl").asText())
                .isEqualTo(CLIENTE_KEY);
    }

    @Test
    @DisplayName("PATCH foto do advogado → GET /me reflete a nova key")
    void shouldUpdateLawyerProfilePhoto() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/advogados/cadastrar",
                Fixtures.advogadoValido("foto.adv@laweact.com", "39053344705", "880001")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> patch = api.patch(
                "/usuarios/me/foto",
                Map.of("fotoUrl", ADVOGADO_KEY)
        );
        assertSuccess(patch, HttpStatus.OK);
        assertThat(patch.getBody().path("data").path("fotoUrl").asText()).isEqualTo(ADVOGADO_KEY);

        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        assertThat(me.getBody().path("data").path("advogado").path("perfil").path("fotoUrl").asText())
                .isEqualTo(ADVOGADO_KEY);
    }

    @Test
    @DisplayName("rejeita foto em branco")
    void shouldRejectBlankPhoto() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("foto.blank@laweact.com", "11144477735")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> patch = api.patch(
                "/usuarios/me/foto",
                Map.of("fotoUrl", "   ")
        );
        assertErrorDetailContains(patch, HttpStatus.UNPROCESSABLE_ENTITY, "foto");
    }

    @Test
    @DisplayName("rejeita key de outro tipo de perfil")
    void shouldRejectKeyFromOtherRole() {
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("foto.wrong@laweact.com", "15350946056")
        );
        assertSuccess(cadastro, HttpStatus.CREATED);
        api.authenticate(cadastro.getBody().path("data").path("token").asText());

        ResponseEntity<JsonNode> patch = api.patch(
                "/usuarios/me/foto",
                Map.of("fotoUrl", ADVOGADO_KEY)
        );
        assertErrorCode(patch, HttpStatus.BAD_REQUEST, "INVALID_OBJECT_KEY");
    }
}
