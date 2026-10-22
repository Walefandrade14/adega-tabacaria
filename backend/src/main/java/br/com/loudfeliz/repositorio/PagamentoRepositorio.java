package br.com.loudfeliz.repositorio;

import br.com.loudfeliz.dominio.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoRepositorio extends JpaRepository<Pagamento, Long> {
}
