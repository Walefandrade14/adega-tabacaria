package br.com.loudfeliz.repositorio;

import br.com.loudfeliz.dominio.Pedido;
import br.com.loudfeliz.dominio.StatusPedido;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepositorio extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteIdOrderByIdDesc(Long clienteId);

    Optional<Pedido> findByIdAndClienteId(Long id, Long clienteId);

    Page<Pedido> findByStatusIn(Collection<StatusPedido> status, Pageable pagina);

    boolean existsByEnderecoEntregaId(Long enderecoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> travar(@Param("id") Long id);

    @Query("select p.pedido.id from Pagamento p where p.id = :pagamentoId")
    Optional<Long> idDoPedidoDoPagamento(@Param("pagamentoId") Long pagamentoId);

    @Query("select p.id from Pedido p where p.status in :status and p.cliente is not null and p.criadoEm < :limite order by p.id")
    List<Long> idsCriadosAntesDe(@Param("status") Collection<StatusPedido> status, @Param("limite") LocalDateTime limite);
}
