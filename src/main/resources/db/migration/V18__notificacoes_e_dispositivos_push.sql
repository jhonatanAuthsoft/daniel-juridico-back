ALTER TABLE usuarios
    ADD COLUMN notificacoes_push_habilitadas BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE dispositivos_push (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    expo_push_token VARCHAR(255) NOT NULL,
    plataforma VARCHAR(20),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_registro_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    CONSTRAINT uk_dispositivos_push_token UNIQUE (expo_push_token)
);

CREATE INDEX idx_dispositivos_push_usuario_ativo
    ON dispositivos_push (usuario_id, ativo);

CREATE TABLE notificacoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    destinatario_id UUID NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    remetente_id UUID NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    titulo VARCHAR(200) NOT NULL,
    texto TEXT NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    referencia_tipo VARCHAR(50) NOT NULL,
    referencia_id UUID NOT NULL,
    lida_em TIMESTAMP,
    status_envio VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    enviado_em TIMESTAMP,
    erro_envio VARCHAR(500),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    CONSTRAINT ck_notificacoes_tipo CHECK (
        tipo IN ('CONEXAO_SOLICITADA', 'CONEXAO_ACEITA')
    ),
    CONSTRAINT ck_notificacoes_referencia_tipo CHECK (
        referencia_tipo IN ('CONEXAO')
    ),
    CONSTRAINT ck_notificacoes_status_envio CHECK (
        status_envio IN ('PENDENTE', 'ENVIADA', 'SKIPPED', 'ERROR')
    )
);

CREATE INDEX idx_notificacoes_destinatario_criado
    ON notificacoes (destinatario_id, criado_em DESC);

CREATE INDEX idx_notificacoes_destinatario_nao_lidas
    ON notificacoes (destinatario_id)
    WHERE lida_em IS NULL;
