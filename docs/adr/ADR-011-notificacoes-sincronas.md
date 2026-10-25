# ADR-011 — Notificações por e-mail continuam síncronas na aplicação

- **Status:** Aceito
- **Data:** 2026-10-10 (reconfirmado em 2026-10-23)

## Contexto

A introdução do enunciado menciona "adotar soluções serverless para autenticação e notificações",
mas a lista de requisitos obrigatórios exige apenas a Function Serverless de autenticação. A
aplicação já envia e-mails (orçamento para aprovação, mudanças de status) via SMTP, de forma
síncrona.

## Decisão

Manter o envio de e-mail **na aplicação, via SMTP**, fora do escopo serverless desta fase. As falhas
de envio são tratadas como falhas de integração e aparecem nos logs e no dashboard de erros
(ver [RFC-005](../rfc/RFC-005-observabilidade-new-relic.md)).

## Alternativas consideradas

- **Notificação serverless**: a aplicação publicaria um evento (SNS/SQS) e uma Lambda enviaria o
  e-mail. Desacopla o envio e permite reprocessamento, mas adiciona fila, função, permissões e
  mudanças no contexto `atendimento`, fora da lista de obrigatórios e do prazo da fase.

## Consequências

- Uma lentidão ou falha do SMTP afeta o tempo de resposta da operação que dispara o e-mail.
- Evolução natural documentada: extrair o envio para uma função serverless orientada a eventos,
  aproveitando a mesma infraestrutura de Lambda e o contrato via SSM.
