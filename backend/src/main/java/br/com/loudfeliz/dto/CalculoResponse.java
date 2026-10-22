package br.com.loudfeliz.dto;

import java.math.BigDecimal;
import java.util.List;

public record CalculoResponse(
        List<ItemCalculadoResponse> itens,
        BigDecimal subtotal,
        BigDecimal descontoTotal,
        BigDecimal taxaEntrega,
        BigDecimal total,
        boolean exigeConferenciaDocumento,
        boolean podeFinalizar,
        List<ProblemaResponse> problemas) {
}
