# CLAUDE.md

Contexto para sessões novas ou após compactação. Leia este arquivo e depois
[`FASE3_PLANO.md`](FASE3_PLANO.md) antes de qualquer ação.

## O que é este repositório

Tech Challenge FIAP/SOAT, Grupo 15. API REST de oficina mecânica (ordens de serviço,
orçamentos, estoque, cadastro), Java 21 / Spring Boot 3, arquitetura hexagonal por
contexto (`acesso`, `cadastro`, `atendimento`, `estoque`, `acompanhamento`, `shared`).

- Código, manifests e Terraform: [`tech-challenge-backend/`](tech-challenge-backend/)
- Pipelines: [`.github/workflows/`](.github/workflows/)
- Enunciado da fase atual: `13SOAT - Fase 3 - Tech Challenge.pdf` (raiz; extrair texto
  com `pdftotext -layout`, o leitor de PDF por página não funciona nesta máquina)
- Remotes: `origin` = daniellrsouza/postech-16soat-app (renomeado de postech-16SOAT em
  2026-10-12; o diretório local continua `postech-16SOAT`), `upstream` =
  lucianobsm/postech-16SOAT (repositório do grupo, Fases 1 e 2)

## Repositórios da Fase 3 (conta daniellrsouza, públicos)

| Repositório | Clone local |
|---|---|
| `postech-16soat-app` (este) | `C:\Users\Daniel\postech\postech-16SOAT` |
| `postech-16soat-infra-k8s` | `C:\Users\Daniel\postech\postech-16soat-infra-k8s` |
| `postech-16soat-infra-db` | `C:\Users\Daniel\postech\postech-16soat-infra-db` |
| `postech-16soat-lambda` | `C:\Users\Daniel\postech\postech-16soat-lambda` |

Plano e progresso dos quatro ficam só aqui, em `FASE3_PLANO.md`.

## Trabalho em andamento: Fase 3

[`FASE3_PLANO.md`](FASE3_PLANO.md) é a fonte de verdade: decisões já tomadas com o
usuário, plano de implementação e checklist de progresso. **Atualize-o a cada decisão
ou etapa concluída**, não só no fim da sessão.

## Regras de trabalho

1. **Escopo é o PDF.** Não implementar nada além do que o enunciado da Fase 3 pede.
   Melhorias fora do enunciado entram apenas como sugestão, na seção de sugestões do
   plano.
2. **Não adivinhar.** Qualquer escolha de tecnologia, nome, estratégia ou trade-off
   ainda não registrado em `FASE3_PLANO.md` deve ser perguntada ao usuário
   (`AskUserQuestion`) antes de ser adotada.
3. **Decisão registrada não se rediscute.** O que está na seção de decisões do plano
   já foi escolhido pelo usuário.
4. **Git:** commit e push **só em branches `feature/*`** (autorizado em 2026-10-23, D22).
   Nunca commitar ou dar push direto em `develop`/`main`; PR, merge e criação de
   repositório só a pedido explícito. Ações no GitHub (settings, secrets, convites)
   exigem confirmação a cada ação.
5. **Conta AWS é AWS Academy (Learner Lab):** só existe a `LabRole`. Não é possível
   criar IAM roles/policies, OIDC provider nem IRSA; as credenciais do pipeline
   (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`) expiram a cada
   sessão do lab. Todo recurso novo (Lambda, RDS, API Gateway) precisa funcionar com a
   `LabRole`.
6. Ao mexer no backend, valem também as regras de
   [`tech-challenge-backend/AGENTS.md`](tech-challenge-backend/AGENTS.md) (testes
   obrigatórios, armadilhas do Surefire/Failsafe, colisão de nome de bean, etc.).

## Armadilhas desta máquina (Windows + Git Bash)

- **Push:** o Git Credential Manager trava o `git push` por minutos. Use
  `git -c credential.helper= -c 'credential.helper=!gh auth git-credential' push`.
- **Caminhos `/...` na AWS CLI:** o Git Bash converte `/tech-challenge/...` em caminho do
  Windows; prefixe com `MSYS_NO_PATHCONV=1` (parâmetros SSM, log groups).
- **CRLF:** `aws.exe` e `kubectl.exe` terminam linhas com ``; use `tr -d ''` ao
  comparar saídas em scripts. Não edite scripts com Python em modo texto quando houver
  `` envolvido (a leitura converte o CR).
- **Sessão do lab encerrada:** a política `voc-cancel-cred` nega tudo, inclusive a
  `LabRole` dos nós. Nós trocados nesse intervalo não se registram no EKS: rodar
  `scripts/atualizar-secrets-aws.sh` (reinicia só os não registrados; `SO_VERIFICAR=1`
  para só relatar). Nunca testar esse trecho sem `SO_VERIFICAR=1`.

## Comandos úteis (dentro de `tech-challenge-backend/`)

- Build: `./mvnw clean package -DskipTests`
- Testes unitários + Cucumber: `./mvnw test`
- Testes de integração (`*IT`, Testcontainers): `./mvnw verify -Pintegration-tests -Dsurefire.skip=true`
- Ambiente local: `docker compose up --build` (API em `:8080`, Swagger em `/swagger-ui.html`)
- Terraform: `cd infra && terraform init && terraform plan`
