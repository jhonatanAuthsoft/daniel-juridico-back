-- Alinha usuarios ao DER (auth) e cria perfil de cliente + endereço

ALTER TABLE usuarios
  ADD COLUMN IF NOT EXISTS tentativas_login_falhas INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS bloqueado_ate TIMESTAMP;

CREATE TABLE IF NOT EXISTS clientes (
  usuario_id UUID PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,

  nome_completo VARCHAR(255) NOT NULL,
  profissao VARCHAR(255) NOT NULL,
  tipo_documento VARCHAR(10) NOT NULL,
  numero_documento VARCHAR(20) NOT NULL,
  rg VARCHAR(30) NOT NULL,
  data_nascimento DATE NOT NULL,
  pronomes VARCHAR(20) NOT NULL,
  foto_url VARCHAR(500),
  faixa_renda VARCHAR(100),
  estado_civil VARCHAR(50),

  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_clientes_numero_documento ON clientes(numero_documento);
CREATE INDEX IF NOT EXISTS idx_clientes_tipo_documento ON clientes(tipo_documento);

CREATE TABLE IF NOT EXISTS enderecos (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  usuario_id UUID NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,

  cep VARCHAR(9) NOT NULL,
  logradouro VARCHAR(255) NOT NULL,
  numero VARCHAR(30) NOT NULL,
  bairro VARCHAR(120) NOT NULL,
  cidade VARCHAR(120) NOT NULL,
  estado VARCHAR(2) NOT NULL,

  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_enderecos_usuario ON enderecos(usuario_id);
CREATE INDEX IF NOT EXISTS idx_enderecos_cidade_estado ON enderecos(cidade, estado);
