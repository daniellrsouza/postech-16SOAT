# RFC-001 — Escolha da nuvem e da conta

- **Status:** Aceita
- **Data:** 2026-10-10

## Problema

A Fase 3 exige, em nuvem de livre escolha: API Gateway, Function Serverless, banco gerenciado,
cluster Kubernetes com escalabilidade e Terraform. A Fase 2 já provisionava VPC, EKS e ECR na AWS
com Terraform, em uma conta AWS Academy (Learner Lab).

## Opções

| Critério | AWS Academy (Learner Lab) | Conta AWS própria | Outra nuvem (Azure/GCP) |
|---|---|---|---|
| Custo para o grupo | Crédito do lab (US$ 50) | Cobrança real no cartão | Créditos de estudante, variáveis |
| Reaproveita a Fase 2 | Sim (Terraform, pipelines) | Sim | Não: reescrever toda a infra |
| IAM | Só a `LabRole`; sem criar roles, OIDC ou IRSA | Completo | Completo |
| Credenciais do CI | Expiram a cada sessão do lab | OIDC do GitHub, sem chave fixa | Federação de identidade |
| Serviços exigidos | API Gateway, Lambda, RDS, EKS disponíveis | Todos | Todos (equivalentes) |

## Recomendação

**AWS, na conta AWS Academy**, aceitando as restrições de IAM:

- Cluster, nós e Lambdas usam a `LabRole`; a sessão do lab assume a role `voclabs`, que recebe
  acesso de administrador ao EKS via Access Entry.
- Sem IRSA: nada de AWS Load Balancer Controller nem de autoscaler de nós
  ([ADR-005](../adr/ADR-005-nlb-nodeport-sem-ingress.md), [ADR-007](../adr/ADR-007-escalabilidade-hpa.md)).
- As credenciais do CI (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`) são
  renovadas a cada sessão por um script que atualiza os quatro repositórios.

## Riscos e mitigações

| Risco | Mitigação |
|---|---|
| Orçamento de US$ 50 | Um cluster e uma instância RDS para os dois ambientes; db.t3.micro; nós desligáveis (0–2); sem NAT |
| Fim de sessão bloqueia inclusive a `LabRole` dos nós; nós trocados nesse intervalo não se registram no EKS | Verificação e reboot automáticos dos nós não registrados ao renovar a sessão |
| Credenciais do CI expiram | Script único de renovação; pipelines falham com mensagem clara |
