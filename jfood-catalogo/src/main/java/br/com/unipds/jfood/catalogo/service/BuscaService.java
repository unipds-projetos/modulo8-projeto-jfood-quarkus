package br.com.unipds.jfood.catalogo.service;

import br.com.unipds.jfood.catalogo.domain.ItemCardapio;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.eclipse.jnosql.databases.mongodb.mapping.MongoDBTemplate;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;

/**
 * A busca global por item: "quero pizza de calabresa perto de mim".
 */
@ApplicationScoped
public class BuscaService {

    @Inject
    DocumentTemplate template;

    /**
     * O template ESPECIFICO do MongoDB, um degrau abaixo do DocumentTemplate.
     *
     * Ele aceita um filtro Bson nativo e ainda devolve a entidade mapeada -- e a
     * saida documentada na secao 8.10 para o que o driver do JNoSQL nao cobre,
     * comecando pelo IGNORE_CASE.
     */
    @Inject
    MongoDBTemplate mongoDBTemplate;

    /**
     * E o driver CRU, um degrau abaixo de tudo.
     *
     * Ele existe aqui por UMA razao: ler o subset do restaurante, que a Jakarta
     * NoSQL nao consegue mapear de jeito nenhum.
     */
    @Inject
    MongoClient mongoClient;

    /**
     * Busca textual por parte do nome.
     *
     * O `like` do QueryMapper usa curinga de SQL -- `%` --, e nao regex. Medido:
     *
     *   like("Pizza")            -> 0 itens   (comporta-se como igualdade)
     *   like(".*Pizza.*")        -> 0 itens   (regex nao e interpretado)
     *   like("%Pizza%")          -> 6 itens
     *   like("Pizza Margherita") -> 1 item
     *
     * Nada disso esta na assinatura do metodo, e o caso errado nao da erro: da
     * lista vazia. Passar um regex aqui -- o reflexo de quem vem do MongoDB --
     * simplesmente nao acha nada, em silencio.
     */
    public List<ItemCardapio> porNome(String termo) {
        return template.select(ItemCardapio.class)
                .where("nome").like("%" + termo + "%")
                .and("disponivel").eq(true)
                .result();
    }

    /**
     * O card do resultado de busca: item + subset do restaurante.
     *
     * Aqui a abstracao ficou para tras. O documento e lido pelo driver nativo
     * porque o campo `restaurante` -- o subdocumento do Subset Pattern -- nao e
     * mapeavel pela extensao.
     */
    public List<Map<String, Object>> cardComSubset(String termo) {
        var colecao = mongoClient.getDatabase("jfood_catalogo").getCollection("itens_cardapio");
        var filtro = Filters.and(
                Filters.regex("nome", ".*" + java.util.regex.Pattern.quote(termo) + ".*", "i"),
                Filters.eq("disponivel", true));

        List<Map<String, Object>> cards = new ArrayList<>();
        for (Document doc : colecao.find(filtro)) {
            Document restaurante = doc.get("restaurante", Document.class);
            cards.add(Map.of(
                    "id", String.valueOf(doc.getObjectId("_id")),
                    "nome", doc.getString("nome"),
                    "preco", String.valueOf(doc.get("preco")),
                    "restaurante", restaurante == null ? Map.of() : Map.of(
                            "id", String.valueOf(restaurante.getObjectId("restaurante_id")),
                            "nome", restaurante.getString("nome"),
                            "notaMedia", String.valueOf(restaurante.get("nota_media")),
                            "taxaEntrega", String.valueOf(restaurante.get("taxa_entrega")))));
        }
        return cards;
    }

    /**
     * Pattern.quote() NAO serve aqui: ele produz \Q...\E, que o motor de regex
     * do MongoDB nao entende -- e a busca volta vazia, sem erro. Escapar os
     * metacaracteres um a um e o caminho.
     */
    private static String escapaRegex(String termo) {
        return termo.replaceAll("([\\\\.\\[\\]{}()*+?^$|])", "\\\\$1");
    }

    /**
     * A mesma busca, agora INSENSIVEL A MAIUSCULAS.
     *
     * O driver MongoDB do JNoSQL nao implementa IGNORE_CASE -- nem por derived
     * query, nem por @Query com LOWER(). A saida e descer para o
     * MongoDBTemplate com uma regex nativa: o filtro e do banco, mas o retorno
     * continua sendo a entidade mapeada.
     */
    public List<ItemCardapio> porNomeIgnorandoCaixa(String termo) {
        var filtro = Filters.regex("nome", java.util.regex.Pattern.quote(termo), "i");
        return mongoDBTemplate.select(ItemCardapio.class, filtro).toList();
    }

    public List<ItemCardapio> porTag(String tag) {
        return template.select(ItemCardapio.class)
                .where("tags").eq(tag)
                .result();
    }

    /**
     * Filtrar por campo DENTRO do subdocumento tambem nao passa pela abstracao,
     * pela mesma razao. Driver nativo de novo.
     */
    public List<ItemCardapio> porRestaurante(String restauranteId) {
        var colecao = mongoClient.getDatabase("jfood_catalogo").getCollection("itens_cardapio");
        List<ItemCardapio> itens = new ArrayList<>();
        for (Document doc : colecao.find(Filters.eq("restaurante.restaurante_id",
                                                    new org.bson.types.ObjectId(restauranteId)))) {
            template.find(ItemCardapio.class, String.valueOf(doc.getObjectId("_id")))
                    .ifPresent(itens::add);
        }
        return itens;
    }
}
