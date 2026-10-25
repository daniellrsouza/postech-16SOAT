# ADR-004 — JWT HS256 validado por Lambda Authorizer e pela aplicação

- **Status:** Aceito
- **Data:** 2026-10-10 (detalhes do token e do authorizer: 2026-10-12 e 2026-10-25)

## Contexto

O enunciado pede proteger as rotas sensíveis com autenticação via CPF, e uma Function Serverless
que valide o CPF, consulte a existência e o status do cliente e devolva um JWT válido para as APIs
protegidas. A aplicação já emitia JWT HS256 para ADMIN e FUNCIONARIO (`POST /auth/login`, e-mail e
senha) e já os validava com Spring Security. A estratégia de autenticação foi discutida na
[RFC-003](../rfc/RFC-003-estrategia-autenticacao.md).

## Decisão

- **Clientes** autenticam só pelo CPF, na Lambda `auth-cpf` (`POST /auth/cpf`). **ADMIN e
  FUNCIONARIO** continuam no `POST /auth/login` da aplicação, exposto pelo gateway.
- Token do cliente: HS256, `sub` = id do cliente (UUID), `role` = `CLIENTE`,
  `iss` = `tech-challenge-auth`, validade de 1 h. **O CPF não vai no token.**
- Respostas da Lambda: CPF inválido → 400; cliente inexistente → 404; cliente não ativo → 403;
  sucesso → 200 `{"accessToken": "..."}`, no mesmo formato do `/auth/login`.
- O **Lambda Authorizer** valida na borda todo token enviado às rotas protegidas: assinatura HS256,
  expiração e emissor (`tech-challenge-auth` ou `tech-challenge-backend`). Ele repassa `sub` e
  `role` ao gateway.
- A aplicação continua validando o token (defesa em profundidade) e é dona da autorização por
  perfil (`@PreAuthorize`).
- Um segredo JWT **por ambiente**, gerado pelo Terraform e compartilhado entre Lambda e aplicação
  (ver [ADR-010](ADR-010-segredos-terraform-ssm.md)).

## Alternativas consideradas

- **Validação só na aplicação**: o gateway apenas rotearia, e nada seria barrado na borda.
- **RS256 + JWKS + JWT authorizer nativo do HTTP API**: dispensaria a Lambda de autorização e
  eliminaria o segredo compartilhado, mas exigiria trocar a validação do backend e manter dois
  esquemas de assinatura convivendo com o `/auth/login` atual.
- **CPF + senha para todos**: mais seguro, mas vai além do que o enunciado descreve e exigiria que
  todo cliente tivesse usuário com senha.

## Consequências

- Requisições sem token válido são barradas no gateway, sem consumir a aplicação.
- O segredo HS256 precisa ser o mesmo na Lambda e na aplicação de cada ambiente; por isso ele é
  gerado em um único lugar.
- Autenticar só com CPF é um fator fraco: quem conhece o CPF de um cliente ativo obtém um token
  com permissões de `CLIENTE`. O escopo dessas permissões precisa ficar restrito às consultas do
  próprio cliente.
