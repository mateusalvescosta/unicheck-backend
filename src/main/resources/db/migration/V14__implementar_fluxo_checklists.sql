ALTER TABLE tb_checklist ADD COLUMN ordem INTEGER;
WITH ordenados AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS nova_ordem FROM tb_checklist
)
UPDATE tb_checklist c SET ordem = o.nova_ordem FROM ordenados o WHERE o.id = c.id;
ALTER TABLE tb_checklist ALTER COLUMN ordem SET NOT NULL;
ALTER TABLE tb_checklist ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'RASCUNHO';
UPDATE tb_checklist SET descricao = '' WHERE descricao IS NULL;
ALTER TABLE tb_checklist ALTER COLUMN descricao SET NOT NULL;
UPDATE tb_checklist SET xp = 0 WHERE xp IS NULL;
UPDATE tb_checklist SET horas_complementares = 0 WHERE horas_complementares IS NULL;
ALTER TABLE tb_checklist ALTER COLUMN xp SET NOT NULL;
ALTER TABLE tb_checklist ALTER COLUMN horas_complementares SET NOT NULL;
ALTER TABLE tb_checklist ADD CONSTRAINT ck_checklist_ordem_positiva CHECK (ordem > 0);
ALTER TABLE tb_checklist ADD CONSTRAINT ck_checklist_xp_nao_negativo CHECK (xp >= 0);
ALTER TABLE tb_checklist ADD CONSTRAINT ck_checklist_horas_nao_negativas CHECK (horas_complementares >= 0);
ALTER TABLE tb_checklist ADD CONSTRAINT ck_checklist_status CHECK (status IN ('RASCUNHO', 'PUBLICADO', 'ARQUIVADO'));

ALTER TABLE tb_item_checklist ADD COLUMN conteudo TEXT;
ALTER TABLE tb_item_checklist ADD COLUMN xp INTEGER NOT NULL DEFAULT 0;
WITH ordenados AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY checklist_id ORDER BY ordem, id) AS nova_ordem
    FROM tb_item_checklist
)
UPDATE tb_item_checklist i SET ordem = o.nova_ordem FROM ordenados o WHERE o.id = i.id;
UPDATE tb_item_checklist SET descricao = '' WHERE descricao IS NULL;
UPDATE tb_item_checklist SET conteudo = '' WHERE conteudo IS NULL;
ALTER TABLE tb_item_checklist ALTER COLUMN descricao SET NOT NULL;
ALTER TABLE tb_item_checklist ALTER COLUMN conteudo SET NOT NULL;
ALTER TABLE tb_item_checklist ADD CONSTRAINT ck_item_ordem_positiva CHECK (ordem > 0);
ALTER TABLE tb_item_checklist ADD CONSTRAINT ck_item_xp_nao_negativo CHECK (xp >= 0);
ALTER TABLE tb_item_checklist ADD CONSTRAINT uk_item_checklist_ordem UNIQUE (checklist_id, ordem);

ALTER TABLE tb_progresso_item ADD COLUMN xp_creditado INTEGER;
UPDATE tb_progresso_item SET status = 'CONCLUIDO' WHERE status = 'CONCLUÍDO';
ALTER TABLE tb_progresso_item ADD CONSTRAINT ck_progresso_status CHECK (status IN ('PENDENTE', 'CONCLUIDO'));
ALTER TABLE tb_progresso_item ADD CONSTRAINT ck_progresso_conclusao_coerente CHECK (status <> 'CONCLUIDO' OR data_conclusao IS NOT NULL);
ALTER TABLE tb_progresso_item ADD CONSTRAINT ck_progresso_xp_nao_negativo CHECK (xp_creditado IS NULL OR xp_creditado >= 0);

ALTER TABLE tb_checklist_concluido ADD COLUMN status_lancamento_horas VARCHAR(20);
ALTER TABLE tb_checklist_concluido ADD COLUMN data_lancamento_horas TIMESTAMP WITH TIME ZONE;
ALTER TABLE tb_checklist_concluido ADD COLUMN admin_lancamento_id BIGINT REFERENCES tb_admin(id) ON DELETE RESTRICT;
UPDATE tb_checklist_concluido
SET status_lancamento_horas = CASE
    WHEN horas_creditadas = 0 THEN 'NAO_APLICAVEL'
    WHEN horas_creditadas > 0 THEN 'PENDENTE'
    ELSE NULL
END;
ALTER TABLE tb_checklist_concluido ADD CONSTRAINT ck_conclusao_xp_nao_negativo CHECK (xp_creditado IS NULL OR xp_creditado >= 0);
ALTER TABLE tb_checklist_concluido ADD CONSTRAINT ck_conclusao_horas_nao_negativas CHECK (horas_creditadas IS NULL OR horas_creditadas >= 0);
ALTER TABLE tb_checklist_concluido ADD CONSTRAINT ck_status_lancamento_horas CHECK (
    status_lancamento_horas IS NULL OR status_lancamento_horas IN ('PENDENTE', 'LANCADO', 'NAO_APLICAVEL')
);
ALTER TABLE tb_checklist_concluido ADD CONSTRAINT ck_status_horas_coerente CHECK (
    status_lancamento_horas IS NULL
    OR (status_lancamento_horas = 'NAO_APLICAVEL' AND horas_creditadas = 0)
    OR (status_lancamento_horas IN ('PENDENTE', 'LANCADO') AND horas_creditadas > 0)
);
ALTER TABLE tb_checklist_concluido ADD CONSTRAINT ck_dados_lancamento_horas CHECK (
    (status_lancamento_horas = 'LANCADO' AND data_lancamento_horas IS NOT NULL AND admin_lancamento_id IS NOT NULL)
    OR (status_lancamento_horas IS DISTINCT FROM 'LANCADO' AND data_lancamento_horas IS NULL AND admin_lancamento_id IS NULL)
);

ALTER TABLE tb_checklist_prerequisito ADD CONSTRAINT ck_prerequisito_distinto CHECK (checklist_id <> checklist_requisito_id);

CREATE TABLE tb_selo (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE,
    descricao VARCHAR(500),
    imagem_url VARCHAR(500)
);

CREATE TABLE tb_nivel (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE,
    xp_minimo INTEGER NOT NULL UNIQUE CHECK (xp_minimo >= 0),
    selo_id BIGINT NOT NULL UNIQUE REFERENCES tb_selo(id) ON DELETE RESTRICT
);

CREATE TABLE tb_aluno_selo (
    aluno_id BIGINT NOT NULL REFERENCES tb_aluno(id) ON DELETE RESTRICT,
    selo_id BIGINT NOT NULL REFERENCES tb_selo(id) ON DELETE RESTRICT,
    data_conquista TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (aluno_id, selo_id)
);

CREATE INDEX ix_progresso_item_checklist ON tb_progresso_item (item_checklist_id, aluno_id);
CREATE INDEX ix_conclusao_horas_status ON tb_checklist_concluido (status_lancamento_horas);
