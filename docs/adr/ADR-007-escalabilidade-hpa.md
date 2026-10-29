# ADR-007 — Escalabilidade com HPA e node group gerenciado

- **Status:** Aceito
- **Data:** 2026-10-25

## Contexto

O enunciado exige cluster Kubernetes com escalabilidade. A aplicação é stateless (estado no RDS),
o que permite escalar horizontalmente por réplicas. O orçamento do laboratório limita o número de
nós que podem ficar ligados.

## Decisão

- **HorizontalPodAutoscaler** na aplicação, **por CPU**, alimentado pelo **metrics-server**
  (instalado pelo `infra-k8s` via Helm). Em prod, 2 a 6 réplicas, com alvo de 70% de CPU; em
  homolog, 1 réplica.
- **Memória não é métrica de escala** (revisto em 2026-10-26, após o deploy em prod): a JVM ocupa
  ~80% da memória reservada mesmo sem carga e não a devolve. Com alvo de 75% de memória, o HPA
  criaria réplicas sem demanda real, até o máximo, e as excedentes ficariam pendentes nos 2 nós.
  A memória continua limitada por pod (`limits`) e monitorada no New Relic.
- **PodDisruptionBudget** e rolling update sem indisponibilidade (`maxUnavailable: 0`).
- **Node group gerenciado** do EKS de 0 a 2 nós `t3.medium`: o mínimo 0 permite desligar os nós
  fora do horário de trabalho, e o máximo 2 protege o orçamento.

## Alternativas consideradas

- **Cluster Autoscaler ou Karpenter**: escalariam nós conforme a demanda, mas exigem IAM roles
  próprias (IRSA), indisponíveis no AWS Academy.
- **Réplicas fixas**: mais simples, sem demonstrar escalabilidade.

## Consequências

- A escalabilidade de pods é automática dentro da capacidade de 2 nós; acima disso, os pods ficam
  pendentes até um nó ser adicionado manualmente (aumentando o máximo do node group).
- As métricas de CPU e memória que alimentam o HPA são as mesmas monitoradas no New Relic.
