package br.com.loudfeliz.dto;

import java.math.BigDecimal;

public record ItemCalculadoResponse(
        Long produtoId,
        String nome,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal desconto,
        String combo,
        BigDecimal total) {
}
