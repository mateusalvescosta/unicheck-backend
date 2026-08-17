CREATE TABLE tb_beneficio_curso (
    beneficio_id BIGINT NOT NULL REFERENCES tb_beneficio(id),
    curso_id BIGINT NOT NULL REFERENCES tb_curso(id),
    PRIMARY KEY (beneficio_id, curso_id)
);

ALTER TABLE tb_beneficio DROP COLUMN curso_id;

