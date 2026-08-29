package br.com.unipds.jfood.tracking.repository;

import br.com.unipds.jfood.tracking.domain.Ping;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Row;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TrackingRepository {

    /** 90 dias, o mesmo da versao Spring -- agora escrito no proprio CQL. */
    private static final int TTL_SEGUNDOS = (int) Duration.ofDays(90).toSeconds();

    @Inject
    CqlSession session;

    private PreparedStatement inserirRotaDia;
    private PreparedStatement inserirRotaEntrega;
    private PreparedStatement atualizarPosicao;
    private PreparedStatement lerRotaDia;
    private PreparedStatement lerRotaEntrega;
    private PreparedStatement lerPosicao;

    /**
     * Os statements sao preparados UMA VEZ, na subida.
     *
     * Preparar custa um round-trip ao coordenador, que devolve um id e o
     * metadata das colunas. Preparar a cada request desperdicaria esse
     * round-trip -- e o driver ainda emitiria um aviso de "re-preparing already
     * prepared query", que e o sintoma classico de quem prepara dentro do laco.
     *
     * Na versao Spring isso e feito pelo Spring Data, por baixo. Aqui e
     * explicito, e o @PostConstruct e o lugar certo: o bean e @ApplicationScoped,
     * entao roda uma vez por aplicacao.
     */
    @PostConstruct
    void prepararStatements() {
        inserirRotaDia = session.prepare("""
                INSERT INTO rota_por_entregador_dia
                       (entregador_id, dia, instante, latitude, longitude, velocidade_kmh, entrega_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                USING TTL """ + " " + TTL_SEGUNDOS);

        inserirRotaEntrega = session.prepare("""
                INSERT INTO rota_por_entrega
                       (entrega_id, instante, entregador_id, latitude, longitude, velocidade_kmh)
                VALUES (?, ?, ?, ?, ?, ?)
                USING TTL """ + " " + TTL_SEGUNDOS);

        // A posicao atual NAO leva TTL: ela e sobrescrita a cada ping.
        atualizarPosicao = session.prepare("""
                INSERT INTO posicao_atual_entregador
                       (entregador_id, instante, latitude, longitude, velocidade_kmh, entrega_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """);

        lerRotaDia = session.prepare(
                "SELECT * FROM rota_por_entregador_dia WHERE entregador_id = ? AND dia = ?");
        lerRotaEntrega = session.prepare(
                "SELECT * FROM rota_por_entrega WHERE entrega_id = ?");
        lerPosicao = session.prepare(
                "SELECT * FROM posicao_atual_entregador WHERE entregador_id = ?");
    }

    /** Um ping, tres escritas -- uma por pergunta, como na versao Spring. */
    public void registrar(Ping ping) {
        Instant instante = ping.instanteOuAgora();
        LocalDate dia = instante.atZone(ZoneOffset.UTC).toLocalDate();

        session.execute(inserirRotaDia.bind(ping.entregadorId(), dia, instante,
                ping.latitude(), ping.longitude(), ping.velocidadeKmh(), ping.entregaId()));
        session.execute(inserirRotaEntrega.bind(ping.entregaId(), instante,
                ping.entregadorId(), ping.latitude(), ping.longitude(), ping.velocidadeKmh()));
        session.execute(atualizarPosicao.bind(ping.entregadorId(), instante,
                ping.latitude(), ping.longitude(), ping.velocidadeKmh(), ping.entregaId()));
    }

    public Map<String, Object> posicaoAtual(Long entregadorId) {
        Row row = session.execute(lerPosicao.bind(entregadorId)).one();
        return row == null ? null : Map.of(
                "entregadorId", row.getLong("entregador_id"),
                "instante", String.valueOf(row.getInstant("instante")),
                "latitude", row.getDouble("latitude"),
                "longitude", row.getDouble("longitude"),
                "velocidadeKmh", row.getDouble("velocidade_kmh"));
    }

    public List<Map<String, Object>> rotaDoDia(Long entregadorId, LocalDate dia) {
        List<Map<String, Object>> pontos = new ArrayList<>();
        for (Row row : session.execute(lerRotaDia.bind(entregadorId, dia))) {
            pontos.add(Map.of("instante", String.valueOf(row.getInstant("instante")),
                              "latitude", row.getDouble("latitude"),
                              "longitude", row.getDouble("longitude"),
                              "velocidadeKmh", row.getDouble("velocidade_kmh")));
        }
        return pontos;
    }

    public List<Map<String, Object>> rotaDaEntrega(Long entregaId) {
        List<Map<String, Object>> pontos = new ArrayList<>();
        for (Row row : session.execute(lerRotaEntrega.bind(entregaId))) {
            pontos.add(Map.of("instante", String.valueOf(row.getInstant("instante")),
                              "latitude", row.getDouble("latitude"),
                              "longitude", row.getDouble("longitude")));
        }
        return pontos;
    }

    /** Usado pelo teste de carga: um bind por ping, sem montar objeto de resposta. */
    public BoundStatement bindRotaDia(Ping ping, LocalDate dia, Instant instante) {
        return inserirRotaDia.bind(ping.entregadorId(), dia, instante, ping.latitude(),
                ping.longitude(), ping.velocidadeKmh(), ping.entregaId());
    }
}
