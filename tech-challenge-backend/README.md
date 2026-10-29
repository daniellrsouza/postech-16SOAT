# Tech Challenge Backend

API REST para gerenciamento de ordens de serviço em oficina mecânica, desenvolvida com Spring Boot 3, PostgreSQL e arquitetura hexagonal.

> Visão geral da Fase 3 (arquitetura na nuvem, ambientes, CI/CD, observabilidade e documentação): [README da raiz](../README.md). Este arquivo detalha a execução local.

## 📋 Visão Geral

Sistema completo para:
- Autenticação e controle de acesso (usuários)
- Gerenciamento de clientes e veículos
- Criação e acompanhamento de ordens de serviço (OS)
- Gerenciamento de orçamentos e aprovações
- Controle de peças e estoque
- Rastreamento de histórico de status e operações

### Principais entregas
- Abertura de OS com cliente, veículo, serviços e peças
- Consulta e atualização do status da OS
- Aprovação/rejeição de orçamento por endpoint público
- Listagem priorizada de OS ativas
- Notificações por e-mail a cada mudança de status
- Containerização com Docker e manifestos Kubernetes

## 🛠️ Tecnologias

| Tecnologia | Versão | Uso |
|-----------|---------|-----|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.5.15-SNAPSHOT | Framework web |
| PostgreSQL | 16 | Banco de dados |
| Spring Security | 6.x | Autenticação e autorização |
| OAuth2 Resource Server | 6.x | Segurança de API |
| Spring Data JPA | 3.x | Persistência |
| Flyway | 11.7.2 | Migrações de banco |
| SpringDoc OpenAPI | 2.8.16 | Documentação Swagger |
| WireMock | 3.9.1 | Mocking de chamadas externas |
| ArchUnit | 1.3.0 | Testes de arquitetura (fitness functions) |
| Maven | 3.x | Gerenciador de dependências |
| Docker + Docker Compose | Latest | Containerização |

## 📦 Pré-requisitos

