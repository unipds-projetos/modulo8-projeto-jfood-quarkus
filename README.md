# JFood em Quarkus — implementação de referência

Gabarito das partes **opcionais** do projeto JFood: as que a apostila de Bancos de Dados propõe em
**Quarkus**, com Jakarta Data, Jakarta NoSQL, Liquibase e os drivers nativos.

A parte principal — o JFood em Spring, das Etapas 1 a 11 — está em
[`modulo8-projeto-jfood-spring`](https://github.com/unipds-projetos/modulo8-projeto-jfood-spring).
Este repositório é o par dele, do mesmo jeito que o Javify separa `administrativo-spring` de
`administrativo-quarkus`.

## As branches

Cumulativas, e com lacunas: só as etapas que **têm** parte Quarkus ganham branch.

| Branch | Etapa | O que tem |
|---|---|---|
| `aula03` | 3 — Parte 2 | `jfood-administrativo` com Jakarta Data, `@Find` e a armadilha da `StatelessSession` |
| `aula04-sql` | 4 — Parte 2 | Liquibase com os changelogs em SQL formatado |
| `aula04` | 4 — Parte 2 | Liquibase idiomático, com os changelogs em XML declarativo |
| `aula08` | 8 — item 8 | Busca de itens do cardápio com Jakarta NoSQL |
| `aula09` | 9 — item 8 | Cache do cardápio com `@CacheResult` e `@CacheInvalidate` |
| `aula10` | 10 — item 7 | Tracking com o driver nativo do Cassandra (`CqlSession`) |
| `aula11` | 11 — item 7 | Recomendação com o driver nativo do Neo4J |

`main` aponta para o estado final.

## Os serviços

| Serviço | Banco | Porta | Nasce na |
|---|---|---|---|
| `jfood-administrativo` | PostgreSQL | 8080 | `aula03` |
| `jfood-catalogo` | MongoDB + Redis | 8082 | `aula08` |
| `jfood-tracking` | Cassandra | 8083 | `aula10` |
| `jfood-recomendacoes` | Neo4J | 8084 | `aula11` |

**Quarkus 3.39.0** sobre **Java 25**. Cada serviço é um projeto Maven independente, com o próprio
`mvnw`.

## Subindo

> **Um stack por vez.** As portas e os `container_name` são os mesmos do repositório Spring. Se o
> outro estiver de pé, derrube antes com `docker compose down` — senão o `up` falha com
> `container name "/jfood-postgres" is already in use`.

```bash
docker compose up -d

cd jfood-administrativo && ./mvnw quarkus:dev
```

O banco deste repositório é **separado** do banco do repo Spring: o volume leva o nome do projeto
Compose no prefixo. Na `aula03`, carregue o esquema e o seed antes de subir a aplicação:

```bash
docker exec -i jfood-postgres psql -U postgres -d jfood-db < sql/aula03/01-ddl.sql
docker exec -i jfood-postgres psql -U postgres -d jfood-db < sql/aula03/02-seed.sql
```

Da `aula04` em diante isso não é mais necessário: o Liquibase assume o esquema.

## Onde está o resto

| Pasta | O que tem |
|---|---|
| `docs/` | O gabarito escrito de cada etapa, com as medições reais e a comparação com a versão Spring |
| `postman/` | Uma collection por serviço |
| `sql/aula03/` | DDL e seed iniciais, até o Liquibase assumir |
