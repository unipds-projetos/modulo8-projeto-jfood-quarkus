# Etapa 3, Parte 2 — A camada de persistência em Quarkus + Jakarta Data

Gabarito da **Parte 2** da Etapa 3 do JFood, correspondente à segunda metade da Aula 3 (seções 3.7 a
3.12). O serviço é o [`jfood-administrativo`](../jfood-administrativo): **Quarkus 3.39.0** sobre
**Java 25**.

A Parte 1 — a mesma camada em Spring Data JPA — está em
[`modulo8-projeto-jfood-spring`, branch `aula03`](https://github.com/unipds-projetos/modulo8-projeto-jfood-spring/tree/aula03).
Este documento compara as duas o tempo todo.

## Como reproduzir

```bash
docker compose up -d
docker exec -i jfood-postgres psql -U postgres -d jfood-db < sql/aula03/01-ddl.sql
docker exec -i jfood-postgres psql -U postgres -d jfood-db < sql/aula03/02-seed.sql

cd jfood-administrativo && ./mvnw quarkus:dev
```

> O banco deste repositório é **separado** do banco do repo Spring — o volume leva o nome do projeto
> Compose no prefixo. Por isso o esquema e o seed vêm de `sql/aula03/`. Da Etapa 4 em diante quem
> assume é o Liquibase.

---

## 1. O projeto

Quatro dependências com papéis distintos, como a seção 3.7 lista: `quarkus-arc` (o container de
injeção), `quarkus-jdbc-postgresql` (o driver), `quarkus-hibernate-orm` (**o mesmo Hibernate da
Parte 1**) e `jakarta.data-api` — que traz **apenas interfaces e anotações**.

Isso deixa a pergunta da aula em aberto: se a API não implementa nada e não há proxy em runtime,
**quem** escreve a implementação? Resposta: o compilador.

```xml
<parameters>true</parameters>
<annotationProcessorPathsUseDepMgmt>true</annotationProcessorPathsUseDepMgmt>
<annotationProcessorPaths>
    <path>
        <groupId>org.hibernate.orm</groupId>
        <artifactId>hibernate-processor</artifactId>
    </path>
</annotationProcessorPaths>
```

**`<parameters>true</parameters>` não é opcional.** O Jakarta Data casa os **parâmetros do método**
com os atributos da entidade. Sem essa flag o compilador descarta os nomes (`arg0`, `arg1`) e o
`@Find` não tem como fazer a correspondência.

### O que sumiu das entidades

```properties
quarkus.hibernate-orm.physical-naming-strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy
```

Com a estratégia de nomes declarada, **`@Table(name = ...)` e a maioria dos `@Column(name = ...)`
desaparecem**: `senhaHash` vira `senha_hash`, `EnderecoEntrega` vira `endereco_entrega`. Sobraram
apenas os `@JoinColumn`, onde o nome da FK não é derivável do nome do atributo.

São 5 anotações a menos que na Parte 1 — e cada uma que sobra passa a **significar alguma coisa**,
em vez de repetir o que a convenção já diz.

> **`schema-management.strategy=validate`**, e não `update`. É o equivalente exato do
> `ddl-auto=validate` da Parte 1: o esquema foi criado por `sql/aula03/` e queremos a conferência.
> (A seção 3.7 da apostila mostra `update` porque lá o projeto do Javify cria o próprio esquema.)

## 2. `@Find`: o nome do método deixa de ser contrato

```java
@Repository
public interface EnderecoEntregaRepository extends CrudRepository<EnderecoEntrega, Long> {

    @Find
    List<EnderecoEntrega> porCep(String cep);
}
```

O que vale são os **parâmetros**. O processador olha para `cep`, confirma que `EnderecoEntrega` tem
esse atributo e gera a query. O método poderia se chamar `buscaPeloCep` que funcionaria igual.

### O typo, reproduzido

Trocando `cep` por `ceep`, **o build falha**:

```
[ERROR] .../EnderecoEntregaRepository.java:[39,41] no matching field named 'ceep'
        in entity class 'br.com.unipds.jfood.administrativo.domain.EnderecoEntrega'
```

Arquivo, linha, coluna e o nome da entidade — na sua máquina, antes de qualquer commit.

**O contraste com a Parte 1 é o ponto da aula.** Lá, `findByStattusOrderByDataPedidoDesc`, com dois
tês, **compila normalmente**: para o `javac` é só um nome de método. O erro aparece na subida da
aplicação, e em um projeto grande isso significa depois do build, depois do push, às vezes depois do
deploy.

> **O ganho central não é performance. É o momento em que você descobre que errou.**

### E um erro que eu não tinha planejado cometer

A primeira versão do repositório tinha isto:

```java
@Find
List<EnderecoEntrega> porClienteEApelido(Long cliente, String apelido);
```

Parece razoável — filtrar pelo id do cliente. O build quebrou:

```
[ERROR] matching field has type 'br.com.unipds.jfood.administrativo.domain.Cliente'
        in entity class 'br.com.unipds.jfood.administrativo.domain.EnderecoEntrega'
```

O atributo `cliente` da entidade é um `Cliente`, não um `Long`. O `@Find` casa por **nome e por
tipo** — e apontou a divergência na compilação. Para filtrar pelo id da associação, o caminho é a
`@Query` com `where cliente.id = :clienteId`.

Vale mais que o typo proposital: foi um erro de verdade, cometido sem intenção, pego antes de rodar.

## 3. Projeção por record

```java
@Query("""
       select new br.com.unipds.jfood.administrativo.repository.projection.ResumoPedido(
                  p.id, r.nome, p.valorTotal)
         from Pedido p join p.restaurante r
        where p.cliente.id = :clienteId
       """)
List<ResumoPedido> resumoDoCliente(Long clienteId);
```

O nome **totalmente qualificado** é obrigatório: a JPQL não tem `import`, e o Hibernate precisa do
caminho exato para achar o construtor.

### Qual das duas falha em silêncio?

**A projeção por interface, da Parte 1.**

| | Parte 1 — interface | Parte 2 — record |
|---|---|---|
| Como casa | Alias do `SELECT` × nome do *getter* | Argumentos × **construtor** |
| Se você errar | Campo volta **nulo**, sem erro, sem log | Quebra na hora, dizendo qual construtor não achou |
| Onde aparece | Como dado vazio na tela, dias depois | No build |

Na Parte 1 isso está documentado: sem os `AS`, a projeção volta com todos os campos nulos. Aqui,
errar a ordem, o tipo ou a quantidade dos argumentos é um erro de compilação.

## 4. A armadilha: aqui não existe sessão persistente

Esta é a seção mais importante da aula, e a que justifica a etapa inteira.

O experimento: carregar um `Restaurante`, mudar o nome com o *setter*, **não chamar nada**.

```
nome no banco ANTES:  Pizzaria Bella Napoli
PUT /api/v1/restaurantes/1/renomear-sem-salvar?nome=NOME QUE NAO VAI PERSISTIR
  -> {"nomeEmMemoria":"NOME QUE NAO VAI PERSISTIR","nomeAntes":"Pizzaria Bella Napoli"}
nome no banco DEPOIS: Pizzaria Bella Napoli
```

**Nenhum `UPDATE` saiu.** O log mostra a mudança; o banco, não. E nenhum erro foi lançado.

Na Parte 1 esse mesmo código gravaria: o *dirty checking* do contexto de persistência detecta a
alteração e emite o `UPDATE` no commit. Aqui o repositório gerado pelo `hibernate-processor` é
apoiado por uma **`StatelessSession`**:

| | `Session` (Parte 1) | `StatelessSession` (Parte 2) |
|---|---|---|
| Contexto de persistência | Sim — mapa de identidade | **Não** |
| *Dirty checking* | Sim | **Não** |
| *Lazy loading* de coleção | Sim | **Não** |
| Cache de primeiro nível | Sim | Não |
| Consequência | Conveniência, com N+1 escondido | Previsibilidade |

Com `save()` explícito, funciona:

```
PUT /api/v1/restaurantes/1/renomear?nome=Pizzaria Bella Napoli II
nome no banco: Pizzaria Bella Napoli II
```

## 5. `left join fetch` — e por que o `left` é obrigatório

```java
@Query("""
       select p from Pedido p
         left join fetch p.itens i
         left join fetch i.itemCardapio
         left join fetch p.restaurante
         left join fetch p.entregador
        where p.id = :id
       """)
Optional<Pedido> comItens(Long id);
```

**Duas razões, e a segunda só existe aqui.**

A primeira é a mesma da Parte 1: pedido ainda não despachado tem `entregador_id` nulo, e um `join
fetch` comum é *inner* — esses pedidos **sumiriam** do resultado.

A segunda é a consequência da `StatelessSession`. E aqui a medição refina o que o enunciado diz.

### Sem o fetch, o N+1 vira falha? Depende do que você não buscou

**Coleção (`@OneToMany`) — vira falha, e o enunciado está certo:**

```
GET /api/v1/pedidos/1/itens-sem-fetch
{
  "resultado": "FALHOU ao acessar a colecao",
  "excecao": "org.hibernate.LazyInitializationException",
  "mensagem": "Cannot lazily initialize collection of role
               'br.com.unipds.jfood.administrativo.domain.Pedido.itens'
               with key '1' (no session)"
}
```

**Associação `@ManyToOne` — NÃO vira falha:**

```
GET /api/v1/pedidos?status=ENTREGUE   -> 16 pedidos, todos com restaurante e entregador
queries: 9
```

Uma consulta para os pedidos, mais oito para as associações — o Hibernate resolve cada `@ManyToOne`
com uma query própria, no momento em que ela é lida, e ainda deduplica as repetidas (16 pedidos,
5 restaurantes e 3 entregadores distintos).

**Ou seja: o N+1 não desaparece na `StatelessSession` — ele fica `@ManyToOne` a `@ManyToOne`.** O que
desaparece é o *lazy loading de coleção*, e é aí que o bug de performance da Parte 1 vira bug de
correção.

Vale saber a diferença: em coleção, você descobre no primeiro request; em `@ManyToOne`, você
descobre no monitoramento, seis meses depois.

## 6. Contagem de queries por endpoint

| Endpoint | Queries | Por quê |
|---|---|---|
| `/clientes/10/pedidos` | **1** | `join fetch` do restaurante + `left join fetch` do entregador |
| `/clientes/10/pedidos/resumo` | **1** | Projeção por record: três colunas, nenhuma entidade |
| `/pedidos/1/itens` | **1** | `left join fetch` da coleção e das associações |
| `/pedidos?status=ENTREGUE` | **9** | Sem fetch: 1 + 8 associações resolvidas uma a uma |

Na Parte 1, o mesmo histórico do cliente 10 custava **6 queries** sem `JOIN FETCH` e **1** com. Aqui,
com o fetch, é **1** também — o Hibernate é o mesmo; o que muda é quem o dirige e o que ele faz
quando você **não** pede.

## 7. Tempo de startup

Medido com os **dois empacotados** — `java -jar`, e não `spring-boot:run` nem `quarkus:dev` —, na
mesma máquina, contra o mesmo banco e o mesmo esquema. Três execuções cada:

| | Execuções | Mediana |
|---|---|---|
| **Spring Boot 4.1.0** | 2,474 / 2,510 / 2,489 s | **2,489 s** |
| **Quarkus 3.39.0 (JVM)** | 1,487 / 1,272 / 1,292 s | **1,292 s** |

**Cerca de 1,9× mais rápido** — e é honesto dizer que é isso mesmo, e não as ordens de grandeza que
aparecem em material de marketing. Aqueles números são de **compilação nativa** (GraalVM), que não
foi usada aqui. O ganho de 1,2 s em JVM vem do trabalho que o Quarkus moveu para o build time:
injeção de dependências, mapeamento de entidades e configuração já resolvidos.

> **Onde 1,2 s importa e onde não importa.** Em um serviço que sobe uma vez por deploy, não importa.
> Importa em escala horizontal agressiva (um pod novo por pico de tráfego), em *serverless* — onde o
> *cold start* é cobrado do usuário — e no laço de desenvolvimento, em que você reinicia dezenas de
> vezes por dia.

---

## Critério de pronto

A escolha entre Spring Data JPA e Quarkus + Jakarta Data para o JFood, com o cenário concreto do
projeto e não a preferência pessoal:

**Para o `jfood-administrativo`, fica o Spring Data JPA.** As Etapas 5 e 6 dependem inteiramente do
que a `StatelessSession` não tem: `@Lock(PESSIMISTIC_WRITE)` com timeout, `@Version`, `@Transactional`
com propagação `REQUIRES_NEW`, *dirty checking* nos serviços de confirmação e cancelamento. Reescrever
tudo isso à mão para ganhar 1,2 s de startup em um serviço que sobe uma vez por deploy é uma troca
ruim.

**Onde o Quarkus ganharia no JFood:** no `jfood-tracking`. Ele é *append-only*, não tem transação
multi-tabela, não tem lock e escala horizontalmente com o pico de entregadores — exatamente o perfil
em que *cold start* e footprint de memória valem dinheiro. Não por acaso, é o serviço em que a Etapa
10 propõe o driver nativo.

**E o ganho que não se mede em milissegundos:** o erro de digitação que quebra o build. Em um time
grande, com muitos repositórios e uma suíte de testes que demora, descobrir na compilação em vez de
na subida muda o custo de cada engano — e esse benefício não depende de escala nenhuma.
