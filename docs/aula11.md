# Etapa 11, item 7 — Recomendação com o driver nativo do Neo4J

Gabarito do item 7 da Etapa 11 do JFood, correspondente à seção 11.8 da Aula 11. O serviço é o
[`jfood-recomendacoes`](../jfood-recomendacoes), na porta **8084**, com
`io.quarkiverse.neo4j:quarkus-neo4j` **6.6.1**.

A Parte 1 — o mesmo serviço com Spring Data Neo4J — está em
[`modulo8-projeto-jfood-spring`, branch `aula11`](https://github.com/unipds-projetos/modulo8-projeto-jfood-spring/tree/aula11).

## Como reproduzir

```bash
docker compose up -d
docker exec -i jfood-neo4j cypher-shell -u neo4j -p neo4jpassword < cypher/01-restricoes.cypher
python3 scripts/sincronizar-grafo.py | \
  docker exec -i jfood-neo4j cypher-shell -u neo4j -p neo4jpassword
docker exec -i jfood-neo4j cypher-shell -u neo4j -p neo4jpassword \
  < cypher/02-demo-avaliacoes-e-vinculos.cypher

cd jfood-recomendacoes && ./mvnw quarkus:dev
```

---

## 1. Os resultados são idênticos — e isso é o ponto de partida

| | Spring Data Neo4J | Quarkus + driver |
|---|---|---|
| Recomendação por vizinhança (cliente 10) | Esfiha 4/13, Sabor 4/10, Verde 3/11, Forno 2/4, Bella 2/3 | **idêntico** |
| Ranking ponderado | Esfiha 13, Verde 11, Sabor 10, Forno 4, Bella 3 | **idêntico** |
| Contas vinculadas (fraude) | 4 pares | **idêntico** |

**Nenhuma linha de Cypher mudou.** As três consultas são as mesmas, caractere por caractere.

E é por isso que a comparação desta etapa é diferente das anteriores: aqui não há discussão sobre o
que cada abordagem *consegue fazer*. As duas fazem a mesma coisa. A pergunta é sobre **esforço**.

## 2. O esforço de troca de banco, item por item

A pergunta do item 7 é essa. A resposta, comparando os dois repositórios lado a lado:

| | Spring Data Neo4J | Quarkus + driver nativo |
|---|---|---|
| **Nós e arestas em Java** | `@Node`, `@Relationship(direction)`, `@RelationshipProperties`, `@TargetNode`, `@RelationshipId` — 4 classes de domínio | **Nenhuma classe.** O grafo não é modelado no Java |
| **A consulta** | `@Query` em uma interface `Neo4jRepository` | `String` constante + `session.run(cypher, parametros)` |
| **O mapeamento do resultado** | Automático: alias do `RETURN` → componente do record | 5 linhas por projeção, lendo `Record` campo a campo |
| **Ciclo de vida da conexão** | Do framework | Seu: `try (Session session = driver.session())` |
| **Linhas de código** | ~140 (4 domínios + repositório + projeções) | **~180 em um arquivo só** |

### O que se ganha ao trocar

**Some a camada de mapeamento do grafo.** As quatro classes `@Node`/`@RelationshipProperties` da
versão Spring existiam para o OGM navegar objetos — e as consultas desta etapa **nunca navegam
objetos**: elas devolvem projeções. Eram quatro classes mantidas para nada, no caminho que importa.

**A consulta fica onde ela é executada.** Na versão Spring, o Cypher mora em uma anotação de uma
interface sem corpo, e o mapeamento acontece em um lugar que não aparece no código.

**Startup 3× mais rápido:**

| | Execuções | Mediana |
|---|---|---|
| Spring Boot 4.1.0 | 1,202 / 1,224 / 1,211 s | **1,211 s** |
| Quarkus 3.39.0 (JVM) | 0,488 / 0,397 / 0,402 s | **0,402 s** |

### O que se perde

**O mapeamento do resultado vira código, e código erra.** Cinco `registro.get("nome")` por projeção,
cada um com um nome de coluna em String. Errar um deles falha **em runtime**, e não na subida — a
versão Spring falharia igual, mas ela tem uma linha em vez de cinco.

**A `Session` vira responsabilidade sua.** Esquecer o `try-with-resources` vaza conexão do pool, e o
sintoma aparece sob carga, longe do código que causou.

**O modelo do grafo sai do Java.** É a mesma perda da Etapa 10, com a mesma consequência: quem lê o
código não descobre que a nota mora na aresta `AVALIOU`, nem que `Cliente.id` vem do PostgreSQL. Esse
conhecimento fica só nos arquivos `.cypher` e no `scripts/sincronizar-grafo.py`.

## 3. A rota agnóstica: o que mudou desde a apostila

A apostila registra que a extensão `quarkus-jnosql-neo4j` **não se mostrou madura** — erros
sucessivos no `Neo4JRepository`, no `Neo4JTemplate` e na criação de repositórios.

O que dá para verificar hoje, sem tentar de novo:

```
io.quarkiverse.jnosql:quarkus-jnosql-neo4j    latest 3.5.0   (2026-08-11)
io.quarkiverse.jnosql:quarkus-jnosql-mongodb  latest 3.5.0   (2026-08-11)
```

A extensão **continua publicada e liberada junto com as outras da família**, na mesma versão e no
mesmo dia. Não é um artefato abandonado.

**Mas a Etapa 8 deste repositório mudou o cálculo.** Ela usou a extensão de **MongoDB** — o driver
mais maduro da família JNoSQL — e encontrou seis vazamentos, dois deles silenciosos, incluindo um
`update()` que responde HTTP 200 e não grava. Se o driver mais maduro está nesse estado, tentar o de
Neo4J para uma consulta de três saltos com agregação seria otimismo, e não avaliação.

> É a lição da própria apostila, agora com evidência dos dois lados: **a maturidade de uma
> especificação varia por driver.** E a decisão de arquitetura correta não é "a especificação é boa"
> ou "a especificação é ruim" — é medir o driver que você vai usar, na versão que você vai usar.

## 4. E a consulta em que o grafo ganha de verdade

A Etapa 11 Parte 1 mediu: em três saltos, o PostgreSQL empata ou ganha; a vantagem do grafo aparece
na **profundidade**. O driver nativo expõe isso diretamente:

```
GET /api/v1/recomendacoes/caminho?de=10&para=19
{ "encontrado": true, "saltos": 2, "caminho": ["Ana Lima", "Sushi Yuki", "João Vitor Ramos"],
  "duracaoMs": ... }
```

`shortestPath((a)-[:AVALIOU*..6]-(b))` — trocar a profundidade é trocar o `6`. A `WITH RECURSIVE`
equivalente, medida na Parte 1 em **348 ms** contra ~3 ms, precisaria ser reescrita.

## 5. A decisão para o JFood

**Para o `jfood-recomendacoes`, o driver nativo é a escolha melhor — mas a decisão anterior a ela
continua valendo mais.**

A Parte 1 concluiu que o Neo4J, para o JFood no estágio atual, é **decisão de roadmap**: a
recomendação de três saltos cabe em um `WITH RECURSIVE` no PostgreSQL, e o grafo só se paga quando o
produto pedir profundidade variável.

Essa conclusão não muda com o Quarkus. O que esta etapa acrescenta é: **se e quando o Neo4J entrar**,
entre com o driver nativo. As classes `@Node` do OGM não estavam pagando o próprio custo — as
consultas que interessam devolvem projeções, e projeção não precisa de grafo de objetos.

> **A regra que sai das Etapas 10 e 11, juntas:** onde o acesso a dados é um punhado de consultas
> conhecidas e otimizadas à mão, o driver nativo é mais honesto que o mapeamento — ele custa mais
> código e devolve controle. Onde o acesso é amplo, variado e navega objetos — o
> `jfood-administrativo` das Etapas 5 e 6 —, o mapeamento paga o próprio preço com folga.
