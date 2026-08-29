package br.com.unipds.jfood.recomendacoes.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

/**
 * O driver nativo do Neo4J.
 *
 * O Cypher e IDENTICO ao da versao Spring -- linha por linha. O que muda e o que
 * envolve o Cypher: la uma interface @Query anotada, aqui um Session.run com o
 * mapeamento do Record feito a mao.
 */
@ApplicationScoped
public class RecomendacaoRepository {

    private static final String RECOMENDACAO_POR_VIZINHANCA = """
            MATCH (eu:Cliente {id: $clienteId})-[a1:AVALIOU]->(:Restaurante)
                  <-[a2:AVALIOU]-(vizinho:Cliente)-[a3:AVALIOU]->(sugestao:Restaurante)
            WHERE a1.nota >= $notaMinima
              AND a2.nota >= $notaMinima
              AND a3.nota >= $notaMinima
              AND vizinho <> eu
              AND NOT (eu)-[:AVALIOU]->(sugestao)
              AND NOT (eu)-[:PEDIU]->(sugestao)
            OPTIONAL MATCH (sugestao)-[:DO_TIPO]->(cat:Categoria)
            RETURN sugestao.id                        AS restauranteId,
                   sugestao.nome                      AS nome,
                   coalesce(cat.nome, 'Sem categoria') AS categoria,
                   count(DISTINCT vizinho)            AS forcaRecomendacao,
                   sum((a2.nota - 3) * (a3.nota - 3)) AS pesoTotal
            ORDER BY forcaRecomendacao DESC, pesoTotal DESC
            LIMIT $limite
            """;

    private static final String RECOMENDACAO_PONDERADA = """
            MATCH (eu:Cliente {id: $clienteId})-[a1:AVALIOU]->(base:Restaurante)
                  <-[a2:AVALIOU]-(vizinho:Cliente)-[a3:AVALIOU]->(sugestao:Restaurante)
            WHERE a1.nota >= $notaMinima
              AND a2.nota >= $notaMinima
              AND a3.nota >= $notaMinima
              AND vizinho <> eu
              AND NOT (eu)-[:AVALIOU]->(sugestao)
              AND NOT (eu)-[:PEDIU]->(sugestao)
            WITH sugestao,
                 count(DISTINCT vizinho)            AS forcaRecomendacao,
                 sum((a2.nota - 3) * (a3.nota - 3)) AS pesoTotal,
                 collect(DISTINCT base)             AS bases
            OPTIONAL MATCH (sugestao)-[:DO_TIPO]->(cat:Categoria)<-[:DO_TIPO]-(b:Restaurante)
            WHERE b IN bases
            WITH sugestao, forcaRecomendacao, pesoTotal, count(DISTINCT cat) AS categoriasEmComum
            OPTIONAL MATCH (sugestao)-[:DO_TIPO]->(minhaCat:Categoria)
            RETURN sugestao.id   AS restauranteId,
                   sugestao.nome AS nome,
                   coalesce(minhaCat.nome, 'Sem categoria') AS categoria,
                   forcaRecomendacao,
                   pesoTotal
            ORDER BY pesoTotal DESC, categoriasEmComum DESC, forcaRecomendacao DESC
            LIMIT $limite
            """;

    private static final String CONTAS_VINCULADAS = """
            MATCH (c1:Cliente)-[:PAGA_COM|ACESSA_DE]->(vinculo)
                  <-[:PAGA_COM|ACESSA_DE]-(c2:Cliente)
            WHERE elementId(c1) < elementId(c2)
            RETURN c1.nome AS contaA,
                   c2.nome AS contaB,
                   labels(vinculo)[0] AS tipoDeVinculo,
                   coalesce(vinculo.token, vinculo.id) AS vinculo
            ORDER BY contaA, contaB
            """;

    @Inject
    Driver driver;

    public List<RecomendacaoRestaurante> porVizinhanca(Long clienteId, int notaMinima, int limite) {
        return recomendar(RECOMENDACAO_POR_VIZINHANCA, clienteId, notaMinima, limite);
    }

    public List<RecomendacaoRestaurante> ponderado(Long clienteId, int notaMinima, int limite) {
        return recomendar(RECOMENDACAO_PONDERADA, clienteId, notaMinima, limite);
    }

    private List<RecomendacaoRestaurante> recomendar(String cypher, Long clienteId,
                                                     int notaMinima, int limite) {
        // try-with-resources: a Session E um recurso, e esquecer de fecha-la
        // vaza conexao do pool. Na versao Spring isso e responsabilidade do
        // framework -- e a primeira diferenca pratica entre as duas.
        try (Session session = driver.session()) {
            return session.run(cypher, Values.parameters(
                            "clienteId", clienteId,
                            "notaMinima", notaMinima,
                            "limite", limite))
                    .list(this::paraRecomendacao);
        }
    }

    /**
     * O mapeamento a mao.
     *
     * Na versao Spring, o alias do RETURN casa com o componente do record e o
     * framework faz isso sozinho. Aqui sao cinco linhas -- e cinco chances de
     * errar um nome de coluna, que so falha em runtime.
     */
    private RecomendacaoRestaurante paraRecomendacao(Record registro) {
        return new RecomendacaoRestaurante(
                registro.get("restauranteId").asString(),
                registro.get("nome").asString(),
                registro.get("categoria").asString(),
                registro.get("forcaRecomendacao").asLong(),
                registro.get("pesoTotal").asLong());
    }

    public List<ContaVinculada> contasVinculadas() {
        try (Session session = driver.session()) {
            return session.run(CONTAS_VINCULADAS).list(r -> new ContaVinculada(
                    r.get("contaA").asString(),
                    r.get("contaB").asString(),
                    r.get("tipoDeVinculo").asString(),
                    r.get("vinculo").asString()));
        }
    }

    /** O caminho mais curto -- a consulta em que o grafo abre 100x de vantagem. */
    public Map<String, Object> caminhoMaisCurto(Long de, Long para, int maxSaltos) {
        String cypher = """
                MATCH p = shortestPath((a:Cliente {id: $de})-[:AVALIOU*..%d]-(b:Cliente {id: $para}))
                RETURN length(p) AS saltos, [n IN nodes(p) | coalesce(n.nome, n.id)] AS caminho
                """.formatted(maxSaltos);
        try (Session session = driver.session()) {
            long inicio = System.nanoTime();
            var registro = session.run(cypher, Values.parameters("de", de, "para", para)).list();
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            if (registro.isEmpty()) {
                return Map.of("encontrado", false, "duracaoMs", ms);
            }
            return Map.of("encontrado", true,
                          "saltos", registro.getFirst().get("saltos").asInt(),
                          "caminho", registro.getFirst().get("caminho").asList(v -> v.asString()),
                          "duracaoMs", ms);
        }
    }
}
