package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.Combo;
import java.math.BigDecimal;
import java.util.List;

public record ComboResponse(
        Long id,
        String nome,
        BigDecimal descontoPercentual,
        BigDecimal descontoValorFixo,
        boolean ativo,
        List<ComboItemResponse> itens) {

    public static ComboResponse de(Combo c) {
        List<ComboItemResponse> itens = c.getItens().stream()
                .map(i -> new ComboItemResponse(i.getProduto().getId(), i.getProduto().getNome(), i.getQuantidade()))
                .toList();
        return new ComboResponse(c.getId(), c.getNome(), c.getDescontoPercentual(), c.getDescontoValorFixo(), c.isAtivo(), itens);
    }
}
