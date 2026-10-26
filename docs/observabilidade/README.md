# Observabilidade (New Relic)

Como a Fase 3 atende aos requisitos de monitoramento do enunciado. A escolha da ferramenta está na
[RFC-005](../rfc/RFC-005-observabilidade-new-relic.md). Conta New Relic: **8662599** (região US).

## O que é coletado e de onde

| Fonte | Como chega ao New Relic | O que traz |
|---|---|---|
| Aplicação (Spring Boot) | Agente Java 9.5.0 (`-javaagent`, license key no Secret do Kubernetes) | APM: latência e vazão por endpoint, erros, **distributed tracing**, chamadas ao banco, logs da aplicação ligados ao trace (*logs in context*) |
| Eventos de negócio | API do agente (`TelemetriaAtendimentoPort`) | `OrdemServicoCriada`, `OrdemServicoStatusAlterado` (com `segundosNoStatusAnterior`), `OrdemServicoFalha`, `IntegracaoFalha` |
| Cluster EKS | Chart `nri-bundle` (repo `postech-16soat-infra-k8s`) | CPU e memória de nós, pods e containers; estado dos objetos (deployments, HPA); eventos do Kubernetes |
| Uptime | Monitor sintético (ping) no `/actuator/health` de cada ambiente, pelo API Gateway | Disponibilidade vista de fora |
| API Gateway e Lambdas | CloudWatch Logs (JSON) | Access logs com `requestId`; logs da autenticação por CPF e do authorizer |

**Correlação entre requisições:** o API Gateway gera um `requestId` por requisição e o envia à
aplicação no header `X-Correlation-Id`. O mesmo id aparece:

- no access log do gateway (`correlationId`) e nos logs das Lambdas, no CloudWatch;
- nos logs JSON da aplicação (campo `correlationId`, via MDC) e no atributo `correlationId` da
  transação APM e dos eventos de negócio, no New Relic.

Para seguir uma requisição: no New Relic, `FROM Log WHERE correlationId = '<id>'`; no CloudWatch,
filtre os log groups `/aws/apigateway/api-tech-challenge-<env>` e `/aws/lambda/*-<env>` pelo mesmo id.

## Dashboard

Arquivo: [`dashboard-oficina.json`](dashboard-oficina.json), com 3 páginas e 18 gráficos, cada um uma
consulta NRQL.

| Página | Exigência do enunciado |
|---|---|
| Negócio | Volume diário de OS; tempo médio por status (Diagnóstico, Execução, Finalização); falhas no processamento de OS |
| Erros e integrações | Erros e falhas nas integrações (SMTP, banco); erros da aplicação; logs de erro |
| APIs e Kubernetes | Latência das APIs (média e p95); taxa de erro; CPU e memória por namespace e por nó; réplicas do HPA; uptime |

**Importar:** New Relic → **Dashboards** → **Import dashboard** → cole o conteúdo do JSON →
**Import dashboard**.

"Tempo médio por status" é o tempo que a OS passou em `EM_DIAGNOSTICO`, `EM_EXECUCAO` e
`FINALIZADA`, medido quando ela sai desse status (atributo `segundosNoStatusAnterior` do evento
`OrdemServicoStatusAlterado`, calculado a partir do histórico `os_historico_status`).

## Healthcheck e uptime (monitor sintético)

New Relic → **Synthetic Monitoring** → **Create monitor** → **Ping**, um por ambiente:

| Nome | URL | Frequência |
|---|---|---|
| `oficina-health-homolog` | `<API homolog>/actuator/health` | 15 min |
| `oficina-health-prod` | `<API prod>/actuator/health` | 5 min |

As URLs ficam em `/tech-challenge/<env>/apigateway-api-endpoint` (SSM). Em *Text validation*, use
`"status":"UP"`. Dentro do cluster, as probes do Kubernetes usam o mesmo endpoint (startup,
readiness e liveness).

## Alertas

New Relic → **Alerts** → **Alert policies** → **New alert policy** `Oficina - Fase 3` → **Add a
condition** → **Write your own query** (NRQL), com as condições abaixo.

| Condição | NRQL | Limite |
|---|---|---|
| **Falha no processamento de OS** | `SELECT count(*) FROM OrdemServicoFalha` | Crítico: acima de 0 por pelo menos 1 minuto |
| Falha de integração (SMTP) | `SELECT count(*) FROM IntegracaoFalha` | Alerta: acima de 0 em 5 minutos |
| Taxa de erro da API (prod) | `SELECT percentage(count(*), WHERE error IS true) FROM Transaction WHERE appName = 'tech-challenge-backend (prod)'` | Crítico: acima de 5% por 5 minutos |
| Latência p95 da API (prod) | `SELECT percentile(duration, 95) FROM Transaction WHERE appName = 'tech-challenge-backend (prod)'` | Crítico: acima de 2 s por 5 minutos |
| Health indisponível | `SELECT count(*) FROM SyntheticCheck WHERE result != 'SUCCESS'` | Crítico: acima de 0 por pelo menos 1 minuto |
| Pod da aplicação reiniciando | `SELECT max(restartCountDelta) FROM K8sContainerSample WHERE clusterName = 'eks-tech-challenge' AND containerName = 'app' FACET namespaceName` | Alerta: acima de 0 |

Para a notificação: na política, **Add a workflow** → destino e-mail com o endereço do time.

## Logs

- **Aplicação:** JSON (formato ECS do Spring Boot) no stdout (`kubectl -n <env> logs deploy/tech-challenge-app`)
  e enviados pelo agente ao New Relic (**Logs**, filtro `entity.name LIKE 'tech-challenge-backend%'`).
- **Lambdas:** JSON no CloudWatch (`/aws/lambda/auth-cpf-<env>`, `/aws/lambda/authorizer-<env>`), com o
  CPF mascarado.
- **API Gateway:** access log JSON no CloudWatch (`/aws/apigateway/api-tech-challenge-<env>`).
- **Banco:** log do Postgres no CloudWatch, incluindo consultas acima de 500 ms.
