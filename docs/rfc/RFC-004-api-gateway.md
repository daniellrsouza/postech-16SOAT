# RFC-004 — Escolha do API Gateway

- **Status:** Aceita
- **Data:** 2026-10-10 (uma API por ambiente: 2026-10-20)

## Problema

O enunciado exige um API Gateway para controle e roteamento, protegendo as rotas sensíveis. Ele
precisa rotear para uma Lambda (autenticação) e para a aplicação dentro do EKS, que não tem
endereço público.

## Opções

| Critério | **AWS API Gateway HTTP API** | AWS API Gateway REST API | Kong no cluster | Traefik no cluster |
|---|---|---|---|---|
| Gerenciado | Sim | Sim | Não (Helm, operação própria) | Não |
| Integração com Lambda | Nativa | Nativa | Plugin `aws-lambda` | Não nativa |
| Alcançar o EKS privado | VPC Link → NLB/ALB | VPC Link → NLB | Dentro do cluster | Dentro do cluster |
| Autorização na borda | Lambda Authorizer, JWT authorizer | Lambda/Cognito authorizer, API keys | Plugins JWT | Middlewares |
| Custo | Menor, por requisição | Maior, por requisição | Nós do cluster | Nós do cluster |
| Recursos extras | Menos (sem usage plans, sem WAF) | Usage plans, API keys, WAF, validação | Rate limit, muitos plugins | Limitado como gateway |

## Recomendação

**AWS API Gateway HTTP API**, provisionado com Terraform:

- **Uma API por ambiente**, com stage `$default`. O spike mostrou que o Lambda Authorizer não pode
  ser escolhido por stage variable ([ADR-003](../adr/ADR-003-uma-api-gateway-por-ambiente.md)).
- Integração privada: VPC Link compartilhado → NLB interno → NodePort da aplicação
  ([ADR-005](../adr/ADR-005-nlb-nodeport-sem-ingress.md)).
- Rotas: `POST /auth/cpf` → Lambda; `POST /auth/login`, `/api/public/**`, health e Swagger →
  aplicação, sem authorizer; demais rotas → aplicação, **com Lambda Authorizer**.
- Access log em JSON por API e header `X-Correlation-Id` (= `$context.requestId`) injetado em
  toda requisição à aplicação.

## Riscos e mitigações

| Risco | Mitigação |
|---|---|
| HTTP API não tem WAF nem usage plans | Fora do escopo; documentado como evolução possível |
| Dependência de um load balancer interno criado fora do Kubernetes | NLB, listeners e target groups criados e versionados pelo Terraform |
