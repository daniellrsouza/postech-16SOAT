# Diagrama de componentes

Visão da solução na nuvem (AWS, `us-east-1`): APIs, autenticação, cluster, banco e monitoramento.
Os dois ambientes (`homolog` e `prod`) compartilham o cluster EKS, a instância RDS, o NLB e o VPC
Link, e têm cada um sua HTTP API, suas Lambdas, seu namespace, seu database e seus segredos
([ADR-002](../adr/ADR-002-ambientes-por-namespace.md)).

```mermaid
flowchart LR
    CLI["Cliente da oficina<br/>(CPF)"]
    FUN["Funcionário / Admin<br/>(e-mail + senha)"]

    subgraph AWS["AWS · us-east-1"]
        subgraph GW["API Gateway · uma HTTP API por ambiente"]
            R1["POST /auth/cpf"]
            R2["Rotas públicas<br/>/auth/login · /api/public/** · health · Swagger"]
            R3["Rotas protegidas<br/>(demais)"]
            AUTHZ["Lambda Authorizer<br/>valida JWT HS256"]
        end

        LAUTH["Lambda auth-cpf<br/>valida CPF · consulta status · emite JWT"]

        subgraph VPC["VPC 10.0.0.0/16"]
            subgraph PRIV["Subnets privadas · sem internet"]
                VPCL["VPC Link"]
                NLB["NLB interno<br/>:30080 prod · :30081 homolog"]
                RDS[("RDS PostgreSQL 16<br/>tech_challenge_homolog<br/>tech_challenge_prod")]
            end
            subgraph PUB["Subnets públicas"]
                subgraph EKS["EKS eks-tech-challenge · node group 0–2"]
                    APPH["Aplicação Spring Boot<br/>namespace homolog · NodePort 30081"]
                    APPP["Aplicação Spring Boot<br/>namespace prod · NodePort 30080 · HPA 2–6"]
                    NRK["Integração Kubernetes<br/>do New Relic"]
                    MS["metrics-server"]
                end
            end
        end

        ECR[("ECR<br/>tech-challenge-backend")]
        SSM[("SSM Parameter Store<br/>contrato + SecureString")]
        CW["CloudWatch Logs<br/>access logs do gateway · Lambdas · Postgres"]
    end

    NR["New Relic<br/>APM · traces · logs · dashboards · alertas · Synthetics"]
    SMTP["Servidor SMTP<br/>(e-mails da oficina)"]

    CLI --> R1 --> LAUTH --> RDS
    CLI & FUN --> R2
    CLI & FUN --> R3
    R3 -.->|antes de rotear| AUTHZ
    R2 & R3 --> VPCL --> NLB --> APPH & APPP
    APPH & APPP --> RDS
    APPH & APPP --> SMTP
    ECR -.->|imagem| APPH & APPP
    SSM -.->|segredos e endereços no deploy| LAUTH & APPH & APPP
    MS -.->|CPU e memória| APPP
    APPH & APPP & NRK -->|telemetria| NR
    GW & LAUTH & AUTHZ & RDS -->|logs| CW
    NR -.->|monitor sintético| R2
```

## Componentes

| Componente | Repositório | Papel |
|---|---|---|
| HTTP API (por ambiente) | infra-k8s (API, stage, integração) e lambda (rotas, authorizer) | Entrada única: roteamento, autorização na borda, access log JSON, `X-Correlation-Id` |
| Lambda `auth-cpf` | lambda | Valida o CPF, consulta existência e status do cliente, emite o JWT do cliente |
| Lambda Authorizer | lambda | Valida o JWT de cliente e de funcionário antes das rotas protegidas |
| VPC Link + NLB interno | infra-k8s | Integração privada do gateway com o cluster, um listener por ambiente |
| EKS + node group | infra-k8s | Executa a aplicação; metrics-server alimenta o HPA |
| Aplicação Spring Boot | app | API da oficina: cadastro, OS, orçamentos, estoque, acompanhamento |
| RDS PostgreSQL | infra-db | Dados de cada ambiente em database próprio, com TLS obrigatório |
| SSM Parameter Store | todos | Contrato entre repositórios e distribuição de segredos |
| ECR | infra-k8s | Imagens da aplicação |
| New Relic | app + infra-k8s | APM, traces, logs, métricas do cluster, dashboards, alertas e uptime |
| CloudWatch Logs | AWS | Access logs do gateway, logs das Lambdas e do Postgres |

Fluxos detalhados: [autenticação por CPF](sequencia-autenticacao-cpf.md) e
[abertura de ordem de serviço](sequencia-abertura-os.md).
