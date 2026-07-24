package com.laweact.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

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
}
