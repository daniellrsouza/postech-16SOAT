# RFC-002 — Escolha do banco de dados gerenciado

- **Status:** Aceita
- **Data:** 2026-10-10 (instância e usuários: 2026-10-25)

## Problema

Até a Fase 2, o PostgreSQL rodava como StatefulSet dentro do cluster, com volume EBS. A Fase 3
exige banco gerenciado e pede uma justificativa formal da escolha, garantindo consistência e
performance. A justificativa completa e o modelo relacional estão em
[docs/banco/modelo-de-dados.md](../banco/modelo-de-dados.md).

## Opções

| Critério | RDS for PostgreSQL | Aurora PostgreSQL | DynamoDB | PostgreSQL no cluster (atual) |
|---|---|---|---|---|
| Compatibilidade com a aplicação | Total (JPA, Flyway e migrations atuais) | Total | Exige reescrever a persistência | Total |
| Modelo dos dados | Relacional, com FKs e transações | Relacional | Chave-valor/documento | Relacional |
| Gerenciado (backup, patch, TLS) | Sim | Sim | Sim | Não |
| Custo mínimo | db.t3.micro, ~US$ 0,02/h | Maior (instância mínima mais cara) | Por requisição | Só os nós |
| Atende "banco gerenciado" | Sim | Sim | Sim | Não |

## Recomendação

**Amazon RDS for PostgreSQL 16**:

- O domínio é fortemente relacional (ordem de serviço → orçamentos → peças e serviços; cliente ↔
  veículo; histórico de status) e depende de integridade referencial e transações.
- Mesma engine e versão da Fase 2: as migrations Flyway rodam sem mudança.
- `db.t3.micro`, **Single-AZ**, 20 GB gp3 criptografado, em subnets privadas, TLS obrigatório,
  backup automático e log de consultas lentas (> 500 ms) exportado para o CloudWatch.
- Uma instância com um database por ambiente; por ambiente, um usuário da aplicação (dono do
  schema) e um usuário só-leitura em `clientes` para a Lambda.
- Senha master gerenciada pelo RDS no Secrets Manager.

## Riscos e mitigações

| Risco | Mitigação |
|---|---|
| Single-AZ: sem failover automático | Limitação aceita pelo orçamento do lab; em produção real, `multi_az = true` e uma classe maior |
| Uma instância para dois ambientes | Databases, usuários e permissões de conexão separados por ambiente |
| Consultas dos dashboards e listagens ficarem lentas com volume | Índices definidos na revisão do modelo; log de consultas lentas |
