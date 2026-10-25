# Diagrama de sequência — autenticação por CPF

O cliente obtém um token com o CPF e o usa nas rotas protegidas. A decisão está no
[ADR-004](../adr/ADR-004-jwt-hs256-lambda-authorizer.md) e na
[RFC-003](../rfc/RFC-003-estrategia-autenticacao.md).

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant GW as API Gateway<br/>(HTTP API do ambiente)
    participant L as Lambda auth-cpf
    participant DB as RDS PostgreSQL
    participant A as Lambda Authorizer
    participant APP as Aplicação (EKS)

    C->>GW: POST /auth/cpf {"cpf": "529.982.247-25"}
    GW->>L: invoca (requestId = correlationId)
    L->>L: normaliza e valida os dígitos verificadores
    alt CPF inválido
        L-->>GW: 400 CPF inválido
        GW-->>C: 400
    else CPF válido
        L->>DB: SELECT id, status FROM clientes WHERE cpf_cnpj = $1 (TLS)
        alt cliente não existe
            DB-->>L: nenhuma linha
            L-->>GW: 404 Cliente não encontrado
            GW-->>C: 404
        else cliente INATIVO ou BLOQUEADO
            DB-->>L: id, status
            L-->>GW: 403 Cliente não está ativo
            GW-->>C: 403
        else cliente ATIVO
            DB-->>L: id, status = ATIVO
            L->>L: assina JWT HS256 (sub = id, role = CLIENTE, iss = tech-challenge-auth, 1 h)
            L-->>GW: 200 {"accessToken": "..."}
            GW-->>C: 200 {"accessToken": "..."}
        end
    end

    Note over C,APP: Consumo de uma API protegida

    C->>GW: GET /clientes/{id}/ordens<br/>Authorization: Bearer <token>
    GW->>A: authorizer (header Authorization)
    A->>A: valida assinatura, expiração e emissor
    alt token inválido ou expirado
        A-->>GW: isAuthorized = false
        GW-->>C: 403 Forbidden
    else token válido
        A-->>GW: isAuthorized = true (sub, role)
        GW->>APP: VPC Link → NLB → NodePort<br/>X-Correlation-Id = requestId
        APP->>APP: Spring Security revalida o JWT e aplica @PreAuthorize
        APP->>DB: consulta as ordens
        APP-->>GW: 200 [ordens]
        GW-->>C: 200 [ordens]
    end
```

Sem o header `Authorization`, o gateway responde 401 sem chamar o authorizer. O mesmo
`correlationId` aparece no access log do gateway, nos logs das Lambdas e nos logs da aplicação.
