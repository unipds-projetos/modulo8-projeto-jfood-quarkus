package br.com.unipds.jfood.administrativo.repository;

import br.com.unipds.jfood.administrativo.domain.Restaurante;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RestauranteRepository extends CrudRepository<Restaurante, Long> {

    @Find
    Optional<Restaurante> porId(Long id);

    @Find
    List<Restaurante> porNome(String nome);
}
