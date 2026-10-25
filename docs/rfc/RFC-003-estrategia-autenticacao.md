# RFC-003 — Estratégia de autenticação por CPF

- **Status:** Aceita
- **Data:** 2026-10-10 (contrato e claims: 2026-10-12)

## Problema

O enunciado pede proteger as rotas sensíveis com autenticação via CPF e uma Function Serverless
que valide o CPF, consulte a existência e o status do cliente e devolva um JWT válido para as APIs
protegidas. A aplicação já tinha login por e-mail e senha para ADMIN e FUNCIONARIO, emitindo JWT
HS256 validado pelo Spring Security.

## Opções

**Quem autentica por CPF**

| Opção | Prós | Contras |
|---|---|---|
| **CPF só para clientes; funcionários no `/auth/login`** | Segue o enunciado; preserva o login com senha de quem tem permissões administrativas | Dois emissores de token |
| CPF para todos, sem senha | Um único fluxo | Quem conhece o CPF de um admin obtém acesso administrativo |
| CPF + senha para todos | Mais seguro | Vai além do enunciado; todo cliente precisaria de senha |

**Assinatura e validação do token**

| Opção | Prós | Contras |
|---|---|---|
| **HS256 + Lambda Authorizer + validação na aplicação** | Menor mudança no backend; barra tokens inválidos na borda | Segredo compartilhado entre Lambda e aplicação |
| HS256 validado só na aplicação | Mínimo de trabalho | O gateway não protege nada |
| RS256 + JWKS + JWT authorizer nativo | Sem segredo compartilhado; sem Lambda de autorização | Troca a validação do backend; convive com o HS256 do `/auth/login` |

**Status do cliente**: a tabela `clientes` não tinha status. Opções: coluna `status` (ATIVO,
INATIVO, BLOQUEADO), booleano `ativo`, ou reaproveitar soft delete.

## Recomendação

- Cliente autentica só com CPF na Lambda `auth-cpf`; ADMIN e FUNCIONARIO continuam no
  `/auth/login`.
- Fluxo da Lambda: normaliza o CPF (11 dígitos ou `000.000.000-00`) → valida os dígitos
  verificadores → consulta `clientes` → só emite token para `status = ATIVO`.
- Respostas distintas, para tornar explícitas as validações do enunciado: 400 (CPF inválido),
  404 (cliente inexistente), 403 (cliente não ativo), 200 com `{"accessToken": "..."}`.
- Token HS256, `sub` = id do cliente, `role` = `CLIENTE`, `iss` = `tech-challenge-auth`, 1 h. O CPF
  não vai no token, e aparece mascarado nos logs (LGPD).
- Lambda Authorizer em cada API valida os tokens da Lambda e do `/auth/login`; a aplicação
  revalida e aplica a autorização por perfil.
- Nova coluna `clientes.status` com `CHECK` e default `ATIVO`.

Decisão registrada no [ADR-004](../adr/ADR-004-jwt-hs256-lambda-authorizer.md).

## Riscos e mitigações

| Risco | Mitigação |
|---|---|
| Respostas distintas permitem descobrir se um CPF está cadastrado | Escolha consciente pela clareza das validações; rate limiting no gateway é uma evolução possível |
| CPF é um fator de autenticação fraco | Token de cliente com escopo restrito ao perfil `CLIENTE` e validade curta |
| Divergência do segredo entre Lambda e aplicação | Segredo gerado em um único lugar por ambiente ([ADR-010](../adr/ADR-010-segredos-terraform-ssm.md)) |
| Dados do seed com CPF inválido (`10387398790`), que a aplicação aceita por validar só o formato | A Lambda recusa com 400; registrado na revisão do modelo |
