package br.com.unipds.jfood.administrativo.domain;

/**
 * Espelha, em Java, o CHECK de pedido.status criado na Etapa 2.
 * As duas protecoes sao necessarias: o enum protege quem passa pela aplicacao,
 * o CHECK protege quem entra pelo DBeaver.
 */
public enum StatusPedido {
    CRIADO,
    CONFIRMADO,
    EM_PREPARO,
    A_CAMINHO,
    ENTREGUE,
    CANCELADO
}
