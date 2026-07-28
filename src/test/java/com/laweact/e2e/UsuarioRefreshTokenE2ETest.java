package com.laweact.e2e;

import static com.laweact.e2e.support.ApiAssertions.assertErrorDetailContains;
import static com.laweact.e2e.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laweact.dto.usuario.LoginUsuarioInputDTO;
import com.laweact.dto.usuario.RefreshTokenInputDTO;
import com.laweact.e2e.support.Fixtures;

@DisplayName("E2E — POST /usuarios/refresh")
class UsuarioRefreshTokenE2ETest extends BaseE2ETest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("login deve retornar access (~1h) e refresh (~7d)")
    void shouldReturnAccessAndRefreshOnLogin() throws Exception {
        String email = "refresh.login@laweact.com";
        api.post("/clientes/cadastrar", Fixtures.clienteValido(email, "52998224725"));

        ResponseEntity<JsonNode> login = api.post(
                "/usuarios/login",
                LoginUsuarioInputDTO.builder().email(email).senha(Fixtures.VALID_PASSWORD).build()
        );
        assertSuccess(login, HttpStatus.OK);

        String token = login.getBody().path("data").path("token").asText();
        String refresh = login.getBody().path("data").path("refreshToken").asText();
        assertThat(token).isNotBlank();
        assertThat(refresh).isNotBlank().isNotEqualTo(token);

        long accessTtlMs = ttlMs(token);
        long refreshTtlMs = ttlMs(refresh);
        assertThat(accessTtlMs).isBetween(3_500_000L, 3_700_000L);
        assertThat(refreshTtlMs).isBetween(600_000_000L, 610_000_000L);
        assertThat(refreshTtlMs).isGreaterThan(accessTtlMs * 100);
        assertThat(claim(token, "typ")).isEqualTo("access");
        assertThat(claim(refresh, "typ")).isEqualTo("refresh");
    }

    @Test
    @DisplayName("deve renovar o par de tokens com refresh válido")
    void shouldRefreshTokensSuccessfully() {
        String email = "refresh.ok@laweact.com";
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido(email, "11144477735")
        );
        String token = cadastro.getBody().path("data").path("token").asText();
        String refresh = cadastro.getBody().path("data").path("refreshToken").asText();

        ResponseEntity<JsonNode> response = api.post(
                "/usuarios/refresh",
                RefreshTokenInputDTO.builder().token(token).refreshToken(refresh).build()
        );

        assertSuccess(response, HttpStatus.OK);
        String newToken = response.getBody().path("data").path("token").asText();
        String newRefresh = response.getBody().path("data").path("refreshToken").asText();
        assertThat(newToken).isNotBlank().isNotEqualTo(token);
        assertThat(newRefresh).isNotBlank().isNotEqualTo(refresh);

        api.authenticate(newToken);
        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertSuccess(me, HttpStatus.OK);
        assertThat(me.getBody().path("data").path("usuario").path("email").asText()).isEqualTo(email);
    }

    @Test
    @DisplayName("refresh token não deve autenticar rotas protegidas")
    void shouldRejectRefreshTokenAsBearer() {
        String email = "refresh.bearer@laweact.com";
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido(email, "39053344705")
        );
        String refresh = cadastro.getBody().path("data").path("refreshToken").asText();

        api.authenticate(refresh);
        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        assertThat(me.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    @DisplayName("deve rejeitar refreshToken inválido")
    void shouldRejectInvalidRefreshToken() {
        String email = "refresh.invalid@laweact.com";
        ResponseEntity<JsonNode> cadastro = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido(email, "15350946056")
        );
        String token = cadastro.getBody().path("data").path("token").asText();

        ResponseEntity<JsonNode> response = api.post(
                "/usuarios/refresh",
                RefreshTokenInputDTO.builder().token(token).refreshToken("nao.e.um.jwt").build()
        );
        assertErrorDetailContains(response, HttpStatus.UNAUTHORIZED, "Refresh token inválido");
    }

    @Test
    @DisplayName("deve rejeitar quando token e refreshToken são de usuários diferentes")
    void shouldRejectMismatchedSubjects() {
        ResponseEntity<JsonNode> a = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("refresh.a@laweact.com", "71428793860")
        );
        ResponseEntity<JsonNode> b = api.post(
                "/clientes/cadastrar",
                Fixtures.clienteValido("refresh.b@laweact.com", "12345678909")
        );

        String tokenA = a.getBody().path("data").path("token").asText();
        String refreshB = b.getBody().path("data").path("refreshToken").asText();

        ResponseEntity<JsonNode> response = api.post(
                "/usuarios/refresh",
                RefreshTokenInputDTO.builder().token(tokenA).refreshToken(refreshB).build()
        );
        assertErrorDetailContains(response, HttpStatus.UNAUTHORIZED, "não correspondem");
    }

    private long ttlMs(String jwt) throws Exception {
        JsonNode payload = decodePayload(jwt);
        long exp = payload.path("exp").asLong() * 1000L;
        long iat = payload.path("iat").asLong() * 1000L;
        return exp - iat;
    }

    private String claim(String jwt, String name) throws Exception {
        return decodePayload(jwt).path(name).asText(null);
    }

    private JsonNode decodePayload(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        assertThat(parts).hasSize(3);
        byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
        return MAPPER.readTree(new String(decoded, StandardCharsets.UTF_8));
    }
}
