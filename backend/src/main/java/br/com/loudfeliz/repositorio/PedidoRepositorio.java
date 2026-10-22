package br.com.loudfeliz.repositorio;

import br.com.loudfeliz.dominio.Pedido;
import br.com.loudfeliz.dominio.StatusPedido;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepositorio extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteIdOrderByIdDesc(Long clienteId);

    Optional<Pedido> findByIdAndClienteId(Long id, Long clienteId);

    List<Pedido> findByStatusOrderByIdDesc(StatusPedido status);

    List<Pedido> findAllByOrderByIdDesc();
}
