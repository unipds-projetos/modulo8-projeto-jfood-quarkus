package br.com.unipds.jfood.administrativo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * A tabela que a 3FN criou: sem ela, a categoria seria uma string repetida em
 * cada restaurante -- e renomear "Italiana" seria um UPDATE em N linhas.
 */
@Entity
public class CategoriaRestaurante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;                          // SERIAL -> Integer

    @Column(nullable = false, unique = true, length = 50)
    private String nome;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
