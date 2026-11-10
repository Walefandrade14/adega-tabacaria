package br.com.loudfeliz.repositorio;

import br.com.loudfeliz.dominio.CategoriaProduto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepositorio extends JpaRepository<CategoriaProduto, Long> {

    List<CategoriaProduto> findAllByOrderByNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);
}
