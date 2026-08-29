package br.com.unipds.jfood.administrativo.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import java.util.ArrayList;
import java.util.List;

/**
 * A PK de cliente e a PK do usuario -- e isso que impede um usuario de virar
 * dois clientes. Em JPA, e o @PrimaryKeyJoinColumn que diz o nome dessa coluna.
 */
@Entity
@PrimaryKeyJoinColumn(name = "usuario_id")
public class Cliente extends Usuario {

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @OneToMany(mappedBy = "cliente",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<EnderecoEntrega> enderecos = new ArrayList<>();

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public List<EnderecoEntrega> getEnderecos() { return enderecos; }

    public void adicionarEndereco(EnderecoEntrega endereco) {
        enderecos.add(endereco);
        endereco.setCliente(this);               // atualiza os dois lados
    }
}
