package br.com.loudfeliz.repositorio;

import br.com.loudfeliz.dominio.Fornecedor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FornecedorRepositorio extends JpaRepository<Fornecedor, Long> {

    List<Fornecedor> findAllByOrderByRazaoSocialAsc();
}
