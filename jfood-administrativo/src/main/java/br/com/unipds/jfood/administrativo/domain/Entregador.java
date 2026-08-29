package br.com.unipds.jfood.administrativo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;

@Entity
@PrimaryKeyJoinColumn(name = "usuario_id")
public class Entregador extends Usuario {

    @Column(nullable = false, unique = true, length = 11)
    private String cnh;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoVeiculo tipoVeiculo;

    @Column(nullable = false)
    private boolean disponivel = true;

    public String getCnh() { return cnh; }
    public void setCnh(String cnh) { this.cnh = cnh; }

    public TipoVeiculo getTipoVeiculo() { return tipoVeiculo; }
    public void setTipoVeiculo(TipoVeiculo tipoVeiculo) { this.tipoVeiculo = tipoVeiculo; }

    public boolean isDisponivel() { return disponivel; }
    public void setDisponivel(boolean disponivel) { this.disponivel = disponivel; }
}
