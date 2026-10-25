# ADR-009 — Migrations do schema na aplicação (Flyway)

- **Status:** Aceito
- **Data:** 2026-10-10

## Contexto

Com o banco em um repositório próprio de infraestrutura, era preciso decidir quem é dono das
mudanças de schema. A aplicação já usava Flyway, com as migrations versionadas em
`src/main/resources/db/migration`.

## Decisão

- **As migrations continuam na aplicação** e rodam no startup, com o usuário da aplicação de cada
  ambiente, dono do schema.
- O repositório `infra-db` provisiona só a infraestrutura: instância, parâmetros, rede, databases,
  usuários e permissões de conexão.
- A única permissão que depende do schema, `SELECT` em `clientes` para o papel `leitura_clientes`
  usado pela Lambda, é concedida pela própria migration que cria ou altera a tabela.

## Alternativas consideradas

- **Migrations no repositório do banco**: o schema ficaria versionado junto com a infra, mas o
  pipeline precisaria alcançar o RDS privado e coordenar releases com a aplicação a cada mudança
  de modelo.

## Consequências

- Mudança de código e mudança de schema chegam juntas, no mesmo deploy e na mesma revisão.
- O primeiro pod a subir em cada ambiente aplica as migrations pendentes; o Flyway controla a
  concorrência pela tabela de histórico.
