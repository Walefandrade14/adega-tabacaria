package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.Pedido;
import br.com.loudfeliz.dominio.StatusPedido;
import br.com.loudfeliz.dominio.TipoEntrega;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        StatusPedido status,
        TipoEntrega tipoEntrega,
        Long clienteId,
        String clienteNome,
        String funcionarioResponsavel,
        EnderecoResponse enderecoEntrega,
        List<ItemPedidoResponse> itens,
        BigDecimal subtotal,
        BigDecimal descontoTotal,
        BigDecimal taxaEntrega,
        BigDecimal total,
        List<PagamentoResponse> pagamentos,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public static PedidoResponse de(Pedido p) {
        return new PedidoResponse(
                p.getId(),
                p.getStatus(),
                p.getTipoEntrega(),
                p.getCliente() == null ? null : p.getCliente().getId(),
                p.getCliente() == null ? null : p.getCliente().getNome(),
                p.getFuncionario() == null ? null : p.getFuncionario().getNome(),
                EnderecoResponse.de(p.getEnderecoEntrega()),
                p.getItens().stream().map(ItemPedidoResponse::de).toList(),
                p.getSubtotal(),
                p.getDescontoTotal(),
                p.getTaxaEntrega(),
                p.getTotal(),
                p.getPagamentos().stream().map(PagamentoResponse::de).toList(),
                p.getCriadoEm(),
                p.getAtualizadoEm());
    }
}
