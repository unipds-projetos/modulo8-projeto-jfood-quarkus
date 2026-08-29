package br.com.unipds.jfood.administrativo.repository;

import br.com.unipds.jfood.administrativo.domain.Pedido;
import br.com.unipds.jfood.administrativo.domain.StatusPedido;
import br.com.unipds.jfood.administrativo.repository.projection.ResumoPedido;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends CrudRepository<Pedido, Long> {

    /**
     * @Query: a String continua, a validacao muda de lugar.
     *
     * Continua sendo JPQL em texto -- mas ela e conferida contra o modelo em
     * tempo de COMPILACAO. Um atributo inexistente no WHERE quebra o build, e
     * nao a subida.
     */
    /**
     * DE PROPOSITO sem fetch da colecao de itens.
     *
     * O endpoint /pedidos/{id}/itens-sem-fetch usa este metodo e tenta ler
     * pedido.getItens(). O resultado esta medido em docs/aula03.md.
     */
    @Find
    Optional<Pedido> semFetch(Long id);

    @Query("where status = :status order by dataPedido desc")
    List<Pedido> porStatus(StatusPedido status);

    /**
     * LEFT JOIN FETCH -- e o `left` NAO e opcional.
     *
     * Duas razoes, e a segunda so existe aqui:
     *
     * 1. A mesma da Parte 1: pedido ainda nao despachado tem entregador_id nulo,
     *    e um inner join fetch faria esses pedidos SUMIREM do historico.
     *
     * 2. Aqui nao ha lazy loading. Na Parte 1, esquecer o fetch produzia N+1 --
     *    lento, mas funcionando. Com a StatelessSession, esquecer o fetch produz
     *    uma FALHA ao acessar a associacao: nao ha sessao aberta para ir buscar.
     *    O bug de performance vira bug de correcao.
     */
    @Query("""
           select p from Pedido p
             left join fetch p.itens i
             left join fetch i.itemCardapio
             left join fetch p.restaurante
             left join fetch p.entregador
            where p.id = :id
           """)
    Optional<Pedido> comItens(Long id);

    @Query("""
           select p from Pedido p
             join fetch p.restaurante
             left join fetch p.entregador
            where p.cliente.id = :clienteId
            order by p.dataPedido desc
           """)
    List<Pedido> historicoDoCliente(Long clienteId);

    /**
     * A projecao por record, via `select new` com o nome TOTALMENTE QUALIFICADO.
     *
     * O nome completo e obrigatorio: a JPQL nao tem import, e o Hibernate precisa
     * do caminho exato da classe para achar o construtor.
     */
    @Query("""
           select new br.com.unipds.jfood.administrativo.repository.projection.ResumoPedido(
                      p.id, r.nome, p.valorTotal)
             from Pedido p join p.restaurante r
            where p.cliente.id = :clienteId
            order by p.dataPedido desc
           """)
    List<ResumoPedido> resumoDoCliente(Long clienteId);
}
