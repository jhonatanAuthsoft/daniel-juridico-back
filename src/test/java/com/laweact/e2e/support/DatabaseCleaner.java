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
                  assinatura_eventos,
                  assinaturas,
                  notificacoes,
                  dispositivos_push,
                  conexoes,
                  avaliacoes_advogado,
                  solicitacao_matches,
                  solicitacoes,
                  advogado_especialidades,
                  advogado_formas_cobranca,
                  advogado_modalidades,
                  pos_graduacoes_advogado,
                  areas_atuacao_advogado,
                  oab_fotos,
                  oabs,
                  advogados,
                  clientes,
                  enderecos,
                  termos_aceite,
                  tokens_recuperacao_senha,
                  auditoria_eventos,
                  logs_acesso_tela,
                  tokens_revogados,
                  sessoes,
                  usuarios
                RESTART IDENTITY CASCADE
                """);
    }
}