### Obrigatório
- **Java 21** - [Download](https://adoptium.net/)
- **Docker** (para executar com containers) - [Download](https://www.docker.com/products/docker-desktop)
- **Docker Compose** - Incluído no Docker Desktop

### Opcional (para rodar sem Docker)
- **PostgreSQL 16** - [Download](https://www.postgresql.org/download/)
- **Maven 3.8+** - [Download](https://maven.apache.org/)

## 🚀 Como Executar Localmente

### Opção 1: Com Docker Compose (Recomendado)

Mais simples e garante que o ambiente esteja correto.

#### 1. Preparar variáveis de ambiente
```bash
cp .env.example .env
```

O arquivo `.env` padrão já contém os valores necessários:
```env
DB_NAME=tech_challenge
DB_USER=postgres
DB_PASS=postgres
```

#### 2. Iniciar a aplicação
```bash
docker compose up --build
```

Aguarde até ver mensagens indicando que a aplicação está pronta:
- PostgreSQL: `database system is ready to accept connections`
- WireMock: `WireMock started`
- Aplicação: `Started TechChallengeBackendApplication`

#### 3. Acessar a aplicação
- **API**: [http://localhost:8080](http://localhost:8080)
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Health Check**: [http://localhost:8080/ping](http://localhost:8080/ping)
- **MailHog (e-mails enviados em dev)**: [http://localhost:8025](http://localhost:8025)

#### 4. Parar a aplicação
```bash
docker compose down
```

Para remover os volumes (dados do banco também):
```bash
docker compose down -v
```

---

### Opção 2: Banco em Docker + Aplicação Local

Útil para desenvolvimento e debug.

#### 1. Iniciar apenas o banco de dados
```bash
docker compose up postgres wiremock -d
```

Aguarde 10-15 segundos para o banco inicializar.

#### 2. Configurar variáveis de ambiente locais

**Em Windows (PowerShell):**
```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5433/tech_challenge"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "postgres"
```

**Em Linux/Mac (Bash):**
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/tech_challenge
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
```

Ou copie o `.env` e use em sua IDE.

#### 3. Executar a aplicação

**Via Maven:**
```bash
./mvnw spring-boot:run
```

Ou no Windows:
```bash
mvnw.cmd spring-boot:run
```

**Via IDE:**
- Abra o projeto em sua IDE (IntelliJ, Eclipse, VS Code)
- Configure o Java 21 como JDK
- Execute `TechChallengeBackendApplication.main()`

#### 4. Acessar a aplicação
- **API**: [http://localhost:8080](http://localhost:8080)
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

#### 5. Parar a aplicação
Pressione `Ctrl+C` no terminal
```bash
docker compose down
```

---

### Opção 3: Tudo Local (sem Docker)

#### 1. Configurar PostgreSQL

**No Windows (usando pgAdmin ou psql):**
```sql
CREATE DATABASE tech_challenge;
```

**No Linux/Mac:**
```bash
createdb -h localhost -U postgres -p 5432 tech_challenge
```

#### 2. Configurar variáveis de ambiente

**Windows (PowerShell):**
```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/tech_challenge"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "postgres"
```

**Linux/Mac (Bash):**
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tech_challenge
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
```

#### 3. Executar a aplicação
```bash
./mvnw spring-boot:run
```

#### 4. Migrações de banco
As migrações são executadas automaticamente via Flyway na inicialização.

---

## 🔌 Verificar se está funcionando

### Endpoint de Health Check
```bash
curl http://localhost:8080/ping
```

Resposta esperada:
```json
{
  "status": "ok",
  "db": "PostgreSQL 16.x on x86_64-pc-linux-musl, compiled by gcc (GCC) 12.2.0, 64-bit"
}
```

### Kubernetes

Os manifestos da aplicação estão em [`k8s/`](k8s/) (Kustomize: `base` + overlays `homolog` e
`prod`) e são aplicados pelo pipeline no EKS. Veja [`k8s/README.md`](k8s/README.md).

### Acessar Swagger UI
Abra no navegador: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## ☁️ Deploy na AWS (Fase 3)

A infraestrutura saiu deste repositório na Fase 3: rede, EKS e API Gateway estão em
[postech-16soat-infra-k8s](https://github.com/daniellrsouza/postech-16soat-infra-k8s), e o banco
RDS em [postech-16soat-infra-db](https://github.com/daniellrsouza/postech-16soat-infra-db).
A aplicação é publicada pelo pipeline [`.github/workflows/pipeline.yml`](../.github/workflows/pipeline.yml):
push na `develop` → homolog; push na `main` → prod. Visão geral, URLs dos ambientes e
documentação de arquitetura: [README da raiz](../README.md).

---

## 📚 Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/fiap/tech_challenge_backend/
│   │   ├── acesso/                          # Módulo de autenticação
│   │   │   ├── domain/
│   │   │   ├── application/                 # ports/in, ports/out, services, dto, exceptions
│   │   │   └── adapters/                    # in/web (controllers), out (persistence, infra)
│   │   │
│   │   ├── cadastro/                        # Módulo de clientes e veículos
│   │   │   ├── domain/
│   │   │   ├── application/
│   │   │   └── adapters/
│   │   │
│   │   ├── atendimento/                     # Módulo de ordens, orçamentos e relatórios
│   │   │   ├── domain/
│   │   │   ├── application/
│   │   │   └── adapters/
│   │   │
│   │   ├── estoque/                         # Módulo de peças e insumos
│   │   │   ├── domain/
│   │   │   ├── application/
│   │   │   └── adapters/
│   │   │
│   │   ├── acompanhamento/                  # Módulo de acompanhamento (bounded context de leitura)
│   │   │   ├── application/
│   │   │   └── adapters/
│   │   │
│   │   ├── config/                          # Configurações gerais (ex.: Swagger)
│   │   │
│   │   ├── shared/                          # Código compartilhado
│   │   │   ├── domain/                      # value objects
│   │   │   ├── application/                 # dto, exceptions
│   │   │   └── infrastructure/              # security, web (GlobalExceptionHandler), config
│   │   │
│   │   └── TechChallengeBackendApplication.java
│   │
│   └── resources/
│       ├── application.yml                  # Configurações principais
│       ├── application-dev.yml
│       ├── application-test.yml
│       └── db/
│           └── migration/                   # Scripts SQL do Flyway
│
├── test/
│   ├── java/
│   │   ├── acesso/ … estoque/               # Testes espelham a estrutura de main/ por módulo (unitários e *ControllerIT lado a lado)
│   │   ├── architecture/                    # Testes de arquitetura (ArchUnit)
│   │   ├── config/                          # Configurações de segurança para testes
│   │   └── cucumber/                        # Testes BDD
│   │
│   └── resources/
│       ├── features/                        # Cenários Gherkin
│       ├── wiremock/                        # Configurações WireMock
│       └── archunit.properties              # Configuração da baseline do ArchUnit
│
├── pom.xml                                  # Configuração Maven
├── Dockerfile                               # Build da imagem Docker
├── docker-compose.yml                       # Orquestração de containers
├── .env.example                             # Template de variáveis de ambiente
├── .gitignore
└── README.md
```

---

## 🔐 Autenticação

A API usa OAuth2 Resource Server com JWT.

### Para testar sem autenticação:
Use o endpoint `/ping` que não requer token.

### Para testar com autenticação:
1. Acesse [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
2. Use a seção de autenticação no Swagger para fazer login
3. Um token JWT será gerado
4. Use o token em chamadas subsequentes

---

## 🧪 Testes

### Executar todos os testes
```bash
./mvnw test
```

Isso já inclui os testes unitários e os cenários Cucumber/BDD (a suíte não usa `@Tag`/grupos do JUnit, então não há como filtrar por `-Dgroups`).

### Executar uma classe de teste específica
```bash
./mvnw test -Dtest=NomeDaClasseTest
```

> ⚠️ O projeto não tem o plugin Failsafe configurado, então classes `*ControllerIT` (testes de integração com Testcontainers) **não** rodam via `./mvnw test` nem `./mvnw verify` — só rodam se pedidas explicitamente com `-Dtest=`.

### Gerar relatório de cobertura
```bash
./mvnw clean test jacoco:report
```

Acesse o relatório em: `target/site/jacoco/index.html`

---

## 🐛 Debug

### Debug com Docker
Se estiver usando Docker Compose, o debugger está disponível na porta `5005`:

**Em sua IDE (IntelliJ/Eclipse):**
1. Run → Edit Configurations → Add new → Remote JVM Debug
2. Host: `localhost`, Port: `5005`
3. Clique em Debug

### Debug Local
1. Abra a aplicação via IDE
2. Set breakpoints
3. Run → Debug

---

## 📡 Endpoints Principais

### Autenticação
- `POST /auth/login` - Fazer login
- `GET /me` - Obter dados do usuário autenticado

### Clientes
- `POST /clientes` - Criar cliente
- `GET /clientes` - Listar clientes
- `GET /clientes/{cpfCnpj}` - Buscar cliente
- `PUT /clientes/{cpfCnpj}` - Atualizar cliente
- `DELETE /clientes/{cpfCnpj}` - Deletar cliente

### Veículos
- `POST /veiculos` - Criar veículo
- `GET /veiculos` - Listar veículos
- `GET /veiculos/{placa}` - Buscar veículo
- `PUT /veiculos/{placa}` - Atualizar veículo
- `DELETE /veiculos/{placa}` - Deletar veículo

### Ordens de Serviço
- `POST /api/v1/ordens-servico/criar` - Criar OS
- `GET /api/v1/ordens-servico/listar-os` - Listar OS
- `GET /api/v1/ordens-servico/listar-os-priorizadas` - Listar OS ativas priorizadas
- `GET /api/v1/ordens-servico/buscar?id={id}` - Buscar OS
- `PUT /api/v1/ordens-servico/editar?id={id}` - Atualizar OS
- `DELETE /api/v1/ordens-servico/deletar?id={id}` - Remover OS
- `PATCH /api/v1/ordens-servico/alterar-status?id={id}` - Alterar status da OS
- `POST /api/v1/ordens-servico/criar-orcamento?id={id}` - Criar orçamento
- `GET /api/v1/ordens-servico/buscar-orcamento?idOS={id}&idOrcamento={orcamentoId}` - Buscar orçamento

### Relatórios
- `GET /api/os/atendimento/relatorios/ordens-servico?expand={...}` - Relatório detalhado de ordens de serviço
- `GET /api/os/atendimento/relatorios/ordens-servico/por-status?status={status}&expand={...}` - Relatório de ordens de serviço por status

### Acompanhamento (cliente)
- `GET /clientes/{clienteId}/ordens` - Listar ordens de serviço do cliente
- `GET /clientes/{clienteId}/ordens/{osId}` - Consultar detalhe de uma ordem de serviço do cliente

### Público
- `GET /api/public/atendimento/ordens/{id}/autorizar` - Autorizar orçamento por link
- `PATCH /api/public/atendimento/ordens/{id}/orcamentos/{orcamentoId}/status` - Responder orçamento
- `GET /api/public/atendimento/ordens/{id}/orcamentos/{orcamentoId}/aprovar` - Aprovar orçamento via link do e-mail
- `GET /api/public/atendimento/ordens/{id}/orcamentos/{orcamentoId}/rejeitar` - Rejeitar orçamento via link do e-mail

### Estoque
- `POST /estoque/itens` - Cadastrar peça ou insumo
- `GET /estoque/itens` - Listar itens (filtro opcional por tipo: PECA ou INSUMO)
- `GET /estoque/itens/{id}` - Buscar item por ID
- `PUT /estoque/itens/{id}` - Atualizar item
- `DELETE /estoque/itens/{id}` - Remover item
- `GET /estoque/itens/abaixo-do-minimo` - Listar itens com estoque abaixo do mínimo
- `POST /estoque/itens/entrada` - Dar entrada no estoque (cadastra se não existir ou repõe se já existir)
- `PATCH /estoque/itens/{id}/entrada` - Registrar entrada de estoque
- `PATCH /estoque/itens/{id}/saida` - Registrar saída de estoque
- `GET /estoque/movimentacoes/item/{pecaInsumoId}` - Listar movimentações de um item

Veja a documentação completa no Swagger: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 📝 Variáveis de Ambiente

As variáveis estão definidas em `.env.example`. Principais:

```env
# Spring Profile
SPRING_PROFILES_ACTIVE=dev

# Banco de Dados
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/tech_challenge
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
DB_NAME=tech_challenge
DB_USER=postgres
DB_PASS=postgres

# Autenticação (chave HMAC-SHA256, mínimo 32 caracteres)
JWT_SECRET=uma-chave-local-de-desenvolvimento-com-32-caracteres

# E-mail (SMTP) - em dev, aponta para o MailHog do docker-compose
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=

# Flyway
FLYWAY_ENABLED=true

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_FIAP=DEBUG
LOGGING_LEVEL_SPRING_MAIL=DEBUG
```

No Kubernetes, os valores sensíveis vão para `Secret` e os demais parâmetros para `ConfigMap`.

---

## 🐳 Comandos Docker Úteis

```bash
# Iniciar tudo
docker compose up --build

# Iniciar em background
docker compose up -d

# Ver logs
docker compose logs -f

# Ver logs de um serviço específico
docker compose logs -f app

# Parar tudo
docker compose down

# Remover volumes (dados do banco também)
docker compose down -v

# Reconstruir imagens
docker compose up --build

# Executar comando no container
docker compose exec app bash

# Ver status dos containers
docker compose ps
```

---

## 🔧 Solução de Problemas Comuns

### Erro do Flyway: "Migration checksum mismatch"

```
Migration checksum mismatch for migration version X
-> Applied to database : ...
-> Resolved locally    : ...
```

Acontece quando o volume do Postgres já tem uma migration aplicada com um conteúdo diferente do arquivo `.sql` atual (por exemplo, após um `git pull` que alterou uma migration já versionada, ou ao trocar de branch). Como o volume local é descartável, a forma mais rápida de resolver é recriá-lo do zero:

```bash
docker compose down -v
docker compose up --build
```

Isso apaga os dados do Postgres local e deixa o Flyway reaplicar todas as migrations (incluindo o seed de dados de teste) do começo.

---

## 📚 Coleções Postman/Insomnia

Disponível em: `Insomnia_Collection_TechChallenge.json`

Importe no Insomnia ou Postman para testar todos os endpoints.

---

## 🤝 Contribuindo

1. Crie uma branch para sua feature: `git checkout -b feature/minha-feature`
2. Commit suas mudanças: `git commit -m "feat: descrição"`
3. Push para a branch: `git push origin feature/minha-feature`
4. Abra um Pull Request

---

## 📞 Suporte e Dúvidas

Para dúvidas sobre a estrutura ou como rodar o projeto:

1. Verifique se todas as portas estão livres (8080, 5433)
2. Verifique o Java 21: `java -version`
3. Verifique o Docker: `docker --version` e `docker compose --version`
4. Consulte os logs: `docker compose logs -f`

---

## 📄 Licença

Este projeto foi desenvolvido como Tech Challenge para o programa de Pós-Graduação em Arquitetura de Software da FIAP (Especialização 16SOAT).

---

