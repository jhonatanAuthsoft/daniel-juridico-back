-- Catálogo completo de especialidades/subespecialidades e nome_pai opcional

ALTER TABLE advogados
  ALTER COLUMN nome_pai DROP NOT NULL;

-- Nomes alinhados com o catálogo oficial do produto
UPDATE especialidades SET nome = 'Direito Penal' WHERE codigo = 'CRIMINAL';
UPDATE especialidades SET nome = 'Direito Empresarial / Societário' WHERE codigo = 'EMPRESARIAL';

INSERT INTO especialidades (id, codigo, nome) VALUES
  (gen_random_uuid(), 'IMOBILIARIO', 'Direito Imobiliário'),
  (gen_random_uuid(), 'DIGITAL', 'Direito Digital / Tecnologia'),
  (gen_random_uuid(), 'PROPRIEDADE_INTELECTUAL', 'Propriedade Intelectual'),
  (gen_random_uuid(), 'AMBIENTAL', 'Direito Ambiental'),
  (gen_random_uuid(), 'SAUDE', 'Direito à Saúde'),
  (gen_random_uuid(), 'INTERNACIONAL', 'Direito Internacional'),
  (gen_random_uuid(), 'CORRESPONDENCIA', 'Correspondência Jurídica')
ON CONFLICT (codigo) DO NOTHING;

-- 1. Direito Civil
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('CONTRATOS', 'Contratos'),
  ('RESPONSABILIDADE_CIVIL', 'Responsabilidade Civil'),
  ('INDENIZACOES', 'Indenizações (danos morais e materiais)'),
  ('COBRANCA_EXECUCAO', 'Cobrança e Execução'),
  ('OBRIGACOES', 'Direito das Obrigações'),
  ('POSSE_PROPRIEDADE', 'Posse e Propriedade'),
  ('USUCAPIAO', 'Usucapião'),
  ('INVENTARIO_PARTILHA', 'Inventário e Partilha'),
  ('SUCESSOES', 'Sucessões'),
  ('TESTAMENTOS', 'Testamentos'),
  ('CONDOMINIOS', 'Condomínios'),
  ('DIREITOS_REAIS', 'Direitos Reais')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'CIVIL'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;

-- 2. Direito do Consumidor
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('RELACOES_CONSUMO', 'Relações de Consumo'),
  ('ACOES_BANCOS', 'Ações contra Bancos'),
  ('ACOES_PLANOS_SAUDE', 'Ações contra Planos de Saúde'),
  ('ACOES_EMPRESAS_AEREAS', 'Ações contra Empresas Aéreas'),
  ('PROCON_JUIZADO', 'Procon e Juizado Especial'),
  ('CLAUSULAS_ABUSIVAS', 'Cláusulas Abusivas'),
  ('COBRANCA_INDEVIDA', 'Cobrança Indevida'),
  ('VICIOS_PRODUTO_SERVICO', 'Vícios de Produto e Serviço'),
  ('NEGATIVACAO_INDEVIDA', 'Negativação Indevida (Serasa/SPC)')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'CONSUMIDOR'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;

-- 3. Direito Trabalhista
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('RECLAMACAO_TRABALHISTA', 'Reclamação Trabalhista'),
  ('RESCISAO_CONTRATO', 'Rescisão de Contrato'),
  ('VERBAS_RESCISORIAS', 'Verbas Rescisórias'),
  ('HORAS_EXTRAS', 'Horas Extras'),
  ('ASSEDIO_MORAL', 'Assédio Moral'),
  ('ASSEDIO_SEXUAL', 'Assédio Sexual'),
  ('ACIDENTE_TRABALHO', 'Acidente de Trabalho'),
  ('ESTABILIDADE_PROVISORIA', 'Estabilidade Provisória'),
  ('ACORDOS_TRABALHISTAS', 'Acordos Trabalhistas'),
  ('DEFESA_EMPREGADOR', 'Direito do Empregador (Defesa)')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'TRABALHISTA'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;

