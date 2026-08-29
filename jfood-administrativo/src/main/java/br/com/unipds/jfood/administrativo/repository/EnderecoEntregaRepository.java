package br.com.unipds.jfood.administrativo.repository;

import br.com.unipds.jfood.administrativo.domain.EnderecoEntrega;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

/**
 * O repositorio da especificacao -- nao do framework.
 *
 * Tres diferencas em relacao ao Spring Data JPA da Parte 1:
 *
 * 1. @Repository e CrudRepository vem de jakarta.data.repository. Sao da
 *    ESPECIFICACAO; o Hibernate e apenas quem a implementa.
 * 2. Nao existe proxy em runtime. O hibernate-processor le esta interface durante
 *    a COMPILACAO e gera a classe concreta em bytecode ali mesmo.
 * 3. O nome do metodo deixa de ser contrato.
 */
@Repository
public interface EnderecoEntregaRepository extends CrudRepository<EnderecoEntrega, Long> {

    /**
     * O que vale aqui sao os PARAMETROS, e nao o nome do metodo.
     *
     * O processador olha para `cep`, confirma que EnderecoEntrega tem esse
     * atributo e gera a query. Este metodo poderia se chamar `buscaPeloCep` que
     * funcionaria igual -- e e por isso que o <parameters>true</parameters> do
     * pom.xml nao e opcional: sem ele o compilador descarta os nomes (arg0) e o
     * @Find nao tem como fazer a correspondencia.
     *
     * E o typo? Trocar `cep` por `ceep` QUEBRA O BUILD, com o compilador dizendo
     * que EnderecoEntrega nao tem esse atributo. O contraste com o
     * findByStattus da Parte 1 -- que compilava e so quebrava na subida -- esta
     * medido em docs/aula03.md.
     */
    @Find
    List<EnderecoEntrega> porCep(String cep);

    /**
     * O @Find casa por NOME e por TIPO do parametro.
     *
     * A primeira versao deste metodo recebia `Long cliente` -- e o build quebrou:
     *
     *   matching field has type 'br.com.unipds.jfood.administrativo.domain.Cliente'
     *   in entity class 'br.com.unipds.jfood.administrativo.domain.EnderecoEntrega'
     *
     * O atributo `cliente` da entidade e um Cliente, nao um Long. O processador
     * conferiu o tipo, achou a divergencia e apontou o arquivo e a linha, na
     * compilacao. Nenhum framework de runtime tem essa informacao a tempo.
     *
     * Para filtrar pelo ID da associacao, o caminho e a @Query -- veja abaixo.
     */
    @Find
    List<EnderecoEntrega> porApelido(String apelido);

    @Query("where cliente.id = :clienteId and apelido = :apelido")
    List<EnderecoEntrega> porClienteEApelido(Long clienteId, String apelido);
}
