package br.com.loudfeliz.dominio.conversor;

import br.com.loudfeliz.dominio.StatusPedido;
import jakarta.persistence.Converter;

@Converter
public class StatusPedidoConverter extends EnumMinusculoConverter<StatusPedido> {

    public StatusPedidoConverter() {
        super(StatusPedido.class);
    }
}
