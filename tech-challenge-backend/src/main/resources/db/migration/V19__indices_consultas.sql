-- V19__indices_consultas.sql
-- Fase 3 · ajustes C, D e E do modelo de dados (performance).
-- Contexto: atendimento, cadastro, acesso

-- C. Índices compostos alinhados ao WHERE/ORDER BY das consultas reais
--    (OrdemServicoRepository e OsHistoricoStatusRepository).

-- findAllByStatusPrioritized: WHERE status = ? ORDER BY urgente DESC, data_criacao
-- findAllAtivasPrioritized:   WHERE status IN (...) ORDER BY ..., data_criacao
CREATE INDEX IF NOT EXISTS idx_ordens_servico_status_urgente_criacao
    ON ordens_servico (status, urgente DESC, data_criacao);

-- findByClienteIdWithDetails (acompanhamento do cliente):
-- WHERE cliente_id = ? ORDER BY data_criacao DESC. Substitui o índice simples.
CREATE INDEX IF NOT EXISTS idx_ordens_servico_cliente_criacao
    ON ordens_servico (cliente_id, data_criacao DESC);
DROP INDEX IF EXISTS idx_ordens_servico_cliente_id;

-- Histórico de status (base do tempo por status):
-- WHERE ordem_servico_id IN (...) ORDER BY ordem_servico_id, data_mudanca.
-- Substitui o índice simples.
CREATE INDEX IF NOT EXISTS idx_os_historico_status_os_data
    ON os_historico_status (ordem_servico_id, data_mudanca);
DROP INDEX IF EXISTS idx_os_historico_status_ordem_servico_id;

-- D. Chaves estrangeiras sem índice: joins de enriquecimento da OS e verificação
--    das FKs ao excluir veículos (ON DELETE RESTRICT) e usuários (SET NULL).
CREATE INDEX IF NOT EXISTS idx_ordens_servico_veiculo_id ON ordens_servico (veiculo_id);
CREATE INDEX IF NOT EXISTS idx_ordens_servico_mecanico_id ON ordens_servico (mecanico_id);

-- E. Índices redundantes: as constraints UNIQUE já criam índice nas mesmas
--    colunas; os duplicados só custam escrita e espaço.
DROP INDEX IF EXISTS idx_usuario_email;      -- uk_usuario_email
DROP INDEX IF EXISTS idx_usuario_cpf_cnpj;   -- uk_usuario_cpf_cnpj
DROP INDEX IF EXISTS idx_cliente_cpf_cnpj;   -- uk_cliente_cpf_cnpj
DROP INDEX IF EXISTS idx_veiculo_placa;      -- uk_veiculo_placa
