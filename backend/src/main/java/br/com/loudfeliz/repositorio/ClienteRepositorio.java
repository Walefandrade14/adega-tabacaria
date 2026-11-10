package br.com.loudfeliz.repositorio;

import br.com.loudfeliz.dominio.Cliente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByEmailIgnoreCase(String email);

    Optional<Cliente> findByCpf(String cpf);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCpf(String cpf);
}
