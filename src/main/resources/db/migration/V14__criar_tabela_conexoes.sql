-- Conexões cliente ↔ advogado vinculadas a uma solicitação
CREATE TABLE conexoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitacao_id UUID NOT NULL REFERENCES solicitacoes (id) ON DELETE CASCADE,
    cliente_id UUID NOT NULL REFERENCES clientes (usuario_id) ON DELETE CASCADE,
    advogado_id UUID NOT NULL REFERENCES advogados (usuario_id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    decidido_em TIMESTAMP,
    cancelado_em TIMESTAMP,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,

    CONSTRAINT uk_conexoes_solicitacao_advogado UNIQUE (solicitacao_id, advogado_id),
    CONSTRAINT ck_conexoes_status CHECK (
        status IN ('PENDENTE', 'ACEITA', 'RECUSADA', 'CANCELADA')
    )
);

CREATE INDEX idx_conexoes_cliente ON conexoes (cliente_id, status, criado_em DESC);
CREATE INDEX idx_conexoes_advogado ON conexoes (advogado_id, status, criado_em DESC);
CREATE INDEX idx_conexoes_solicitacao ON conexoes (solicitacao_id, criado_em DESC);
