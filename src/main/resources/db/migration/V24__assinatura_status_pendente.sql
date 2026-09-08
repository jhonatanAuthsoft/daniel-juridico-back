-- O mês grátis passa a ser concedido pela loja (introductory offer / free trial phase).
-- O servidor não concede mais trial próprio: advogado nasce PENDENTE (sem acesso) e só
-- libera o app depois de assinar. Durante o mês grátis a loja reporta ATIVA com
-- periodo_fim_em no fim do trial, então TRIAL deixa de existir.

ALTER TABLE assinaturas DROP CONSTRAINT ck_assinaturas_status;

UPDATE assinaturas
SET status = 'PENDENTE',
    atualizado_em = CURRENT_TIMESTAMP
WHERE status = 'TRIAL';

ALTER TABLE assinaturas ADD CONSTRAINT ck_assinaturas_status CHECK (
    status IN ('PENDENTE', 'ATIVA', 'EM_ATRASO', 'EXPIRADA', 'CANCELADA')
);

DROP INDEX IF EXISTS idx_assinaturas_trial_fim;

-- trial_inicio_em / trial_fim_em ficam como colunas mortas nesta release e serão
-- dropadas depois de confirmar que nenhum ambiente ainda lê os valores antigos.
