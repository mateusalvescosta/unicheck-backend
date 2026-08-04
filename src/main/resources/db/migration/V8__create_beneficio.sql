CREATE TABLE tb_beneficio (
    id BIGSERIAL PRIMARY KEY,
    curso_id BIGINT REFERENCES tb_curso(id),
    icone_url VARCHAR(255),
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    url VARCHAR(255) NOT NULL
);

CREATE TABLE tb_categoria (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL
);

CREATE TABLE tb_beneficio_categoria (
    beneficio_id BIGINT NOT NULL REFERENCES tb_beneficio(id),
    categoria_id BIGINT NOT NULL REFERENCES tb_categoria(id),
    PRIMARY KEY (beneficio_id, categoria_id)
);
