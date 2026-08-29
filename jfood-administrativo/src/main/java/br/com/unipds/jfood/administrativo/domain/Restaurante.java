package br.com.unipds.jfood.administrativo.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Restaurante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaRestaurante categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dono_id", nullable = false)
    private DonoRestaurante dono;

    @Column(nullable = false, length = 8)
    private String cep;

    @Column(nullable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @OneToMany(mappedBy = "restaurante",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<ItemCardapio> cardapio = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public CategoriaRestaurante getCategoria() { return categoria; }
    public void setCategoria(CategoriaRestaurante categoria) { this.categoria = categoria; }

    public DonoRestaurante getDono() { return dono; }
    public void setDono(DonoRestaurante dono) { this.dono = dono; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }

    public List<ItemCardapio> getCardapio() { return cardapio; }

    public void adicionarItemCardapio(ItemCardapio item) {
        cardapio.add(item);
        item.setRestaurante(this);               // atualiza os dois lados
    }
}
