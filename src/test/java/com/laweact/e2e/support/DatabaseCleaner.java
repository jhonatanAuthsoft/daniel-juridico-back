package com.laweact.e2e.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Limpa todas as tabelas de negócio, mantendo o schema Flyway.
     * Equivalente ao clearDatabase() do exemplo Node — cada teste fica isolado.
     */
    public void clear() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                  areas_atuacao_advogado,
                  oabs,
                  advogados,
                  clientes,
                  enderecos,
                  tokens_revogados,
                  usuarios
                RESTART IDENTITY CASCADE
                """);
    }
}
