# Diagrama de sequência — abertura de ordem de serviço

Abertura de OS por um cliente autenticado (o mesmo endpoint aceita `ADMIN` e `FUNCIONARIO`).
Caso de uso: `CriarOrdemServicoClienteService`, contexto `atendimento`.

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente autenticado
    participant GW as API Gateway
    participant A as Lambda Authorizer
    participant APP as Aplicação · OrdemServicoController
    participant UC as CriarOrdemServicoClienteService
    participant DB as RDS PostgreSQL
    participant NR as New Relic

    C->>GW: POST /api/v1/ordens-servico/criar<br/>Bearer <token><br/>{cpfCnpjCliente, placaVeiculo, queixaCliente, urgente}
    GW->>A: valida o JWT
    A-->>GW: isAuthorized = true (sub, role = CLIENTE)
    GW->>APP: VPC Link → NLB → NodePort<br/>X-Correlation-Id = requestId
    APP->>APP: Spring Security revalida o JWT<br/>@PreAuthorize(ADMIN, FUNCIONARIO, CLIENTE)<br/>Bean Validation do corpo
    APP->>UC: criar(request)
    UC->>UC: valida CPF/CNPJ e placa (value objects)
    UC->>DB: busca cliente por CPF/CNPJ
    alt cliente ou veículo inexistente
        DB-->>UC: vazio
        UC-->>APP: erro de negócio
        APP-->>GW: 4xx {mensagem}
        GW-->>C: 4xx
        APP-)NR: evento de falha no processamento da OS (alerta)
    else dados válidos
        UC->>DB: busca veículo por placa
        UC->>DB: nextval(seq_ordem_servico_id)
        Note over UC,DB: mesma transação
        UC->>DB: INSERT ordens_servico (status = RECEBIDA)
        UC->>DB: INSERT os_historico_status (null → RECEBIDA)
        UC-->>APP: OrdemServicoResponseDTO (enriquecido)
        APP-)NR: evento de OS criada (volume diário)
        APP-->>GW: 200 {id, status: RECEBIDA, ...}
        GW-->>C: 200
    end
```

Depois da abertura, cada mudança de status (`EM_DIAGNOSTICO`, `AGUARDANDO_APROVACAO`,
`EM_EXECUCAO`, `FINALIZADA`, `ENTREGUE`) grava uma linha em `os_historico_status` e emite um evento
com o tempo gasto no status anterior. Esses eventos alimentam o dashboard de tempo médio por status.
