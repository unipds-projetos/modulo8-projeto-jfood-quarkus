# Etapa 9, item 8 — O cache do cardápio no Quarkus

Gabarito do item 8 da Etapa 9 do JFood, correspondente à seção 9.9 da Aula 9. O cache entra no mesmo
[`jfood-catalogo`](../jfood-catalogo) da Etapa 8.

A Parte 1 — o mesmo cache com Spring Cache e Spring Data Redis — está em
[`modulo8-projeto-jfood-spring`, branch `aula09`](https://github.com/unipds-projetos/modulo8-projeto-jfood-spring/tree/aula09).

## Como reproduzir

```bash
docker compose up -d
cd jfood-catalogo && ./mvnw quarkus:dev
docker exec jfood-redis redis-cli KEYS '*'
```

---

## 1. O dicionário, aplicado

| Spring | Quarkus |
|---|---|
| `@Cacheable` | `@CacheResult` |
| `@CacheEvict(key = "#id")` | `@CacheInvalidate` + `@CacheKey` no parâmetro |
| `@CacheEvict(allEntries = true)` | `@CacheInvalidateAll` |
| `@CachePut` | **não existe** |

E **uma dependência em vez de duas**: no Spring foram `starter-cache` (o `CacheManager`) e
`starter-data-redis` (o cliente); aqui, `quarkus-redis-cache` sozinho.

Funcionando — duas chamadas à mesma busca:

```
MISS no log: 1
HIT: 6 itens para o termo 'pizza'
chaves no Redis: cache:itens-busca:pizza   (TTL 3585s)
```

## 2. Três diferenças de projeto que valem mais que a tabela

### A chave vem dos parâmetros, e não de uma expressão

No Spring, a chave é SpEL: `key = "#pageable.pageNumber + '-' + #pageable.pageSize"`. Aqui ela é
derivada dos parâmetros do método, e o resultado é `cache:itens-busca:pizza` — legível, previsível.

É **menos flexível**: não dá para compor uma chave a partir de partes de um objeto. Em troca, **não
há linguagem de expressão para errar em runtime**. Se os parâmetros identificam o resultado, a chave
está certa por construção. A Parte 1 gastou um parágrafo explicando por que não se deve deixar o
`toString()` do `Pageable` virar chave; aqui esse erro não é possível.

### O tipo é configuração, e não conteúdo

```properties
quarkus.cache.redis.itens-busca.value-type=br.com.unipds.jfood.catalogo.domain.ResultadoBusca
```

No Spring, o tipo viaja **dentro** do JSON — é o *default typing* do Jackson, que exige o
`PolymorphicTypeValidator` para não virar superfície de RCE. Aqui o tipo é declarado **fora** do
dado, cache por cache.

O efeito é visível no `redis-cli`:

```json
{"termo":"pizza","total":6,"itens":[{"id":"6a93...","nome":"Pizza Margherita","preco":59.29,...
```

JSON limpo, sem o `["classe", {...}]`. **Não há desserialização polimórfica**, e portanto não há o
que blindar — o problema de segurança que a seção 9.7 da apostila trata em profundidade simplesmente
não existe deste lado.

O preço: um cache sem `value-type` declarado falha em runtime, e o `value-type` é **uma** classe —
ele não expressa `List<ItemCardapio>`. Foi preciso um record próprio,
[`ResultadoBusca`](../jfood-catalogo/src/main/java/br/com/unipds/jfood/catalogo/domain/ResultadoBusca.java).

> Repare que é a **mesma conclusão** a que a versão Spring chegou com o `PaginaRestaurantes`, por um
> caminho completamente diferente. Nos dois casos, o que resolve é não cachear tipo genérico nem
> classe de framework.

### As invalidações se empilham

```java
@CacheInvalidate(cacheName = "item-detalhe")
@CacheInvalidateAll(cacheName = "itens-busca")
public ItemCardapio atualizarPreco(@CacheKey String id, BigDecimal novoPreco) { ... }
```

No Spring, o `@Caching(evict = {...})` agrupa; aqui as anotações se acumulam sobre o método. Medido:

```
antes:  cache:itens-busca:pizza  cache:item-detalhe:6a93...
PUT /itens/{id}/preco
depois: []
MISS no log ao reler: 2
```

Os dois caches invalidados, os dois voltando ao banco na leitura seguinte.

## 3. Onde a ausência de `@CachePut` mudou o desenho

**Não mudou** — e a razão é a mesma que a Parte 1 já tinha registrado.

O `@CachePut` grava o retorno do método em **uma** chave, sem consultar o cache. Ele serve para
manter uma entrada quente depois de uma escrita. O problema do JFood não é esse: quando o preço de um
item muda, o que fica errado são **N chaves de busca** — uma por termo pesquisado —, e não dá para
saber em quais aquele item aparecia.

A Parte 1 chegou a essa conclusão **tendo** o `@CachePut` disponível e escolhendo não usá-lo. Aqui a
escolha é a mesma, sem a opção existir.

> **Onde a falta doeria:** em um cache de escrita frequente e leitura imediata — um contador, um
> status que a própria tela acabou de mudar —, em que atualizar a entrada sai mais barato do que
> invalidá-la e pagar o próximo miss. Nesses casos, no Quarkus, você grava direto pelo
> `@Inject Cache` programático. Existe saída; ela só não é declarativa.

## 4. Os vazamentos da Jakarta NoSQL que só apareceram com o cache

A Etapa 8 documentou quatro. O cache expôs mais dois, e são os piores da série — porque o **sintoma**
aponta para o lugar errado.

### `find()` por id em String não acha documento com `_id` ObjectId

```
GET /api/v1/catalogo/itens/{id}
-> java.lang.IllegalArgumentException: Cannot cache `null` value
```

Uma mensagem sobre **cache**, para um problema de **mapeamento**. O
`template.find(ItemCardapio.class, id)` devolveu `Optional.empty()` porque comparou a `String` com um
`ObjectId` sem converter e sem reclamar; o Quarkus então recusou cachear o nulo — corretamente — e
foi a recusa que virou o erro visível.

Na versão Spring isso tem solução declarativa: `@Field(targetType = FieldType.OBJECT_ID)`. Aqui não
há equivalente; a saída é `Filters.eq("_id", new ObjectId(id))` pelo `MongoDBTemplate`.

### `update()` reporta sucesso e não grava

Este é o mais grave de toda a Parte 2. Medido, isoladamente:

```
preco no banco ANTES:  59.29
PUT /itens/{id}/preco?valor=123.45   ->  HTTP 200
corpo devolvido diz:   123.45         (o objeto em memoria)
preco no banco DEPOIS: 59.29          (nada mudou)
documentos na colecao: 21             (nao duplicou)
```

`template.update(item)`, com o mesmo descasamento String × ObjectId, **não lança exceção, não cria
documento novo e não altera nada**. O cliente recebe 200, o corpo confirma o valor novo, os caches
são invalidados corretamente — e o dado não existe.

Um bug assim não aparece em teste: aparece quando alguém reclama que "a alteração não pegou". A
correção foi descer para o `updateOne` do driver:

```java
mongoClient.getDatabase("jfood_catalogo").getCollection("itens_cardapio")
    .updateOne(Filters.eq("_id", new ObjectId(id)), Updates.set("preco", new Decimal128(novoPreco)));
```

Depois disso: `antes 59.29 → depois 123.45`. Funciona.

---

## O balanço das duas etapas

O `quarkus-redis-cache` é **bom**: uma dependência, chave previsível, JSON limpo, sem superfície de
desserialização polimórfica. Se o cache fosse o único assunto, a comparação favoreceria o Quarkus.

A Jakarta NoSQL, nesta versão da extensão, é outra história. Somando as duas etapas, foram **seis**
pontos em que foi preciso descer para a API específica do MongoDB — e dois deles falham **em
silêncio**, um na leitura e outro na escrita.

| Vazamento | Onde aparece | Custo |
|---|---|---|
| `getDisponivel()` em vez de `isDisponivel()` | Build | Baixo |
| Subdocumento embutido não mapeável | Runtime, 1ª consulta | Alto — é o Subset Pattern |
| `like` usa `%`, não regex | **Silêncio** — lista vazia | Alto |
| Sem `IgnoreCase` | Runtime / silêncio | Médio |
| `find()` por id String | Erro enganoso sobre cache | Alto |
| `update()` não grava | **Silêncio** — HTTP 200 | **Crítico** |

**A recomendação para o JFood é não adotar a Jakarta NoSQL neste serviço** — e isso não é um veredito
sobre a especificação, que na Etapa 3 (Jakarta Data sobre Hibernate) se mostrou sólida e trouxe um
ganho real de detecção em tempo de compilação. É um veredito sobre a **maturidade desta extensão**,
para este banco, nesta versão.

> É exatamente a lição que a Aula 11 registra sobre o `quarkus-jnosql-neo4j`: **a maturidade de uma
> especificação varia por driver.** Avaliar o driver específico, e não só a especificação, faz parte
> do trabalho de arquitetura. A Etapa 8 e a Etapa 9 acabaram de mostrar que isso vale também para o
> MongoDB, que é o driver mais maduro da família.
