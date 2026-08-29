package br.com.unipds.jfood.tracking.domain;

import java.time.Instant;

/**
 * POJO PURO -- no "modo hard", sem @Table, sem @PrimaryKeyClass, sem @Column.
 *
 * Na versao Spring, a chave composta virou uma classe propria anotada com
 * @PrimaryKeyClass e @PrimaryKeyColumn(PARTITIONED/CLUSTERED). Aqui a chave nao
 * existe no Java: ela vive no CQL, que e onde ela sempre viveu.
 *
 * O que se perde: o mapeamento deixa de documentar a modelagem fisica. Quem ler
 * este record nao descobre que (entregador_id, dia) e a particao.
 * O que se ganha: nada entre o objeto e o statement.
 */
public record Ping(
        Long entregadorId,
        Long entregaId,
        Double latitude,
        Double longitude,
        Double velocidadeKmh,
        Instant instante
) {

    public Instant instanteOuAgora() {
        return instante == null ? Instant.now() : instante;
    }
}
