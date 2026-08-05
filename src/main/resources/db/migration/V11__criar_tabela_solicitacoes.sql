CREATE TABLE solicitacoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID NOT NULL REFERENCES clientes (usuario_id) ON DELETE CASCADE,

    titulo VARCHAR(255) NOT NULL,
    modalidade VARCHAR(30) NOT NULL,
    especialidade_codigo VARCHAR(50) NOT NULL,
    subespecialidade_codigo VARCHAR(50),
    uf VARCHAR(2) NOT NULL,
    cidade VARCHAR(120) NOT NULL,
    urgencia VARCHAR(30) NOT NULL,
    descricao VARCHAR(800) NOT NULL,
    forma_cobranca VARCHAR(30),
    experiencia_minima_meses INTEGER,
    status VARCHAR(40) NOT NULL DEFAULT 'ABERTA',

    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP
);

CREATE INDEX idx_solicitacoes_cliente_id ON solicitacoes (cliente_id);
CREATE INDEX idx_solicitacoes_status ON solicitacoes (status);
CREATE INDEX idx_solicitacoes_urgencia ON solicitacoes (urgencia);
