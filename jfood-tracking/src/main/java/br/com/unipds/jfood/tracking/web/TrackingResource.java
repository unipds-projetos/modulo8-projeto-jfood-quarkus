package br.com.unipds.jfood.tracking.web;

import br.com.unipds.jfood.tracking.domain.Ping;
import br.com.unipds.jfood.tracking.repository.TrackingRepository;
import br.com.unipds.jfood.tracking.service.CargaService;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Os MESMOS endpoints da versao Spring, inclusive o 202. */
@Path("/api/v1/tracking")
@Produces(MediaType.APPLICATION_JSON)
public class TrackingResource {

    private final TrackingRepository repository;
    private final CargaService cargaService;

    public TrackingResource(TrackingRepository repository, CargaService cargaService) {
        this.repository = repository;
        this.cargaService = cargaService;
    }

    /** 202 Accepted: o app do entregador dispara e nao espera nada de volta. */
    @POST
    @Path("/pings")
    public Response registrar(Ping ping) {
        repository.registrar(ping);
        return Response.accepted().build();
    }

    @GET
    @Path("/entregadores/{entregadorId}/posicao")
    public Response posicao(@PathParam("entregadorId") Long entregadorId) {
        Map<String, Object> posicao = repository.posicaoAtual(entregadorId);
        return posicao == null ? Response.status(Response.Status.NOT_FOUND).build()
                               : Response.ok(posicao).build();
    }

    @GET
    @Path("/entregadores/{entregadorId}/rota")
    public List<Map<String, Object>> rotaDoDia(@PathParam("entregadorId") Long entregadorId,
                                               @QueryParam("dia") String dia) {
        return repository.rotaDoDia(entregadorId, LocalDate.parse(dia));
    }

    @GET
    @Path("/entregas/{entregaId}/rota")
    public List<Map<String, Object>> rotaDaEntrega(@PathParam("entregaId") Long entregaId) {
        return repository.rotaDaEntrega(entregaId);
    }

    @POST
    @Path("/experimentos/carga")
    public Map<String, Object> carga(@QueryParam("pontos") @DefaultValue("100000") int pontos,
                                     @QueryParam("entregadores") @DefaultValue("200") int entregadores,
                                     @QueryParam("concorrencia") @DefaultValue("128") int concorrencia)
            throws InterruptedException {
        return cargaService.disparar(pontos, entregadores, concorrencia);
    }
}
