package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.Combo;
import br.com.loudfeliz.dominio.Produto;
import br.com.loudfeliz.dto.CalculoResponse;
import br.com.loudfeliz.dto.ItemCalculadoResponse;
import br.com.loudfeliz.dto.ProblemaResponse;
import br.com.loudfeliz.excecao.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Calculo {

    public record Linha(Produto produto, int quantidade, BigDecimal precoUnitario, BigDecimal desconto, Combo combo) {

        public BigDecimal total() {
            return precoUnitario.multiply(BigDecimal.valueOf(quantidade)).subtract(desconto);
        }
    }

    private final List<Linha> linhas = new ArrayList<>();
    private final List<ProblemaResponse> problemas = new ArrayList<>();
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal descontoTotal = BigDecimal.ZERO;
    private BigDecimal taxaEntrega = BigDecimal.ZERO;
    private boolean exigeConferenciaDocumento;

    void adicionarLinha(Linha linha) {
        linhas.add(linha);
    }

    void adicionarProblema(String regra, String mensagem) {
        problemas.add(new ProblemaResponse(regra, mensagem));
    }

    void definirTotais(BigDecimal subtotal, BigDecimal descontoTotal, BigDecimal taxaEntrega) {
        this.subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        this.descontoTotal = descontoTotal.setScale(2, RoundingMode.HALF_UP);
        this.taxaEntrega = taxaEntrega.setScale(2, RoundingMode.HALF_UP);
    }

    void exigirConferenciaDocumento() {
        this.exigeConferenciaDocumento = true;
    }

    public void exigirSemProblemas() {
        if (!problemas.isEmpty()) {
            ProblemaResponse primeiro = problemas.get(0);
            throw new RegraNegocioException(primeiro.regra(), primeiro.mensagem());
        }
    }

    public List<Linha> getLinhas() {
        return linhas;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDescontoTotal() {
        return descontoTotal;
    }

    public BigDecimal getTaxaEntrega() {
        return taxaEntrega;
    }

    public BigDecimal getTotal() {
        return subtotal.subtract(descontoTotal).add(taxaEntrega);
    }

    public CalculoResponse paraResposta() {
        List<ItemCalculadoResponse> itens = linhas.stream()
                .map(l -> new ItemCalculadoResponse(l.produto().getId(), l.produto().getNome(), l.quantidade(), l.precoUnitario(),
                        l.desconto(), l.combo() == null ? null : l.combo().getNome(), l.total()))
                .toList();
        return new CalculoResponse(itens, subtotal, descontoTotal, taxaEntrega, getTotal(), exigeConferenciaDocumento,
                problemas.isEmpty(), List.copyOf(problemas));
    }
}
