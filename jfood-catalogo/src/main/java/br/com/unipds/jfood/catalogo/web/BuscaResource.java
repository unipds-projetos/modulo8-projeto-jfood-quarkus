package br.com.unipds.jfood.catalogo.web;

import br.com.unipds.jfood.catalogo.domain.ItemCardapio;
import br.com.unipds.jfood.catalogo.domain.ResultadoBusca;
import br.com.unipds.jfood.catalogo.service.BuscaService;
import br.com.unipds.jfood.catalogo.service.CardapioCacheService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
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
    private final CardapioCacheService cacheService;

    public BuscaResource(BuscaService buscaService, CardapioCacheService cacheService) {
        this.buscaService = buscaService;
        this.cacheService = cacheService;
    }

    /** A mesma busca, agora com @CacheResult. */
    @GET
    @Path("/itens/busca-cacheada")
    public ResultadoBusca buscaCacheada(@QueryParam("termo") String termo) {
        return cacheService.busca(termo);
    }

    @GET
    @Path("/itens/{id}")
    public ItemCardapio detalhe(@PathParam("id") String id) {
        return cacheService.detalhe(id);
    }

    /** Invalida o detalhe daquele item E todas as buscas. */
    @PUT
    @Path("/itens/{id}/preco")
    public ItemCardapio atualizarPreco(@PathParam("id") String id,
                                       @QueryParam("valor") java.math.BigDecimal valor) {
        return cacheService.atualizarPreco(id, valor);
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
