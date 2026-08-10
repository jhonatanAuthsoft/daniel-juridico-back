-- Remove ENCERRADA: status não usado. ABERTA e AGUARDANDO_MATCHING são equivalentes (pendentes).
UPDATE solicitacoes
SET status = 'CANCELADA',
    atualizado_em = CURRENT_TIMESTAMP
WHERE status = 'ENCERRADA';
