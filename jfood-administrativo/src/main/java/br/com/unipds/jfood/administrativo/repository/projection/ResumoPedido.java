package br.com.unipds.jfood.administrativo.repository.projection;

import java.math.BigDecimal;

/**
 * A projecao por RECORD -- o contraste com a projecao por interface da Parte 1.
 *
 * A diferenca que interessa nao e de sintaxe, e de quando o erro aparece:
 *
 *   Parte 1 (interface): o Spring casa cada alias do SELECT com o nome do
 *     getter. Esquecer um `AS` devolve o campo NULO -- sem erro, sem log. Falha
 *     em SILENCIO, e o bug se manifesta como dado vazio na tela.
 *
 *   Parte 2 (record): o `select new` precisa casar com um CONSTRUTOR. Errar a
 *     ordem, o tipo ou a quantidade dos argumentos quebra na hora, com o
 *     Hibernate dizendo qual construtor ele nao achou.
 *
 * A resposta do item 3 da etapa e essa: a projecao por INTERFACE e a que falha
 * em silencio.
 */
public record ResumoPedido(Long numero, String restaurante, BigDecimal valorTotal) {
}
