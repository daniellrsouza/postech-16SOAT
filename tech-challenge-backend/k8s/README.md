# Kubernetes — manifests da aplicação

Manifests da aplicação, organizados com [Kustomize](https://kustomize.io/): uma `base` comum e um
overlay por ambiente. São aplicados no **Amazon EKS** pelo pipeline
[`.github/workflows/pipeline.yml`](../../.github/workflows/pipeline.yml).

O que é do **cluster** (namespaces, metrics-server, NLB, integração New Relic) fica no
repositório [postech-16soat-infra-k8s](https://github.com/daniellrsouza/postech-16soat-infra-k8s).
O banco é o **RDS**, provisionado pelo
[postech-16soat-infra-db](https://github.com/daniellrsouza/postech-16soat-infra-db), e não roda mais
no cluster.

## Estrutura

```
k8s/
  base/
    deployment.yaml     Deployment tech-challenge-app (probes, recursos, envFrom)
    service.yaml        Service NodePort (porta do ambiente vem do overlay)
    pdb.yaml            PodDisruptionBudget
    configmap.yaml      Configuração não sensível (logs JSON, forward headers, SMTP host)
  overlays/
    homolog/            namespace homolog · 1 réplica · NodePort 30081
    prod/               namespace prod · HPA 2–6 réplicas · NodePort 30080
      hpa.yaml
```

| Recurso | homolog | prod |
|---|---|---|
| Namespace | `homolog` | `prod` |
| Réplicas | 1 (orçamento do lab) | HPA 2–6 por CPU (alvo 70%) |
| Service NodePort | 30081 | 30080 |
| PDB `minAvailable` | 0 | 1 |
| `NEW_RELIC_APP_NAME` | `tech-challenge-backend (homolog)` | `tech-challenge-backend (prod)` |

O NodePort de cada ambiente é o contrato com o NLB interno do `infra-k8s`, que tem um listener e um
target group por ambiente (ver [ADR-005](../../docs/adr/ADR-005-nlb-nodeport-sem-ingress.md)). Não há
ingress controller.

## Secret `tech-challenge-secrets`

Não é versionado: o pipeline o cria em cada deploy, com valores do SSM e dos GitHub Secrets.

| Chave | Origem |
|---|---|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | SSM (`/tech-challenge/shared/db-host`, `/tech-challenge/<env>/db-*`) |
| `JWT_SECRET` | SSM (`/tech-challenge/<env>/jwt-secret`), o mesmo usado pela Lambda |
| `NEW_RELIC_LICENSE_KEY`, `MAIL_USERNAME`, `MAIL_PASSWORD` | GitHub Secrets |

## Probes

`startupProbe` (até 5 min, cobre o Flyway no primeiro boot), `readinessProbe` e `livenessProbe` em
`/actuator/health`. O target group do NLB usa o mesmo endpoint como health check.

## Comandos úteis

```bash
aws eks update-kubeconfig --region us-east-1 --name eks-tech-challenge

kubectl kustomize overlays/homolog          # ver o que o pipeline aplica
kubectl -n homolog get pods,svc
kubectl -n homolog logs deploy/tech-challenge-app -f      # logs JSON com correlationId
kubectl -n prod get hpa                                    # autoscaling
```

Para gerar carga e ver o HPA escalar em prod, use requisições ao endpoint público de health ou às
APIs protegidas pelo API Gateway, e acompanhe com `kubectl -n prod get hpa -w`.
