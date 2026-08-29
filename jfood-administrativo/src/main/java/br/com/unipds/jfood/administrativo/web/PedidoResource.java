package br.com.unipds.jfood.administrativo.web;

import br.com.unipds.jfood.administrativo.domain.Pedido;
import br.com.unipds.jfood.administrativo.domain.StatusPedido;
import br.com.unipds.jfood.administrativo.repository.PedidoRepository;
import br.com.unipds.jfood.administrativo.repository.projection.ResumoPedido;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
public class PedidoResource {

    private final PedidoRepository pedidoRepository;

    public PedidoResource(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @GET
    @Path("/pedidos")
    @Transactional
    public List<PedidoResumoResponse> porStatus(@QueryParam("status") StatusPedido status) {
        return pedidoRepository.porStatus(status).stream().map(this::paraResumo).toList();
    }

    @GET
    @Path("/clientes/{clienteId}/pedidos")
    @Transactional
    public List<PedidoResumoResponse> historico(@PathParam("clienteId") Long clienteId) {
        return pedidoRepository.historicoDoCliente(clienteId).stream().map(this::paraResumo).toList();
    }

    /** A projecao por record: tres colunas, sem instanciar a entidade. */
    @GET
    @Path("/clientes/{clienteId}/pedidos/resumo")
    @Transactional
    public List<ResumoPedido> resumo(@PathParam("clienteId") Long clienteId) {
        return pedidoRepository.resumoDoCliente(clienteId);
    }

    /** Com left join fetch nos itens. Sem ele, aqui nao ha lazy loading que salve. */
    @GET
    @Path("/pedidos/{id}/itens")
    @Transactional
    public Response itens(@PathParam("id") Long id) {
        return pedidoRepository.comItens(id)
                .map(p -> Response.ok(Map.of(
                        "pedido", p.getId(),
                        "restaurante", p.getRestaurante().getNome(),
                        "itens", p.getItens().stream()
                                .map(i -> Map.of("item", i.getItemCardapio().getNome(),
                                                 "quantidade", i.getQuantidade(),
                                                 "precoUnitario", i.getPrecoUnitario()))
                                .toList())).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * O MESMO pedido, sem o fetch da colecao. O que acontece ao ler getItens()
     * esta registrado em docs/aula03.md.
     */
    @GET
    @Path("/pedidos/{id}/itens-sem-fetch")
    @Transactional
    public Response itensSemFetch(@PathParam("id") Long id) {
        Pedido pedido = pedidoRepository.semFetch(id).orElseThrow();
        try {
            int quantos = pedido.getItens().size();
            return Response.ok(Map.of("pedido", pedido.getId(),
                                      "itens", quantos,
                                      "resultado", "a colecao carregou")).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("pedido", id,
                                   "resultado", "FALHOU ao acessar a colecao",
                                   "excecao", e.getClass().getName(),
                                   "mensagem", String.valueOf(e.getMessage()))).build();
        }
    }

    private PedidoResumoResponse paraResumo(Pedido pedido) {
        return new PedidoResumoResponse(
                pedido.getId(),
                pedido.getRestaurante().getNome(),
                pedido.getEntregador() == null ? null : pedido.getEntregador().getNome(),
                pedido.getStatus(),
                pedido.getDataPedido(),
                pedido.getValorTotal());
    }
}
