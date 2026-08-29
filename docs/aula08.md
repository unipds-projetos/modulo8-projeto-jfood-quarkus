# Etapa 8, item 8 — A busca do cardápio com Jakarta NoSQL

Gabarito do item 8 da Etapa 8 do JFood, correspondente à seção 8.10 da Aula 8. O serviço é o
[`jfood-catalogo`](../jfood-catalogo), na porta **8082**, com **Quarkus 3.39.0** e
`quarkus-jnosql-mongodb` **3.4.13**.

A Parte 1 — o catálogo completo com Spring Data MongoDB — está em
[`modulo8-projeto-jfood-spring`, branch `aula08`](https://github.com/unipds-projetos/modulo8-projeto-jfood-spring/tree/aula08).

O item pede o **serviço de busca**, e não o CRUD inteiro: é ele que estressa a abstração.

## Como reproduzir

```bash
docker compose up -d
docker cp mongo/seed-catalogo.js jfood-mongodb:/tmp/seed.js
docker exec jfood-mongodb mongosh --quiet -u admin -p adminpassword \
  --authenticationDatabase admin jfood_catalogo --file /tmp/seed.js

cd jfood-catalogo && ./mvnw quarkus:dev
```

---

## 1. `<proc>full</proc>` e `@Column` em tudo

```xml
<configuration>
    <parameters>true</parameters>
    <proc>full</proc>
</configuration>
```

A partir do **JDK 21**, o `javac` desativou por padrão a execução automática de *annotation
processors* vindos do **classpath**. A extensão JNoSQL depende do `mapping-lite-processor` para gerar
os metadados, e ele vem por ali. Sem a linha, o projeto compila e a aplicação falha na subida com
`Unsatisfied dependency for type ...EntitiesMetadata`.

> Na Etapa 3 isso não foi necessário, e vale entender por quê: lá o `hibernate-processor` está
> declarado em `annotationProcessorPaths`, e **declarar o caminho já é** configuração explícita de
> processador. A regra do JDK 21 vale para o que vem implícito pelo classpath.

**E `@Column` em todos os campos**, inclusive quando o nome coincide. Na Jakarta NoSQL, campo sem
anotação é **ignorado**: não há mapeamento implícito, não há erro, não há aviso. Na leitura ele volta
nulo; na gravação ele não vai. É o inverso da JPA, onde tudo é mapeado por padrão e `@Transient` é
que exclui.

---

## 2. Onde a abstração vazou

O item pede *"o primeiro ponto em que a abstração vazou e o que você fez"*. Foram **quatro**, e vale
listar na ordem em que apareceram — porque a ordem diz muito sobre o custo de cada um.

### Vazamento 1 — `getDisponivel()`, e não `isDisponivel()`

Na primeira compilação:

```
org.eclipse.jnosql.lite.mapping.ValidationException: There is not valid getter
method to the field: disponivel in the class: ItemCardapio
```

O `mapping-lite-processor` exige o prefixo `get` **inclusive para `boolean`**. A convenção JavaBeans
— a que a JPA, o Jackson e o próprio Java usam — manda `is`. Aqui ela não vale, e a mensagem não diz
qual é a regra: diz apenas que não existe getter válido, para um getter que está ali, público, com o
nome que o resto do ecossistema espera.

**Custo: baixo.** Quebra o build, na primeira tentativa. É o melhor tipo de vazamento.

### Vazamento 2 — o subdocumento embutido não é mapeável

Este é o grave.

```java
@Column("restaurante")
private RestauranteResumo restaurante;   // o subset do Subset Pattern
```

Compila. E na **primeira consulta**, em runtime:

```
java.lang.UnsupportedOperationException: The type class
br.com.unipds.jfood.catalogo.domain.RestauranteResumo is not supported yet
```

Anotar `RestauranteResumo` com `@Embeddable` **não resolve** — testado, mesmo erro. E o `"yet"` da
mensagem é a própria implementação admitindo que é limitação dela, não da especificação.

**O peso disso é específico do JFood:** o subdocumento em questão é o **subset do restaurante**,
duplicado dentro de cada item — a coisa que a Etapa 8 Parte 1 construiu inteira. A busca global por
item existe para devolver, em **uma consulta**, o prato *e* o nome, a nota e a taxa do restaurante.
É exatamente isso que a abstração não entrega.

**O que foi feito:** o campo saiu do mapeamento, e o card completo é montado pelo driver nativo
(`MongoClient`). A abstração ficou com o item; o subset ficou com o driver.

### Vazamento 3 — `like` usa `%`, e não regex

O reflexo de quem vem do MongoDB é passar uma regex. Medido:

| Chamada | Resultado |
|---|---|
| `like("Pizza")` | **0 itens** — comporta-se como igualdade |
| `like(".*Pizza.*")` | **0 itens** — a regex não é interpretada |
| `like("%Pizza%")` | **6 itens** ✓ |
| `like("Pizza Margherita")` | 1 item |

Nada disso está na assinatura do método, e o caso errado **não dá erro**: dá lista vazia. Uma busca
que não acha nada parece um banco sem dados, e não um curinga na convenção errada.

**Custo: alto**, porque falha em silêncio e em produção.

### Vazamento 4 — não existe `IgnoreCase`

Este a apostila já documenta na seção 8.10, e o experimento confirma:

```
termo minusculo "pizza", pelo like da Jakarta NoSQL  ->  0 itens
```

O driver MongoDB do JNoSQL não implementa `IGNORE_CASE` — nem por *derived query*, nem via `@Query`
com `LOWER()`. A saída documentada é o `MongoDBTemplate`, que aceita um filtro `Bson` nativo e
**ainda devolve a entidade mapeada**.

---

## 3. Os três níveis, medidos com o mesmo termo

Buscando `pizza` — minúsculo — nas três implementações:

| Nível | Como | Resultado |
|---|---|---|
| 1. `DocumentTemplate` (Jakarta NoSQL puro) | `.where("nome").like("%pizza%")` | **0 itens** |
| 2. `MongoDBTemplate` (específico do Mongo) | `Filters.regex("nome", termo, "i")` | **6 itens** |
| 3. `MongoClient` (driver cru) | `find(Filters.and(regex, eq))` | **6 cards**, com o subset |

Cada degrau resolve o que o de cima não alcança — e cada degrau custa portabilidade.

> **A escada é a lição.** Não é que a Jakarta NoSQL "não funciona": ela cobre o CRUD e as consultas
> por igualdade sem esforço nenhum, e o `@Repository` é literalmente o mesmo da Etapa 3, com outro
> banco embaixo. O que ela não cobre é o que cada banco tem de próprio — e, no MongoDB, isso inclui
> duas coisas centrais para este domínio: **subdocumento embutido** e **busca insensível a
> maiúsculas**.

## 4. O trade-off, respondido

**Para a busca do JFood, a Jakarta NoSQL não se paga.**

O argumento dela é portabilidade: trocar o MongoDB por outro banco de documentos sem reescrever a
camada de acesso. Mas duas das quatro funcionalidades que esta tela precisa já exigiram descer para a
API específica — e o código que sobra na abstração é o que menos custaria reescrever de qualquer
jeito.

Pior: a portabilidade que se paga é **parcial e invisível**. Um projeto que use `MongoDBTemplate` em
metade dos métodos não é portável, mas continua parecendo portável — até o dia da migração.

**Onde ela se pagaria:** em um serviço cujo acesso a dados seja majoritariamente CRUD e consulta por
igualdade, e em que a troca de banco seja um requisito real e datado. Não é o caso da busca de
cardápio, que é justamente a tela mais dependente do que o MongoDB tem de particular.

> A frase que a Aula 8 já dizia, agora com número: **abstração não elimina o banco; só adia o momento
> de encará-lo.** Aqui, o momento chegou na segunda consulta.
