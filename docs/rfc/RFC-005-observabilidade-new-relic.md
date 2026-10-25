# RFC-005 — Ferramenta e estratégia de observabilidade

- **Status:** Aceita
- **Data:** 2026-10-10 (simplificações por prazo: 2026-10-25)

## Problema

O enunciado exige integração com uma ferramenta como Datadog ou New Relic, monitorando latência
das APIs, consumo de recursos do Kubernetes, healthchecks e uptime, alertas para falhas no
processamento de ordens de serviço e logs estruturados em JSON com correlação entre requisições.
Exige também dashboards de volume diário de OS, tempo médio de execução por status e erros nas
integrações.

## Opções

| Critério | **New Relic** | Datadog |
|---|---|---|
| Custo para o projeto | Free tier permanente: 100 GB/mês e 1 usuário completo | Trial de 14 dias (ou GitHub Student Pack) |
| APM Java | Agente Java | Agente Java |
| Kubernetes | Integração via Helm | Agente via Helm/Operator |
| Logs, traces, dashboards, alertas | Sim (NRQL) | Sim |
| Monitor de uptime | Synthetics | Synthetics |

## Recomendação

**New Relic**, pelo free tier permanente, que cobre o projeto e a avaliação sem prazo de expiração.

| Exigência | Como é atendida |
|---|---|
| Latência das APIs | APM do agente Java (tempo de resposta por transação) e access logs do API Gateway |
| CPU e memória do Kubernetes | Integração Kubernetes do New Relic no cluster |
| Healthchecks e uptime | Probes no `/actuator/health` + monitor sintético pelo gateway de cada ambiente |
| Alertas de falha no processamento de OS | Evento de falha emitido pela aplicação + condição de alerta NRQL |
| Logs JSON com correlação | Log estruturado do Spring Boot com `correlationId` vindo do header `X-Correlation-Id` |
| Volume diário de OS | Evento a cada OS criada |
| Tempo médio por status | Evento a cada transição de status, com o tempo gasto no status anterior |
| Erros nas integrações | Erros de banco e SMTP na aplicação; falhas da Lambda nos logs do CloudWatch |

Simplificações adotadas pelo prazo de entrega:

- Dashboards e alertas montados na interface do New Relic, com o JSON exportado e versionado em
  `docs/`, em vez de Terraform.
- Logs da Lambda só no CloudWatch: ela fica em subnet privada sem NAT
  ([ADR-006](../adr/ADR-006-rede-privada-sem-nat.md)) e não alcança o New Relic. O correlation id
  liga as duas fontes.

## Riscos e mitigações

| Risco | Mitigação |
|---|---|
| Logs em DEBUG estourando os 100 GB/mês | Nível INFO nos ambientes da nuvem |
| Dashboards recriados à mão se a conta New Relic mudar | JSON exportado versionado, importável pela interface |
