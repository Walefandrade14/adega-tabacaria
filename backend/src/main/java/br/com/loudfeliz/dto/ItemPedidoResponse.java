package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.ItemPedido;
import java.math.BigDecimal;

public record ItemPedidoResponse(
        Long produtoId,
        String produtoNome,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal descontoAplicado,
        String combo) {

    public static ItemPedidoResponse de(ItemPedido i) {
        return new ItemPedidoResponse(i.getProduto().getId(), i.getProduto().getNome(), i.getQuantidade(),
                i.getPrecoUnitario(), i.getDescontoAplicado(), i.getCombo() == null ? null : i.getCombo().getNome());
    }
}
