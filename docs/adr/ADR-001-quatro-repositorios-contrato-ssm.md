# ADR-001 — Quatro repositórios com contrato via SSM Parameter Store

- **Status:** Aceito
- **Data:** 2026-10-10 (contrato via SSM: 2026-10-12)

## Contexto

O enunciado da Fase 3 exige quatro repositórios separados, cada um com CI/CD e deploy automático:
Lambda, infraestrutura Kubernetes, infraestrutura do banco gerenciado e aplicação. Até a Fase 2,
aplicação, manifests Kubernetes e Terraform viviam no mesmo repositório, e o Terraform aplicava
inclusive os manifests da aplicação.

Separar os repositórios cria dependências entre eles: o banco precisa da VPC criada pela infra
Kubernetes; a Lambda precisa do banco, da VPC e da API do gateway; o pipeline da aplicação precisa
do cluster, do ECR e das credenciais do banco.

## Decisão

| Repositório | Responsabilidade |
|---|---|
| `postech-16soat-infra-k8s` | VPC, EKS, ECR, NLB interno, VPC Link e uma HTTP API por ambiente |
| `postech-16soat-infra-db` | RDS PostgreSQL, databases, usuários e segredos por ambiente |
| `postech-16soat-lambda` | Funções de autenticação por CPF e authorizer, e as rotas do gateway |
| `postech-16soat-app` | Aplicação, manifests Kubernetes da aplicação e esta documentação |

Ordem de provisionamento: infra-k8s → infra-db → lambda → app.

O contrato entre os repositórios é o **SSM Parameter Store**. Cada repositório publica suas saídas
em `/tech-challenge/shared/...` (recursos compartilhados) ou `/tech-challenge/<env>/...` (por
ambiente), e os seguintes leem esses parâmetros. Dados sensíveis vão como `SecureString`.

O repositório atual da aplicação foi mantido (histórico, PRs e SonarCloud); o Terraform de
infraestrutura foi extraído para `postech-16soat-infra-k8s` com o histórico da pasta preservado.

## Alternativas consideradas

- **`terraform_remote_state`** lendo o state dos outros repositórios: dispensa recursos extras, mas
  acopla cada repositório ao formato interno do state alheio e não serve ao pipeline da aplicação,
  que não usa Terraform.
- **Variáveis copiadas manualmente** entre repositórios: frágil e sujeito a divergência.

## Consequências

- Cada repositório evolui e faz deploy de forma independente, desde que o contrato seja mantido.
- O contrato fica explícito e documentado no README de cada repositório.
- Remover ou renomear um parâmetro passa a ser uma mudança de contrato, e pode quebrar os
  repositórios que o consomem.
