-- Campos exibidos nos cards e no card expandido da tela de benefícios.
-- As colunas obrigatórias entram com DEFAULT apenas para não quebrar linhas
-- já existentes; o DEFAULT é removido em seguida para que toda inserção nova
-- seja obrigada a informar o valor.
ALTER TABLE tb_beneficio ADD COLUMN tipo VARCHAR(30) NOT NULL DEFAULT 'DESCONTO';
ALTER TABLE tb_beneficio ALTER COLUMN tipo DROP DEFAULT;

ALTER TABLE tb_beneficio ADD COLUMN chamada VARCHAR(150) NOT NULL DEFAULT '';
ALTER TABLE tb_beneficio ALTER COLUMN chamada DROP DEFAULT;

ALTER TABLE tb_beneficio ADD COLUMN publico_elegivel VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE tb_beneficio ALTER COLUMN publico_elegivel DROP DEFAULT;

ALTER TABLE tb_beneficio ADD COLUMN observacao_elegibilidade VARCHAR(500);
ALTER TABLE tb_beneficio ADD COLUMN aviso_importante VARCHAR(255);
ALTER TABLE tb_beneficio ADD COLUMN disponibilidade VARCHAR(100);

-- Tags do card, usadas também na busca por palavra-chave.
CREATE TABLE tb_beneficio_palavra_chave (
    beneficio_id BIGINT NOT NULL REFERENCES tb_beneficio(id) ON DELETE CASCADE,
    palavra_chave VARCHAR(60) NOT NULL,
    PRIMARY KEY (beneficio_id, palavra_chave)
);

-- Bloco "Como conseguir" do card expandido.
CREATE TABLE tb_beneficio_passo (
    beneficio_id BIGINT NOT NULL REFERENCES tb_beneficio(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    titulo VARCHAR(100) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    PRIMARY KEY (beneficio_id, ordem)
);

-- "Meus favoritos".
CREATE TABLE tb_beneficio_favorito (
    aluno_id BIGINT NOT NULL REFERENCES tb_aluno(id) ON DELETE CASCADE,
    beneficio_id BIGINT NOT NULL REFERENCES tb_beneficio(id) ON DELETE CASCADE,
    favoritado_em TIMESTAMP NOT NULL,
    PRIMARY KEY (aluno_id, beneficio_id)
);

CREATE INDEX idx_beneficio_favorito_aluno ON tb_beneficio_favorito(aluno_id, favoritado_em DESC);

-- Apoio aos filtros e à busca textual da listagem.
CREATE INDEX idx_beneficio_tipo ON tb_beneficio(tipo);
CREATE INDEX idx_beneficio_nome_lower ON tb_beneficio(LOWER(nome));
CREATE INDEX idx_beneficio_categoria_categoria ON tb_beneficio_categoria(categoria_id);
