ALTER TABLE tb_notificacao ADD COLUMN tipo VARCHAR(30) NOT NULL;
ALTER TABLE tb_notificacao ADD COLUMN referencia_id BIGINT NOT NULL;
ALTER TABLE tb_notificacao ADD CONSTRAINT ck_notificacao_tipo
    CHECK (tipo IN ('BENEFICIO_NOVO', 'CHECKLIST_NOVO', 'MANUAL_NOVO', 'CHECKLIST_PARADO'));

-- Excluir um aluno nao pode falhar por causa das notificacoes dele.
ALTER TABLE tb_notificacao DROP CONSTRAINT tb_notificacao_usuario_id_fkey;
ALTER TABLE tb_notificacao ADD CONSTRAINT fk_notificacao_usuario
    FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id) ON DELETE CASCADE;

CREATE INDEX ix_notificacao_usuario ON tb_notificacao (usuario_id, criado_em DESC);
CREATE INDEX ix_notificacao_referencia ON tb_notificacao (tipo, referencia_id);

-- Tipos que o aluno desligou; sem linha significa ligado.
CREATE TABLE tb_notificacao_desativada (
    usuario_id BIGINT NOT NULL REFERENCES tb_usuario(id) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL,
    PRIMARY KEY (usuario_id, tipo),
    CONSTRAINT ck_notificacao_desativada_tipo
        CHECK (tipo IN ('BENEFICIO_NOVO', 'CHECKLIST_NOVO', 'MANUAL_NOVO', 'CHECKLIST_PARADO'))
);
