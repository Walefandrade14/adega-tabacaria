package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.TaxaEntregaRegiao;
import java.math.BigDecimal;

public record TaxaEntregaResponse(Long id, String bairro, String cepPrefixo, BigDecimal valor) {

    public static TaxaEntregaResponse de(TaxaEntregaRegiao t) {
        return new TaxaEntregaResponse(t.getId(), t.getBairro(), t.getCepPrefixo(), t.getValor());
    }
}
