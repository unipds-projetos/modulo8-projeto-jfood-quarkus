package br.com.unipds.jfood.recomendacoes.repository;

/**
 * A projecao -- identica a da versao Spring, e nao por acaso.
 *
 * Nas duas implementacoes a consulta devolve quatro colunas, e nao o grafo
 * hidratado. A diferenca esta em QUEM faz o mapeamento: la o Spring Data Neo4J,
 * a partir dos aliases do RETURN; aqui, tres linhas de codigo lendo o Record.
 */
public record RecomendacaoRestaurante(
        String restauranteId,
        String nome,
        String categoria,
        long forcaRecomendacao,
        long pesoTotal
) {}
