package br.com.unipds.jfood.catalogo.web;

import br.com.unipds.jfood.catalogo.domain.ItemCardapio;
import br.com.unipds.jfood.catalogo.service.BuscaService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Path("/api/v1/catalogo")
@Produces(MediaType.APPLICATION_JSON)
public class BuscaResource {

    private final BuscaService buscaService;

    public BuscaResource(BuscaService buscaService) {
        this.buscaService = buscaService;
    }

    @GET
    @Path("/itens/busca")
    public List<ItemCardapio> busca(@QueryParam("termo") String termo) {
        return buscaService.porNome(termo);
    }

    /** A mesma busca, insensivel a maiusculas, via MongoDBTemplate. */
    @GET
    @Path("/itens/busca-ignore-case")
    public List<ItemCardapio> buscaIgnorandoCaixa(@QueryParam("termo") String termo) {
        return buscaService.porNomeIgnorandoCaixa(termo);
    }

    /** O card completo: item + subset. Passa pelo driver nativo. */
    @GET
    @Path("/itens/busca-card")
    public List<Map<String, Object>> buscaCard(@QueryParam("termo") String termo) {
        return buscaService.cardComSubset(termo);
    }

    @GET
    @Path("/itens/por-tag")
    public List<ItemCardapio> porTag(@QueryParam("tag") String tag) {
        return buscaService.porTag(tag);
    }

    @GET
    @Path("/restaurantes/{id}/itens")
    public List<ItemCardapio> porRestaurante(@PathParam("id") String id) {
        return buscaService.porRestaurante(id);
    }
}
