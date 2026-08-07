-- Avaliações individuais de advogados (listagem V1; criar/apagar fora deste escopo)
CREATE TABLE avaliacoes_advogado (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    advogado_id UUID NOT NULL REFERENCES advogados (usuario_id) ON DELETE CASCADE,
    cliente_id UUID NOT NULL REFERENCES clientes (usuario_id) ON DELETE CASCADE,
    nota NUMERIC(2, 1) NOT NULL,
    comentario VARCHAR(800) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,

    CONSTRAINT uk_avaliacoes_advogado_cliente UNIQUE (advogado_id, cliente_id),
    CONSTRAINT ck_avaliacoes_advogado_nota CHECK (
        nota >= 0.5 AND nota <= 5.0 AND (nota * 2) = FLOOR(nota * 2)
    )
);

CREATE INDEX idx_avaliacoes_advogado_advogado ON avaliacoes_advogado (advogado_id, criado_em DESC);
CREATE INDEX idx_avaliacoes_advogado_cliente ON avaliacoes_advogado (cliente_id);
