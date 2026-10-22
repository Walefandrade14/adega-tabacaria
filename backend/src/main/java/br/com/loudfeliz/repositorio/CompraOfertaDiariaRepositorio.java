package br.com.loudfeliz.repositorio;

import br.com.loudfeliz.dominio.CompraOfertaDiaria;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraOfertaDiariaRepositorio extends JpaRepository<CompraOfertaDiaria, Long> {

    Optional<CompraOfertaDiaria> findByClienteIdAndProdutoIdAndDataReferencia(Long clienteId, Long produtoId, LocalDate dataReferencia);
}
