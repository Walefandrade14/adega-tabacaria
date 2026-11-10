package br.com.loudfeliz.dominio.conversor;

import br.com.loudfeliz.dominio.FormaPagamento;
import jakarta.persistence.Converter;

@Converter
public class FormaPagamentoConverter extends EnumMinusculoConverter<FormaPagamento> {

    public FormaPagamentoConverter() {
        super(FormaPagamento.class);
    }
}
