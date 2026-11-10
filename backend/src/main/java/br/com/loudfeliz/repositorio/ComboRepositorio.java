package br.com.loudfeliz.repositorio;

import br.com.loudfeliz.dominio.Combo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComboRepositorio extends JpaRepository<Combo, Long> {

    @EntityGraph(attributePaths = {"itens", "itens.produto"})
    List<Combo> findByAtivoTrueOrderByIdAsc();

    @EntityGraph(attributePaths = {"itens", "itens.produto"})
    List<Combo> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"itens", "itens.produto"})
    Optional<Combo> findComItensById(Long id);
}
