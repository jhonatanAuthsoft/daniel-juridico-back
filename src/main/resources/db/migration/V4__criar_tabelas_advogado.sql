-- Perfil de advogado + OAB + áreas de atuação (DER)

CREATE TABLE IF NOT EXISTS advogados (
  usuario_id UUID PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,

  nome_completo VARCHAR(255) NOT NULL,
  nome_social VARCHAR(255),
  rg VARCHAR(30) NOT NULL,
  rg_orgao_emissor VARCHAR(20) NOT NULL,
  rg_uf VARCHAR(2) NOT NULL,
  cpf VARCHAR(11) NOT NULL,
  nome_pai VARCHAR(255) NOT NULL,
  nome_mae VARCHAR(255) NOT NULL,
  pronome_tratamento VARCHAR(20) NOT NULL,
  foto_url VARCHAR(500),
  universidade VARCHAR(255) NOT NULL,
  curso VARCHAR(255) NOT NULL,
  ano_formacao INTEGER NOT NULL,
  atuacao_desde DATE NOT NULL,
  biografia TEXT,
  disponibilidade VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL',
  status_verificacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
  media_avaliacoes NUMERIC(3,2) NOT NULL DEFAULT 0,
  total_avaliacoes INTEGER NOT NULL DEFAULT 0,

  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_advogados_cpf ON advogados(cpf);
CREATE INDEX IF NOT EXISTS idx_advogados_status_verificacao ON advogados(status_verificacao);
CREATE INDEX IF NOT EXISTS idx_advogados_disponibilidade ON advogados(disponibilidade);

CREATE TABLE IF NOT EXISTS oabs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  advogado_id UUID NOT NULL REFERENCES advogados(usuario_id) ON DELETE CASCADE,

  numero VARCHAR(30) NOT NULL,
  uf VARCHAR(2) NOT NULL,
  principal BOOLEAN NOT NULL DEFAULT FALSE,
  foto_frente_url VARCHAR(500),
  foto_verso_url VARCHAR(500),
  status_validacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',

  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_oabs_numero_uf ON oabs(numero, uf);
CREATE INDEX IF NOT EXISTS idx_oabs_advogado ON oabs(advogado_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_oabs_principal_por_advogado
  ON oabs(advogado_id)
  WHERE principal = TRUE;

CREATE TABLE IF NOT EXISTS areas_atuacao_advogado (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  advogado_id UUID NOT NULL REFERENCES advogados(usuario_id) ON DELETE CASCADE,
  estado VARCHAR(2) NOT NULL,
  cidade VARCHAR(120) NOT NULL,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_areas_atuacao_advogado ON areas_atuacao_advogado(advogado_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_areas_atuacao_advogado_cidade
  ON areas_atuacao_advogado(advogado_id, estado, cidade);
