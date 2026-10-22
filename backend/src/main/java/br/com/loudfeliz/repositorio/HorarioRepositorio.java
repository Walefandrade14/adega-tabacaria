package br.com.loudfeliz.repositorio;

import br.com.loudfeliz.dominio.HorarioFuncionamento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioRepositorio extends JpaRepository<HorarioFuncionamento, Long> {

    List<HorarioFuncionamento> findAllByOrderByDiaSemanaAsc();

    Optional<HorarioFuncionamento> findByDiaSemana(Integer diaSemana);
}
