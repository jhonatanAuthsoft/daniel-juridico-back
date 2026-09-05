-- Avaliação passa a ser por conexão; comentário opcional.
ALTER TABLE avaliacoes_advogado
    ALTER COLUMN comentario DROP NOT NULL;

ALTER TABLE avaliacoes_advogado
    ADD COLUMN conexao_id UUID REFERENCES conexoes (id) ON DELETE SET NULL;

UPDATE avaliacoes_advogado a
SET conexao_id = (
    SELECT c.id
    FROM conexoes c
    WHERE c.advogado_id = a.advogado_id
      AND c.cliente_id = a.cliente_id
      AND c.status = 'ACEITA'
    ORDER BY c.criado_em ASC
    LIMIT 1
);

ALTER TABLE avaliacoes_advogado
    DROP CONSTRAINT uk_avaliacoes_advogado_cliente;

CREATE UNIQUE INDEX uk_avaliacoes_advogado_conexao
    ON avaliacoes_advogado (conexao_id)
    WHERE conexao_id IS NOT NULL;

CREATE INDEX idx_avaliacoes_advogado_conexao ON avaliacoes_advogado (conexao_id);
