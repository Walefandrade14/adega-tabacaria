package br.com.loudfeliz.repositorio;

import br.com.loudfeliz.dominio.Configuracao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoRepositorio extends JpaRepository<Configuracao, String> {

    List<Configuracao> findAllByOrderByChaveAsc();
}
