-- Ranking de advogados calculado na criação da solicitação (sem recálculo posterior)
CREATE TABLE solicitacao_matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitacao_id UUID NOT NULL REFERENCES solicitacoes (id) ON DELETE CASCADE,
    advogado_id UUID NOT NULL REFERENCES advogados (usuario_id) ON DELETE CASCADE,

    posicao INTEGER NOT NULL,
    score INTEGER NOT NULL,
    nivel_localidade VARCHAR(20) NOT NULL,

    pontos_modalidade SMALLINT NOT NULL,
    pontos_localidade SMALLINT NOT NULL,
    pontos_especialidade SMALLINT NOT NULL,
    pontos_subespecialidade SMALLINT NOT NULL,
    pontos_experiencia SMALLINT NOT NULL,
    pontos_cobranca SMALLINT NOT NULL,

    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,

    CONSTRAINT uk_solicitacao_matches_advogado UNIQUE (solicitacao_id, advogado_id)
);

CREATE INDEX idx_solicitacao_matches_solicitacao ON solicitacao_matches (solicitacao_id, posicao);
CREATE INDEX idx_solicitacao_matches_advogado ON solicitacao_matches (advogado_id);
