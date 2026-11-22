package br.com.loudfeliz.repositorio;

import br.com.loudfeliz.dominio.TentativaLogin;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TentativaLoginRepositorio extends JpaRepository<TentativaLogin, String> {

    @Transactional
    @Modifying
    @Query("delete from TentativaLogin t where t.inicio < :limite and (t.bloqueadoAte is null or t.bloqueadoAte < :agora)")
    int apagarVencidas(@Param("limite") Instant limite, @Param("agora") Instant agora);
}
