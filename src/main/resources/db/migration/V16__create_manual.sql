
CREATE TABLE tb_manual (
    id BIGSERIAL PRIMARY KEY,
    ordem INTEGER NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    tempo_leitura INTEGER NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    resumo VARCHAR(255) NOT NULL,
    link_texto VARCHAR(100),
    link_url VARCHAR(255),
    quando_se_aplica VARCHAR(500),
    o_que_saber VARCHAR(1000) NOT NULL,
    resultado_esperado VARCHAR(255),
    proximo_passo_descricao VARCHAR(255),
    proximo_passo_manual_id BIGINT REFERENCES tb_manual(id) ON DELETE SET NULL,
    aviso_atualizacao VARCHAR(255),
    CONSTRAINT ck_manual_categoria CHECK (categoria IN (
        'MATRICULA_VIDA_ACADEMICA', 'PORTAL_FERRAMENTAS_DIGITAIS',
        'UTILIDADES_INSTITUCIONAIS', 'BIBLIOTECA_ESTRUTURA_CAMPUS',
        'SERVICOS_APOIO_ESTUDANTE', 'AVALIACOES_NOTAS_FREQUENCIA',
        'FORMACAO_CARREIRA_ACADEMICA', 'COMECANDO_UNISALES'
    )),
    CONSTRAINT ck_manual_ordem_positiva CHECK (ordem > 0),
    CONSTRAINT ck_manual_tempo_leitura_positivo CHECK (tempo_leitura > 0),
    CONSTRAINT ck_manual_proximo_passo_diferente CHECK (proximo_passo_manual_id <> id),
    -- Deferida porque reordenar desloca várias linhas num UPDATE só, e no meio
    -- dele duas linhas chegam a ter a mesma ordem.
    CONSTRAINT uk_manual_ordem UNIQUE (ordem) DEFERRABLE INITIALLY DEFERRED
);

-- Caixa "Atenção".
CREATE TABLE tb_manual_atencao (
    manual_id BIGINT NOT NULL REFERENCES tb_manual(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    PRIMARY KEY (manual_id, ordem)
);

-- Bloco "Antes de começar".
CREATE TABLE tb_manual_requisito (
    manual_id BIGINT NOT NULL REFERENCES tb_manual(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    PRIMARY KEY (manual_id, ordem)
);

-- Bloco "Como fazer".
CREATE TABLE tb_manual_passo (
    manual_id BIGINT NOT NULL REFERENCES tb_manual(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    PRIMARY KEY (manual_id, ordem)
);

-- Chips do bloco "Onde resolver"; a url é opcional.
CREATE TABLE tb_manual_local (
    manual_id BIGINT NOT NULL REFERENCES tb_manual(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    nome VARCHAR(100) NOT NULL,
    url VARCHAR(255),
    PRIMARY KEY (manual_id, ordem)
);

-- Bloco "Prazo e regras".
CREATE TABLE tb_manual_prazo_regra (
    manual_id BIGINT NOT NULL REFERENCES tb_manual(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    PRIMARY KEY (manual_id, ordem)
);

-- Bloco recolhível "O que você precisa saber".
CREATE TABLE tb_manual_detalhe (
    manual_id BIGINT NOT NULL REFERENCES tb_manual(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    PRIMARY KEY (manual_id, ordem)
);

-- Bloco recolhível "Entenda melhor".
CREATE TABLE tb_manual_entenda_melhor (
    manual_id BIGINT NOT NULL REFERENCES tb_manual(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    PRIMARY KEY (manual_id, ordem)
);

-- "Você também pode precisar", na ordem escolhida pelo admin. O limite de três
-- é validado na aplicação.
CREATE TABLE tb_manual_relacionado (
    manual_id BIGINT NOT NULL REFERENCES tb_manual(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    relacionado_id BIGINT NOT NULL REFERENCES tb_manual(id) ON DELETE CASCADE,
    PRIMARY KEY (manual_id, ordem),
    -- Deferida porque o Hibernate reordena a lista atualizando uma linha por vez,
    -- e no meio da troca o mesmo relacionado aparece em duas posições.
    CONSTRAINT uk_manual_relacionado UNIQUE (manual_id, relacionado_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_manual_relacionado_diferente CHECK (manual_id <> relacionado_id)
);

-- "Atalhos do dia a dia": nome próprio, apontando para um manual.
CREATE TABLE tb_manual_atalho (
    id BIGSERIAL PRIMARY KEY,
    manual_id BIGINT NOT NULL REFERENCES tb_manual(id) ON DELETE CASCADE,
    nome VARCHAR(60) NOT NULL,
    ordem INTEGER NOT NULL,
    CONSTRAINT uk_manual_atalho_manual UNIQUE (manual_id),
    CONSTRAINT uk_manual_atalho_ordem UNIQUE (ordem),
    CONSTRAINT ck_manual_atalho_ordem_positiva CHECK (ordem > 0)
);

CREATE INDEX idx_manual_categoria ON tb_manual(categoria, ordem);
CREATE INDEX idx_manual_proximo_passo ON tb_manual(proximo_passo_manual_id);
CREATE INDEX idx_manual_relacionado_relacionado ON tb_manual_relacionado(relacionado_id);
