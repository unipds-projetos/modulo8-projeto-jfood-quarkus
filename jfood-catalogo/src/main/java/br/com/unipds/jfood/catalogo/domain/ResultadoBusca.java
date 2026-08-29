package br.com.unipds.jfood.catalogo.domain;

import java.util.List;

/**
 * O que vai para o cache no lugar de uma List<ItemCardapio>.
 *
 * O `value-type` do Quarkus e UMA classe por cache -- ele nao expressa
 * "List<ItemCardapio>". Um record proprio resolve, e de quebra guarda o termo e
 * a contagem, que a tela usa.
 *
 * E a mesma conclusao a que a versao Spring chegou com o PaginaRestaurantes,
 * por um caminho diferente: nao cacheie tipos genericos nem classes de
 * framework.
 */
public record ResultadoBusca(String termo, int total, List<ItemCardapio> itens) {

    public ResultadoBusca() {
        this(null, 0, List.of());   // o desserializador precisa de um construtor sem argumentos
    }
}
