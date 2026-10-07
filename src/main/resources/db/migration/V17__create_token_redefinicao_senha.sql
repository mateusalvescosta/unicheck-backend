CREATE TABLE tb_token_redefinicao_senha (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES tb_usuario(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expira_em TIMESTAMPTZ NOT NULL,
    usado_em TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_token_redefinicao_hash UNIQUE (token_hash)
);

CREATE INDEX ix_token_redefinicao_usuario ON tb_token_redefinicao_senha (usuario_id, criado_em);