-- 4. Direito Previdenciário
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('APOSENTADORIA_IDADE', 'Aposentadoria por Idade'),
  ('APOSENTADORIA_TEMPO', 'Aposentadoria por Tempo de Contribuição'),
  ('APOSENTADORIA_INVALIDEZ', 'Aposentadoria por Invalidez'),
  ('AUXILIO_DOENCA', 'Auxílio-Doença'),
  ('AUXILIO_ACIDENTE', 'Auxílio-Acidente'),
  ('BPC_LOAS', 'BPC/LOAS'),
  ('REVISAO_BENEFICIOS', 'Revisão de Benefícios'),
  ('PLANEJAMENTO_PREVIDENCIARIO', 'Planejamento Previdenciário'),
  ('PENSAO_MORTE', 'Pensão por Morte')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'PREVIDENCIARIO'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;

-- 5. Direito Penal
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('DEFESA_CRIMINAL', 'Defesa Criminal'),
  ('TRIBUNAL_JURI', 'Tribunal do Júri'),
  ('CRIMES_CONTRA_PESSOA', 'Crimes Contra a Pessoa'),
  ('CRIMES_PATRIMONIAIS', 'Crimes Patrimoniais'),
  ('CRIMES_DIGITAIS', 'Crimes Digitais'),
  ('LEI_MARIA_PENHA', 'Lei Maria da Penha'),
  ('EXECUCAO_PENAL', 'Execução Penal'),
  ('HABEAS_CORPUS', 'Habeas Corpus'),
  ('ANPP', 'Acordo de Não Persecução Penal (ANPP)')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'CRIMINAL'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;

-- 6. Direito de Família
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('DIVORCIO', 'Divórcio'),
  ('PENSAO_ALIMENTICIA', 'Pensão Alimentícia'),
  ('GUARDA', 'Guarda'),
  ('REGULAMENTACAO_VISITAS', 'Regulamentação de Visitas'),
  ('RECONHECIMENTO_PATERNIDADE', 'Reconhecimento de Paternidade'),
  ('ADOCAO', 'Adoção'),
  ('UNIAO_ESTAVEL', 'União Estável'),
  ('PARTILHA_BENS', 'Partilha de Bens'),
  ('MEDIACAO_FAMILIAR', 'Mediação Familiar')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'FAMILIA'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;

-- 7. Direito Empresarial / Societário
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('CONSTITUICAO_EMPRESAS', 'Constituição de Empresas'),
  ('CONTRATOS_EMPRESARIAIS', 'Contratos Empresariais'),
  ('ALTERACAO_CONTRATUAL', 'Alteração Contratual'),
  ('DISSOLUCAO_SOCIEDADE', 'Dissolução de Sociedade'),
  ('RECUPERACAO_JUDICIAL', 'Recuperação Judicial'),
  ('FALENCIA', 'Falência'),
  ('GOVERNANCA_CORPORATIVA', 'Governança Corporativa'),
  ('ACORDO_SOCIOS', 'Acordo de Sócios'),
  ('COMPLIANCE_EMPRESARIAL', 'Compliance Empresarial')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'EMPRESARIAL'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;

-- 8. Direito Tributário
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('PLANEJAMENTO_TRIBUTARIO', 'Planejamento Tributário'),
  ('RECUPERACAO_TRIBUTOS', 'Recuperação de Tributos'),
  ('DEFESA_EXECUCAO_FISCAL', 'Defesa em Execução Fiscal'),
  ('ICMS', 'ICMS'),
  ('ISS', 'ISS'),
  ('IMPOSTO_RENDA', 'Imposto de Renda'),
  ('SIMPLES_NACIONAL', 'Simples Nacional'),
  ('AUTOS_INFRACAO', 'Autos de Infração'),
  ('CONTENCIOSO_FISCAL', 'Contencioso Administrativo Fiscal')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'TRIBUTARIO'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;

-- 9. Direito Imobiliário
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('COMPRA_VENDA_IMOVEIS', 'Compra e Venda de Imóveis'),
  ('CONTRATOS_LOCACAO', 'Contratos de Locação'),
  ('DESPEJO', 'Despejo'),
  ('REGULARIZACAO_IMOVEIS', 'Regularização de Imóveis'),
  ('USUCAPIAO', 'Usucapião'),
  ('CONDOMINIOS', 'Condomínios'),
  ('INCORPORACAO_IMOBILIARIA', 'Incorporação Imobiliária'),
  ('DISTRATO_IMOBILIARIO', 'Distrato Imobiliário')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'IMOBILIARIO'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;

