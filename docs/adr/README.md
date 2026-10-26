# ADRs — Architecture Decision Records

Decisões arquiteturais permanentes da Fase 3. Cada ADR registra o contexto, a decisão, as
alternativas consideradas e as consequências. Decisões técnicas que passaram por discussão de
opções estão nas [RFCs](../rfc/README.md).

| ADR | Título | Status |
|---|---|---|
| [ADR-001](ADR-001-quatro-repositorios-contrato-ssm.md) | Quatro repositórios com contrato via SSM Parameter Store | Aceito |
| [ADR-002](ADR-002-ambientes-por-namespace.md) | Ambientes homolog e prod em um cluster e uma instância RDS | Aceito |
| [ADR-003](ADR-003-uma-api-gateway-por-ambiente.md) | Uma HTTP API do API Gateway por ambiente | Aceito |
| [ADR-004](ADR-004-jwt-hs256-lambda-authorizer.md) | JWT HS256 validado por Lambda Authorizer e pela aplicação | Aceito |
| [ADR-005](ADR-005-nlb-nodeport-sem-ingress.md) | Entrada do cluster por NLB interno e NodePort, sem ingress controller | Aceito |
| [ADR-006](ADR-006-rede-privada-sem-nat.md) | Subnets privadas sem NAT Gateway | Aceito |
| [ADR-007](ADR-007-escalabilidade-hpa.md) | Escalabilidade com HPA e node group gerenciado | Aceito |
| [ADR-008](ADR-008-comunicacao-sincrona-rest.md) | Comunicação síncrona REST via API Gateway | Aceito |
| [ADR-009](ADR-009-migrations-na-aplicacao.md) | Migrations do schema na aplicação (Flyway) | Aceito |
| [ADR-010](ADR-010-segredos-terraform-ssm.md) | Segredos gerados pelo Terraform e distribuídos por SSM SecureString | Aceito |
| [ADR-011](ADR-011-notificacoes-sincronas.md) | Notificações por e-mail continuam síncronas na aplicação | Aceito |
