# ADR-008 — Comunicação síncrona REST via API Gateway

- **Status:** Aceito
- **Data:** 2026-10-25

## Contexto

Os clientes da API (front-ends, ferramentas de teste e integrações) consomem operações de
cadastro, abertura e acompanhamento de ordens de serviço, que precisam de resposta imediata. A
aplicação é um monólito modular (contextos `acesso`, `cadastro`, `atendimento`, `estoque`,
`acompanhamento`) com um único banco.

## Decisão

- Toda comunicação externa é **síncrona, HTTP/REST com JSON**, entrando pelo API Gateway do
  ambiente.
- O gateway roteia: `POST /auth/cpf` para a Lambda de autenticação; rotas públicas e protegidas
  para a aplicação no EKS pela integração privada (VPC Link → NLB → NodePort).
- Os contextos da aplicação se comunicam em processo, por portas e adaptadores, sem chamadas de
  rede entre si.
- Toda requisição recebe do gateway o header `X-Correlation-Id` (= `$context.requestId`), que a
  aplicação e a Lambda registram nos logs. É assim que se correlacionam as requisições.

## Alternativas consideradas

- **Mensageria assíncrona** (SQS/SNS) entre partes do sistema: útil para notificações e cargas
  desacopladas, mas não há hoje fluxo que exija processamento assíncrono (ver
  [ADR-011](ADR-011-notificacoes-sincronas.md)).
- **Separar os contextos em microsserviços**: sem ganho para o escopo atual, e com mais custo
  operacional.

## Consequências

- Fluxo simples de seguir e de depurar, com um id único por requisição do gateway até o banco.
- Falhas de dependências síncronas (banco, SMTP) aparecem diretamente para o cliente e precisam
  ser monitoradas como falhas de integração.
