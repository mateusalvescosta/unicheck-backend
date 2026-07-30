CREATE TABLE tb_checklist (
    id BIGSERIAL PRIMARY KEY,
    curso_id BIGINT REFERENCES tb_curso(id),
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(500),
    horas_complementares INTEGER
);

CREATE TABLE tb_item_checklist (
    id BIGSERIAL PRIMARY KEY,
    checklist_id BIGINT NOT NULL REFERENCES tb_checklist(id),
    titulo VARCHAR(150) NOT NULL,
    descricao VARCHAR(500),
    ordem INT NOT NULL
);

CREATE TABLE tb_item_prerequisito (
    item_checklist_id BIGINT NOT NULL REFERENCES tb_item_checklist(id),
    item_requisito_id BIGINT NOT NULL REFERENCES tb_item_checklist(id),
    PRIMARY KEY (item_checklist_id, item_requisito_id)
);

CREATE TABLE tb_checklist_prerequisito (
    checklist_id BIGINT NOT NULL REFERENCES tb_checklist(id),
    checklist_requisito_id BIGINT NOT NULL REFERENCES tb_checklist(id),
    PRIMARY KEY (checklist_id, checklist_requisito_id)
);

CREATE TABLE tb_progresso_item (
    aluno_id BIGINT NOT NULL REFERENCES tb_aluno(id),
    item_checklist_id BIGINT NOT NULL REFERENCES tb_item_checklist(id),
    status VARCHAR(20) NOT NULL,
    data_conclusao TIMESTAMP,
    PRIMARY KEY (aluno_id, item_checklist_id)
);

CREATE TABLE tb_checklist_concluido (
    aluno_id BIGINT NOT NULL REFERENCES tb_aluno(id),
    checklist_id BIGINT NOT NULL REFERENCES tb_checklist(id),
    data_conclusao TIMESTAMP NOT NULL,
    horas_creditadas INTEGER,
    PRIMARY KEY (aluno_id, checklist_id)
);