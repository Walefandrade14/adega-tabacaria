package br.com.loudfeliz.repositorio;

import br.com.loudfeliz.dominio.Funcionario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuncionarioRepositorio extends JpaRepository<Funcionario, Long> {

    Optional<Funcionario> findByUsuario(String usuario);

    boolean existsByUsuario(String usuario);

    boolean existsByCpf(String cpf);

    List<Funcionario> findAllByOrderByNomeAsc();
}
