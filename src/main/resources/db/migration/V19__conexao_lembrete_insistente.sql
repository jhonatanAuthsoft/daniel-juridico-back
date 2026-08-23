ALTER TABLE conexoes
    ADD COLUMN ultimo_lembrete_insistente_em TIMESTAMP;

CREATE INDEX idx_notificacoes_referencia_tipo
    ON notificacoes (referencia_id, tipo);

CREATE INDEX idx_conexoes_status_lembrete
    ON conexoes (status, ultimo_lembrete_insistente_em);
