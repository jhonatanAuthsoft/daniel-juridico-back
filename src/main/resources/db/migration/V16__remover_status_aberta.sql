-- ABERTA removido: pendente passa a ser só AGUARDANDO_MATCHING.
UPDATE solicitacoes
SET status = 'AGUARDANDO_MATCHING',
    atualizado_em = CURRENT_TIMESTAMP
WHERE status = 'ABERTA';

ALTER TABLE solicitacoes
    ALTER COLUMN status SET DEFAULT 'AGUARDANDO_MATCHING';
