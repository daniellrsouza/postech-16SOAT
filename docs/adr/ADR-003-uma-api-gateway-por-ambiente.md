# ADR-003 — Uma HTTP API do API Gateway por ambiente

- **Status:** Aceito (substitui a decisão inicial de um stage por ambiente)
- **Data:** 2026-10-20

## Contexto

A decisão inicial era usar uma única HTTP API com dois stages (`homolog` e `prod`) e selecionar a
Lambda, o authorizer e o backend de cada ambiente por **stage variables**. Como isso dependia de
comportamentos pouco documentados do HTTP API, foi feito um **spike** descartável na conta do lab
(Lambdas de eco, Function URL como backend e testes com `curl`, destruídos ao final).

Resultados do spike:

| Pergunta | Resultado |
|---|---|
| Integração Lambda escolhida por stage variable | Funciona |
| Path reescrito com prefixo do ambiente (`overwrite:path`) | Funciona, com a forma `${...}` |
| **Lambda Authorizer escolhido por stage variable** | **Não funciona**: o gateway nunca invoca a função; com o nome fixo, o mesmo authorizer funciona |
| Access logs em JSON | Funcionam; o formato exige `$context.requestId` |

Sem o authorizer por stage, um único authorizer teria que validar tokens dos dois ambientes.

## Decisão

Uma **HTTP API por ambiente** (`api-tech-challenge-homolog` e `api-tech-challenge-prod`), cada
uma com stage `$default`, seu próprio Lambda Authorizer, suas Lambdas e seu segredo JWT. As duas
compartilham o mesmo VPC Link e o mesmo NLB interno.

A API, o stage, os access logs e a integração com o cluster são criados pelo `infra-k8s`; as
funções, o authorizer e as rotas, pelo repositório da Lambda.

## Alternativas consideradas

- **Uma API, authorizer único ciente do stage**: mantém um stage por ambiente, mas a mesma função
  guardaria os segredos de homolog e prod, e qualquer deploy dela afetaria os dois ambientes,
  quebrando o fluxo `develop` → homolog.
- **Uma API com segredo JWT único**: mais simples, mas um token emitido em homolog passaria a
  valer em prod.

## Consequências

- Isolamento completo de credenciais e deploy entre ambientes.
- Cada ambiente tem sua própria URL de API.
- Com stage `$default`, o path chega inteiro ao backend, sem reescrita.
- O custo não muda: HTTP API cobra por requisição e o VPC Link é compartilhado.
