package br.com.loudfeliz.dominio.conversor;

import br.com.loudfeliz.dominio.StatusPagamento;
import jakarta.persistence.Converter;

@Converter
public class StatusPagamentoConverter extends EnumMinusculoConverter<StatusPagamento> {

    public StatusPagamentoConverter() {
        super(StatusPagamento.class);
    }
}
