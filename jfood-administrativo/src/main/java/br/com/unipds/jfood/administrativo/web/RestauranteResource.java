package br.com.unipds.jfood.administrativo.web;

import br.com.unipds.jfood.administrativo.domain.Restaurante;
import br.com.unipds.jfood.administrativo.service.RestauranteService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/** JAX-RS, e nao Spring MVC: @Path no lugar de @RequestMapping. */
@Path("/api/v1/restaurantes")
@Produces(MediaType.APPLICATION_JSON)
public class RestauranteResource {

    private final RestauranteService restauranteService;

    public RestauranteResource(RestauranteService restauranteService) {
        this.restauranteService = restauranteService;
    }

    @GET
    @Path("/{id}")
    public Response porId(@PathParam("id") Long id) {
        return restauranteService.porId(id)
                .map(r -> Response.ok(new RestauranteResponse(r.getId(), r.getNome(), r.getCep())).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    /** O experimento do dirty checking: muda o nome e NAO salva. */
    @PUT
    @Path("/{id}/renomear-sem-salvar")
    public Map<String, String> renomearSemSalvar(@PathParam("id") Long id,
                                                 @QueryParam("nome") String nome) {
        String antigo = restauranteService.renomearSemSalvar(id, nome);
        return Map.of("nomeAntes", antigo,
                      "nomeEmMemoria", nome,
                      "aviso", "confira no banco: nada mudou");
    }

    /** O mesmo, agora com o save() explicito. */
    @PUT
    @Path("/{id}/renomear")
    public Map<String, String> renomear(@PathParam("id") Long id, @QueryParam("nome") String nome) {
        String antigo = restauranteService.renomear(id, nome);
        return Map.of("nomeAntes", antigo, "nomeAgora", nome);
    }

    public record RestauranteResponse(Long id, String nome, String cep) {}
}
