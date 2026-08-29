package br.com.unipds.jfood.recomendacoes.web;

import br.com.unipds.jfood.recomendacoes.repository.ContaVinculada;
import br.com.unipds.jfood.recomendacoes.repository.RecomendacaoRepository;
import br.com.unipds.jfood.recomendacoes.repository.RecomendacaoRestaurante;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("/api/v1/recomendacoes")
@Produces(MediaType.APPLICATION_JSON)
public class RecomendacaoResource {

    private final RecomendacaoRepository repository;

    public RecomendacaoResource(RecomendacaoRepository repository) {
        this.repository = repository;
    }

    @GET
    @Path("/clientes/{clienteId}")
    public Response recomendar(@PathParam("clienteId") Long clienteId,
                               @QueryParam("notaMinima") @DefaultValue("4") int notaMinima,
                               @QueryParam("limite") @DefaultValue("5") int limite) {
        List<RecomendacaoRestaurante> r = repository.porVizinhanca(clienteId, notaMinima, limite);
        return r.isEmpty() ? Response.noContent().build() : Response.ok(r).build();
    }

    @GET
    @Path("/clientes/{clienteId}/ponderado")
    public Response ponderado(@PathParam("clienteId") Long clienteId,
                              @QueryParam("notaMinima") @DefaultValue("4") int notaMinima,
                              @QueryParam("limite") @DefaultValue("5") int limite) {
        List<RecomendacaoRestaurante> r = repository.ponderado(clienteId, notaMinima, limite);
        return r.isEmpty() ? Response.noContent().build() : Response.ok(r).build();
    }

    @GET
    @Path("/fraude/contas-vinculadas")
    public List<ContaVinculada> contasVinculadas() {
        return repository.contasVinculadas();
    }

    /** A consulta de profundidade variavel -- onde o grafo ganha de verdade. */
    @GET
    @Path("/caminho")
    public Map<String, Object> caminho(@QueryParam("de") Long de,
                                       @QueryParam("para") Long para,
                                       @QueryParam("maxSaltos") @DefaultValue("6") int maxSaltos) {
        return repository.caminhoMaisCurto(de, para, maxSaltos);
    }
}