-- 10. Direito Digital / Tecnologia
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('LGPD', 'LGPD'),
  ('PROTECAO_DADOS', 'Proteção de Dados'),
  ('CRIMES_DIGITAIS', 'Crimes Digitais'),
  ('CONTRATOS_TECNOLOGIA', 'Contratos de Tecnologia'),
  ('COMPLIANCE_DIGITAL', 'Compliance Digital'),
  ('PROPRIEDADE_INTELECTUAL_DIGITAL', 'Propriedade Intelectual Digital'),
  ('DIREITO_STARTUPS', 'Direito para Startups'),
  ('TERMOS_USO_POLITICAS', 'Termos de Uso e Políticas de Privacidade')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'DIGITAL'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;

-- 11. Propriedade Intelectual
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('REGISTRO_MARCA', 'Registro de Marca'),
  ('REGISTRO_PATENTE', 'Registro de Patente'),
  ('DIREITOS_AUTORAIS', 'Direitos Autorais'),
  ('CONTRATOS_LICENCIAMENTO', 'Contratos de Licenciamento'),
  ('CONCORRENCIA_DESLEAL', 'Concorrência Desleal'),
  ('FRANQUIAS', 'Franquias (PI)')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'PROPRIEDADE_INTELECTUAL'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;

-- 12. Direito Administrativo
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('LICITACOES', 'Licitações'),
  ('CONTRATOS_ADMINISTRATIVOS', 'Contratos Administrativos'),
  ('SERVIDORES_PUBLICOS', 'Servidores Públicos'),
  ('PROCESSOS_ADMINISTRATIVOS', 'Processos Administrativos'),
  ('IMPROBIDADE_ADMINISTRATIVA', 'Improbidade Administrativa'),
  ('TRIBUNAIS_CONTAS', 'Defesa em Tribunais de Contas')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'ADMINISTRATIVO'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;

-- 13. Direito Ambiental
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('LICENCIAMENTO_AMBIENTAL', 'Licenciamento Ambiental'),
  ('MULTAS_AMBIENTAIS', 'Multas Ambientais'),
  ('CRIMES_AMBIENTAIS', 'Crimes Ambientais'),
  ('COMPLIANCE_AMBIENTAL', 'Compliance Ambiental'),
  ('REGULARIZACAO_AMBIENTAL', 'Regularização Ambiental')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'AMBIENTAL'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;

-- 14. Direito à Saúde
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('ACOES_PLANOS_SAUDE', 'Ações contra Planos de Saúde'),
  ('FORNECIMENTO_MEDICAMENTOS', 'Fornecimento de Medicamentos'),
  ('JUDICIALIZACAO_SAUDE', 'Judicialização da Saúde'),
  ('ERRO_MEDICO', 'Erro Médico'),
  ('DEFESA_PROFISSIONAIS_SAUDE', 'Defesa de Profissionais da Saúde')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'SAUDE'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;

-- 15. Direito Internacional
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('CONTRATOS_INTERNACIONAIS', 'Contratos Internacionais'),
  ('IMIGRACAO', 'Imigração'),
  ('VISTOS', 'Vistos'),
  ('HOMOLOGACAO_SENTENCA', 'Homologação de Sentença Estrangeira'),
  ('INTERNACIONAL_PRIVADO', 'Direito Internacional Privado')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'INTERNACIONAL'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;

-- 16. Correspondência Jurídica
INSERT INTO subespecialidades (id, especialidade_id, codigo, nome)
SELECT gen_random_uuid(), e.id, s.codigo, s.nome
FROM especialidades e
JOIN (VALUES
  ('CARGA_PROCESSOS', 'Carga Rápida de Processos'),
  ('COPIA_AUTOS', 'Cópia de Autos'),
  ('PROTOCOLOS', 'Protocolos'),
  ('DILIGENCIAS_FORUM', 'Diligências em Fórum'),
  ('AUDIENCIAS_PREPOSTO', 'Audiências como Preposto'),
  ('DESPACHOS_JUIZ', 'Despachos com Juiz'),
  ('SUSTENTACAO_ORAL', 'Sustentação Oral')
) AS s(codigo, nome) ON TRUE
WHERE e.codigo = 'CORRESPONDENCIA'
ON CONFLICT (especialidade_id, codigo) DO NOTHING;
