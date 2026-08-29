package br.com.unipds.jfood.administrativo.web;

import br.com.unipds.jfood.administrativo.domain.StatusPedido;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PedidoResumoResponse(
        Long id,
        String restaurante,
        String entregador,
        StatusPedido status,
        OffsetDateTime dataPedido,
        BigDecimal valorTotal
) {}
