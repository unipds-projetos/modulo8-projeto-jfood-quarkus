# Etapa 10, item 7 — Tracking com o driver nativo do Cassandra

Gabarito do item 7 da Etapa 10 do JFood, correspondente à seção 10.7 da Aula 10. O serviço é o
[`jfood-tracking`](../jfood-tracking), na porta **8083**, com `cassandra-quarkus-client` **1.4.1**.

A Parte 1 — o mesmo serviço com Spring Data Cassandra — está em
[`modulo8-projeto-jfood-spring`, branch `aula10`](https://github.com/unipds-projetos/modulo8-projeto-jfood-spring/tree/aula10).

## Como reproduzir

```bash
docker compose up -d
docker exec -i jfood-cassandra cqlsh < cql/01-keyspace-e-tabelas.cql
cd jfood-tracking && ./mvnw quarkus:dev
```

---

## 1. Por que o driver nativo, e não a Jakarta NoSQL

A Etapa 8 seguiu o caminho agnóstico com MongoDB e mediu o custo: **seis** pontos em que foi preciso
descer para a API específica, dois deles falhando em silêncio.

Aqui a escolha é pelo driver desde o começo, e por um motivo anterior a esse: **este é o caminho mais
quente do sistema**. A Etapa 7 calculou 40.000 escritas por segundo no pico, e qualquer abstração
adiciona *overhead* de serialização exatamente onde ele mais custa.

### O `pom.xml` — e uma coordenada que não resolve

```xml
<dependency>
    <groupId>com.datastax.oss.quarkus</groupId>
    <artifactId>cassandra-quarkus-bom</artifactId>
    <version>1.4.1</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

> **Atenção.** A seção 10.7 da apostila e o projeto `javify-historico-reproducao-quarkus` importam
> `io.quarkus:quarkus-cassandra-bom` com `${quarkus.platform.version}`. **Esse artefato não existe no
> Maven Central em nenhuma versão** — testei 3.36.2 e 3.39.0, e as duas devolvem 404. O build falha
> com `Non-resolvable import POM`.
>
> As coordenadas corretas são `com.datastax.oss.quarkus:cassandra-quarkus-bom`, e a versão é a do
> **conector** (1.4.1), não a da plataforma Quarkus.

## 2. A entidade vira um POJO puro

```java
public record Ping(Long entregadorId, Long entregaId, Double latitude,
                   Double longitude, Double velocidadeKmh, Instant instante) { }
```

Sem `@Table`, sem `@PrimaryKeyClass`, sem `@PrimaryKeyColumn`. **A chave não existe no Java**: ela
vive no CQL, que é onde ela sempre viveu.

O que se perde é real: na Parte 1, a classe de chave **documentava a modelagem física** —
`PARTITIONED` no `(entregador_id, dia)`, `CLUSTERED` no `instante`, `Ordering.DESCENDING`. Quem lê
este record não descobre nada disso. O bucketing, que é a decisão mais importante da Etapa 10, some
do código Java e passa a existir só no arquivo `.cql`.

**Em troca:** nada entre o objeto e o statement.

## 3. Os statements preparados no `@PostConstruct`

```java
@PostConstruct
void prepararStatements() {
    inserirRotaDia = session.prepare("""
            INSERT INTO rota_por_entregador_dia (...) VALUES (?, ?, ?, ?, ?, ?, ?)
            USING TTL """ + " " + TTL_SEGUNDOS);
    ...
}
```

Preparar custa **um round-trip** ao coordenador, que devolve um id e o metadata das colunas. Preparar
dentro do laço desperdiça esse round-trip a cada chamada — e o driver ainda emite o aviso
`re-preparing already prepared query`, que é o sintoma clássico. O bean é `@ApplicationScoped`, então
o `@PostConstruct` roda uma vez por aplicação.

Na Parte 1 isso é feito pelo Spring Data, por baixo. Aqui é explícito — e o custo dessa explicitude
apareceu na primeira execução:

```
SyntaxError: line 4:6 no viable alternative at input 'TTL7776000'
```

Um `USING TTL """ + TTL_SEGUNDOS` sem espaço entre o *text block* e a concatenação. **CQL montado por
concatenação de String é CQL sem validação até a subida** — e o `prepare()` no `@PostConstruct` é o
que salva: o erro apareceu ao iniciar a aplicação, e não na primeira requisição de produção.

> Vale registrar o contraste: na Etapa 3, a `@Query` do Jakarta Data é conferida em **tempo de
> compilação**. Aqui não há nada disso; o melhor que se consegue é falhar na subida.

TTL confirmado no banco: `TTL(velocidade_kmh) = 7775999` — 90 dias, como na Parte 1.

## 4. Teste de carga: os dois números lado a lado

O **mesmo harness**, os mesmos parâmetros, a mesma máquina, o mesmo Cassandra de um nó em container:
100 mil pontos, 200 entregadores, três escritas por ping.

| Concorrência | Spring Data | **Quarkus + driver** | Diferença |
|---|---|---|---|
| 32 | 7.302 pontos/s | 7.053 pontos/s | −3% |
| 64 | 13.126 pontos/s | 13.359 pontos/s | +2% |
| **128** | **17.266 pontos/s** | **21.990 pontos/s** | **+27%** |

Zero falhas em todas as rodadas, dos dois lados.

Em escritas por segundo, na concorrência 128: **51.797 → 65.971**.

### Lendo o resultado com honestidade

**Em concorrência baixa, empatam** — e isso não é surpresa: o gargalo ali é a latência de rede e o
commitlog do Cassandra, e nenhuma camada Java aparece na conta.

**A vantagem só surge sob pressão.** Em 128 threads concorrentes, o custo por operação da camada de
mapeamento — converter a entidade em `BoundStatement`, resolver a chave composta, aplicar as
conversões de tipo — passa a competir por CPU com o próprio driver. Os 27% são isso.

**E 27% é menos do que "driver nativo é muito mais rápido" sugere.** O Spring Data Cassandra é uma
camada fina: ele *usa* o mesmo driver DataStax por baixo. A diferença não é entre "com abstração" e
"sem abstração"; é entre uma abstração fina e nenhuma.

### Startup

| | Execuções | Mediana |
|---|---|---|
| Spring Boot 4.1.0 | 1,194 / 1,218 / 1,187 s | **1,194 s** |
| Quarkus 3.39.0 (JVM) | — | **0,440 s** |

**2,7× mais rápido**, e aqui a diferença é maior do que a do administrativo (2,489 s × 1,292 s,
1,9×): este serviço tem menos para inicializar, então a fatia que o Quarkus resolve em build time
pesa proporcionalmente mais.

## 5. A decisão para o JFood

**Para o `jfood-tracking`, o Quarkus com driver nativo é a escolha certa** — e é o único serviço do
JFood em que eu diria isso sem hesitar.

Os três argumentos, em ordem de peso:

1. **Escala horizontal agressiva.** Este serviço escala com o número de entregadores em corrida, que
   varia com a hora do dia. Um pod novo a cada pico é rotina, e 0,44 s contra 1,19 s de *cold start*
   muda a velocidade com que a frota responde.
2. **Nada aqui precisa de abstração.** Não há transação multi-tabela, não há lock, não há
   relacionamento a navegar. São três `INSERT` e três `SELECT` por partição — e o Spring Data não tem
   o que simplificar.
3. **Os 27% sob carga.** Não decidem sozinhos, mas são de graça: eles vêm junto com as duas razões
   acima.

**O que se perde, e vale dizer em voz alta:** a modelagem física sumiu do Java. A decisão mais
importante da Etapa 10 — o bucketing por `(entregador_id, dia)`, e a conta que o justifica — não está
em nenhuma classe. Um desenvolvedor novo lê `Ping` e não faz ideia. Em um projeto real, esse
conhecimento precisa estar no `.cql` **e** em um comentário no repositório — que é exatamente o que
este gabarito faz.
