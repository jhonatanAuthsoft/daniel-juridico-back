-- Sessões de app (dispositivo/login). Substitui tokens_invalidos_antes para invalidação em massa.
CREATE TABLE IF NOT EXISTS sessoes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  device_id VARCHAR(255),
  expira_em TIMESTAMP NOT NULL,
  criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
  atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sessoes_usuario_id ON sessoes(usuario_id);
CREATE INDEX IF NOT EXISTS idx_sessoes_expira_em ON sessoes(expira_em);

ALTER TABLE usuarios DROP COLUMN IF EXISTS tokens_invalidos_antes;
