-- O hash BCrypt da senha do primeiro admin vem de ADMIN_SEED_SENHA_HASH (ver .env.exemplo), fora do codigo.
-- Sem um hash valido a migration falha, em vez de criar um admin sem senha utilizavel.
DO $$
BEGIN
    IF '${admin_seed_senha_hash}' !~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$' THEN
        RAISE EXCEPTION 'Defina ADMIN_SEED_SENHA_HASH com um hash BCrypt antes de migrar (ver .env.exemplo).';
    END IF;
END
$$;

INSERT INTO tb_usuario (nome, ra, senha, data_nascimento, role, email, criado_em, trocar_senha_no_login)
VALUES ('Administrador Bootstrap', 'ADMIN0001', '${admin_seed_senha_hash}', DATE '1990-01-01', 'ADMIN', 'ADMIN@unicheck.local', CURRENT_TIMESTAMP, TRUE);

INSERT INTO tb_admin (usuario_id, cargo)
SELECT id, 'COORDENADOR' FROM tb_usuario WHERE ra = 'ADMIN0001';
