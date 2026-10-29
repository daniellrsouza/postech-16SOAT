# AGENTS.md — regras de trabalho para a refatoração Hexagonal

Este arquivo existe para sobreviver a uma compactação de contexto ou a uma nova sessão.
Se você é um agente retomando este trabalho e não tem o histórico da conversa, **leia este
arquivo e o `REFATORACAO_HEXAGONAL.md` (mesmo diretório) antes de fazer qualquer coisa.**

## O que é este projeto

Uma auditoria arquitetural purista (Hexagonal/Ports & Adapters) foi feita sobre todo o
backend (`tech-challenge-backend`), publicada como um artefato Claude ("Auditoria
Hexagonal"). A partir dela, o usuário pediu para implementar o plano de refatoração
(seção 21 do relatório), **um ou dois contextos por vez**, não a aplicação inteira de uma vez.

`REFATORACAO_HEXAGONAL.md` é o rastreamento vivo: o que já foi feito, o que está em
andamento, o que falta, e por quê. Ele tem uma seção por contexto (`acesso`, `cadastro`,
`atendimento`, ...) e uma seção de pendências. **Atualize-o incrementalmente conforme o
trabalho avança** — não deixe para atualizar só no fim, porque o contexto pode ser
compactado no meio do trabalho.

## Regras de escopo

1. **Só toque nos contextos explicitamente autorizados na conversa atual.** Se corrigir
   algo "direito" em um contexto exigiria editar um arquivo de outro contexto ainda não
   autorizado, **não faça isso silenciosamente**. Duas opções, nessa ordem de preferência:
   - Se o impedimento é análogo a uma decisão já tomada antes (ver histórico de decisões
     no `REFATORACAO_HEXAGONAL.md`, seção 0), aplique o mesmo raciocínio e documente a
     decisão como pendência explícita, com o motivo técnico exato.
   - Se é uma decisão nova, com trade-offs reais, pergunte ao usuário (`AskUserQuestion`)
     antes de decidir sozinho.
2. **Nunca invente violação.** Toda alteração feita deve corresponder a um item concreto
   do relatório de auditoria (achado numerado ou item do plano P0–P3). Se você notar algo
   que "poderia ser melhor" mas não está no relatório, não implemente por conta própria —
   isso é escopo além do que foi pedido.
3. Uma correção "purista" que precise mexer em `JpaRepository`/tipo genérico de repositório
   usado diretamente por testes/fixtures de **contextos ainda não tocados** (ex.:
   `acompanhamento`, `cucumber/steps`, `estoque`) é um sinal de que ela é grande demais para
   esta etapa — normalmente esse tipo de mudança (separar entidade de domínio de entidade
   de persistência) só pode ser feita com segurança como um passo final único, depois que
   todos os contextos já tiverem sido tratados. Não tente fatiar isso por contexto.

## Regras de execução

4. **Todo código novo ou alterado precisa de teste.** Se a classe já tinha teste, atualize-o
   sem perder cobertura. Se não tinha (comum neste projeto), crie um.
5. **Nunca declare algo concluído sem compilar e testar.** Ordem mínima antes de marcar
   `[x]` em qualquer item do tracking:
   - `./mvnw compile`
   - `./mvnw test-compile`
   - os testes específicos do que foi alterado
   - ao final de cada contexto (não a cada arquivo), a suíte completa: `./mvnw test`
6. **Armadilha conhecida de relatório de teste**: rodar com `-Dtest=NomeDaClasse` faz o
   resumo em **texto** do Surefire (`target/surefire-reports/*.txt`) mostrar
   "Tests run: 0" para qualquer classe que use `@Nested` (é um defeito cosmético do
   resumo em texto deste projeto, não uma falha real). **A fonte de verdade é o XML**:
   `target/surefire-reports/TEST-*.xml`. Mas cuidado: o próprio atributo `tests=` da tag
   `<testsuite>` no XML também pode subcontar quando classes `@Nested` diferentes têm métodos
   de teste com o **mesmo nome** (ex.: `deveLancarExcecaoQuandoNaoEncontrado` repetido em várias
   classes aninhadas) — confirmado em `EstoqueServiceTest` (atributo dizia `tests="21"`, mas
   havia 30 elementos `<testcase>` reais, todos com `errors="0" failures="0"` individualmente).
   **A contagem realmente confiável é o número de elementos `<testcase>` filhos** (e a ausência
   de filhos `<failure>`/`<error>` dentro deles), não o atributo `tests=` do cabeçalho. Ao somar
   resultados de vários arquivos XML para relatar um total agregado, conte `<testcase>`, não o
   atributo. Rodar `./mvnw test` (suíte completa, sem `-Dtest`) e depois inspecionar o XML dessa
   forma é o jeito mais seguro de confirmar "0 falhas" de verdade.
7. **Antes de mudanças arriscadas em massa** (mover pacote, deletar classe usada em vários
   lugares, mudar assinatura consumida por muitos arquivos): rode `grep`/`Grep` para listar
   todos os usos primeiro. Não assuma.
8. Ao alterar contrato de resposta de endpoint (formato de erro, por exemplo), documente a
   mudança explicitamente na seção do contexto no `REFATORACAO_HEXAGONAL.md` — "antes" e
   "depois" — mesmo que os testes existentes não quebrem.
9. **Armadilha conhecida de nome de bean do Spring**: se um novo adapter/serviço tiver o mesmo
   nome de classe simples que uma classe já existente em outro pacote (ex.: dois
   `PecaInsumoRepositoryAdapter`, um em `estoque`, outro em `atendimento`), o Spring colide os
   dois no mesmo nome de bean por padrão (nome de classe decapitalizado) — só aparece como
   `ConflictingBeanDefinitionException` ao subir o contexto Spring (`@SpringBootTest`/
   `@WebMvcTest`), não na compilação nem em teste unitário puro. Ao introduzir uma classe nova,
   confira rapidamente (`grep` pelo nome simples) se já existe outra com o mesmo nome em outro
   contexto; se sim, dê um nome de bean explícito (`@Component("nomeUnico")`) em vez de renomear
   a classe (preserva a convenção `XxxPort` → `XxxAdapter` já adotada).

## Convenções de nomenclatura adotadas até agora

- Portas de entrada ficam em `application/ports/in/`. Nome da interface: `XxxUseCase`
  quando não há conflito com uma classe concreta já existente (padrão de `atendimento`);
  `XxxInputPort` quando a classe concreta já se chama `XxxUseCase` (padrão adotado em
  `cadastro`, para não forçar renomear a classe concreta e tudo que a referencia).
- Portas de saída ficam em `application/ports/out/` nos contextos que já nasceram assim
  (`atendimento`); em `acesso`/`cadastro` as portas de saída pré-existentes **não foram
  movidas** para esse padrão — só isso não é uma violação apontada pela auditoria, mover
  seria reorganização cosmética sem correção correspondente, então foi deixado como está
  para não ampliar o raio de mudança.
- Adapters de saída ficam em `infrastructure/adapters` (ou `infrastructure/repositories`,
  `infrastructure/security`, conforme o que já existia no contexto) — nunca em
  `application/adapters` (achado corrigido em `acesso`).

## Git

- **Nunca commitar sem o usuário pedir explicitamente.** Cada etapa fica no working tree
  até receber instrução contrária.
- Antes de qualquer operação que possa descartar trabalho (`git stash`, etc.), rodar
  `git status` e ter certeza do que está sendo preservado. Se usar `git stash` para
  comparar com o `HEAD` (ex.: para confirmar se um problema é pré-existente), sempre dar
  `git stash pop` em seguida, na mesma sessão, e conferir que o número de arquivos
  modificados voltou ao esperado.

## Regra crítica para o passo final (separação de entidade/persistência)

10. **O grafo JPA inteiro precisa ficar consistente antes de qualquer teste que suba o
    Hibernate de verdade.** Ao converter uma entidade de `@Entity` para POJO puro (domínio) +
    `XxxJpaEntity` nova, QUALQUER outra classe que ainda seja `@Entity` e tenha um
    `@ManyToOne`/`@OneToOne` apontando pra ela quebra o boot do Hibernate imediatamente
    (`AnnotationException: Association ... targets the type ... which is not an '@Entity' type`)
    — isso derruba `EntityManagerFactory`, ou seja, **todo teste `@SpringBootTest` (todas as
    ITs, todo o Cucumber) para de funcionar**, não só o teste da entidade que você acabou de
    mexer. Isso é esperado e não é bug a perseguir durante as fases intermediárias.
    - **Durante as fases intermediárias (antes de todas as 12 entidades + as 6 relações
      cross-context estarem convertidas), verifique só com**: `./mvnw clean compile`,
      `./mvnw clean test-compile`, e testes unitários puros (domínio, adapter com mock,
      qualquer coisa SEM `@SpringBootTest`). **Não rode a suíte completa nem qualquer IT/
      Cucumber até a última entidade do grafo estar convertida** — vai dar erro em massa e
      não significa regressão nova.
    - Descoberto por engano nesta sessão: rodei a suíte completa ao final da fase F1 e o
      resumo agregado do Maven mostrou "Tests run: 769, Errors: 83" — os 83 eram todos os
      cenários Cucumber (contam como um por cenário dentro de uma única classe de suíte), mas
      o relato inicial para o usuário disse "0 falhas" por engano (confundi "769 total" com
      "769 aprovados"). **Sempre leia a linha `Tests run: X, Failures: Y, Errors: Z` do
      resumo do Maven inteira, não só o total — Y e Z têm que ser 0 antes de declarar sucesso.**
    - Ordem seguindo o plano: F0-F4 convertem as 12 entidades + as 6 relações por ID; só na F5
      (depois de tudo convertido) a suíte completa volta a fazer sentido.
    - **Corolário que já pegou esta sessão de surpresa duas vezes**: antes de converter QUALQUER
      entidade isoladamente, confira se ela é alvo de um `@OneToMany`/`@ManyToOne` com
      `cascade` vindo de outra entidade AINDA `@Entity` (ex.: `OsPeca`/`OsServico` só existem
      via `@OneToMany(cascade=ALL)` a partir de `OsOrcamento`, que só existe via `OrdemServico`
      — nenhum dos três tem repositório próprio). Se sim, ela **não pode** ser convertida
      sozinha — precisa entrar na mesma fase do "pai" que a referencia via cascade, senão o
      Hibernate quebra com o mesmo `AnnotationException` do item acima assim que QUALQUER
      `@SpringBootTest` tentar subir (o que só se descobre tentando compilar/rodar, não no
      `test-compile`, já que isso é validação em runtime do Hibernate).

## Armadilha de infraestrutura de teste: `*ControllerIT` nunca roda no CI

11. **`./mvnw test` e `./mvnw verify` NÃO executam as classes `*ControllerIT`.** O projeto não
    tem o plugin Failsafe configurado — só o Surefire, com os padrões default
    (`**/*Test.java`, `**/Test*.java`, `**/*Tests.java`, `**/*TestCase.java`), que não casam com
    `*IT.java`. O workflow de CI (`.github/workflows/sonar.yml`) roda `./mvnw verify`, então essas
    classes nunca rodaram automaticamente. Confirmado durante a F5 do passo final: `Tests run: 769`
    do `./mvnw clean verify` não continha nenhuma menção a `ControllerIT` (nem no log, nem em
    `target/surefire-reports/`). Isso é uma lacuna pré-existente do projeto, não algo introduzido
    por qualquer refatoração — mas **se você alterar uma classe `*ControllerIT`, o `mvn verify`
    normal não vai confirmar que ela ainda passa**; rode-a explicitamente via
    `./mvnw test -Dtest=<FQCN-completo>` (uma por vez — mesma cautela do item 6 sobre não
    combinar `@SpringBootTest` no mesmo `-Dtest=A,B,C`). Classes existentes:
    `cadastro.presentation.ClienteControllerIT`, `integration.controller.ClienteControllerIT`
    (sim, duas classes com o mesmo nome simples em pacotes diferentes),
    `cadastro.presentation.VeiculoControllerIT`, `integration.controller.VeiculoControllerIT`,
    `acesso.presentation.AuthControllerIT`, `estoque.presentation.ItemEstoqueControllerIT`,
    `estoque.presentation.MovimentacaoControllerIT`,
    `acompanhamento.presentation.AcompanhamentoControllerIT`,
    `atendimento.presentation.OrdemServicoControllerIT`.

## Onde encontrar o resto

- Relatório completo da auditoria original: artefato Claude "Auditoria Hexagonal"
  (publicado via ferramenta Artifact nesta conta — usar `Artifact` com `action: "list"`
  para recuperar a URL se necessário).
- Estado atual do plano, contexto por contexto: `REFATORACAO_HEXAGONAL.md`.
