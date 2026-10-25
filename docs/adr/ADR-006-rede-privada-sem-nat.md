# ADR-006 — Subnets privadas sem NAT Gateway

- **Status:** Aceito
- **Data:** 2026-10-12 (terceira subnet privada: 2026-10-23)

## Contexto

O banco gerenciado e a Lambda de autenticação não devem ser alcançáveis pela internet. Um NAT
Gateway custa cerca de US$ 0,045/h mais tráfego (~US$ 1/dia), um peso relevante no orçamento de
US$ 50 do laboratório.

## Decisão

- **Subnets públicas** (3 AZs): nós do EKS, com IP público para baixar imagens do ECR e de
  registries públicos.
- **Subnets privadas** (3 AZs), **sem rota para a internet**: RDS, Lambda, NLB interno e VPC Link.
- **Sem NAT Gateway.**
- Acesso ao RDS só pelos security groups dos nós do EKS e dos clientes autorizados (Lambda), com
  TLS obrigatório (`rds.force_ssl`).

## Alternativas consideradas

- **NAT Gateway**: permitiria à Lambda falar com serviços públicos (Secrets Manager, New Relic),
  com custo fixo diário.
- **Usar as subnets públicas para tudo**: menos trabalho, com isolamento apenas por security group.

## Consequências

- A Lambda na VPC não alcança serviços públicos: os segredos chegam a ela por variáveis de ambiente
  no deploy, e seus logs ficam no CloudWatch (não vão direto para o New Relic).
- Os nós ficam em subnets públicas, protegidos pelo security group do cluster, que só libera as
  portas NodePort para dentro da VPC.
