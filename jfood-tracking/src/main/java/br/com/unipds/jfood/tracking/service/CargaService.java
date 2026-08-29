package br.com.unipds.jfood.tracking.service;

import br.com.unipds.jfood.tracking.domain.Ping;
import br.com.unipds.jfood.tracking.repository.TrackingRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.jboss.logging.Logger;

/**
 * O MESMO harness da versao Spring, com os mesmos parametros -- e e essa
 * igualdade que torna a comparacao valida.
 *
 * Ele exercita o caminho do endpoint de ingestao: tres escritas por ping, pelo
 * driver, sem batch e sem async. Medir com batch daria um numero maior e
 * responderia outra pergunta.
 */
@ApplicationScoped
public class CargaService {

    private static final Logger log = Logger.getLogger(CargaService.class);

    @Inject
    TrackingRepository repository;

    public Map<String, Object> disparar(int pontos, int entregadores, int concorrencia)
            throws InterruptedException {

        AtomicLong gravados = new AtomicLong();
        AtomicLong falhas = new AtomicLong();

        ExecutorService pool = Executors.newFixedThreadPool(concorrencia);
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch chegada = new CountDownLatch(concorrencia);
        int porThread = pontos / concorrencia;

        for (int t = 0; t < concorrencia; t++) {
            final int indice = t;
            pool.submit(() -> {
                try {
                    largada.await();
                    for (int i = 0; i < porThread; i++) {
                        long entregadorId = 1000L + ((long) indice * porThread + i) % entregadores;
                        try {
                            repository.registrar(pingSintetico(entregadorId, i));
                            gravados.incrementAndGet();
                        } catch (RuntimeException e) {
                            falhas.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    chegada.countDown();
                }
            });
        }

        long inicio = System.nanoTime();
        largada.countDown();
        boolean terminou = chegada.await(10, TimeUnit.MINUTES);
        Duration duracao = Duration.ofNanos(System.nanoTime() - inicio);
        pool.shutdown();

        double pontosPorSegundo = gravados.get() / (duracao.toMillis() / 1000.0);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("pontosGravados", gravados.get());
        resultado.put("falhas", falhas.get());
        resultado.put("entregadoresSimulados", entregadores);
        resultado.put("concorrencia", concorrencia);
        resultado.put("duracaoSegundos", Math.round(duracao.toMillis() / 100.0) / 10.0);
        resultado.put("pontosPorSegundo", Math.round(pontosPorSegundo));
        resultado.put("escritasPorSegundo", Math.round(pontosPorSegundo * 3));
        resultado.put("terminouNoPrazo", terminou);

        log.infof("Teste de carga: %s", resultado);
        return resultado;
    }

    private Ping pingSintetico(long entregadorId, int sequencia) {
        ThreadLocalRandom aleatorio = ThreadLocalRandom.current();
        return new Ping(entregadorId, 9000L + entregadorId,
                -23.55 + aleatorio.nextDouble(-0.1, 0.1),
                -46.63 + aleatorio.nextDouble(-0.1, 0.1),
                aleatorio.nextDouble(0, 60),
                Instant.now().minusSeconds(5L * sequencia));
    }
}
