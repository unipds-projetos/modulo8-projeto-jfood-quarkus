package br.com.unipds.jfood.administrativo.service;

import br.com.unipds.jfood.administrativo.domain.Restaurante;
import br.com.unipds.jfood.administrativo.repository.RestauranteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.jboss.logging.Logger;

/**
 * A ARMADILHA DA AULA: aqui nao existe sessao persistente.
 *
 * Na Parte 1, o dirty checking era a demonstracao favorita: carregue a entidade
 * dentro de uma transacao, mude um campo com o setter, nao chame save() -- e o
 * Hibernate emite o UPDATE no commit.
 *
 * Repita isso aqui e o UPDATE NAO SAI.
 *
 * O motivo e arquitetural: o repositorio gerado pelo hibernate-processor e
 * apoiado por uma StatelessSession, e nao pela Session com contexto de
 * persistencia da Parte 1.
 *
 *   Session (JPA / Spring Data)          StatelessSession (Jakarta Data)
 *   ---------------------------          -------------------------------
 *   contexto de persistencia: sim        nao
 *   dirty checking: sim                  NAO
 *   lazy loading depois da consulta: sim NAO
 *   cache de primeiro nivel: sim         nao
 *   consequencia: conveniencia,          previsibilidade:
 *     com N+1 escondido                    uma chamada, uma query
 *
 * Isso nao e bug: e coerente com o resto da aula. Contexto de persistencia custa
 * memoria e esconde queries.
 */
@ApplicationScoped
public class RestauranteService {

    private static final Logger log = Logger.getLogger(RestauranteService.class);

    private final RestauranteRepository restauranteRepository;

    public RestauranteService(RestauranteRepository restauranteRepository) {
        this.restauranteRepository = restauranteRepository;
    }

    public Optional<Restaurante> porId(Long id) {
        return restauranteRepository.porId(id);
    }

    /**
     * ERRADO -- deixado no codigo de proposito.
     *
     * O log mostra o nome novo. O banco, nao. Nenhum UPDATE e emitido, e nenhum
     * erro e lancado: a entidade simplesmente nao esta sendo observada por
     * ninguem.
     */
    @Transactional
    public String renomearSemSalvar(Long id, String novoNome) {
        Restaurante restaurante = restauranteRepository.porId(id).orElseThrow();
        String nomeAntigo = restaurante.getNome();

        restaurante.setNome(novoNome);

        log.infof("Em memoria: '%s' virou '%s' -- e nenhum UPDATE vai sair daqui",
                nomeAntigo, restaurante.getNome());
        return nomeAntigo;
    }

    /**
     * CERTO -- com a StatelessSession, gravar e uma chamada explicita.
     *
     * E a primeira das duas regras que mudam. A segunda esta no
     * PedidoRepository.comItens: sem lazy loading, o que faltou no fetch nao e
     * buscado depois.
     */
    @Transactional
    public String renomear(Long id, String novoNome) {
        Restaurante restaurante = restauranteRepository.porId(id).orElseThrow();
        String nomeAntigo = restaurante.getNome();

        restaurante.setNome(novoNome);
        restauranteRepository.save(restaurante);

        return nomeAntigo;
    }
}
