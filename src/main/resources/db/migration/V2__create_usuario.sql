CREATE TABLE tb_usuario (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    foto_url VARCHAR(255),
    email VARCHAR(150) UNIQUE NOT NULL,
    criado_em TIMESTAMP NOT NULL
);

CREATE TABLE tb_admin (
    id BIGSERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL UNIQUE REFERENCES tb_usuario(id),
    cargo VARCHAR(25) NOT NULL
);

CREATE TABLE tb_aluno (
    id BIGSERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL UNIQUE REFERENCES tb_usuario(id),
    curso_id INTEGER NOT NULL REFERENCES tb_curso(id),
    ra VARCHAR(25) NOT NULL UNIQUE,
    status_matricula VARCHAR(25) NOT NULL,
    periodo VARCHAR(25) NOT NULL
);