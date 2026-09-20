ALTER TABLE tb_checklist DROP COLUMN curso_id;

ALTER TABLE tb_aluno DROP COLUMN curso_id;

ALTER TABLE tb_beneficio
DROP CONSTRAINT IF EXISTS tb_beneficio_curso_id_fkey;

ALTER TABLE tb_beneficio
DROP COLUMN IF EXISTS curso_id;

DROP TABLE tb_curso;