package br.com.unipds.jfood.catalogo.domain;

import jakarta.nosql.Column;
import jakarta.nosql.Embeddable;
import java.math.BigDecimal;

/**
 * O subset do restaurante, duplicado dentro de cada item.
 *
 * ATENCAO -- a diferenca CRITICA em relacao a JPA:
 *
 *   Na Jakarta NoSQL, campo SEM @Column e simplesmente IGNORADO. Nao ha
 *   mapeamento implicito, nao ha erro, nao ha aviso. Esquecer a anotacao gera um
 *   campo que some silenciosamente do banco -- na leitura ele volta nulo, na
 *   gravacao ele nao vai.
 *
 * Na JPA e o contrario: tudo e mapeado por padrao, e @Transient e que exclui.
 * Aqui a exclusao e o padrao.
 *
 * Por isso TODOS os campos levam @Column explicito, inclusive quando o nome
 * coincide.
 */
@Embeddable
public class RestauranteResumo {

    @Column("restaurante_id")
    private String restauranteId;

    @Column("nome")
    private String nome;

    @Column("foto_url")
    private String fotoUrl;

    @Column("nota_media")
    private BigDecimal notaMedia;

    @Column("taxa_entrega")
    private BigDecimal taxaEntrega;

    public String getRestauranteId() { return restauranteId; }
    public void setRestauranteId(String restauranteId) { this.restauranteId = restauranteId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public BigDecimal getNotaMedia() { return notaMedia; }
    public void setNotaMedia(BigDecimal notaMedia) { this.notaMedia = notaMedia; }

    public BigDecimal getTaxaEntrega() { return taxaEntrega; }
    public void setTaxaEntrega(BigDecimal taxaEntrega) { this.taxaEntrega = taxaEntrega; }
}
