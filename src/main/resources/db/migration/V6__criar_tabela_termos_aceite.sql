-- Registro de aceite de termos (fora do cadastro)

CREATE TABLE IF NOT EXISTS termos_aceite (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  versao VARCHAR(50) NOT NULL,
  scroll_confirmado BOOLEAN NOT NULL DEFAULT FALSE,
  checkbox_confirmado BOOLEAN NOT NULL DEFAULT TRUE,
  aceito_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_termos_aceite_usuario ON termos_aceite(usuario_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_termos_aceite_usuario_versao
  ON termos_aceite(usuario_id, versao);
