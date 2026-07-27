-- Recuperação de senha + auditoria de eventos

ALTER TABLE usuarios
  ADD COLUMN IF NOT EXISTS tokens_invalidos_antes TIMESTAMP;

CREATE TABLE IF NOT EXISTS tokens_recuperacao_senha (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  codigo_hash VARCHAR(255) NOT NULL,
  expira_em TIMESTAMP NOT NULL,
  usado_em TIMESTAMP,
  invalidado_em TIMESTAMP,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tokens_recuperacao_usuario
  ON tokens_recuperacao_senha(usuario_id);

CREATE INDEX IF NOT EXISTS idx_tokens_recuperacao_ativos
  ON tokens_recuperacao_senha(usuario_id, usado_em, invalidado_em, expira_em);

CREATE TABLE IF NOT EXISTS auditoria_eventos (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  evento VARCHAR(100) NOT NULL,
  usuario_id UUID REFERENCES usuarios(id) ON DELETE SET NULL,
  input TEXT,
  response TEXT,
  detalhes TEXT,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auditoria_eventos_evento ON auditoria_eventos(evento);
CREATE INDEX IF NOT EXISTS idx_auditoria_eventos_usuario ON auditoria_eventos(usuario_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_eventos_criado_em ON auditoria_eventos(criado_em);
