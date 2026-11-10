package br.com.loudfeliz.repositorio;

import br.com.loudfeliz.dominio.Endereco;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnderecoRepositorio extends JpaRepository<Endereco, Long> {

    List<Endereco> findByClienteIdAndArquivadoFalseOrderByPrincipalDescIdAsc(Long clienteId);

    Optional<Endereco> findByIdAndClienteIdAndArquivadoFalse(Long id, Long clienteId);
}
