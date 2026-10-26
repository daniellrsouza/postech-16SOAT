-- V17__add_status_clientes.sql
-- Situação cadastral do cliente, consultada pela Lambda de autenticação por CPF
-- (só clientes ATIVO recebem token). Fase 3 · ajuste A do modelo de dados.
-- Contexto: cadastro

ALTER TABLE clientes
    ADD COLUMN IF NOT EXISTS status VARCHAR(10) NOT NULL DEFAULT 'ATIVO';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_cliente_status') THEN
        ALTER TABLE clientes
            ADD CONSTRAINT ck_cliente_status CHECK (status IN ('ATIVO', 'INATIVO', 'BLOQUEADO'));
    END IF;
END $$;

COMMENT ON COLUMN clientes.status IS 'Situação cadastral: ATIVO, INATIVO ou BLOQUEADO. Só ATIVO autentica por CPF';

-- A Lambda acessa o banco com um usuário próprio, membro do papel leitura_clientes
-- (criado pelo repositório postech-16soat-infra-db). Ela só precisa das colunas da
-- consulta de autenticação. O papel não existe no ambiente local (docker compose):
-- nesse caso o GRANT é ignorado.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'leitura_clientes') THEN
        GRANT SELECT (id, cpf_cnpj, status) ON clientes TO leitura_clientes;
    END IF;
END $$;
