-- Hash BCrypt pre-gerado para a senha temporaria 01011990. Nao calculamos hash em SQL.
INSERT INTO tb_usuario (nome, ra, senha, data_nascimento, role, email, criado_em, trocar_senha_no_login)
VALUES ('Administrador Bootstrap', 'ADMIN0001', '$2a$10$1WncamRRG3Jd0QVsSKicV.LF.i/ct0CmcbJnPDLTazdFwVLV1CtO6', DATE '1990-01-01', 'ADMIN', 'ADMIN@unicheck.local', CURRENT_TIMESTAMP, TRUE);

INSERT INTO tb_admin (usuario_id, cargo)
SELECT id, 'COORDENADOR' FROM tb_usuario WHERE ra = 'ADMIN0001';
