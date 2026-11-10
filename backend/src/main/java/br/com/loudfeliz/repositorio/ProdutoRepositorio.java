package br.com.loudfeliz.repositorio;

import br.com.loudfeliz.dominio.Produto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProdutoRepositorio extends JpaRepository<Produto, Long> {

    @EntityGraph(attributePaths = "categoria")
    List<Produto> findByAtivoTrueOrderByNomeAsc();

    @EntityGraph(attributePaths = "categoria")
    List<Produto> findAllByOrderByNomeAsc();

    @EntityGraph(attributePaths = "categoria")
    Optional<Produto> findComCategoriaById(Long id);

    @Modifying(flushAutomatically = true)
    @Query("update Produto p set p.estoqueQuantidade = p.estoqueQuantidade - :quantidade where p.id = :id and p.estoqueQuantidade >= :quantidade")
    int baixarEstoque(@Param("id") Long id, @Param("quantidade") int quantidade);

    @Modifying(flushAutomatically = true)
    @Query("update Produto p set p.estoqueQuantidade = p.estoqueQuantidade + :quantidade where p.id = :id")
    int devolverEstoque(@Param("id") Long id, @Param("quantidade") int quantidade);
}
