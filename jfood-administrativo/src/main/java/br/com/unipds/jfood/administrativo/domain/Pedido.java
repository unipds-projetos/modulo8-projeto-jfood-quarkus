package br.com.unipds.jfood.administrativo.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurante_id", nullable = false)
    private Restaurante restaurante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endereco_entrega_id", nullable = false)
    private EnderecoEntrega enderecoEntrega;

    // Nulo ate o despacho: e o unico nulo do esquema que significa
    // "ainda nao aconteceu", e nao falta de informacao.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entregador_id")
    private Entregador entregador;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StatusPedido status = StatusPedido.CRIADO;

    @Column(nullable = false)
    private OffsetDateTime dataPedido = OffsetDateTime.now();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(length = 255)
    private String observacao;

    @Column(precision = 10, scale = 2)
    private BigDecimal taxaEntrega;

    // ItemPedido e entidade fraca: nasce e morre com o pedido.
    @OneToMany(mappedBy = "pedido",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<ItemPedido> itens = new ArrayList<>();

    // NAO existe aqui um @OneToOne(mappedBy = "pedido") para Pagamento, e a
    // ausencia e deliberada. O lado inverso de um @OneToOne NAO consegue ser lazy:
    // para devolver null ou um proxy, o Hibernate precisa ir ao banco descobrir se
    // a linha existe -- e vai, uma vez por pedido carregado. Era 1/3 do N+1 medido
    // em docs/aula03.md. Quem precisa do pagamento usa o PagamentoRepository.

    /**
     * Atualiza OS DOIS LADOS do relacionamento. Adicionar so na lista nao grava
     * nada: quem tem a coluna de FK e o ItemPedido, e e ele que precisa saber
     * de quem e filho.
     */
    public void adicionarItem(ItemPedido item) {
        itens.add(item);
        item.setPedido(this);
    }

    public void removerItem(ItemPedido item) {
        itens.remove(item);
        item.setPedido(null);
    }

    /**
     * A decisao da Etapa 1: valor_total e coluna materializada, e quem a mantem
     * e a aplicacao, na mesma transacao em que os itens sao gravados.
     */
    public void recalcularValorTotal() {
        BigDecimal soma = itens.stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.valorTotal = soma.add(taxaEntrega == null ? BigDecimal.ZERO : taxaEntrega);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Restaurante getRestaurante() { return restaurante; }
    public void setRestaurante(Restaurante restaurante) { this.restaurante = restaurante; }

    public EnderecoEntrega getEnderecoEntrega() { return enderecoEntrega; }
    public void setEnderecoEntrega(EnderecoEntrega enderecoEntrega) { this.enderecoEntrega = enderecoEntrega; }

    public Entregador getEntregador() { return entregador; }
    public void setEntregador(Entregador entregador) { this.entregador = entregador; }

    public StatusPedido getStatus() { return status; }
    public void setStatus(StatusPedido status) { this.status = status; }

    public OffsetDateTime getDataPedido() { return dataPedido; }
    public void setDataPedido(OffsetDateTime dataPedido) { this.dataPedido = dataPedido; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public BigDecimal getTaxaEntrega() { return taxaEntrega; }
    public void setTaxaEntrega(BigDecimal taxaEntrega) { this.taxaEntrega = taxaEntrega; }

    public List<ItemPedido> getItens() { return itens; }

}
