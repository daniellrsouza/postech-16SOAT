# ADR-010 — Segredos gerados pelo Terraform e distribuídos por SSM SecureString

- **Status:** Aceito (substitui a decisão inicial de cadastrar os segredos como GitHub Secrets)
- **Data:** 2026-10-25

## Contexto

Alguns segredos precisam ter **o mesmo valor em mais de um repositório**: o `JWT_SECRET` de cada
ambiente (emitido e validado pela Lambda, validado pela aplicação) e as senhas dos usuários do
banco (criados pelo `infra-db`, usados pela Lambda e pela aplicação). A conta do AWS Academy não
permite criar IAM roles, e a Lambda fica em subnet privada sem NAT, sem acesso ao Secrets Manager
em tempo de execução.

## Decisão

- O `infra-db` **gera** as senhas dos usuários do banco e o `JWT_SECRET` de cada ambiente
  (`random_password`) e os publica como **SSM SecureString** em `/tech-challenge/<env>/...`.
- O Terraform da Lambda lê esses parâmetros e os injeta como variáveis de ambiente das funções; o
  pipeline da aplicação os lê e cria o Secret do Kubernetes no deploy.
- A **senha master do RDS** é gerada e guardada pelo próprio RDS no Secrets Manager
  (`manage_master_user_password`) e só é lida pelo Terraform, para o Job de bootstrap.
- Credenciais da AWS (sessão do lab) e a license key do New Relic ficam como GitHub Secrets.

## Alternativas consideradas

- **GitHub Secrets cadastrados à mão em cada repositório**: cerca de 10 valores repetidos entre
  repositórios e ambientes, com risco de divergência.
- **Secrets Manager lido em tempo de execução pela Lambda**: exigiria VPC endpoint ou NAT, com
  custo.

## Consequências

- Nenhum segredo de aplicação é digitado por pessoas nem versionado; cada um nasce em um único
  lugar.
- Os valores ficam no state do Terraform, guardado em um bucket S3 privado, criptografado e
  versionado.
- Girar um segredo é um `apply` do `infra-db` seguido dos deploys da Lambda e da aplicação.
