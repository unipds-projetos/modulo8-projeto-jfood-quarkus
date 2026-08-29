# Etapa 4, Parte 2 — Liquibase

Gabarito da **Parte 2** da Etapa 4 do JFood, correspondente à segunda metade da Aula 4 (seções 4.6 a
4.10). Duas branches, como o enunciado pede e como o `javify-administrativo-quarkus` faz:

| Branch | O que tem |
|---|---|
| [`aula04-sql`](https://github.com/unipds-projetos/modulo8-projeto-jfood-quarkus/tree/aula04-sql) | Os mesmos changelogs, em **SQL formatado** |
| [`aula04`](https://github.com/unipds-projetos/modulo8-projeto-jfood-quarkus/tree/aula04) | Os mesmos changelogs, em **XML declarativo** |

A Parte 1 — o mesmo esquema versionado com Flyway — está em
[`modulo8-projeto-jfood-spring`, branch `aula04`](https://github.com/unipds-projetos/modulo8-projeto-jfood-spring/tree/aula04).

## Como reproduzir

```bash
docker compose down -v && docker compose up -d
cd jfood-administrativo && ./mvnw quarkus:dev
```

O `sql/aula03/` não é mais usado: o esquema tem um dono, e é o Liquibase.

---

## 1. A conversão, sem reescrever uma linha de SQL

As oito migrações do Flyway viraram oito changelogs em SQL formatado. **O SQL é o mesmo, byte a
byte** — o que mudou foram quatro linhas no topo de cada arquivo:

```sql
--liquibase formatted sql

--changeset jfood:1-schema-inicial labels:schema
--comment: convertido de V1__schema_inicial.sql
```

E as propriedades:

```properties
quarkus.hibernate-orm.schema-management.strategy=none
quarkus.liquibase.migrate-at-start=true
quarkus.liquibase.change-log=db/changeLog.xml
```

`schema-management.strategy=none` é o equivalente exato do `ddl-auto=none` que o Flyway pediu: onde
existe ferramenta de migração, o esquema tem um dono só.

**Quanto tempo levou:** a apostila afirma que é trabalho de uma tarde. Convertidos os oito arquivos,
é menos que isso — o SQL não muda, e o trabalho real é decidir os `labels` e escrever os `rollback`.

### O changelog mestre, e a armadilha que ele elimina

`change-log` aponta para um **arquivo**, não para um diretório. O Flyway varre `db/migration/` por
convenção de nome; o Liquibase lê exatamente o que estiver na lista, na ordem em que estiver.

Isso mata a armadilha do underscore único da Parte 1. No Flyway, `V1_schema_inicial.sql` — com um
underscore em vez de dois — é ignorado **em silêncio**: sem erro, sem aviso, sem log, e a aplicação
sobe achando que está tudo certo. No Liquibase, um arquivo fora da lista também não roda, mas por um
motivo que salta aos olhos: **ele não está na lista**. A lista é o contrato.

E há um efeito colateral que a Parte 1 sofreu: a colisão de `V9` entre branches. Aqui ela vira um
**conflito de merge no `changeLog.xml`** — que é exatamente onde ela deveria aparecer, e onde o Git
sabe resolver.

### A repetível não tem prefixo, tem atributo

A `R__v_pedidos_do_dia.sql` do Flyway virou:

```sql
--changeset jfood:r-v-pedidos-do-dia labels:schema runOnChange:true
```

No Flyway o comportamento vem do **nome do arquivo**; no Liquibase, de um **atributo do changeset** —
e portanto do diff do code review. O `CREATE OR REPLACE` continua obrigatório nos dois: `runOnChange`
reexecuta o mesmo SQL, e um `CREATE VIEW` puro falharia na segunda vez.

Aplicação completa em banco vazio:

```
ChangeSet db/changelog/1-schema-inicial.sql::1-schema-inicial ... ran
...
ChangeSet db/changelog/r-v-pedidos-do-dia.sql::r-v-pedidos-do-dia ... ran

 usuarios | restaurantes | pedidos | hoje
----------+--------------+---------+------
       19 |            7 |      22 |    3
```

## 2. Os `rollback`, escritos à mão

O `--rollback` de cada changeset, com atenção à **ordem inversa das FKs**: no `1-schema-inicial`, os
`DROP TABLE` vão de `avaliacao` para `usuario`, e não o contrário.

Para o seed, o rollback é o `DELETE` correspondente — também em ordem inversa.

### Dois changesets que NÃO são reversíveis

E declarar isso é melhor do que fingir que são.

**`6-reajuste-precos-cardapio`** — o inverso aritmético seria dividir por 1,08, mas o `ROUND(x, 2)`
do reajuste descartou centavos que não voltam. Um preço de 54,90 virou 59,29; dividir 59,29 por 1,08
dá 54,898…, que arredonda de volta para 54,90 **por sorte**, não por garantia. Em outros valores a
volta erra um centavo — e um centavo errado em preço de cardápio é divergência contábil.

**`7-popula-taxa-entrega`** — por um motivo diferente e pior. O `UPDATE` preencheu com `0.00` as
linhas que estavam **nulas**. Depois dele, não há como distinguir um pedido que era nulo de um que
já tinha `0.00`: a informação de quais linhas foram afetadas foi embora com o próprio `UPDATE`.
Escrever `SET taxa_entrega = NULL WHERE taxa_entrega = 0` seria **pior** do que não ter rollback —
apagaria taxas legitimamente zeradas.

Os dois levam `--rollback empty`.

## 3. `labels`: estrutura e carga, separadas

`schema` para estrutura, `seed` para carga. Subindo um ambiente com **apenas** os de estrutura:

```bash
./mvnw quarkus:dev -Dquarkus.liquibase.labels=schema
```

```
changesets aplicados:
  1-schema-inicial            | schema
  2-add-taxa-entrega-pedido   | schema
  4-add-check-valor-pedido    | schema
  8-taxa-entrega-not-null     | schema
  r-v-pedidos-do-dia          | schema

 tabelas | usuarios | categorias | pedidos
---------+----------+------------+---------
      14 |        0 |          0 |       0
```

**Estrutura completa, dados vazios** — os quatro changesets de `seed` foram pulados. É o que se quer
em homologação e em qualquer ambiente que não deva nascer com dados de demonstração.

> Repare que `3-seed-categorias-restaurante` também ficou de fora, e isso merece uma decisão
> consciente: as categorias de cozinha não são dado de teste, são **dado de domínio** — sem elas a
> aplicação não cadastra restaurante nenhum. Num projeto de verdade, elas mereceriam um label
> próprio (`dominio`), aplicado em todo ambiente.

## 4. Rollback na prática

A extensão `quarkus-liquibase` aplica migrações **na subida** e não expõe rollback — e está certa:
rollback é operação de plantão, não de startup. Para os comandos de plantão, o `liquibase-maven-plugin`,
na versão **4.33.0**, casada com o `liquibase-core` que o BOM do Quarkus traz.

```bash
./mvnw liquibase:tag -Dliquibase.tag=antes-da-view
./mvnw liquibase:updateSQL                              # o SQL, sem executar
./mvnw liquibase:rollbackSQL -Dliquibase.rollbackCount=1
./mvnw liquibase:rollback    -Dliquibase.rollbackCount=1
```

### O que foi desfeito

```
antes:  9 changesets | view v_pedidos_do_dia existe | 22 pedidos
rollback de 1 changeset
depois: 8 changesets | view NAO existe            | 22 pedidos
```

A view sumiu. **Os dados continuaram.** É a primeira ressalva da seção 4.9: *rollback de DDL desfaz
estrutura, não dados*.

### E o que NÃO foi desfeito

Rolando mais dois — o `8-taxa-entrega-not-null` e o `7-popula-taxa-entrega`, este marcado como não
reversível:

```
antes:  taxa_entrega NOT NULL | 2 pedidos com taxa 0.00
depois: taxa_entrega nullable | 2 pedidos com taxa 0.00   <- NAO foi desfeito
        changesets: 6
```

O `SET NOT NULL` voltou atrás corretamente. O `UPDATE` da `7`, não — e **o histórico agora diz que
ela não está aplicada**, enquanto o dado que ela escreveu continua no banco.

> **Esta é a armadilha do `--rollback empty`, e ela merece o destaque.** O Liquibase considera o
> changeset desfeito e o remove do histórico, sem executar nada. O resultado é uma divergência
> silenciosa entre o que o histórico afirma e o que o banco contém — e um `update` posterior vai
> reaplicar o `UPDATE` como se fosse a primeira vez.
>
> `--rollback empty` é honesto sobre a intenção ("não sei desfazer isto") e perigoso na consequência.
> Para dados, a alternativa é guardar o estado anterior antes de alterar — uma tabela de snapshot, ou
> uma coluna `taxa_entrega_anterior` — e escrever o rollback a partir dela. Custa espaço; compra a
> capacidade de voltar.

**A regra que fica:** rode `updateSQL` e `rollbackSQL` **sempre**, antes de qualquer coisa. Ler o SQL
que seria executado é a única forma de descobrir, antes e não depois, que um changeset não desfaz o
que você acha que desfaz.

---

# A branch `aula04`: Liquibase idiomático

Os mesmos changelogs, agora no formato declarativo.

## 5. O `git diff` que responde a pergunta da etapa

```
$ git diff aula04-sql aula04 --stat

 db/changeLog.xml                          |  18 +-
 db/changelog/1-schema-inicial.sql         | 182 ---
 db/changelog/1-schema-inicial.xml         | 289 +++
 db/changelog/2-add-taxa-entrega-pedido.sql|  19 --
 db/changelog/2-add-taxa-entrega-pedido.xml|  17 ++
 ... (os outros changelogs)
 db/changelog/categorias-restaurante.csv   |   6 +
 20 files changed, 584 insertions(+), 389 deletions(-)
```

**O que mudou além dos changelogs? Nada.** O `pom.xml` é idêntico, o `application.properties` é
idêntico, nenhuma linha de Java mudou. A ferramenta é a mesma; muda apenas **como você descreve a
mudança**.

E o banco resultante é o mesmo, conferido linha a linha:

| | `aula04-sql` | `aula04` |
|---|---|---|
| Changesets no histórico | 9 | **22** |
| usuários / restaurantes / pedidos / itens | 19 / 7 / 22 / 36 | 19 / 7 / 22 / 36 |
| `CHECK` constraints | 10 | 10 |
| `taxa_entrega` | `NOT NULL` | `NOT NULL` |

Os 22 changesets contra 9 são a **granularidade de rollback** que o formato declarativo compra: um
changeset por tabela, e não um para o esquema inteiro. Dá para voltar `avaliacao` sem tocar em
`usuario`.

## 6. Como o esquema declarativo foi escrito — e o que ele não viu

Não foi digitado à mão. Foi gerado por engenharia reversa do banco já migrado:

```bash
./mvnw liquibase:generateChangeLog -Dliquibase.outputChangeLogFile=gerado.xml
```

O resultado: **78 changesets** — 12 `createTable`, 17 `addForeignKeyConstraint`, 9
`addUniqueConstraint` e 1 `createView`. Depois curados: ids e autor com significado, `labels`,
um changeset por tabela, as FKs agrupadas no fim.

Duas coisas precisaram ser removidas do gerado, e as duas ensinam algo:

- **`startWith="20"` na PK de `usuario`.** O gerador leu a posição **atual** da sequence e a gravou
  no changelog. Em um banco novo isso faria os ids começarem em 20 sem motivo. Engenharia reversa
  captura o **estado**, não a **intenção**.
- **`taxa_entrega` na tabela `pedido`.** Ela existe no banco de agora, mas pertence à migração 2. O
  gerador não tem como saber a linha do tempo — ele vê o retrato final.

E uma coisa o gerador **não viu**:

> **Nenhuma das `CHECK` constraints foi capturada.** Um esquema obtido por `generateChangeLog` vem
> sem elas, e ninguém é avisado. Se você adotar Liquibase em uma base existente por esse caminho e
> confiar no resultado, perde silenciosamente todas as regras de domínio que o banco garantia.

## 7. Onde a abstração termina

As `CHECK` foram escritas em `<sql>`, com o `<rollback>` explícito — porque não há change type para
elas no Liquibase open source. Uma `CHECK` é uma expressão arbitrária, e não existe vocabulário
portável que a descreva.

E é aqui que a conta do formato declarativo aparece:

| Change type | Rollback |
|---|---|
| `createTable`, `addColumn`, `addNotNullConstraint`, `addForeignKeyConstraint` | **Automático** |
| `loadData` | **Você escreve** — o Liquibase não sabe se as linhas já existiam |
| `<sql>` | **Você escreve** |
| `<sqlFile>` | **Você escreve** |

Repare em `8-taxa-entrega-not-null`: na branch em SQL o rollback era uma linha escrita à mão; aqui,
`<addNotNullConstraint>` traz o `dropNotNullConstraint` de graça. Já a `4-add-check-valor-pedido`
paga o preço integral.

**A regra prática:** quanto mais declarativo o changeset, mais a ferramenta trabalha por você — e o
inverso também vale. Todo `<sql>` é uma renúncia consciente à portabilidade **e** ao rollback
automático.

## 8. Cada seed no seu formato

**Categorias de cozinha → CSV + `loadData`.**

```xml
<loadData tableName="categoria_restaurante" file="db/changelog/categorias-restaurante.csv">
    <column name="nome" type="STRING"/>
</loadData>
```

Três decisões:

- **Sem a coluna `id`.** Ela é `SERIAL`. Listar ids no CSV deixaria a sequence defasada — o mesmo
  problema que o `setval` resolveu no seed da versão Spring.
- **`<column type>` declarado.** Sem ele o Liquibase adivinha pelo conteúdo, e um CSV de códigos
  numéricos vira coluna numérica.
- **`<rollback>` explícito**, com `<delete>`. `loadData` não tem rollback automático.

**Seed grande → `<sqlFile>`.** A regra que separa os dois não é o tamanho, é a **forma**: `loadData`
carrega linhas independentes com valores literais; o seed de demonstração resolve FKs por subquery
sobre o nome, insere ids explícitos e chama `setval` — ele descreve um **procedimento**, não uma
tabela de valores.

**A view → `<sql>` com `runOnChange="true"`.** Existe um `<createView>` declarativo, e ele não foi
usado de propósito: ele aceita o `SELECT` em texto de qualquer jeito — ou seja, o corpo da view nunca
é portável de verdade — e ainda adiciona uma camada entre o que você escreve e o que roda.

---

## Qual escolher, para o JFood

**Fica o Flyway**, na versão Spring — e a razão não é técnica, é de contexto.

O JFood roda em **um** PostgreSQL e vai continuar rodando. Não há segundo banco alvo, e portanto a
portabilidade do XML declarativo — o principal argumento do Liquibase — não compra nada. Em troca,
custa um formato a mais para o time aprender e uma camada entre a intenção e o SQL executado.

**O que o Liquibase entregaria de verdade aqui**, e vale reconhecer:

- **Rollback versionado no open source.** O Flyway só tem `undo` na versão paga. Se o time precisa
  de rollback como procedimento de plantão documentado, isso sozinho decide.
- **`labels`.** Subir homologação sem dados de demonstração é uma flag, e não um script à parte.
- **A colisão de versões entre branches vira conflito de merge** no `changeLog.xml`, em vez do erro
  de `V9` duplicada que a Etapa 4 Parte 1 reproduziu.

**O que decidiria a favor dele:** o dia em que o JFood precisar rodar o mesmo esquema em outro banco
— um cliente enterprise com Oracle, por exemplo. Aí o XML declarativo deixa de ser cerimônia e passa
a ser a única forma de não manter dois conjuntos de migrações.

> **As duas resolvem o mesmo problema com o mesmo modelo mental**: linha do tempo ordenada, tabela de
> histórico, checksum, imutabilidade do que já rodou. Quem entendeu a Parte 1 já entendeu a maior
> parte da Parte 2. Muda o vocabulário, e muda quanto trabalho você delega para a ferramenta.
