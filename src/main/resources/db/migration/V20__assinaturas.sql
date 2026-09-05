CREATE TABLE assinaturas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL UNIQUE REFERENCES usuarios (id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    plataforma VARCHAR(20),
    ambiente VARCHAR(20),
    product_id VARCHAR(120),
    purchase_token VARCHAR(500) UNIQUE,
    original_transaction_id VARCHAR(120) UNIQUE,
    trial_inicio_em TIMESTAMP,
    trial_fim_em TIMESTAMP,
    periodo_fim_em TIMESTAMP,
    auto_renovacao BOOLEAN NOT NULL DEFAULT FALSE,
    ultima_sincronizacao_em TIMESTAMP,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_assinaturas_status CHECK (
        status IN ('TRIAL', 'ATIVA', 'EM_ATRASO', 'EXPIRADA', 'CANCELADA')
    )
);

CREATE TABLE assinatura_eventos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assinatura_id UUID NOT NULL REFERENCES assinaturas (id) ON DELETE CASCADE,
    origem VARCHAR(30) NOT NULL,
    evento_externo_id VARCHAR(200) NOT NULL UNIQUE,
    tipo VARCHAR(80) NOT NULL,
    payload JSONB,
    criado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_assinaturas_status_periodo
    ON assinaturas (status, periodo_fim_em);

CREATE INDEX idx_assinaturas_trial_fim
    ON assinaturas (status, trial_fim_em);

CREATE INDEX idx_assinatura_eventos_assinatura
    ON assinatura_eventos (assinatura_id);

-- Advogados já cadastrados recebem trial retroativo a partir da data de criação.
INSERT INTO assinaturas (
    usuario_id,
    status,
    trial_inicio_em,
    trial_fim_em,
    criado_em,
    atualizado_em
)
SELECT
    u.id,
    'TRIAL',
    u.criado_em,
    u.criado_em + INTERVAL '30 days',
    NOW(),
    NOW()
FROM usuarios u
WHERE u.perfil = 'ADVOGADO'
  AND NOT EXISTS (
      SELECT 1 FROM assinaturas a WHERE a.usuario_id = u.id
  );
