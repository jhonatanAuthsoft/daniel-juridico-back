-- Complemento endereço, OAB data expedição, cliente CPF/CNPJ, catálogos advogado

ALTER TABLE enderecos
  ADD COLUMN IF NOT EXISTS complemento VARCHAR(255);

ALTER TABLE oabs
  ADD COLUMN IF NOT EXISTS data_expedicao DATE;

UPDATE oabs SET data_expedicao = CURRENT_DATE WHERE data_expedicao IS NULL;

ALTER TABLE oabs
  ALTER COLUMN data_expedicao SET NOT NULL;

ALTER TABLE clientes
  ALTER COLUMN profissao DROP NOT NULL,
  ALTER COLUMN rg DROP NOT NULL,
  ALTER COLUMN data_nascimento DROP NOT NULL;

ALTER TABLE clientes
  ADD COLUMN IF NOT EXISTS razao_social VARCHAR(255),
  ADD COLUMN IF NOT EXISTS area_atuacao VARCHAR(255);

CREATE TABLE IF NOT EXISTS pos_graduacoes_advogado (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  advogado_id UUID NOT NULL REFERENCES advogados(usuario_id) ON DELETE CASCADE,
  nome_curso VARCHAR(255) NOT NULL,
  instituicao VARCHAR(255) NOT NULL,
  ano_formacao INTEGER,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pos_graduacoes_advogado ON pos_graduacoes_advogado(advogado_id);

CREATE TABLE IF NOT EXISTS modalidades_atuacao (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  codigo VARCHAR(50) NOT NULL UNIQUE,
  nome VARCHAR(120) NOT NULL,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em TIMESTAMP
);

CREATE TABLE IF NOT EXISTS advogado_modalidades (
  advogado_id UUID NOT NULL REFERENCES advogados(usuario_id) ON DELETE CASCADE,
  modalidade_id UUID NOT NULL REFERENCES modalidades_atuacao(id) ON DELETE CASCADE,
  PRIMARY KEY (advogado_id, modalidade_id)
);

CREATE TABLE IF NOT EXISTS formas_cobranca (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  codigo VARCHAR(50) NOT NULL UNIQUE,
  nome VARCHAR(120) NOT NULL,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em TIMESTAMP
);

CREATE TABLE IF NOT EXISTS advogado_formas_cobranca (
  advogado_id UUID NOT NULL REFERENCES advogados(usuario_id) ON DELETE CASCADE,
  forma_cobranca_id UUID NOT NULL REFERENCES formas_cobranca(id) ON DELETE CASCADE,
  PRIMARY KEY (advogado_id, forma_cobranca_id)
);

CREATE TABLE IF NOT EXISTS especialidades (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  codigo VARCHAR(50) NOT NULL UNIQUE,
  nome VARCHAR(120) NOT NULL,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subespecialidades (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  especialidade_id UUID NOT NULL REFERENCES especialidades(id) ON DELETE CASCADE,
  codigo VARCHAR(50) NOT NULL,
  nome VARCHAR(120) NOT NULL,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em TIMESTAMP,
  UNIQUE (especialidade_id, codigo)
);

CREATE TABLE IF NOT EXISTS advogado_especialidades (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  advogado_id UUID NOT NULL REFERENCES advogados(usuario_id) ON DELETE CASCADE,
  especialidade_id UUID REFERENCES especialidades(id) ON DELETE CASCADE,
  subespecialidade_id UUID REFERENCES subespecialidades(id) ON DELETE SET NULL,
  especialidade_livre VARCHAR(255),
  subespecialidade_livre VARCHAR(255),
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em TIMESTAMP,
  CONSTRAINT chk_advogado_especialidade_origem CHECK (
    especialidade_id IS NOT NULL OR especialidade_livre IS NOT NULL
  )
);

CREATE INDEX IF NOT EXISTS idx_advogado_especialidades ON advogado_especialidades(advogado_id);

INSERT INTO modalidades_atuacao (id, codigo, nome) VALUES
  (gen_random_uuid(), 'CORRESPONDENTE', 'Correspondente / Outras atividades'),
  (gen_random_uuid(), 'PAUTISTA', 'Pautista'),
  (gen_random_uuid(), 'CONSULTOR', 'Consultor'),
  (gen_random_uuid(), 'GENERALISTA', 'Generalista'),
  (gen_random_uuid(), 'NENHUMA_DAS_ANTERIORES', 'Nenhuma das anteriores')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO formas_cobranca (id, codigo, nome) VALUES
  (gen_random_uuid(), 'HONORARIOS_CONTRATUAIS', 'Honorários contratuais'),
  (gen_random_uuid(), 'HONORARIOS_PERCENTUAIS', 'Honorários percentuais'),
  (gen_random_uuid(), 'HONORARIOS_ARBITRADOS', 'Honorários arbitrados judicialmente'),
  (gen_random_uuid(), 'OUTROS_A_COMBINAR', 'Outros (A combinar)')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO especialidades (id, codigo, nome) VALUES
  (gen_random_uuid(), 'CIVIL', 'Direito Civil'),
  (gen_random_uuid(), 'TRABALHISTA', 'Direito Trabalhista'),
  (gen_random_uuid(), 'CRIMINAL', 'Direito Criminal'),
  (gen_random_uuid(), 'FAMILIA', 'Direito de Família'),
  (gen_random_uuid(), 'CONSUMIDOR', 'Direito do Consumidor'),
  (gen_random_uuid(), 'EMPRESARIAL', 'Direito Empresarial'),
  (gen_random_uuid(), 'TRIBUTARIO', 'Direito Tributário'),
  (gen_random_uuid(), 'PREVIDENCIARIO', 'Direito Previdenciário'),
  (gen_random_uuid(), 'ADMINISTRATIVO', 'Direito Administrativo'),
  (gen_random_uuid(), 'OUTROS', 'Outros')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, 'LIVRE', 'Especificar'
FROM especialidades e
WHERE e.codigo = 'OUTROS'
ON CONFLICT DO NOTHING;
