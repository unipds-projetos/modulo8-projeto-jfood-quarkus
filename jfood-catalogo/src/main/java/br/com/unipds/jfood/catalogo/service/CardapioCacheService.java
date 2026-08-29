package br.com.unipds.jfood.catalogo.service;

import br.com.unipds.jfood.catalogo.domain.ItemCardapio;
import br.com.unipds.jfood.catalogo.domain.ResultadoBusca;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.bson.types.ObjectId;
import org.eclipse.jnosql.databases.mongodb.mapping.MongoDBTemplate;
import org.jboss.logging.Logger;

/**
 * O dicionario Spring -> Quarkus, aplicado:
 *
 *   @Cacheable                      -> @CacheResult
 *   @CacheEvict(key = "#id")        -> @CacheInvalidate + @CacheKey no parametro
 *   @CacheEvict(allEntries = true)  -> @CacheInvalidateAll
 *   @CachePut                       -> NAO EXISTE
 */
@ApplicationScoped
public class CardapioCacheService {

    private static final Logger log = Logger.getLogger(CardapioCacheService.class);

    @Inject
    MongoDBTemplate template;

    @Inject
    MongoClient mongoClient;

    @Inject
    BuscaService buscaService;

    /**
     * @CacheResult, o analogo do @Cacheable.
     *
     * A CHAVE e derivada dos parametros do metodo, e nao de uma expressao SpEL.
     * E menos flexivel -- nao da para compor "#page + '-' + #size" -- e em troca
     * nao ha linguagem de expressao para errar em runtime: se os parametros
     * identificam o resultado, a chave esta certa por construcao.
     */
    @CacheResult(cacheName = "item-detalhe")
    public ItemCardapio detalhe(String id) {
        log.infof("MISS -> consultando o MongoDB: item %s", id);
        return porObjectId(id);
    }

    /**
     * MAIS UM VAZAMENTO, e este custou duas excecoes para aparecer.
     *
     * template.find(ItemCardapio.class, id) com o id em String NAO acha o
     * documento: o _id no MongoDB e um ObjectId, e a abstracao compara String
     * com ObjectId sem converter e sem reclamar -- devolve Optional.empty().
     *
     * Na versao Spring isso tem solucao declarativa:
     *   @Field(targetType = FieldType.OBJECT_ID)
     * Aqui nao ha equivalente. A saida e o filtro nativo, de novo.
     *
     * E o sintoma seguinte foi ainda menos obvio: o Quarkus recusa cachear nulo
     * (como o disableCachingNullValues do Spring), entao o erro que chega ao
     * cliente e `IllegalArgumentException: Cannot cache null value` -- uma
     * mensagem sobre CACHE, para um problema de MAPEAMENTO.
     */
    private ItemCardapio porObjectId(String id) {
        return template.select(ItemCardapio.class, Filters.eq("_id", new ObjectId(id)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item " + id + " nao encontrado"));
    }

    @CacheResult(cacheName = "itens-busca")
    public ResultadoBusca busca(String termo) {
        log.infof("MISS -> consultando o MongoDB: busca por '%s'", termo);
        List<ItemCardapio> itens = buscaService.porNomeIgnorandoCaixa(termo);
        return new ResultadoBusca(termo, itens.size(), itens);
    }

    /**
     * Ao atualizar um item, dois caches ficam errados -- e cada um precisa de uma
     * anotacao diferente.
     *
     * @CacheInvalidate mira UMA entrada, identificada pelo parametro marcado com
     * @CacheKey. @CacheInvalidateAll limpa o cache inteiro, porque nao ha como
     * saber em quais buscas aquele item aparecia.
     *
     * E o mesmo raciocinio da versao Spring, com uma diferenca de forma: la o
     * @Caching agrupava os dois evicts em uma anotacao so; aqui elas se empilham.
     */
    /**
     * O VAZAMENTO MAIS GRAVE DESTA ETAPA: uma escrita que reporta sucesso e nao
     * grava.
     *
     * template.update(item), com o id em String contra um _id ObjectId, nao
     * lanca excecao, nao cria documento novo e nao altera nada. Medido:
     *
     *   preco no banco ANTES:  59.29
     *   PUT .../preco?valor=123.45  ->  HTTP 200
     *   corpo devolvido diz:   123.45      (o objeto em memoria)
     *   preco no banco DEPOIS: 59.29       (nada mudou)
     *   documentos na colecao: 21          (nao duplicou)
     *
     * O cliente recebe 200, o corpo confirma o valor novo, os caches sao
     * invalidados corretamente -- e o dado nao existe. Um bug assim so aparece
     * quando alguem reclama que "a alteracao nao pegou".
     */
    @CacheInvalidate(cacheName = "item-detalhe")
    @CacheInvalidateAll(cacheName = "itens-busca")
    public ItemCardapio atualizarPreco(@CacheKey String id, java.math.BigDecimal novoPreco) {
        // template.update(item) NAO grava -- ver o comentario acima do metodo.
        // A escrita tambem desce para o driver.
        var resultado = mongoClient.getDatabase("jfood_catalogo")
                .getCollection("itens_cardapio")
                .updateOne(Filters.eq("_id", new ObjectId(id)),
                           Updates.set("preco", new org.bson.types.Decimal128(novoPreco)));
        log.infof("Preco do item %s atualizado: %d documento(s)", id, resultado.getModifiedCount());

        return porObjectId(id);
    }
}
