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
     * Limpa dados de negócio, preservando catálogos seedados (modalidades, especialidades, cobrança).
     */
    public void clear() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                  advogado_especialidades,
                  advogado_formas_cobranca,
                  advogado_modalidades,
                  pos_graduacoes_advogado,
                  areas_atuacao_advogado,
                  oabs,
                  advogados,
                  clientes,
                  enderecos,
                  termos_aceite,
                  tokens_recuperacao_senha,
                  auditoria_eventos,
                  tokens_revogados,
                  sessoes,
                  usuarios
                RESTART IDENTITY CASCADE
                """);
    }
}
