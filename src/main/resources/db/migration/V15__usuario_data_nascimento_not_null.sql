ALTER TABLE tb_usuario ALTER COLUMN data_nascimento SET NOT NULL;

ALTER TABLE tb_categoria ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_categoria_ativo ON tb_categoria(ativo);
