package br.com.unipds.jfood.catalogo.repository;

import br.com.unipds.jfood.catalogo.domain.ItemCardapio;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import java.util.List;

/**
 * O repositorio da Jakarta Data -- a MESMA especificacao da Etapa 3.
 *
 * E esse e o argumento da Jakarta NoSQL: o padrao Repository nao muda quando o
 * banco muda. O que muda e a implementacao por baixo -- la o hibernate-processor
 * sobre JDBC, aqui o JNoSQL sobre o driver do MongoDB.
 */
@Repository
public interface ItemCardapioRepository extends BasicRepository<ItemCardapio, String> {

    @Find
    List<ItemCardapio> porSecao(String secao);

    @Find
    List<ItemCardapio> porDisponivel(boolean disponivel);
}
