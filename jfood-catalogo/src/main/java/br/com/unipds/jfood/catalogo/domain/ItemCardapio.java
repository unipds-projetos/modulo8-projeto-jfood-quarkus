package br.com.unipds.jfood.catalogo.domain;

import jakarta.nosql.Column;
import jakarta.nosql.Entity;
import jakarta.nosql.Id;
import java.math.BigDecimal;
import java.util.List;

/**
 * A colecao itens_cardapio, criada pelo Subset Pattern na Etapa 8 Parte 1.
 *
 * @Entity e @Id vem de jakarta.nosql, e nao de jakarta.persistence: sao da
 * especificacao Jakarta NoSQL. O nome da colecao vai no @Entity.
 */
@Entity("itens_cardapio")
public class ItemCardapio {

    @Id
    private String id;

    @Column("nome")
    private String nome;

    @Column("descricao")
    private String descricao;

    @Column("preco")
    private BigDecimal preco;

    @Column("foto_url")
    private String fotoUrl;

    @Column("tags")
    private List<String> tags;

    @Column("secao")
    private String secao;

    @Column("disponivel")
    private boolean disponivel;

    /*
     * O SUBSET DO RESTAURANTE NAO ESTA AQUI -- e nao e por esquecimento.
     *
     * Mapeado como um POJO embutido, com ou sem @Embeddable, a extensao explode
     * em TEMPO DE EXECUCAO, na primeira consulta:
     *
     *   java.lang.UnsupportedOperationException: The type class
     *   br.com.unipds.jfood.catalogo.domain.RestauranteResumo is not supported yet
     *
     * "not supported YET" -- a propria mensagem admite que e limitacao da
     * implementacao, e nao da especificacao.
     *
     * O contorno esta em BuscaService.cardComSubset: o driver nativo do MongoDB,
     * para os campos que a abstracao nao alcanca. Ver docs/aula08.md.
     */

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getSecao() { return secao; }
    public void setSecao(String secao) { this.secao = secao; }

    /**
     * getDisponivel(), e nao isDisponivel().
     *
     * O PRIMEIRO PONTO EM QUE A ABSTRACAO VAZOU. O mapping-lite-processor do
     * JNoSQL exige o prefixo `get` inclusive para boolean, e quebra o build com
     *
     *   org.eclipse.jnosql.lite.mapping.ValidationException: There is not valid
     *   getter method to the field: disponivel in the class: ItemCardapio
     *
     * A convencao JavaBeans -- e a que a JPA, o Jackson e o proprio Java usam --
     * manda `is` para boolean. Aqui ela nao vale, e o erro nao diz qual e a
     * regra: diz apenas que nao existe getter valido, para um getter que esta
     * ali, publico, com o nome que o resto do ecossistema espera.
     */
    public boolean getDisponivel() { return disponivel; }
    public void setDisponivel(boolean disponivel) { this.disponivel = disponivel; }

}
