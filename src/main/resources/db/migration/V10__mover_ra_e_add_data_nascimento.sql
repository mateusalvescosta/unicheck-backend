ALTER TABLE tb_usuario ADD COLUMN ra VARCHAR(25);
ALTER TABLE tb_usuario ADD COLUMN data_nascimento DATE;


UPDATE tb_usuario u
SET ra = a.ra
    FROM tb_aluno a
WHERE a.usuario_id = u.id;

ALTER TABLE tb_usuario ALTER COLUMN ra SET NOT NULL;
ALTER TABLE tb_usuario ADD CONSTRAINT uk_usuario_ra UNIQUE (ra);

ALTER TABLE tb_aluno DROP COLUMN ra;