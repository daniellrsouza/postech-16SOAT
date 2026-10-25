# ADR-005 — Entrada do cluster por NLB interno e NodePort, sem ingress controller

- **Status:** Aceito (substitui a decisão inicial de usar ingress-nginx)
- **Data:** 2026-10-23

## Contexto

O API Gateway precisa alcançar a aplicação dentro do EKS por uma integração privada: VPC Link →
load balancer interno. Na conta do AWS Academy não é possível criar IAM roles nem usar IRSA, o que
impede o AWS Load Balancer Controller.

A decisão inicial era um NLB criado pelo Terraform apontando para o **ingress-nginx**, que
separaria os namespaces por prefixo de path. O projeto ingress-nginx, porém, **chegou ao fim da
vida em março de 2026**: sem novos releases, correções de bugs ou de vulnerabilidades
([anúncio do Kubernetes](https://kubernetes.io/blog/2025/11/11/ingress-Nginx-retirement/)).

## Decisão

- Sem ingress controller. O Service da aplicação em cada namespace é do tipo `NodePort`, com porta
  fixa: **prod 30080**, **homolog 30081**.
- **NLB interno** criado pelo Terraform nas subnets privadas das três AZs dos nós, com **um listener
  e um target group por ambiente** (instâncias do node group, health check HTTP em
  `/actuator/health`).
- Cada HTTP API (uma por ambiente, [ADR-003](ADR-003-uma-api-gateway-por-ambiente.md)) aponta para o
  listener do seu ambiente via VPC Link. O roteamento L7 fica todo no API Gateway.

O caminho foi validado ponta a ponta com recursos temporários (pod de eco, Service NodePort e rota
de teste): resposta 200, path e query preservados, ~0,43 s por requisição a partir do Brasil.

## Alternativas consideradas

- **Manter o ingress-nginx**: funciona, mas sem correções de segurança.
- **Traefik**: substituto com suporte a Ingress; adiciona um componente a operar sem necessidade,
  já que o gateway faz o roteamento.
- **Gateway API (Envoy Gateway)**: caminho recomendado pela comunidade, com mais peças (CRDs,
  controller) do que o projeto precisa.
- **Service `LoadBalancer`**: o NLB nasceria fora do Terraform, e o ARN do listener precisaria ser
  descoberto depois para a integração do gateway.

## Consequências

- Nenhum componente extra no cluster e nenhum dependente de IRSA.
- A porta de cada ambiente é um contrato entre o `infra-k8s` e os manifests da aplicação.
- O teste revelou que o NLB só usa alvos em AZs onde tem subnet; por isso as subnets privadas
  cobrem as três AZs dos nós.
