-- V18__corrige_check_tipo_movimentacao.sql
-- Fase 3 · ajuste B do modelo de dados (consistência).
--
-- A V8 criou movimentacoes_estoque com CHECK (ENTRADA, SAIDA, AJUSTE). A V9 tentou
-- recriar a tabela com VENDA e RESERVA, mas usou CREATE TABLE IF NOT EXISTS e não
-- teve efeito. O enum TipoMovimentacao e o EstoqueService usam VENDA, que era
-- recusada pelo banco.
-- Contexto: estoque

ALTER TABLE movimentacoes_estoque DROP CONSTRAINT IF EXISTS ck_movimentacao_tipo;

ALTER TABLE movimentacoes_estoque
    ADD CONSTRAINT ck_movimentacao_tipo
    CHECK (tipo_movimentacao IN ('ENTRADA', 'SAIDA', 'VENDA', 'RESERVA', 'AJUSTE'));
