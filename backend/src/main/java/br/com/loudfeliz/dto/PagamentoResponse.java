package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.FormaPagamento;
import br.com.loudfeliz.dominio.Pagamento;
import br.com.loudfeliz.dominio.StatusPagamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagamentoResponse(
        Long id,
        Long pedidoId,
        FormaPagamento forma,
        StatusPagamento status,
        BigDecimal valor,
        String transacaoExternaId,
        String pixCopiaECola,
        LocalDateTime criadoEm) {

    public static PagamentoResponse de(Pagamento p) {
        String pix = p.getForma() == FormaPagamento.PIX && p.getStatus() == StatusPagamento.PENDENTE
                ? "PIX-SIMULADO|LOUD-FELIZ|" + p.getTransacaoExternaId() + "|" + p.getValor().toPlainString()
                : null;
        return new PagamentoResponse(p.getId(), p.getPedido().getId(), p.getForma(), p.getStatus(), p.getValor(),
                p.getTransacaoExternaId(), pix, p.getCriadoEm());
    }
}
