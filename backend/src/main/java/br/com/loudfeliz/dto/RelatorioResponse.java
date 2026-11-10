package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.TipoEntrega;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RelatorioResponse(
        LocalDate de,
        LocalDate ate,
        long pedidos,
        BigDecimal faturamento,
        BigDecimal ticketMedio,
        long cancelados,
        List<Canal> canais,
        List<Dia> dias,
        List<ProdutoVendido> maisVendidos,
        int limiteEstoqueBaixo,
        List<EstoqueBaixo> estoqueBaixo) {

    public record Canal(TipoEntrega tipo, long pedidos, BigDecimal faturamento) {
    }

    public record Dia(LocalDate data, long pedidos, BigDecimal faturamento) {
    }

    public record ProdutoVendido(Long produtoId, String nome, long quantidade, BigDecimal faturamento) {
    }

    public record EstoqueBaixo(Long produtoId, String nome, int estoque) {
    }
}
