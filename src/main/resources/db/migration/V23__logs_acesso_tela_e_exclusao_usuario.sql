-- Soft delete de conta + log de acesso a telas (termos)

ALTER TABLE usuarios
  ADD COLUMN IF NOT EXISTS excluido_em TIMESTAMP;

CREATE TABLE IF NOT EXISTS logs_acesso_tela (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  tela VARCHAR(50) NOT NULL,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_logs_acesso_tela_usuario ON logs_acesso_tela(usuario_id);
CREATE INDEX IF NOT EXISTS idx_logs_acesso_tela_criado_em ON logs_acesso_tela(criado_em);
