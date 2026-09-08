package com.laweact.e2e;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.JsonNode;
import com.laweact.e2e.support.ApiClient;
import com.laweact.e2e.support.DatabaseCleaner;
import com.laweact.repository.AdvogadoRepository;
import com.laweact.repository.ClienteRepository;
import com.laweact.repository.UsuarioRepository;

/**
 * Um único Postgres para toda a suíte E2E.
 * Isolamento entre testes = TRUNCATE (DatabaseCleaner), sem destruir o container.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(DatabaseCleaner.class)
public abstract class BaseE2ETest {

    /**
     * Singleton: sobe uma vez na JVM e fica vivo até o fim do {@code ./mvnw test}.
     * Não usamos {@code @Container}/{@code @Testcontainers} para evitar restart por classe.
     */
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = createAndStartPostgres();

    private static PostgreSQLContainer<?> createAndStartPostgres() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("laweact_test")
                .withUsername("laweact")
                .withPassword("laweact");
        container.start();
        return container;
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected DatabaseCleaner databaseCleaner;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected ClienteRepository clienteRepository;

    @Autowired
    protected AdvogadoRepository advogadoRepository;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected ApiClient api;

    @BeforeEach
    void setUpE2E() {
        api = new ApiClient(restTemplate);
        api.logout();
        databaseCleaner.clear();
    }

    @AfterEach
    void tearDownE2E() {
        api.logout();
        databaseCleaner.clear();
    }

    /**
     * Advogado nasce no paywall (assinatura {@code PENDENTE}): o mês grátis é concedido pela
     * loja, não pelo servidor. Chame depois de autenticar para que o usuário atual saia do
     * paywall. Não faz nada para cliente ou para quem já tem acesso, então é seguro deixar
     * nos helpers genéricos de autenticação.
     */
    protected void garantirAssinaturaSeAdvogado() {
        ResponseEntity<JsonNode> me = api.get("/usuarios/me");
        if (!me.getStatusCode().is2xxSuccessful() || me.getBody() == null) {
            return;
        }
        JsonNode data = me.getBody().path("data");
        if (!"ADVOGADO".equals(data.path("usuario").path("perfil").asText())) {
            return;
        }
        if (data.path("assinatura").path("acessoLiberado").asBoolean()) {
            return;
        }
        assinarPlanoDoAdvogadoAutenticado();
    }

    /**
     * Assina o plano pela loja fake, como o app faz na tela de pagamento.
     */
    protected void assinarPlanoDoAdvogadoAutenticado() {
        ResponseEntity<JsonNode> validar = api.post(
                "/assinaturas/validar",
                Map.of(
                        "plataforma", "FAKE",
                        "productId", "laweact_basic_mensal",
                        "purchaseToken", "fake:e2e-" + UUID.randomUUID()
                )
        );
        if (!validar.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Falha ao assinar plano no E2E: " + validar.getBody());
        }
    }
}
