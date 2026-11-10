package br.com.loudfeliz.repositorio;

import br.com.loudfeliz.dominio.TaxaEntregaRegiao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxaEntregaRepositorio extends JpaRepository<TaxaEntregaRegiao, Long> {

    List<TaxaEntregaRegiao> findAllByOrderByBairroAsc();
}
