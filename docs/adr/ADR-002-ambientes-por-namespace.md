# ADR-002 — Ambientes homolog e prod em um cluster e uma instância RDS

- **Status:** Aceito
- **Data:** 2026-10-10

## Contexto

O enunciado exige deploy automático das branches de homologação e produção. A conta usada é a do
AWS Academy (Learner Lab), com orçamento total de US$ 50 até a entrega e credenciais que expiram a
cada sessão. Um cluster EKS custa US$ 0,10/h só pelo control plane, sem contar os nós.

## Decisão

- **Um cluster EKS** com um namespace por ambiente: `homolog` e `prod`.
- **Uma instância RDS** com um database por ambiente (`tech_challenge_homolog`,
  `tech_challenge_prod`), cada um com usuários próprios que só se conectam ao seu database.
- **Uma HTTP API do API Gateway por ambiente** (ver [ADR-003](ADR-003-uma-api-gateway-por-ambiente.md)).
- Fluxo de branches: `feature/*` → PR para `develop` (deploy em homolog) → PR `develop` → `main`
  (deploy em prod). A `main` é protegida e só recebe mudanças por Pull Request.
- Os environments `homolog` e `prod` do GitHub só aceitam deploy das branches `develop` e `main`,
  respectivamente.

Nos repositórios de infraestrutura, o Terraform é dividido em dois stacks: `shared` (aplicado pela
`main`) e `env` (aplicado com `environment=homolog` pela `develop` e `environment=prod` pela `main`).

## Alternativas consideradas

- **Duas stacks completas** (cluster, banco e gateway por ambiente): isolamento total, mas dobra o
  custo e o tempo de provisionamento a cada sessão do lab.
- **Um cluster e duas instâncias RDS**: isola os dados em instâncias distintas, com custo
  intermediário.

## Consequências

- Custo fixo de uma só infraestrutura, compatível com o orçamento do laboratório.
- O isolamento entre ambientes é lógico (namespaces, databases, usuários, segredos e APIs
  distintos), não físico. Um problema no cluster ou na instância afeta os dois ambientes.
- Mudanças nos recursos compartilhados (stack `shared`) só são aplicadas pela `main`.
